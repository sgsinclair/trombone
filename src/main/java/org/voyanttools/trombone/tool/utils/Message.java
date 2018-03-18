/**
 * 
 */
package org.voyanttools.trombone.tool.utils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author sgs
 *
 */
@XStreamAlias("message")
public class Message {
	
	private Type type;
	private String message;
	private String className;
	private String methodName;
	private int lineNumber;

	public enum Type {
		info;
	}

	public Message(String message) {
		
	}
	
	/**
	 * 
	 */
	public Message(Type type, String message, String className, String methodName, int lineNumber) {
		this.type = type;
		this.message = message;
		this.methodName = methodName;
		this.className = className;
		this.lineNumber = lineNumber;
	}

}
