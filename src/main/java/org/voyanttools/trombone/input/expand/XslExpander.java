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

/**
 * @author sgs
 *
 */
public class XslExpander implements Expander {
	
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
	public XslExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
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
		List<StoredDocumentSource> xslStoredDocumentSources = storedDocumentSourceStorage.getMultipleExpandedStoredDocumentSources(id);

		if (xslStoredDocumentSources!=null && xslStoredDocumentSources.isEmpty()==false) {
			return xslStoredDocumentSources;
		}
		
		xslStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		
		// check to see if anything needs to be expanded
		if (parameters.getParameterValue("tableDocumentsColumns", "").isEmpty()==false) {
			return getDocumentsColumns(storedDocumentSource);
		}
		else {
			xslStoredDocumentSources = new ArrayList<StoredDocumentSource>();
			xslStoredDocumentSources.add(storedDocumentSource);
			return xslStoredDocumentSources;
		}

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
		List<StoredDocumentSource> xslStoredDocumentSources = new ArrayList<StoredDocumentSource>();
		int[] columns = getInts("tableDocumentsColumns", true);
		StringBuffer docBuffer = new StringBuffer();
		
		int firstRow = parameters.getParameterBooleanValue("tableNoHeadersRow") ? 0 : 1;
		for (int k = 0; k < wb.getNumberOfSheets(); k++) {
			Sheet sheet = wb.getSheetAt(k);
			int rows = sheet.getPhysicalNumberOfRows();
			
			// we'll consider each column one at a time, even at the cost of iterating over rows
			// multiple times, because we want to buffer one document per column at a time
			for (int c : columns) {
				for (int r = firstRow; r < rows; r++) {
					String value = getValue(sheet, r, c);
					if (value.isEmpty()==false)  {
						if (docBuffer.length()>0) docBuffer.append("\n");
						docBuffer.append(value);
					}
				}
				if (docBuffer.length()>0) {
					xslStoredDocumentSources.add(getChild(metadata, id, docBuffer.toString(), k+"."+c+"."+firstRow));
					docBuffer.setLength(0); // reset buffer
				}
			}
		}
		wb.close();
		return xslStoredDocumentSources;
	}
	
	private String getValue(Sheet sheet, int rowIndex, int cellIndex) {
		
		if (rowIndex < 0 || cellIndex < 0) return "";
		
		Row row = sheet.getRow(rowIndex);
		if (row==null) return "";
		
		Cell cell = row.getCell(cellIndex);
		if (cell==null) return "";
		
		if (cell.getCellType()==Cell.CELL_TYPE_STRING) {
			String value = cell.getStringCellValue();
			return value == null ? "" : value.trim();
		}
		else  return "";
		
	}
	
	private int[] getInts(String key, boolean decrement) {
		List<Integer> intsList = new ArrayList<Integer>();
		for (String string : parameters.getParameterValues(key)) {
			for (String s : string.trim().split(",|;")) {
				try {
					intsList.add(Integer.valueOf(s.trim()));
				}
				catch (NumberFormatException e) {
					throw new IllegalArgumentException(key+" parameter should only contain numbers: "+string, e);
				}
			}
		}
		int[] ints = new int[intsList.size()];
		for (int i=0; i< ints.length; i++) {
			ints[i] = intsList.get(i);
			if (decrement) {ints[i]--;}
		}
		return ints;
	}
	
	
	private StoredDocumentSource getChild(DocumentMetadata parentMetadata, String parentId, String string, String location) throws IOException {
		DocumentMetadata metadata = parentMetadata.asParent(parentId);
		metadata.setModified(parentMetadata.getModified());
		metadata.setSource(Source.STRING);
		metadata.setLocation(location);
		metadata.setDocumentFormat(DocumentFormat.TEXT);
		String id = DigestUtils.md5Hex(parentId + location);
		InputSource inputSource = new StringInputSource(id, metadata, string);
		return storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
	}

	

}
