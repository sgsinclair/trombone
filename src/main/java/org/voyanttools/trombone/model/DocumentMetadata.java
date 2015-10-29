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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;

import org.voyanttools.trombone.input.source.Source;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * This encapsulates various types of metadata about content, including {@link Source},
 * location, last modified timestamp and {@link DocumentFormat}. All modifications
 * to the metadata should be done by the explicit getters and setters.
 * 
 * @author Stéfan Sinclair
 */
//@XStreamConverter(MetadataConverter.class)
public class DocumentMetadata implements Comparable<DocumentMetadata> {
	
	private transient int index = 0;
	
	private FlexibleParameters parameters;
	
//	private Properties properties;
	
	public DocumentMetadata(FlexibleParameters parameters) {
		this.parameters = parameters;
	}
	
	public DocumentMetadata() {
		parameters = new FlexibleParameters();
	}
	
	public boolean equals(DocumentMetadata metadata) {
		return parameters.equals(metadata.parameters);
	}

	/**
	 * Set the location of the source. This is a String representation that will
	 * depend on the {@link Source} but may include a file name, a URI, "memory"
	 * (for a String or transient InputStream).
	 * 
	 * @param location
	 *            the location of the source
	 */
	public void setLocation(String location) {
		setProperty("location", location);
	}

	private void setProperty(String key, String value) {
		parameters.setParameter(key, value);
	}

	/**
	 * Get the location of the source. This is a String representation that will
	 * depend on the {@link Source} but may include a file name, a URI, "memory"
	 * (for a String or transient InputStream).
	 * 
	 * @return the location of the source
	 */
	public String getLocation() {
		return getProperty("location");
	}
	
	

	private String getProperty(String key) {
		return parameters.getParameterValue(key);
//		return properties.getProperty(key);
	}

	/**
	 * Set the {@link Source}.
	 * 
	 * @param source
	 *            the {@link Source}
	 */
	public void setSource(Source source) {
		setProperty("source", source.name().toLowerCase());
	}

	/**
	 * Get the {@link Source} ({@link Source#UNKNOWN} if unknown)
	 * 
	 * @return the {@link Source} ({@link Source#UNKNOWN} if unknown)
	 */
	public Source getSource() {
		String source = getProperty("source");
		return source == null || source.isEmpty() ? Source.UNKNOWN : Source.valueOf(source.toUpperCase());
	}

	/**
	 * Set the last modified timestamp (milliseconds since January 1, 1970 GMT)
	 * 
	 * @param modified
	 *            timestamp (milliseconds since January 1, 1970 GMT)
	 */
	public void setModified(long modified) {
		setProperty("modified", String.valueOf(modified));
	}

	/**
	 * Get the last modified timestamp (milliseconds since January 1, 1970 GMT)
	 * or 0 if unknown.
	 * 
	 * @return modified timestamp (milliseconds since January 1, 1970 GMT) or 0
	 *         if unknown
	 */
	public long getModified() {
		return Long.valueOf(getProperty("modified", "0"));
	}

	/**
	 * Determines if this metadata is the same as the specified metadata
	 * 
	 * @param metadata
	 *            the metadata to compare to this one
	 * @return whether or not they are the same
	 */
//	public boolean equals(Metadata metadata) {
//		return this.equals(metadata);
//	}

	private String getProperty(String key, String defaultValue) {
		return parameters.getParameterValue(key, defaultValue);
	}

	/**
	 * Set the {@link DocumentFormat} of the metadata
	 * 
	 * @param format
	 *            the {@link DocumentFormat} of the metadata
	 */
	public void setDefaultFormat(DocumentFormat format) {
		setProperty("defaultFormat", format.name().toLowerCase());
	}

	/**
	 * Get the default {@link DocumentFormat} of the metadata (or
	 * {@link DocumentFormat#UNKNOWN} if unknown). This differs from the
	 * {@link #getDefaultFormat()} in that it's a back-up format, for instance
	 * the one provided by a web server (even if a document can override it).
	 * 
	 * @return the {@link DocumentFormat} of the metadata (or
	 *         {@link DocumentFormat#UNKNOWN} if unknown)
	 */
	public DocumentFormat getDefaultFormat() {
		String format = getProperty("defaultFormat");
		if (format != null && format.isEmpty() == false) {
			return DocumentFormat.valueOf(format.toUpperCase());
		}
		return DocumentFormat.UNKNOWN;
	}

