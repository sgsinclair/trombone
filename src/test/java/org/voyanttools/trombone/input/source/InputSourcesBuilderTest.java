package org.voyanttools.trombone.input.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;

public class InputSourcesBuilderTest {

	@Test
	public void testLocalSources() throws IOException {
		File base = TestHelper.getTemporaryTestStorageDirectory();
		Storage storage = new FileStorage(base);
		File root = base.getParentFile();
		File localSources = new File(root, "trombone-local-sources");
		File udhr = new File(localSources, "udhr");
		File udhrSub = new File(udhr, "test");
		udhrSub.mkdirs();
		FileUtils.copyDirectory(TestHelper.getResource("udhr"), udhr);
		FileUtils.copyDirectory(TestHelper.getResource("udhr"), udhrSub);
		
		FlexibleParameters parameters;
		InputSourcesBuilder inputSourcesBuilder;
		List<InputSource> sources;
		
		// try with simple filename
		parameters = new FlexibleParameters(new String[]{"localSource=udhr","uri=http://testing.com/udhr-en.txt","uri=http://testing.com/sub/udhr-fr.txt"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof FileInputSource);
		}
		
		// try with subdirectory
		parameters = new FlexibleParameters(new String[]{"localSource=udhr","uri=http://testing.com/udhr/test/udhr-en.txt","uri=http://testing.com/sub/udhr/test/udhr-fr.txt"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof FileInputSource);
		}
		
		// try with URI fallback
		parameters = new FlexibleParameters(new String[]{"localSource=udhr","uri=http://testing.com/noexists.txt","uri=http://testing.com/udhr/noexists.txt"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof UriInputSource);
		}
		
		// try with junk URIs
		parameters = new FlexibleParameters(new String[]{"localSource=udhr","uri=http://testing.com/../../../noexists.txt","uri=http://testing.com/"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof UriInputSource);
		}
		
		// try with junk localSource
		parameters = new FlexibleParameters(new String[]{"localSource=/","uri=http://testing.com/../../../noexists.txt","uri=http://testing.com/"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof UriInputSource);
		}
		
		// try with junk localSource
		parameters = new FlexibleParameters(new String[]{"localSource=udhr/../..","uri=http://testing.com/../../../noexists.txt","uri=http://testing.com/"});
		inputSourcesBuilder = new InputSourcesBuilder(parameters);
		sources = inputSourcesBuilder.getInputSources(storage);
		assertEquals(2, sources.size());
		for (InputSource inputSource : sources) {
			assertTrue(inputSource instanceof UriInputSource);
		}
		
	}

}
