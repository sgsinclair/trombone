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

import java.util.Comparator;

import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;


/**
 * This is a very light wrapper around the ID of a stored document source and
 * its associated metadata. It should be assumed that a StoredDocumentSource
 * has indeed been stored and is ready to be read (via the {@link StoredDocumentSourceStorage}).
 * 
 * @author Stéfan Sinclair
 */
public class StoredDocumentSource {
	
	enum Sort {
		TITLEASC, TITLEDESC, AUTHORASC, AUTHORDESC, PUBDATEASC, PUBDATEDESC;
		
		public static Sort getForgivingly(FlexibleParameters parameters) {
			String sort = parameters.getParameterValue("sort", "").toUpperCase();
			String sortPrefix = "TITLE";
			if (sort.startsWith("AUTHOR")) {sortPrefix="AUTHOR";}
			else if (sort.startsWith("PUBDATE")) {sortPrefix="PUBDATE";}
			String dir = parameters.getParameterValue("dir", "").toUpperCase();
			String dirSuffix = "ASC";
			if (dir.endsWith("DESC")) {dirSuffix="DESC";}
			return valueOf(sortPrefix+dirSuffix);
		}
		
	};
	
	/**
	 * the document's ID (to allow the storage to retrieve it)
	 */
	private String id;
	
	/**
	 * the document's known metadata
	 */
	private DocumentMetadata metadata;
	
	/**
	 * Create a new instance of this object with the specified Id and {@link DocumentMetadata}
	 * @param id the stored document source's ID
	 * @param metadata the stored document source's {@link DocumentMetadata}
	 */
	public StoredDocumentSource(String id, DocumentMetadata metadata) {
		this.id = id;
		this.metadata = metadata;
	}

	/**
	 * Get this stored document source's {@link DocumentMetadata}
	 * @return this stored document source's {@link DocumentMetadata}
	 */
	public DocumentMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Get this stored document source's ID
	 * @return this stored document source's ID
	 */
	public String getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return id+" "+metadata;
	}

	public static Comparator<StoredDocumentSource> getComparator(FlexibleParameters parameters) {
		Sort sort = Sort.getForgivingly(parameters);
		return getComparator(sort);
	}
	
	public static Comparator<StoredDocumentSource> getComparator(Sort sort) {
		switch(sort) {
			case AUTHORASC:
				return AuthorAscendingComparator;
			case AUTHORDESC:
				return AuthorDescendingComparator;	
			case PUBDATEASC:
				return PubDateAscendingComparator;
			case PUBDATEDESC:
				return PubDateDescendingComparator;	
			case TITLEDESC:
				return TitleDescendingComparator;
			default:
				return TitleAscendingComparator;
			}
	}

	
	private static Comparator<StoredDocumentSource> AuthorAscendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.AuthorAscendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};
	
	private static Comparator<StoredDocumentSource> AuthorDescendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.AuthorDescendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};
	
	private static Comparator<StoredDocumentSource> PubDateAscendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.PubDateAscendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};
	
	private static Comparator<StoredDocumentSource> PubDateDescendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.PubDateDescendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};
	
	private static Comparator<StoredDocumentSource> TitleAscendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.TitleAscendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};
	
	private static Comparator<StoredDocumentSource> TitleDescendingComparator =  new Comparator<StoredDocumentSource>() {
		@Override
		public int compare(StoredDocumentSource doc1, StoredDocumentSource doc2) {
			return DocumentMetadata.TitleDescendingComparator.compare(doc1.metadata, doc2.metadata);
		}
	};


}