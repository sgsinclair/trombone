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
public class JsonLinesFeaturesExpander extends AbstractLinesExpander {

	
	/**
	 * 
	 */
	public JsonLinesFeaturesExpander(StoredDocumentSourceStorage storedDocumentSourceStorage, FlexibleParameters parameters) {
		super(storedDocumentSourceStorage, parameters);
	}

	@Override
	protected DocumentFormat getChildDocumentFormat() {
		return DocumentFormat.JSONFEATURES;
	}

}
