package org.voyanttools.trombone.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.voyanttools.trombone.model.Confidence.ConfidenceConverter;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

@XStreamConverter(ConfidenceConverter.class)
public class Confidence implements Serializable {

	public enum Type {
		InitialUppercase(.9f, 1f),
		HasLowerCaseForm(.1f, 1f),
		GeonamesLookup(.5f, 1f),
		IsMultiTerm(.8f,1f),
		Population(.5f, 1f), // value is meant to be set
		PrecededByPunctuation(.2f, 1f),
		GeoDistance(.5f, 1f), // value is meant to be set
		Pacte(.5f, 1f), // value meant to be set
		IsPersonName(.2f, 1f);
		private float value;
		private float weight;
		private Type(float value, float weight) {
			this.value = value;
			this.weight = weight;
		}
	}
	
	private Type type;
	private float value;
	private float weight;
	
	public Confidence(Type type) {
		this(type, type.value, type.weight);
	}
	
	public Confidence(Type type, float value) {
		this(type, value, type.weight);
	}
	
	public Confidence(Type type, float value, float weight) {
		this.type = type;
		this.value = value;
		this.weight = weight;
	}
	public String name() {
		return type.name();
	}
	
	public Type getType() {
		return type;
	}
	
	public float getValue() {
		return value;
	}

	public float getWeight() {
		return weight;
	}
	
	public String toString() {
		return type.name()+":"+value;
	}
	
	public Confidence clone() {
		return new Confidence(type, value, weight);
	}
	
	public static Confidence fromString(String string) {
		String[] parts = string.split(":");
		Type type = Type.valueOf(parts[0]);
		switch (parts.length) {
			case 3: return new Confidence(type, Float.valueOf(parts[1]), Float.valueOf(parts[2]));
			case 2: return new Confidence(type, Float.valueOf(parts[1]));
			default: return new Confidence(type);
		}
	}
	
	public static float getConfidence(Confidence[] confidences) {
		return getConfidence(Arrays.asList(confidences));
	}
	public static float getConfidence(Collection<Confidence> confidences) {
		double weightsSum = confidences.stream()
			.mapToDouble(Confidence::getWeight)
			.sum();
		float val = 0f;
		for (Confidence confidence : confidences) {
			val += (confidence.weight/weightsSum)*confidence.value;
		}
		return val;
	}
	
	public static class ConfidenceConverter implements Converter {

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java.lang.Class)
		 */
		@Override
		public boolean canConvert(Class type) {
			return Confidence.class.isAssignableFrom(type);
		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object, com.thoughtworks.xstream.io.HierarchicalStreamWriter, com.thoughtworks.xstream.converters.MarshallingContext)
		 */
		@Override
		public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
			Confidence confidence = (Confidence) source;
			
	        
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, confidence.type.name(), String.class);
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "value", Float.class);
			writer.setValue(String.valueOf(confidence.value));
			writer.endNode();
	        ExtendedHierarchicalStreamWriterHelper.startNode(writer, "weight", Float.class);
			writer.setValue(String.valueOf(confidence.weight));
			writer.endNode();
			writer.endNode();

		}

		/* (non-Javadoc)
		 * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader, com.thoughtworks.xstream.converters.UnmarshallingContext)
		 */
		@Override
		public Object unmarshal(HierarchicalStreamReader arg0,
				UnmarshallingContext arg1) {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
