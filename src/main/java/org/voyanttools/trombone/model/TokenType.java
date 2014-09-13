package org.voyanttools.trombone.model;

import java.io.Serializable;

public enum TokenType implements Serializable {
	lexical, other;
		
	/**
	 * Get a valid TokenType if one is available from a normalized version of the String argument, or default to {@link TokenType.lexcical} if not.
	 * @param string
	 * @return
	 */
	public static TokenType getTokenTypeForgivingly(String string) {
		string = string.toLowerCase();
		for (TokenType t : values()) {
			if (t.name().equals(string)) return t;
		}
		return lexical;
	}
}
