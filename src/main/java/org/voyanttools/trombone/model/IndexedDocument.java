/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.model;

import java.io.IOException;
import java.util.Comparator;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamConverter;

/**
 * @author sgs
 *
 */
@XStreamConverter(DocumentConverter.class)
public class IndexedDocument implements DocumentContainer, Comparable<IndexedDocument> {

	private String id;
	
	private DocumentMetadata metadata = null;
	
	private Storage storage;
	
	public enum Sort {
		INDEXASC, INDEXDESC, TITLEASC, TITLEDESC, AUTHORASC, AUTHORDESC, TERMSCOUNTLEXICALASC, TERMSCOUNTLEXICALDESC, PUBDATEASC, PUBDATEDESC;

		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "INDEX";
			if (sort.startsWith("TITLE")) {sortPrefix="TITLE";}
			if (sort.startsWith("TERMSCOUNT")) {sortPrefix="TERMSCOUNTLEXICAL";} // TODO: support other kinds of term counts
			else if (sort.startsWith("AUTHOR")) {sortPrefix="AUTHOR";}
			else if (sort.startsWith("PUBDATE")) {sortPrefix="PUBDATE";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "ASC";
			if (dir.endsWith("DESC")) {dirSuffix="DESC";}
			return valueOf(sortPrefix+dirSuffix);
		}
	}
	
	/**
	 * 
	 */
	public IndexedDocument(Storage storage, String id) {
		this.storage = storage;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public StoredDocumentSource asStoredDocumentSource() throws IOException {
		return new StoredDocumentSource(getId(), getMetadata());
	}

	public DocumentMetadata getMetadata() throws IOException {
		if (metadata==null) {
			metadata = storage.getStoredDocumentSourceStorage().getStoredDocumentSourceMetadata(getId());
		}
		return metadata;
	}
	
	public static class IndexedDocumentPriorityQueue {
		
		// used when a size is given – use the Lucene implementation for better memory management (only top items are kept)
		private org.apache.lucene.util.PriorityQueue<IndexedDocument> limitedSizeQueue = null;
		
		// use the Java implementation to allow the queue to grow arbitrarily big
		private java.util.PriorityQueue<IndexedDocument> unlimitedSizeQueue = null;

		public IndexedDocumentPriorityQueue(IndexedDocument.Sort sort) {
			this(Integer.MAX_VALUE, sort);
		}

		public IndexedDocumentPriorityQueue(int size, IndexedDocument.Sort sort) {
			Comparator<IndexedDocument> comparator = IndexedDocument.getComparator(sort);
			if (size==Integer.MAX_VALUE) {
				unlimitedSizeQueue = new java.util.PriorityQueue<IndexedDocument>(11, comparator);
			}
			else {
				limitedSizeQueue = new LimitedSizeQueue<IndexedDocument>(size, comparator);
			}
		}
		
		private class LimitedSizeQueue<IndexedDocument> extends org.apache.lucene.util.PriorityQueue<IndexedDocument> {

			Comparator<IndexedDocument> comparator;
			
			public LimitedSizeQueue(int maxSize, Comparator<IndexedDocument> comparator) {
				super(maxSize);
				this.comparator = comparator;
			}

			@Override
			protected boolean lessThan(IndexedDocument a, IndexedDocument b) {
				return comparator.compare(a, b) < 0;
			}
			
		}

		public void offer(IndexedDocument document) {
			if (limitedSizeQueue!=null) {limitedSizeQueue.insertWithOverflow(document);}
			else if (unlimitedSizeQueue!=null) {unlimitedSizeQueue.offer(document);}
		}
		
		public int size() {
			if (limitedSizeQueue!=null) {return limitedSizeQueue.size();}
			else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.size();}
			return 0;
		}

		public IndexedDocument poll() {
			if (limitedSizeQueue!=null) {return limitedSizeQueue.pop();}
			else if (unlimitedSizeQueue!=null) {return unlimitedSizeQueue.poll();}
			return null;
		}	
	}

	public static Comparator<IndexedDocument> getComparator(Sort sort) {
		switch(sort) {
		case INDEXDESC:
			return IndexDescComparator;
		case TITLEASC:
			return TitleAscComparator;
		case TITLEDESC:
			return TitleDescComparator;
		case AUTHORASC:
			return AuthorAscComparator;
		case AUTHORDESC:
			return AuthorDescComparator;
		case TERMSCOUNTLEXICALASC:
			return TermsCountLexicalAscComparator;
		case TERMSCOUNTLEXICALDESC:
			return TermsCountLexicalDescComparator;
		case PUBDATEASC:
			return PubDateAscendingComparator;
		case PUBDATEDESC:
			return PubDateDescendingComparator;
		default:
			return IndexAscComparator;
		}
	}
	
	private static Comparator<IndexedDocument> IndexAscComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return Integer.compare(doc2.getMetadata().getIndex(), doc1.getMetadata().getIndex());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> IndexDescComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return Integer.compare(doc1.getMetadata().getIndex(), doc2.getMetadata().getIndex());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> TitleAscComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return doc2.getMetadata().getTitle().compareTo(doc1.getMetadata().getTitle());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> TitleDescComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return doc1.getMetadata().getTitle().compareTo(doc2.getMetadata().getTitle());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> AuthorAscComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return doc2.getMetadata().getAuthor().compareTo(doc1.getMetadata().getAuthor());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> AuthorDescComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return doc1.getMetadata().getAuthor().compareTo(doc2.getMetadata().getAuthor());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static Comparator<IndexedDocument> TermsCountLexicalAscComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return Integer.compare(doc1.getMetadata().getTokensCount(TokenType.lexical), doc2.getMetadata().getTokensCount(TokenType.lexical));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> TermsCountLexicalDescComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				return Integer.compare(doc2.getMetadata().getTokensCount(TokenType.lexical), doc1.getMetadata().getTokensCount(TokenType.lexical));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static Comparator<IndexedDocument> PubDateDescendingComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				String d1 = doc1.getMetadata().getPubDate();
				String d2 = doc2.getMetadata().getPubDate();
				if (d1.equals(d2)) {
					return TitleAscComparator.compare(doc1, doc2);
				}
				else {
					return d2.compareTo(d1);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	private static Comparator<IndexedDocument> PubDateAscendingComparator =  new Comparator<IndexedDocument>() {
		@Override
		public int compare(IndexedDocument doc1, IndexedDocument doc2) {
			try {
				String d1 = doc1.getMetadata().getPubDate();
				String d2 = doc2.getMetadata().getPubDate();
				if (d1.equals(d2)) {
					return TitleAscComparator.compare(doc1, doc2);
				}
				else {
					return d1.compareTo(d2);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	};
	
	@Override
	public int compareTo(IndexedDocument o) {
		return IndexAscComparator.compare(this, o);
	}
}
