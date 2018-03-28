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
package org.voyanttools.trombone.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This class encapsulates flexible parameters that map keys to values, except
 * that values can be retrieved as various data types (String, int, long) and
 * arrays of an array of Strings. an int value.
 * 
 * @author Stéfan Sinclair
 */
@XStreamAlias("parameters")
public class FlexibleParameters implements Cloneable, Serializable {

	/**
	 * the serialization ID
	 */
	private static final long serialVersionUID = 2825799029392893403L;

	/* obsolete code, keep for now, see also CorpusTokenizer
	private Map<String, List<Object>> objectEntries = new HashMap<String, List<Object>>();

	public synchronized void addObjectParameters(String key, List<Object> values) {
		
		if (key == null) {
			throw new NullPointerException("illegal key");
		}
		if (values == null) {
			throw new NullPointerException("illegal values");
		}
		if (values.size() < 1) {
			throw new IllegalArgumentException("illegal values");
		}
		
		if (this.objectEntries.put(key, values) != null) {
			throw new IllegalArgumentException("key "+key+" already mapped");
		}
		
	}
	
	public synchronized List<Object> getObjectParameters(String key) {
		
		if (key == null) {
			throw new NullPointerException("illegal key");
		}

		final List<Object> values = this.objectEntries.get(key);
			
		if (values == null) {
			throw new IllegalArgumentException("key "+key+" is not mapped");
		}
		
		return values;

	}
	*/

	/**
	 * An internal {@link Map} to facilitate looking up of properties.
	 */
	private Map<String, List<String>> entries = new HashMap<String, List<String>>();

	/**
	 * Constructs a new instance of the {@link FlexibleParameters} class.
	 */
	public FlexibleParameters() {
		
	}

	/**
	 * Constructs a new instance of the {@link FlexibleParameters} class and
	 * parses the array of {@link String}s adhering to the {@code key=value} pattern.
	 * 
	 * @param args the array of {@link String}s to add
	 */
	public FlexibleParameters(String[] args) {
		
		addParameters(args);
	
	}
	
	/**
	 * Constructs a new instance of the {@link FlexibleParameters} class and
	 * add the specified properties
	 * @param properties the properties to add to add
	 */
	public FlexibleParameters(Properties properties) {
		addProperties(properties);
	}

	public static FlexibleParameters loadFlexibleParameters(File parametersFile) throws IOException {
		XStream xstream = new XStream();		
		InputStream in = null;
		FlexibleParameters parameters = new FlexibleParameters();
		try {
			in = new FileInputStream(parametersFile);
			parameters = (FlexibleParameters) xstream.fromXML(in);
		} catch (FileNotFoundException e) {
		}
		finally {
			if (in!=null) {
				in.close();
			}
		}
		return parameters;
	}
	
	public void saveFlexibleParameters(File file) throws IOException {
		OutputStream outputStream = null;
		outputStream = new FileOutputStream(file);
		Writer writer = new OutputStreamWriter(outputStream, Charset.forName("UTF-8"));
		XStream xStream = new XStream();
		xStream.toXML(this, writer);
		if (outputStream!=null) {
			outputStream.close();
		}
	}
	

	/**
	 * Adds a parameter with a double value. Previously added values are not
	 * discarded.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void addParameter(String key, double value) {
	
		addParameterWithObject(key, value);
	
	}
	
	/**
	 * Adds an array of double values for a parameter. Previously added values are not
	 * discarded.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void addParameter(String key, double[] values) {
	
		for (double v : values) {
			addParameter(key, v);
		}
	
	}

	/**
	 * Adds a parameter with an int value. Previously added values are not
	 * discarded.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void addParameter(String key, int value) {
		
		addParameterWithObject(key, value);
	
	}

	/**
	 * Adds an array of int values for a parameter. Previously added values are
	 * not discarded.
	 * 
	 * @param key the key of the parameter
	 * @param values the array of int values to add
	 */
	public synchronized void addParameter(String key, int[] values) {
		
		for (int v : values) {
			addParameter(key, v);
		}
		
	}

