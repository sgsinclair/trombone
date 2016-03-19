package ca.hermeneuti.trombone.util.keyword;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Stéfan Sinclair, Cyril Briquet
 */
@Deprecated
public class KeywordList implements Serializable {

	private static final long serialVersionUID = 2385849850235436161L;

	protected final static String NO_REF = "NO_REF";

	private final String keywordListRef;
	private final Set<String> keywordList;

	@Deprecated
	public KeywordList() {

		this(NO_REF, new HashSet<String>());
		
	}
	
	@Deprecated
	public KeywordList(String keywordListRef, Set<String> keywordList) {
		
		if (keywordListRef == null) {
			throw new NullPointerException("illegal keyword list reference");
		}
		
		this.keywordListRef = keywordListRef;
		this.keywordList = keywordList;
		
	}
	
	@Deprecated
	public String getReference() {

		return this.keywordListRef;
	
	}

	@Deprecated
	public boolean isKeyword(String keyword) {

		if (keyword == null) {
			throw new NullPointerException("illegal keyword");
		}
		
		return this.keywordList.contains(keyword);
	
	}
	
	@Deprecated
	public boolean isEmpty() {

		return this.keywordList.isEmpty();
	
	}

	@Deprecated
	public Set<String> getKeywords() {

		final Set<String> storedKeywords = new HashSet<String>();
		storedKeywords.addAll(this.keywordList);
		
		return storedKeywords;
		
	}
	
	@Deprecated
	public int size() {
		
		return this.keywordList.size();
	
	}
	
}
