/**
 * 
 */
package org.voyanttools.trombone.nlp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author sgs
 *
 */
public class PosLemmas implements Iterable<PosLemmas>, Iterator<PosLemmas>, Serializable {

	/**
	 * 
	 */
	public static final long serialVersionUID = 5711116720876058883L;
	private String text;
	private List<String> terms;
	private List<String> lemmas;
	private List<String> pos;
	private List<Integer> starts;
	private List<Integer> ends;
	private int counter;
	private Map<Integer, Integer> startsToIndexMap;
	
	/**
	 * 
	 */
	public PosLemmas(String text) {
		this.text = text;
		terms = new ArrayList<String>();
		lemmas = new ArrayList<String>();
		pos = new ArrayList<String>();
		starts = new ArrayList<Integer>();
		ends = new ArrayList<Integer>();
		startsToIndexMap = new HashMap<Integer, Integer>();
	}
	
	public void add(String term, String pos, String lemma, int start, int end) {
		counter = terms.size();
		startsToIndexMap.put(start, counter);
		terms.add(term);
		this.pos.add(pos);
		lemmas.add(lemma);
		starts.add(start);
		ends.add(end);
	}

	@Override
	public Iterator<PosLemmas> iterator() {
		counter = -1;
		return this;
	}

	@Override
	public boolean hasNext() {
		return counter+1<terms.size();
	}

	@Override
	public PosLemmas next() {
		counter++;
		return this;
	}
	
	public String getCurrentTerm() {
		return counter>-1 && counter < terms.size() ? terms.get(counter) : null;
	}

	public String getCurrentLemma() {
		return counter>-1 && counter < lemmas.size() ? lemmas.get(counter) : null;
	}
	
	public String getCurrentPos() {
		return counter>-1 && counter < pos.size() ? pos.get(counter) : null;
	}

	public int getCurrentStart() {
		return counter>-1 && counter < starts.size() ? starts.get(counter) : -1;
	}

	public int getCurrentEnd() {
		return counter>-1 && counter < ends.size() ? ends.get(counter) : -1;
	}

	public void setCurrentByStart(int start) {
		counter = startsToIndexMap.containsKey(start) ? startsToIndexMap.get(start) : -1;
	}

	public void setCurrentOffset(int correctedStart, int correctedEnd) {
		startsToIndexMap.put(correctedStart, counter);
		starts.set(counter, correctedStart);
		ends.set(counter, correctedEnd);
	}
}
