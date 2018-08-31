package org.voyanttools.trombone.results;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JsonHierarchicalStreamDriver;
import com.thoughtworks.xstream.io.json.JsonWriter;

import java.io.Writer;

/**
 * @author Cyril Briquet, Stéfan Sinclair
 */
public enum ResultsOutputFormat {

	none {
		@Override
		public XStream getXStream() {

			return null;
		
		}

		@Override
		public String getContentType() {

			return null;

		}
	},
	
	json {
		@Override
		public XStream getXStream() {

			return new XStream(new JsonHierarchicalStreamDriver() {
				@Override
				public HierarchicalStreamWriter createWriter(Writer writer) {
					return new JsonWriter(writer, JsonWriter.DROP_ROOT_MODE);
				}
			});
			
		}

		@Override
		public String getContentType() {
			// change from text/javascript to application/json
			return "application/json;charset=UTF-8";

		}
	},

	html {
		@Override
		public XStream getXStream() {

			throw new UnsupportedOperationException();
			
		}

		@Override
		public String getContentType() {

			return "text/html;charset=UTF-8";

		}
	},

	text {
		@Override
		public XStream getXStream() {

			throw new UnsupportedOperationException();
			
		}

		@Override
		public String getContentType() {

			return "text/plain;charset=UTF-8";

		}
	},

	xml {
		@Override
		public XStream getXStream() {

			return new XStream();
			
		}

		@Override
		public String getContentType() {

			return "application/xml;charset=UTF-8";

		}
	},
	
	zip {
		@Override
		public XStream getXStream() {

			return new XStream();
			
		}

		@Override
		public String getContentType() {
			return "application/zip";

		}
	};

	public static ResultsOutputFormat getResultsOutputFormat(String outputFormat) {
		
		if (outputFormat == null) {
			throw new NullPointerException("illegal output format");
		}
		
		outputFormat = outputFormat.trim().toLowerCase();
		
		return ResultsOutputFormat.valueOf(outputFormat);
		
	}

	public abstract XStream getXStream();
	public abstract String getContentType();
	
}
