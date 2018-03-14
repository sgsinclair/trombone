/**
 * 
 */
package org.voyanttools.trombone.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * @author sgs
 *
 */
public class Stripper {

	public enum TYPE {
		ALL, BLOCKSONLY, NONE;
		
		public static TYPE getForgivingly(String type) {
			if (type!=null && type.isEmpty()==false) {
				String normalizedType = type.toUpperCase();
				for (TYPE t : values()) {
					if (normalizedType.equals(t.name())) return t;
				}
				if (type.equals("TRUE")) {return ALL;}
			}
			return NONE;
		}
	}
	
	private TYPE type;
	
	private Pattern allTags = Pattern.compile("(<\\/?\\w+.*?>|<\\?.*?\\?>)", Pattern.DOTALL);
	
	private String[] blockTags = new String[]{"p","div"};
	
	private Pattern notBlockTags = Pattern.compile("<\\/?(?!((" + StringUtils.join(blockTags, "|") +")(\\s+|>)))\\w+\\b.*?>", Pattern.DOTALL);
	
	private Pattern isBlockTags = Pattern.compile("<(\\/?)(" + StringUtils.join(blockTags, "|") + ")\\b.*?>", Pattern.DOTALL);
	
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
		return strip(string, false);
	}
	
	public String strip(String string, boolean keepOffsets) {
		Matcher matcher = null;
		if (type==TYPE.ALL) {
			matcher = allTags.matcher(string);
		}
		else if (type==TYPE.BLOCKSONLY && string.contains("<")) {
			matcher = notBlockTags.matcher(string);
			// string = isBlockTags.matcher(string).replaceAll("<!--$1$2-->");
		}
		if (matcher!=null) {
			if (keepOffsets) {
				StringBuffer sb = new StringBuffer();
				while (matcher.find()) {
				    matcher.appendReplacement(sb, StringUtils.repeat(' ', matcher.group().length()));
				}
				matcher.appendTail(sb);
				assert sb.length() == string.length();
				return sb.toString();
			} else {
				return matcher.replaceAll("");
			}
			
		} else {
			return string;		
		}
	}

}
