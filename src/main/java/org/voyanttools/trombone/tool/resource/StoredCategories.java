package org.voyanttools.trombone.tool.resource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.model.Categories;
import org.voyanttools.trombone.model.Corpus;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.tool.corpus.CorpusManager;
import org.voyanttools.trombone.util.FlexibleParameters;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("storedCategories")
public class StoredCategories extends StoredResource {

	public StoredCategories(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
	}

	@Override
	public void run() throws IOException {
		String id = parameters.getParameterValue("retrieveResourceId", "");
		if (id.isEmpty()==false) {
			String localId = null;
			File resourcesDir = new File(Categories.class.getResource("/org/voyanttools/trombone/categories").getFile());
			if (id.equals("auto")) {
				localId = "categories.en.txt"; // default
				if (parameters.containsKey("corpus")) {
					Corpus corpus = CorpusManager.getCorpus(storage, parameters);
					for (String lang : corpus.getLanguageCodes()) {
						// assume it's a language code, but do a check for the file
						if (new File(resourcesDir, "categories."+lang+".txt").exists()) {
							localId = "categories."+lang+".txt";
						}
					}
				}
			} else if (id.length()==2) {
				localId = "categories."+id+".txt"; // assume it's a language code
			} else if (id.matches("categories\\.\\w\\w\\..*?txt")) { // looks like local resource
				localId = id;
			}
			if (localId!=null) {
				File resourcesFile = new File(resourcesDir, localId);
				if (resourcesFile.exists()) {
					resource = FileUtils.readFileToString(resourcesFile, Charset.forName("UTF-8"));
					this.id = id;
					return;
				}
			}
		}
		super.run();
	}
}
