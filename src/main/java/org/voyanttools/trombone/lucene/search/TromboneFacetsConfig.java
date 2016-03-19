/**
 * 
 */
package org.voyanttools.trombone.lucene.search;

import org.apache.lucene.facet.FacetsConfig;

/**
 * @author sgs
 *
 */
public class TromboneFacetsConfig extends FacetsConfig {

	@Override
	protected DimConfig getDefaultDimConfig() {
		DEFAULT_DIM_CONFIG.multiValued = true;
		return DEFAULT_DIM_CONFIG;
	}

}
