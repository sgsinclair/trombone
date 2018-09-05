package org.voyanttools.trombone.model;

import java.io.Serializable;

public enum TokenType implements Serializable {
	lexical, lemma, opentag, closetag, other, emptytag, processinginstruction;
		
	/**
	 * Get a valid TokenType if one is available from a normalized version of the String argument, or default to {@link TokenType.lexcical} if not.
	 * @param string
	 * @return
	 */
	public static TokenType getTokenTypeForgivingly(String string) {
		string = string.toLowerCase().trim();
		for (TokenType t : values()) {
			if (t.name().equals(string)) return t;
		}
		return string.isEmpty() ? lexical : other;
	}
}
