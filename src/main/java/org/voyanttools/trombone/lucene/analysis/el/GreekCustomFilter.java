package org.voyanttools.trombone.lucene.analysis.el;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Normalizes token text to use tonos rather than oxia accent, see
 * 		https://github.com/aurelberra/stopwords/blob/master/revision_notes.md#precombined-diacritics
 */
public final class GreekCustomFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  /**
   * Create a GreekOxiaFilter that normalizes Greek token text.
   * 
   * @param in TokenStream to filter
   */
  public GreekCustomFilter(TokenStream in) {
    super(in);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      char[] chArray = termAtt.buffer();
      int chLen = termAtt.length();
      for (int i = 0; i < chLen;) {
        i += Character.toChars(
            lowerCase(Character.codePointAt(chArray, i, chLen), i+1==chLen), chArray, i);
       }
      return true;
    } else {
      return false;
    }
  }
  
  private int lowerCase(int codepoint,  boolean isLast) {
    switch(codepoint) {
    		// see https://github.com/aurelberra/stopwords/blob/master/revision_notes.md#precombined-diacritics
		// and https://github.com/sgsinclair/Voyant/issues/382#issuecomment-358821892
    		case '\u03C3': // small sigma 
		case '\u03C2': // small final sigma 
		case '\u03F2': // lunate sigma
			return isLast ? '\u03C2' /* small final sigma */ : '\u03C3' /*small final sigma */;
	    case '\u1F71': return '\u03AC'; // Lowercase Alpha + acute 
	    case '\u1FBB': return '\u0386'; // Uppercase Alpha + acute 
	    case '\u1F73': return '\u03AD'; // Lowercase Epsilon + acute 
	    case '\u1FC9': return '\u0388'; // Uppercase Epsilon + acute 
	    case '\u1F75': return '\u03AE'; // Lowercase Eta + acute 
	    case '\u1FCB': return '\u0389'; // Uppercase Eta + acute 
	    case '\u1F77': return '\u03AF'; // Lowercase Iota + acute 
	    case '\u1FDB': return '\u038A'; // Uppercase Iota + acute 
	    case '\u1F79': return '\u03CC'; // Lowercase Omicron + acute 
	    case '\u1FF9': return '\u038C'; // Uppercase Omicron + acute 
	    case '\u1F7B': return '\u03CD'; // Lowercase Upsilon + acute 
	    case '\u1FEB': return '\u038E'; // Uppercase Upsilon + acute 
	    case '\u1F7D': return '\u03CE'; // Lowercase Omega + acute 
	    case '\u1FFB': return '\u038F'; // Uppercase Omega + acute 
	    case '\u1FD3': return '\u0390'; // Lowercase Iota + dialytika + acute 
	    case '\u1FE3': return '\u03B0'; // Lowercase Upsilon + dialytika + acute 
	    default: return Character.toLowerCase(codepoint);
    }
  }
}