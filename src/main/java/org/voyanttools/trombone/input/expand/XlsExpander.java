/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.input.source.StringInputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public class XlsExpander implements Expander {
	
	/**
	 * all parameters sent, only some of which may be relevant to some expanders
	 */
	private FlexibleParameters parameters;
	
	/**
	 * the stored document storage strategy
	 */
	private StoredDocumentSourceStorage storedDocumentSourceStorage;

	/**
	 * @param storedDocumentSourceExpander 
	 * @param storedDocumentSourceStorage 
	 * 
	 */
	public XlsExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.expand.Expander#getExpandedStoredDocumentSources(org.voyanttools.trombone.model.StoredDocumentSource)
	 */
	@Override
	public List<StoredDocumentSource> getExpandedStoredDocumentSources(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		// first try to see if we've been here already
		String id = storedDocumentSource.getId();
		List<StoredDocumentSource> xlsStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);

		if (xlsStoredDocumentSources!=null && xlsStoredDocumentSources.isEmpty()==false) {
			return xlsStoredDocumentSources;
		}
		
		xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		// check to see if anything needs to be expanded
		String tableDocuments = parameters.getParameterValue("tableDocuments", "").toLowerCase();
		if (tableDocuments.isEmpty()==false) {
			if (tableDocuments.equals("rows")) {
				return getDocumentsRowCells(storedDocumentSource);
			}
			else if (tableDocuments.equals("columns")) {
				return getDocumentsColumns(storedDocumentSource);
			}
		}
		
		// otherwise, use the entire table
		xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		xlsStoredDocumentSources.add(storedDocumentSource);
		return xlsStoredDocumentSources;
	}
	
	private Workbook getWorkBook(StoredDocumentSource storedDocumentSource) throws IOException {
		InputStream inputStream = null;
		Workbook wb;
		try {
			inputStream = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
			wb = WorkbookFactory.create(inputStream);
		} catch (InvalidFormatException e) {
			throw new IOException(e);
		}
		finally {
			inputStream.close();
		}
		return wb;
		
	}
	private List<StoredDocumentSource> getDocumentsColumns(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata metadata = storedDocumentSource.getMetadata();
		String id = storedDocumentSource.getId();
		Workbook wb = getWorkBook(storedDocumentSource);
		List<StoredDocumentSource> xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		List<List<Integer>> columns = getInts("tableContent", true);
		StringBuffer docBuffer = new StringBuffer();
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		String title;
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			Sheet sheet = wb.getSheetAt(k);
			int rows = sheet.getLastRowNum();
			
			// no columns defined, so take all, as defined by first row
			if (columns.isEmpty()) {
				short len = sheet.getRow(0).getLastCellNum();
				if (len>0) {
					for (int i=0; i<len; i++) {
						List<Integer> cols = new ArrayList<Integer>();
						cols.add(i);
						columns.add(cols);
					}
				}
			}
			
			for (List<Integer> set : columns) {
				for (int c : set) {
					for (int r = firstRow; r < rows+1; r++) {
						String value = getValue(sheet, r, c);
						if (value.isEmpty()==false)  {
							if (docBuffer.length()>0) docBuffer.append("\n\n");
							docBuffer.append(value);
						}
					}
				}
				if (docBuffer.length()>0) {
					String location = (k+1)+"."+StringUtils.join(set, "+")+"."+(firstRow+1);
					title = firstRow == 0 ? location : getValue(sheet.getRow(0), set, " ");
					xlsStoredDocumentSources.add(getChild(metadata, id, docBuffer.toString(), location, title, null));
					docBuffer.setLength(0); // reset buffer
				}
			}
			
		}
		wb.close();
		return xlsStoredDocumentSources;
	}
	
	private List<StoredDocumentSource> getDocumentsRowCells(StoredDocumentSource storedDocumentSource) throws IOException {
		DocumentMetadata metadata = storedDocumentSource.getMetadata();
		String id = storedDocumentSource.getId();
		Workbook wb = getWorkBook(storedDocumentSource);
		List<StoredDocumentSource> xlsStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		List<List<Integer>> columns = getInts("tableContent", true);
		List<List<Integer>> titles = getInts("tableTitle", true);
		List<List<Integer>> authors = getInts("tableAuthor", true);
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		Row row;
		String contents;
		String location;
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			Sheet sheet = wb.getSheetAt(k);
			int rows = sheet.getLastRowNum();
			for (int r = firstRow; r < rows+1; r++) {
				
				row = sheet.getRow(r);
				if (row==null) {continue;}
				if (columns.isEmpty()) {
					short len = row.getLastCellNum();
					if (len>0) {
						List<Integer> cols = new ArrayList<Integer>();
						for (int i=0; i<len; i++) {
							cols.add(i);
						}
						columns.add(cols);
					}
					
				}
				
				for (List<Integer> columnsSet : columns) {
					contents = columnsSet.isEmpty() ? getValue(row, "\t") : getValue(row, columnsSet, "\t");
					if (contents.isEmpty()==false) {
						location = (k+1)+"."+StringUtils.join(columnsSet, "+")+"."+(r+1);
						String title = location;
						if (titles.isEmpty()==false && columns.size()==1) {
							List<String> currentTitles = new ArrayList<String>();
							for (List<Integer> titleSet : titles) {
								String t = getValue(row, titleSet, " ");
								if (t.isEmpty()==false) {
									currentTitles.add(t);
								}
							}
							if (currentTitles.isEmpty()==false) {
								title = StringUtils.join(currentTitles, " ");
							}
						}
						List<String> currentAuthors = new ArrayList<String>();
						if (authors.isEmpty()==false && columns.size()==1) {
							for (List<Integer> set : authors) {
								String author = getValue(row, set, " ").trim();
								if (author.isEmpty()==false) {
									currentAuthors.add(author);
								}
							}
						}
						xlsStoredDocumentSources.add(getChild(metadata, id, contents, location, title, currentAuthors));
						
					}
				}
			}
		}
		wb.close();
		return xlsStoredDocumentSources;
	}
	
	private String getValue(Row row, String separator) {
		short len = row.getLastCellNum();
		if (len>0) {
			List<Integer> cells = new ArrayList<Integer>();
			for (int i=0; i<len; i++) {
				cells.add(i);
			}
			return getValue(row, cells, separator);
		}
		else {
			return "";
		}
	}
	
	private List<String> getValues(Row row, List<Integer> cells) {
		List<String> strings = new ArrayList<String>();
		for (int i : cells) {
			Cell cell = row.getCell(i);
			if (cell!=null) {
				String s = getValue(cell);
				if (s!=null && s.isEmpty()==false) {
					strings.add(s);
				}
			}
		}
		return strings;
	}

	private String getValue(Row row, List<Integer> cells, String separator) {
		return StringUtils.join(getValues(row, cells), separator);
	}
	
	private String getValue(Sheet sheet, int rowIndex, int cellIndex) {
		
		if (rowIndex < 0 || cellIndex < 0) return "";
		
		Row row = sheet.getRow(rowIndex);
		if (row==null) return "";
		
		Cell cell = row.getCell(cellIndex);
		if (cell==null) return "";
		
		if (cell.getCellType()==Cell.CELL_TYPE_STRING) {
			String value = getValue(cell);
			return value == null ? "" : value.trim();
		}
		else  return "";
		
	}
	
	private String getValue(Cell cell) {
		if (cell!=null) {
			switch (cell.getCellType()) {
				case Cell.CELL_TYPE_STRING:
					return cell.getStringCellValue().trim();
				case Cell.CELL_TYPE_FORMULA:
					return cell.getCellFormula().trim();
				case Cell.CELL_TYPE_NUMERIC:
					return String.valueOf(cell.getNumericCellValue()).trim();
			}
		}
		return null;
	}
	
	private List<List<Integer>> getInts(String key, boolean decrement) {
		List<List<Integer>> outerList = new ArrayList<List<Integer>>();
		for (String string : parameters.getParameterValues(key)) {
			for (String set : string.trim().split(",")) {
				List<Integer> innerList = new ArrayList<Integer>();
				for (String s : set.trim().split("\\+")) {
					try {
						innerList.add(Integer.valueOf(s.trim()) + (decrement ? -1 : 0));
					}
					catch (NumberFormatException e) {
						throw new IllegalArgumentException(key+" parameter should only contain numbers: "+string, e);
					}
				}
				if (innerList.isEmpty()==false) {
					outerList.add(innerList);
				}
			}
		}
		return outerList;
	}
	
	
	private StoredDocumentSource getChild(DocumentMetadata parentMetadata, String parentId, String string, String location, String title, List<String> authors) throws IOException {
		DocumentMetadata metadata = parentMetadata.asParent(parentId, DocumentMetadata.ParentType.EXPANSION);
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setTitle(title);
		if (authors!=null && authors.isEmpty()==false) {
			metadata.setAuthors(authors.toArray(new String[0]));
		}
		metadata.setDocumentFormat(DocumentFormat.TEXT);
		String id = DigestUtils.md5Hex(parentId + location);
		InputSource inputSource = new StringInputSource(id, metadata, string);
		return storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
	}

	

}