	/**
	 * Get the {@link DocumentFormat} of the metadata (or
	 * {@link DocumentFormat#UNKNOWN} if unknown). If this hasn't been set
	 * explicitly (using {@link #setDocumentFormat}) then an attempt is made to
	 * guess at the format using other heuristics (especially file names and
	 * URIs where applicable).
	 * 
	 * @param documentFormat the {@link DocumentFormat} of the metadata 
	 */
	public void setDocumentFormat(DocumentFormat documentFormat) {
		setProperty("format", documentFormat.name().toLowerCase());
	}
	
	/**
	 * Get the {@link DocumentFormat} of the metadata (or
	 * {@link DocumentFormat#UNKNOWN} if unknown). If this hasn't been set
	 * explicitly (using {@link #setDocumentFormat}) then an attempt is made to
	 * guess at the format using other heuristics (especially file names and
	 * URIs where applicable).
	 * 
	 * @return the {@link DocumentFormat} of the metadata (or
	 *         {@link DocumentFormat#UNKNOWN} if unknown)
	 * @throws IOException is thrown when there's a problem determining format
	 */
	public DocumentFormat getDocumentFormat() throws IOException {

		// try regular format
		String format = getProperty("format");
		if (format != null && format.isEmpty() == false) {
			return DocumentFormat.valueOf(format.toUpperCase());
		}

		Source source = getSource();

		if (source == Source.FILE) {
			return DocumentFormat.fromFile(new File(getLocation()));
		}

		else if (source == Source.URI) {

			// first try to guess from file name
			URI uri;
			try {
				uri = new URI(getLocation());
			}
			catch (URISyntaxException e) {
				throw new IOException("Unable to get URI: "+getLocation(), e);
			}
			String path = uri.getPath();
			DocumentFormat documentFormat = DocumentFormat.fromFile(new File(
					path));
			if (documentFormat != DocumentFormat.UNKNOWN) {
				return documentFormat;
			}

			return getDefaultFormat();
		}
		
		else if (source == Source.STREAM) {
			String location = getLocation();
			if (location != null && location.isEmpty() == false) {
				return DocumentFormat.fromFilename(location);
				
			}
		}

		return getDefaultFormat();

	}
	
	/**
	 * Creates a new child Metadata object with this object as its parent, including provided parent ID.
	 * @return a new child Metadata object
	 */
	public DocumentMetadata asParent(String id) {
		setProperty("id", id);
		FlexibleParameters newParameters = new FlexibleParameters();
		for (String key : parameters.getKeys()) {
			newParameters.setParameter("parent_"+key, parameters.getParameterValues(key));
		}
		return new DocumentMetadata(newParameters);
	}
	
	public void setTitle(String value) {
		setProperty("title", value);
	}

	public void setAuthor(String value) {
		setProperty("author", value);
	}
	
	public void setPubPlace(String value) {
		setProperty("pubPlace", value);
	}
	
	public void setPubDate(String value) {
		setProperty("pubDate", value);
	}
	
	public String getPubDate() {
		return getProperty("pubDate", "");
	}
	
	public void setPublisher(String value) {
		setProperty("publisher", value);
	}
	
	public void setExtra(String key, String value) {
		setProperty("extra."+key, value);
	}

	public void setKeywords(String value) {
		setProperty("keywords", value);
	}

	public String getAuthor() {
		return getProperty("author", "");
	}

	public String getTitle() {
		return getProperty("title", "");
	}
	public String getKeywords() {
		return getProperty("keywords", "");
	}

	public void setLanguageCode(String lang) {
		setProperty("language", lang);
	}

	public String getLanguageCode() {
		return getProperty("language", "");
	}

	public void setTokensCount(TokenType tokenType, int total) {
		setProperty("tokensCount-"+tokenType.name(), String.valueOf(total));
	}

