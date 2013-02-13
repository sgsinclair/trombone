package org.voyanttools.trombone.model;

public enum TokenType {
	lexical;
		
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
