package org.voyanttools.trombone.lucene.analysis.el;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Normalizes token text to use tonos rather than oxia accent and normalize sigmas, see
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
          i += Character.toChars(lowerCase(Character.codePointAt(chArray, i, chLen), i+1==chLen), chArray, i);
         }
      return true;
    } else {
      return false;
    }
  }
  
  private int lowerCase(int codepoint, boolean isLast) {
    switch(codepoint) {
		// normalize sigmas, see https://github.com/sgsinclair/Voyant/issues/382#issuecomment-358821892
    		case '\u03C3': // small sigma
    		case '\u03A3': // upper sigma
		case '\u03C2': // small final sigma 
		case '\u03F2': // lunate sigma
		case '\u03F9': // upper lunate sigma
			return isLast ? '\u03C2' /* small final sigma */ : '\u03C3' /*small sigma */;
			
		// normalize oxia to tonos, see https://github.com/aurelberra/stopwords/blob/master/revision_notes.md#precombined-diacritics
	    case '\u1F71': // Lowercase Alpha + acute 
	    case '\u1FBB':  // Uppercase Alpha + acute 
	    		return '\u03AC';
	    case '\u1F73': // Lowercase Epsilon + acute 
	    case '\u1FC9': // Uppercase Epsilon + acute 
	    		return '\u03AD';
	    case '\u1F75': // Lowercase Eta + acute 
	    case '\u1FCB': // Uppercase Eta + acute 
	    		return '\u03AE';
	    case '\u1F77': // Lowercase Iota + acute 
	    case '\u1FDB': // Uppercase Iota + acute 
	    		return '\u03AF';
	    case '\u1F79': // Lowercase Omicron + acute 
	    case '\u1FF9': // Uppercase Omicron + acute 
	    		return '\u03CC';
	    case '\u1F7B': // Lowercase Upsilon + acute 
	    case '\u1FEB': // Uppercase Upsilon + acute
	    		return '\u03CD';
	    case '\u1F7D': // Lowercase Omega + acute 
	    case '\u1FFB': // Uppercase Omega + acute
	    		return '\u03CE';
	    case '\u1FD3': return '\u0390'; // Lowercase Iota + dialytika + acute 
	    case '\u1FE3': return '\u03B0'; // Lowercase Upsilon + dialytika + acute 
	    
	    default: return Character.toLowerCase(codepoint);
    }
  }
}