	public void setTypesCount(TokenType tokenType, int totalTypes) {
		setProperty("typesCount-"+tokenType.name(), String.valueOf(totalTypes));
	}

	public void setTypesCountMean(TokenType tokenType, float mean) {
		setProperty("typesCountMean-"+tokenType.name(), String.valueOf(mean));
	}
	
	public float getTypesCountMean(TokenType tokenType) {
		return Float.parseFloat(getProperty("typesCountMean-"+tokenType.name(), "0"));
	}
	
	public void setTypesCountStdDev(TokenType tokenType, float mean) {
		setProperty("typesCountStdDev-"+tokenType.name(), String.valueOf(mean));
	}

	public float getTypesCountStdDev(TokenType tokenType) {
		return Float.parseFloat(getProperty("typesCountStdDev-"+tokenType.name(), "0"));
	}

	public void setLastTokenPositionIndex(TokenType tokenType, int lastPosition) {
		setProperty("lastTokenPositionIndex-"+tokenType.name(), String.valueOf(lastPosition));
	}

	public int getLastTokenPositionIndex(TokenType tokenType) {
		return Integer.parseInt(getProperty("lastTokenPositionIndex-"+tokenType.name(), "0"));
	}
	
	public void setLastTokenOffsetIndex(TokenType tokenType, int lastOffset) {
		setProperty("lastTokenStartOffset-"+tokenType.name(), String.valueOf(lastOffset));
	}

	public int getTokensCount(TokenType tokenType) {
		return Integer.parseInt(getProperty("tokensCount-"+tokenType, "0"));
	}

	public int getTypesCount(TokenType tokenType) {
		return Integer.parseInt(getProperty("typesCount-"+tokenType, "0"));
	}
	
	public String toString() {
		return getSource().name()+": "+getLocation();
	}

	public void setIndex(int index) {
		setProperty("index", String.valueOf(index));
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}
	
	static Comparator<DocumentMetadata> AuthorAscendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getAuthor().equals(o2.getAuthor())) {return o1.compareTo(o2);}
			return o1.getAuthor().compareTo(o2.getAuthor());
		}
	};

	static Comparator<DocumentMetadata> AuthorDescendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getAuthor().equals(o2.getAuthor())) {return o1.compareTo(o2);}
			return o1.getAuthor().compareTo(o2.getAuthor());
		}
	};
	
	static Comparator<DocumentMetadata> TitleAscendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getTitle().equals(o2.getTitle())) {return o1.compareTo(o2);}
			return o1.getTitle().compareTo(o2.getTitle());
		}
	};

	static Comparator<DocumentMetadata> TitleDescendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getTitle().equals(o2.getTitle())) {return o1.compareTo(o2);}
			return o1.getTitle().compareTo(o2.getTitle());
		}
	};
	
	static Comparator<DocumentMetadata> PubDateAscendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getPubDate().equals(o2.getPubDate())) {return o1.compareTo(o2);}
			return o1.getPubDate().compareTo(o2.getPubDate());
		}
	};

	static Comparator<DocumentMetadata> PubDateDescendingComparator = new Comparator<DocumentMetadata>() {
		@Override
		public int compare(DocumentMetadata o1, DocumentMetadata o2) {
			if (o1.getPubDate().equals(o2.getPubDate())) {return o1.compareTo(o2);}
			return o1.getPubDate().compareTo(o2.getPubDate());
		}
	};
	
	@Override
	public int compareTo(DocumentMetadata o) {
		// don't use static comparators since we may get bounced back here
		
		if (getTitle().equals(o.getTitle())==false) {return getTitle().compareTo(o.getTitle());}
		if (getAuthor().equals(o.getAuthor())==false) {return getAuthor().compareTo(o.getAuthor());}
		if (getPubDate().equals(o.getPubDate())==false) {return getPubDate().compareTo(o.getPubDate());}
		return Integer.compare(hashCode(), o.hashCode()); // give up
		
	}

	public FlexibleParameters getFlexibleParameters() {
		return parameters;
	}

	public boolean containsKey(String string) {
		return parameters.containsKey(string);
	}}
