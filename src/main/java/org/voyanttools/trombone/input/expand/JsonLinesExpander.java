/**
 * 
 */
package org.voyanttools.trombone.input.expand;

import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;

/**
 * @author sgs
 *
 */
public class JsonLinesExpander extends AbstractLinesExpander {

	/**
	 * @param storedDocumentSourceStorage
	 * @param parameters
	 */
	public JsonLinesExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		super(storedDocumentSourceStorage, parameters);
	}

	@Override
	DocumentFormat getChildDocumentFormat() {
		return DocumentFormat.JSON;
	}

}