	/**
	 * Adds a parameter with a long value. Previously added values are not
	 * discarded.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void addParameter(String key, long value) {
	
		addParameterWithObject(key, value);
	
	}

	/**
	 * Adds a parameter with a String value. Previously added values are not
	 * discarded.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void addParameter(String key, String value) {
	
		addParameterWithObject(key, value);
	
	}

	/**
	 * Adds an array of String values for a parameter. Previously added values
	 * are not discarded.
	 * 
	 * @param key the key of the parameter
	 * @param values the array of {@link String}s as a value
	 */
	public synchronized void addParameter(String key, String[] values) {
	
		addParameterWithObjects(key, values);
	
	}

	/**
	 * Adds parameters from an array of {@link String}s adhering to the {@code
	 * key=value} pattern. Previously added values are not discarded.
	 * 
	 * @param parameters an array of {@link String}s adhering to the {@code key=value} pattern
	 */
	public synchronized void addParameters(String[] parameters) {
		
		for (String parameter : parameters) {
			final String[] parts = parameter.split("=", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException(parameter+" is not a parameter");
			}
			addParameter(parts[0], parts[1]);
		}
	
	}
	
	/**
	 * Add the specified {@link Properties}
	 * 
	 * @param properties the {@link Properties} to add
	 */
	public synchronized void addProperties(Properties properties) {

		if (properties == null) {
			throw new NullPointerException("illegal Properties");
		}

		for (Entry<Object, Object> entry : properties.entrySet()) {
			addParameter((String) entry.getKey(), (String) entry.getValue());
		}
	
	}

	/**
	 * Add the specified {@link FlexibleParameters}
	 * 
	 * @param properties the {@link FlexibleParameters} to add
	 */
	public synchronized void addProperties(FlexibleParameters properties) {
		
		if (properties == null) {
			throw new NullPointerException("illegal Properties");
		}
		
		for (String key : properties.getKeys()) {
			this.entries.put(key, properties.entries.get(key));
		}
	
	}

	/**
	 * Add a single object to the map. Previously existing values are not
	 * discarded.
	 * 
	 * @param key the key for the map
	 * @param value object to add
	 */
	private synchronized void addParameterWithObject(String key, Object value) {
		
		if (this.entries.containsKey(key) == false) {
			this.entries.put(key, new ArrayList<String>());
		}
		this.entries.get(key).add(String.valueOf(value));
	
	}

	/**
	 * Add an array of objects to the map. Previously existing values are not
	 * discarded.
	 * 
	 * @param key the key for the map
	 * @param values the array of objects
	 */
	private synchronized void addParameterWithObjects(String key, Object[] values) {

		if (this.entries.containsKey(key) == false) {
			this.entries.put(key, new ArrayList<String>());
		}
		final List<String> vals = this.entries.get(key);
		for (Object val : values) {
			vals.add(String.valueOf(val));
		}
	
	}

	@Override
	public synchronized FlexibleParameters clone() {
		FlexibleParameters params = new FlexibleParameters();
		for (Map.Entry<String, List<String>> entry : entries.entrySet()) {
			params.setParameter(entry.getKey(), entry.getValue().toArray(new String[0]));
		}
		return params;
	}
	
	/**
	 * Create a clone of this object.
	 * 
	 * @return a new instance of {@link FlexibleParameters}
	 */
	public synchronized FlexibleParameters deepClone() {
		
		final FlexibleParameters clone = new FlexibleParameters();
		for (Entry<String, List<String>> entry : this.entries.entrySet()) {
			final List<String> entryValueClone = new ArrayList<String>();
			for (String s : entry.getValue()) {
				entryValueClone.add(new String(s));
			}
			
			clone.entries.put(entry.getKey(), entryValueClone);
		}
		
		return clone;
		
	}

