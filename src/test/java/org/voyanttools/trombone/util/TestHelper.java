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
package org.voyanttools.trombone.util;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.storage.memory.MemoryStorage;

/**
 * @author sgs
 *
 */
public class TestHelper {
	
	public static String RESOURCES_PATH = TestHelper.class.getResource("../texts").getFile();
	public static final String DEFAULT_TROMBOME_DIRECTORY = FileStorage.DEFAULT_TROMBOME_DIRECTORY+"_test";
	public static Storage getDefaultTestStorage() throws IOException {
//		return new FileStorage(getTemporaryTestStorageDirectory());
		return new MemoryStorage();
	}
	public static File getTemporaryTestStorageDirectory() throws IOException {
		File file = new File(FileStorage.DEFAULT_TROMBOME_DIRECTORY+"_test_"+UUID.randomUUID());
		System.out.println("Temporary storage created: "+file.toString());
		return file;
	}
	
	public static File getResource(String relativeToTexts) {
		return new File(RESOURCES_PATH+"/"+relativeToTexts);
	}

}
