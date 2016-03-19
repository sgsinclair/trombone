package ca.hermeneuti.trombone.results.filtering.stoplist;

import java.util.HashSet;
import java.util.Set;

import ca.hermeneuti.trombone.util.keyword.KeywordList;

/**
 * @author Stéfan Sinclair, Cyril Briquet
 */
@Deprecated
public class StopList extends KeywordList {

	private static final long serialVersionUID = 6349267132065128666L;

	private final String legend;
	
	private final static String NO_LEGEND = "";

	@Deprecated
	StopList() {

		this(NO_REF, new HashSet<String>(), null, true);
		
	}
	
	@Deprecated
	public StopList(String stopListRef, Set<String> stopWords) {
		
		this(stopListRef, stopWords, null, true);

	}
	
	@Deprecated
	StopList(String stopListRef, Set<String> stopWords, String legend) {
		
		this(stopListRef, stopWords, legend, false);

	}
	
	@Deprecated
	private StopList(String stopListRef, Set<String> stopWords, String legend, boolean legendIsNull) {
		
		super(stopListRef, stopWords);

		if ((legend == null) && (legendIsNull == false)) {
			throw new NullPointerException("illegal legend");
		}
		if ((legend != null) && (legendIsNull == true)) {
			throw new IllegalArgumentException("illegal legend");
		}
		
		this.legend = (legend != null) ? legend : NO_LEGEND;
		
	}
	
	@Deprecated
	public String getLegend() {
		
		return this.legend;
		
	}
	
	@Deprecated
	public boolean isStopWord(String keyword) {

		return super.isKeyword(keyword);
	
	}
	
}