	/**
	 * Determines if a parameter is defined with the specified key.
	 * 
	 * @param key the key of the parameter
	 * @return whether or not a parameter is defined with the specified key.
	 */
	public synchronized boolean containsKey(String key) {

		int counter = -1;
		while (true) {
			final String lookupKey = key + (counter < 0 ? "" : String.valueOf(counter));
			if (this.entries.containsKey(lookupKey)) {
				return true;
			}
			else if (counter > 1) {
				break;
			}
			counter++;
		}
		return false;

	}

	
	/**
	 * Get a {@link Set} of keys used in these parameters
	 * 
	 * @return a {@link Set} of keys used in these parameters
	 */
	public synchronized Set<String> getKeys() { 
	 	
		return this.entries.keySet(); 
	
	}
	
	/**
	 * Returns a {@link Properties} view of all the properties. If a
	 * key does not exist, a value of null is returned; if a key has multiple
	 * values, only the last one is used.
	 * @return a {@link Properties} view
	 */
	public synchronized Properties getAsProperties() {
		
		return getAsProperties(this.entries.keySet());
	
	}

	/**
	 * Returns a {@link Properties} view that includes the specified keys. If a
	 * key does not exist, a value of null is returned; if a key has multiple
	 * values, only the last one is used.
	 * @param keys a list of keys for which to retrieve values
	 * @return a {@link Properties} view
	 */
	public synchronized Properties getAsProperties(String... keys) {
	
		return getAsProperties(Arrays.asList(keys));
	
	}
	
	/**
	 * Returns a {@link Properties} view that includes the specified keys. If a
	 * key does not exist, a value of null is returned; if a key has multiple
	 * values, only the last one is used.
	 * @param keys a list of keys for which to retrieve values
	 * @return a {@link Properties} view
	 */
	public synchronized Properties getAsProperties(Collection<String> keys) {
	
		final Properties properties = new Properties();
		
		for (String key : keys) {
			properties.put(key, getParameterValue(key));
		}
		
		return properties;
		
	}
	
	/**
	 * Returns a {@link String} view that can be used as a URL query. Each value
	 * is URL encoded (in UTF-8) and a name can have multiple values:
	 * <code>type=one&type=two</code
	 * @return a {@link String} view
	 * @throws UnsupportedEncodingException in the extremely unlikely event that the encoding isn't supported
	 */
	public synchronized String getAsQueryString() throws UnsupportedEncodingException {

		final StringBuilder query = new StringBuilder();
		for (Map.Entry<String, List<String>> entry : this.entries.entrySet()) {
			for (String value : entry.getValue()) {
				if (query.length()>0) query.append("&");
				query.append(entry.getKey()).append("=").append(URLEncoder.encode(value, "UTF-8"));
			}
		}
		
		return query.toString();
	
	}
	
	/**
	 * Gets the parameter value as an float for the specified key. If the key is
	 * not defined, then a value of 0 is returned.
	 * 
	 * @param key the key for the parameter value
	 * @return the paramater value as a float
	 */
	public synchronized float getParameterFloatValue(String key) {
	
		final String value = getParameterValue(key);
		return value == null || value.isEmpty() ? 0f : Float.parseFloat(value);
	
	}

	/**
	 * Gets the parameter value as an float for the specified key, or return the
	 * specified default value if the key does not exist.
	 * 
	 * @param key the key for the parameter value
	 * @param defaultValue the default value to use if the parameter is not defined
	 * @return the paramater value as a float
	 */
	public synchronized float getParameterFloatValue(String key, float defaultValue) {
	
		final String value = getParameterValue(key);
		return value == null || value.isEmpty() ? defaultValue : Float.parseFloat(value);
		
	}

