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
package org.voyanttools.trombone.input.expand;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.voyanttools.trombone.input.source.FileInputSource;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.TestHelper;

/**
 * @author "Stéfan Sinclair"
 *
 */
public class ArchiveExpanderTest {

	@Test
	public void testArchives() throws IOException {
		
		Storage storage = TestHelper.getDefaultTestStorage();
		StoredDocumentSourceStorage storedDocumentSourceStorage = storage.getStoredDocumentSourceStorage();
		StoredDocumentSourceExpander storedDocumentSourceExpander = new StoredDocumentSourceExpander(storedDocumentSourceStorage);
		
		InputSource inputSource;
		StoredDocumentSource storedDocumentSource;
		List<StoredDocumentSource> expandedSourceDocumentSources;
		
		inputSource = new FileInputSource(TestHelper.getResource("formats/Bag-cwrc_b901f23a_e7a2_4d7e_8db7_8c9b6dbf283a.zip"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("BagIt zip file should be the same as original.", 1, expandedSourceDocumentSources.size());
		assertEquals("Bag-cwrc_b901f23a_e7a2_4d7e_8db7_8c9b6dbf283a", expandedSourceDocumentSources.get(0).getMetadata().getTitle());
		assertEquals(DocumentFormat.BAGIT, expandedSourceDocumentSources.get(0).getMetadata().getDocumentFormat());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.zip"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Zip archive file should contain two content files", 2, expandedSourceDocumentSources.size());
		assertEquals("chars_latin1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());
		assertEquals("chars_latin1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar.gz"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());
		assertEquals("chars_latin1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());

		inputSource = new FileInputSource(TestHelper.getResource("archive/archive.tar.bz2"));
		storedDocumentSource = storedDocumentSourceStorage.getStoredDocumentSource(inputSource);
		expandedSourceDocumentSources = storedDocumentSourceExpander.expandArchive(storedDocumentSource);
		assertEquals("Compressed tar archive file should contain two content files", 2, expandedSourceDocumentSources.size());
		assertEquals("chars_latin1", expandedSourceDocumentSources.get(0).getMetadata().getTitle());

		storage.destroy();
	}

}
