/**
 * 
 */
package org.voyanttools.trombone.tool.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.voyanttools.trombone.model.Location;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.GeonamesIterator;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author sgs
 *
 */
@XStreamAlias("geonames")
@XStreamConverter(Geonames.GeonamesConverter.class)
public class Geonames extends AbstractTool {
	
	private List<Location> locations = new ArrayList<Location>();

	public Geonames(Storage storage, FlexibleParameters parameters) {
		super(storage, parameters);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() throws IOException {
		String[] langs = parameters.getParameterValues("lang", new String[]{"en"});
		Set<String> queries = new HashSet<String>();
		for (String s : parameters.getParameterValues("query")) {
			queries.add(s.toLowerCase());
		}
		for (String lang : langs) {
			GeonamesIterator iterator = new GeonamesIterator(lang);
			Location reusableLocation = null;
			while (iterator.hasNext()) {
				reusableLocation = iterator.next(reusableLocation);
				for (String name : reusableLocation.getNames()) {
					if (queries.contains(name.toLowerCase())) {
						locations.add(reusableLocation.clone());
						break;
					}
				}
			}
			iterator.close();
		}
		
		Collections.sort(locations);
	}
	
	public static class GeonamesConverter implements Converter {

		@Override
		public boolean canConvert(Class type) {
			return Geonames.class.isAssignableFrom(type);
		}

		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			Geonames geonames = (Geonames) source;
			
	        writer.startNode("locations");
	        
	        ToolSerializer.startNode(writer, "total", Integer.class);
	        writer.setValue(String.valueOf(geonames.locations.size()));
	        ToolSerializer.endNode(writer);
	        
	        writer.startNode("locations");
			for (Location location : geonames.locations) {
		        writer.startNode(location.getId());
		        
		        writer.startNode("id");
		        writer.setValue(location.getId());
		        writer.endNode();
		        
		        writer.startNode("label");
		        writer.setValue(location.getFullName());
		        writer.endNode();
		        
		        ToolSerializer.startNode(writer, "lat", Float.class);
		        writer.setValue(String.valueOf(location.getLat()));
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "lng", Float.class);
		        writer.setValue(String.valueOf(location.getLng()));
		        ToolSerializer.endNode(writer);
		        
		        ToolSerializer.startNode(writer, "population", Float.class);
		        writer.setValue(String.valueOf(location.getPopulation()));
		        ToolSerializer.endNode(writer);
		        
		        writer.endNode();
			}
			writer.endNode();
			
			writer.endNode();	
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	
	}

}