	/**
	 * Gets the parameter value as an int for the specified key. If the key is
	 * not defined, then a value of 0 is returned.
	 * 
	 * @param key the key for the parameter value
	 * @return the paramater value as an int
	 */
	public synchronized int getParameterIntValue(String key) {

		final String value = getParameterValue(key);
		return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
	
	}

	/**
	 * Gets the parameter value as an int for the specified key, or return the
	 * specified default value if the key does not exist.
	 * 
	 * @param key the key for the parameter value
	 * @param defaultValue the default value to use if the parameter is not set
	 * @return the paramater value as an int
	 */
	public synchronized int getParameterIntValue(String key, int defaultValue) {

		final String value = getParameterValue(key);
		return value == null || value.length() == 0 ? defaultValue : Integer.parseInt(value);
	
	}

	/**
	 * Gets the parameter value as an arrat of int values. If the key does not
	 * exist, an empty array of ints is returned
	 * 
	 * @param key the key for the parameter value
	 * @return the paramater value as an int
	 */
	public synchronized  int[] getParameterIntValues(String key) {
	
		final String[] values = getParameterValues(key);
		final int[] ints = new int[values.length];
		for (int i = 0, size = values.length; i < size; i++) {
			ints[i] = values[i].isEmpty() ? 0 : Integer.valueOf(values[i]);
		}
		return ints;

	}

	/**
	 * Gets the parameter value as a long for the specified key. If the key is
	 * not defined, then a value of 0 is returned.
	 * @param key the key for the parameter value
	 * @return the paramater value as a long
	 */
	public synchronized long getParameterLongValue(String key) {
		
		final String value = getParameterValue(key);
		return value == null || value.isEmpty() ? 0l : Long.valueOf(value);
	
	}

	/**
	 * Gets the parameter value as a long for the specified key, or return the
	 * specified default value if the key does not exist.
	 * 
	 * @param key the key for the parameter value
	 * @param defaultValue the default value to use if the parameter is not set
	 * @return the paramater value as a long
	 */
	public synchronized long getParameterLongValue(String key, long defaultValue) {
	
		final String value = getParameterValue(key);
		return value == null ? defaultValue : Long.valueOf(value);
	
	}

	/**
	 * Gets the parameter value for the specified key. If the key is not defined,
	 * then null is returned.
	 * 
	 * @param key the key for the parameter value
	 * @return the paramater value as a {@link String}
	 */
	public synchronized String getParameterValue(String key) {

		final String[] values = getParameterValues(key);
		// TODO: would it be more symmetrical to return "", more like the other methods?
		return values.length == 0 ? null : values[0];
	
	}

	/**
	 * Gets the parameter value for the specified key, or return the specified
	 * default value if the key does not exist.
	 * 
	 * @param key the key for the parameter value
	 * @param defaultValue the default value to use if the parameter is not set
	 * @return the paramater value as a {@link String}
	 */
	public synchronized String getParameterValue(String key, String defaultValue) {
	
		final String value = getParameterValue(key);
		return value == null ? defaultValue : value;
	
	}
	
	/**
	 * Gets an array of {@link String}s for the specified key. If the key is not
	 * defined, then an empty array of Strings is returned.
	 * 
	 * @param key the key for the parameter
	 * @return an array of {@link String}s for the specified key
	 */
	public synchronized String[] getParameterValues(String key) {
		
		return getParameterValues(key, new String[0]);
	}
	
	/**
	 * Gets an array of {@link String}s for the specified key. If the key is not
	 * defined, then an empty array of Strings is returned.
	 * 
	 * @param key the key for the parameter
	 * @param defaultValue the default String array
	 * @return an array of {@link String}s for the specified key
	 */
	public synchronized String[] getParameterValues(String key, String[] defaultValue) {
		
		final List<String> values = this.entries.get(key);
		return values == null ? defaultValue : (String[]) values.toArray(new String[] {});
	
	}

