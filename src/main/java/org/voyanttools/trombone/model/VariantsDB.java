/**
 * 
 */
package org.voyanttools.trombone.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.voyanttools.trombone.storage.Storage;

import edu.stanford.nlp.util.StringUtils;

/**
 * @author sgs
 *
 */
public class VariantsDB extends AbstractDB {
	
	private Map<String, String[]> map;

	/**
	 * @param storage
	 * @param dbId
	 * @param readOnly
	 * @throws IOException 
	 */
	public VariantsDB(Storage storage, String dbId, boolean readOnly) throws IOException {
		super(storage, "variants-"+dbId, readOnly);
		map = db.getHashMap("variants");
		if (map.isEmpty()) {
			URI variantsUrl;
			try {
				variantsUrl = this.getClass().getResource("/org/voyanttools/trombone/variants/"+dbId+".txt").toURI();
			} catch (URISyntaxException e) {
				throw new IOException("Unable to find local variants directory", e);
			}
			File file = new File(variantsUrl.getPath());
			if (file.exists()) {
				db.close();
				setDB("variants-"+dbId, false);
				map = db.getHashMap("variants");
				for (String line : FileUtils.readLines(file)) {
					List<String> parts = StringUtils.split("\t");
					put(parts.remove(0), parts.toArray(new String[0]));
				}
				if (readOnly!=false) { // give a read-only view
					db.close();
					setDB("variants-"+dbId, readOnly);
					map = db.getHashMap("variants");
				}
			}

		}
	}
	
	public String[] get(String term) {
		return map.get(term);
	}

	public void put(String term, String[] variants) {
		map.put(term, variants);
	}
}
