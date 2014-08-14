/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 * @author sgs
 *
 */
public class SimpleDocIdsCollector extends TotalHitCountCollector {
	
	Set<Integer> luceneDocIds = new HashSet<Integer>();
	Collection<String> docIds = new HashSet<String>();
	
	AtomicReader atomicReader = null;
	
	private static Set<String> FIELDS = new HashSet<String>(Arrays.asList("id"));

	/**
	 * 
	 */
	public SimpleDocIdsCollector() {
		// TODO Auto-generated constructor stub
	}
	
	public void collect(int doc) {
		if (luceneDocIds.contains(doc)==false) {
			Document document;
			try {
				document = atomicReader.document(doc, FIELDS);
			} catch (IOException e) {
				throw new RuntimeException("Unable to read Lucene document.");
			}
			String id = document.get("id");
			docIds.add(id);
		}
		luceneDocIds.add(doc);
		super.collect(doc);
	}
	
	public Collection<String> getDocIds() {
		return docIds;
	}
	
	@Override
	public void setNextReader(AtomicReaderContext context) {
		atomicReader = context.reader();
		luceneDocIds.clear(); // new reader context, so doc ids may not be unique
	}

}