	/**
	 * Gets a boolean value for the key (based on the first value for that key).
	 * This will return false if the key is not defined, if the value is empty
	 * or if the value is "false" or 0.
	 * @param key the key for the parameter
	 * @return a boolean for the specified key
	 */
	public synchronized boolean getParameterBooleanValue(String key) {
		final String val = getParameterValue(key);
		if (val==null || val.equals("false") || val.equals("0") || val.isEmpty()) {
			return false;
		}
		return true;
	}

	/**
	 * Sets the specified parameter while removing any existing values that
	 * might exist.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void setParameter(String key, double value) {
	
		setParameterWithObject(key, value);
	
	}
	
	/**
	 * Sets the specified parameter while removing any existing values that
	 * might exist.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void setParameter(String key, double[] values) {
	
		this.entries.remove(key);
		addParameter(key, values);
	
	}

	/**
	 * Sets the specified parameter while removing any existing values that
	 * might exist.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void setParameter(String key, int value) {
	
		setParameterWithObject(key, value);
	
	}

	/**
	 * Sets the specified parameter while removing any existing values that might exist.
	 * @param key the key of the parameter
	 * @param values the value of the parameter
	 */
	public synchronized void setParameter(String key, int[] values) {
	
		this.entries.remove(key);
		addParameter(key, values);
	
	}

	/**
	 * Sets the specified parameter while removing any existing values that
	 * might exist.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void setParameter(String key, long value) {

		setParameterWithObject(key, value);
	
	}

	/**
	 * Sets the specified parameter while removing any existing values that
	 * might exist.
	 * 
	 * @param key the key of the parameter
	 * @param value the value of the parameter
	 */
	public synchronized void setParameter(String key, String value) {
	
		setParameterWithObject(key, value);
	
	}

	/**
	 * Sets the specified parameter while removing any existing values that might exist.
	 * @param key the key of the parameter
	 * @param values the value of the parameter
	 */
	public synchronized void setParameter(String key, String[] values) {
	
		this.entries.remove(key);
		addParameter(key, values);
	
	}

	/**
	 * Set the parameter, removing any previously existing values.
	 * @param key the key of the parameter
	 * @param value the Object to set
	 */
	private synchronized void setParameterWithObject(String key, Object value) {
	
		this.entries.remove(key);
		addParameterWithObject(key, value);
	
	}

	
	@Override
	public synchronized String toString() {
	
		// we'll create our own string to have a reliable ordering of map entries
		List<String> keys = new ArrayList<String>();
		keys.addAll(entries.keySet());
		Collections.sort(keys);
		StringBuilder sb = new StringBuilder();
		for (String key : keys) {
			sb.append("{").append(key).append(":");
			String[] strings = getParameterValues(key);
			if (strings.length==1) {
				sb.append(strings[0]);
			}
			else {
				sb.append("[").append(StringUtils.join(strings, ",")).append("]");
			}
			sb.append("}");
		}
		return sb.toString();
	
	}
	
    /**
     * Remove the parameter specified by this key.
     * 
     * @param key of the parameter to remove
     */
    public synchronized void removeParameter(String key) { 
    	
    	this.entries.remove(key);
    	
    }
    
    /**
     * Get the number of parameters (keys).
     * 
     * @return the number of parameters (keys)
     */
    public synchronized int getKeyCount() {

    	return this.entries.size();

    }
    
    public boolean equals(FlexibleParameters parameters) {
    	for (String key : getKeys()) {
    		String[] values = getParameterValues(key);
    		if (parameters.containsKey(key)) {
    			String[] vals = parameters.getParameterValues(key);
    			if (values.length == vals.length) {
    				for (int i=0; i<values.length; i++) {
    					if (values[i].equals(vals[i])==false) {
    						return false;
    					}
    				}
    			}
    			else {
    				return false;
    			}
    		}
    		else {
    			return false;
    		}
    	}
    	return true;
    }

	public void clear() {
		entries.clear();
	}
    
}
