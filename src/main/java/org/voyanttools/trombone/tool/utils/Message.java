/**
 * 
 */
package org.voyanttools.trombone.tool.utils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This is a lightweight class meant to wrap a message to the client (for
 * end-user or for debugging purposes). In addition to a message and message
 * type it includes information about the location from which it was called.
 * 
 * @author Stéfan Sinclair
 */
@XStreamAlias("message")
public class Message {

	private Type type;
	private String code;
	private String message;
	private String className;
	private String methodName;
	private int lineNumber;

	public enum Type {
		INFO, WARN, ERROR, DEBUG
	}

	/**
	 * @param type the message type (info, warn, error, debug)
	 * @param code the short codename for the message
	 * @param message the message (no facility for localization for now)
	 * @param className the class that called the message
	 * @param methodName the method from which the message was created
	 * @param lineNumber the line number from which the message was created
	 */
	public Message(Type type, String code, String message, String className, String methodName, int lineNumber) {
		this.type = type;
		this.code = code;
		this.message = message;
		this.methodName = methodName;
		this.className = className;
		this.lineNumber = lineNumber;
	}

}
