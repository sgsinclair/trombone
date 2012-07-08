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
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.storage.file;

import java.io.File;

import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.storage.Storage;

/**
 * @author sgs
 *
 */
public class FileStorage implements Storage {
	
	/**
	 * the default file-system location for storage
	 */
	public static final String DEFAULT_TROMBOME_DIRECTORY = System.getProperty("java.io.tmpdir") + File.separator + "trombone4_0";
	
	
	/**
	 * the actual base directory used for storage
	 */
	File storageLocation;
	
	/**
	 * the handler for InputSource operations
	 */
	private FileStoredDocumentSourceStorage documentSourceStorage = null;

	public FileStorage() {
		this(new File(DEFAULT_TROMBOME_DIRECTORY));
	}

	public FileStorage(File storageLocation) {
		this.storageLocation = storageLocation;
		if (storageLocation.exists()==false) {
			storageLocation.mkdirs();
		}
	}

	public StoredDocumentSourceStorage getStoredDocumentSourceStorage() {
		if (documentSourceStorage==null) {
			documentSourceStorage = new FileStoredDocumentSourceStorage(this.storageLocation);
		}
		return documentSourceStorage;
	}


}
