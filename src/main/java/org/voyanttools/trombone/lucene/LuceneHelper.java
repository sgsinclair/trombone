/**
 * 
 */
package org.voyanttools.trombone.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.voyanttools.trombone.model.IndexedDocument;
import org.voyanttools.trombone.model.Kwic;
import org.voyanttools.trombone.model.TokenType;
import org.voyanttools.trombone.util.Stripper;

/**
 * @author sgs
 *
 */
public class LuceneHelper {

	/**
	 * 
	 */
	public LuceneHelper() {
		// TODO Auto-generated constructor stub
	}

	public static List<Kwic> getKwicsFromPositions(CorpusMapper corpusMapper, IndexedDocument doc, TokenType tokenType, Collection<Integer> positions, int context) throws IOException {
		Set<Integer> allPositions = new HashSet<Integer>();
		int lastTokenIndex = doc.getMetadata().getLastTokenPositionIndex(tokenType);
		for (int position : positions) {
			allPositions.add(position);
			allPositions.add(Math.max(0, position-context));
			allPositions.add(Math.min(lastTokenIndex, position+context));
		}
		Map<Integer, int[]> positionToOffsets = getPositionToOffsetsMap(corpusMapper, doc, tokenType, allPositions);
		String string = doc.getDocumentString();
		List<Kwic> kwics = new ArrayList<Kwic>();
		Stripper stripper = new Stripper(Stripper.TYPE.ALL);
		int docIndex = corpusMapper.getLuceneIdFromDocumentId(doc.getId());
		for (int position : positions) {
			String left = string.substring(positionToOffsets.get(Math.max(0, position-context))[0], positionToOffsets.get(position)[0]-1);
			String middle = string.substring(positionToOffsets.get(position)[0], positionToOffsets.get(position)[1]);
			String right = string.substring(positionToOffsets.get(position)[1], positionToOffsets.get(Math.min(lastTokenIndex, position+context))[1]);
			kwics.add(new Kwic(docIndex, middle, middle, position, left, middle, right));
		}
		return kwics;
		
	}
	public static Map<Integer, int[]> getPositionToOffsetsMap(CorpusMapper corpusMapper, IndexedDocument doc, TokenType tokenType, Set<Integer> positions) throws IOException {
		IndexReader reader = corpusMapper.getIndexReader();
		int luceneDoc = corpusMapper.getLuceneIdFromDocumentId(doc.getId());
		Map<Integer, int[]> map = new HashMap<Integer, int[]>();
		Terms terms = reader.getTermVector(luceneDoc, tokenType.name());
		TermsEnum termsEnum = null;
		if (terms!=null) {
			termsEnum = terms.iterator();
			if (termsEnum!=null) {
				BytesRef bytesRef = termsEnum.next();
				while (bytesRef!=null) {
					PostingsEnum postingsEnum = termsEnum.postings(null, PostingsEnum.OFFSETS);
					postingsEnum.nextDoc();
					int freq = postingsEnum.freq();
					for (int i=0; i<freq; i++) {
						int position = postingsEnum.nextPosition();
						if (positions.contains(position)) {
							map.put(position, new int[] {postingsEnum.startOffset(), postingsEnum.endOffset()});
						}
					}
					bytesRef = termsEnum.next();
				}
			}
		}
		return map;
	}
}
