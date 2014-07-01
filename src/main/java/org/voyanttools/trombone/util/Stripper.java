/**
 * 
 */
package org.voyanttools.trombone.util;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * @author sgs
 *
 */
public class Stripper {

	public enum TYPE {
		all, blocks, none;
		
		public static TYPE getForgivingly(String type) {
			if (type!=null && type.isEmpty()==false) {
				String normalizedType = type.toLowerCase();
				for (TYPE t : values()) {
					if (normalizedType.equals(t.name())) return t;
				}
			}
			return none;
		}
	}
	
	private TYPE type;
	
	private Pattern allTags = Pattern.compile("<\\/?\\w+.*?>");
	
	private String[] blockTags = new String[]{"p","div"};
	
	private Pattern notBlockTags = Pattern.compile("<\\/?\\w+\\b.*?>");
	
	private Pattern isBlockTags = Pattern.compile("<(\\/?)(" + StringUtils.join(blockTags, "|") + ")\\b.*?>");
	
	/**
	 * 
	 */
	public Stripper(String type) {
		this(TYPE.getForgivingly(type));
	}
	
	/**
	 * 
	 */
	public Stripper(TYPE type) {
		this.type = type;
	}
	
	public String strip(String string) {
		if (type==TYPE.all) {
			return allTags.matcher(string).replaceAll("");
		}
		else if (type==TYPE.blocks) {
			string = notBlockTags.matcher(string).replaceAll("");
			string = isBlockTags.matcher(string).replaceAll("<!--$1$2-->");
			return string;
		}
		else {return string;}
	}

}
