package org.voyanttools.trombone.lucene;

public abstract class AbstractLuceneManager implements LuceneManager {

	private long lastAccessed;
	
	public AbstractLuceneManager() {
		access();
	}
	
	protected void access() {
		lastAccessed = System.currentTimeMillis();
	}
	
	public long getLastAccessed() {
		return lastAccessed;
	}
	
	@Override
	public int compareTo(LuceneManager o) {
		return Long.compare(getLastAccessed(), o.getLastAccessed());
	}
}
