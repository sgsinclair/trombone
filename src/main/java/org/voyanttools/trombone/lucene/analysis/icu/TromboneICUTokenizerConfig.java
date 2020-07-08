/**
 * 
 */
package org.voyanttools.trombone.lucene.analysis.icu;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.icu.segmentation.DefaultICUTokenizerConfig;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;

/**
 * @author sgs
 *
 */
public class TromboneICUTokenizerConfig extends DefaultICUTokenizerConfig {

	private String language;
	private static String TIBETAN = "bo";
	private static RuleBasedBreakIterator TROMBONE_WORD_BREAK_ITERATOR;

	public TromboneICUTokenizerConfig(boolean cjkAsWords, boolean myanmarAsWords, String language) {
		super(cjkAsWords, myanmarAsWords);
		this.language = language;
		String rules;
		try (InputStream is = this.getClass().getResourceAsStream("tromboneDefault.rbbi")) {
			rules = IOUtils.toString(is, "UTF-8");
		} catch (IOException e) {
			throw new RuntimeException("Unable to load trombone break iterator rules.", e);
		}
		TROMBONE_WORD_BREAK_ITERATOR = new RuleBasedBreakIterator(rules);
	}

	  @Override
	  public RuleBasedBreakIterator getBreakIterator(int script) {
		  if (language.equals(TIBETAN)) {
			  return (RuleBasedBreakIterator) TROMBONE_WORD_BREAK_ITERATOR.clone();
		  } else {
			 return super.getBreakIterator(script);
		  }
	  }


	  public static void main (String[] args) {
		  TromboneICUTokenizerConfig config = new TromboneICUTokenizerConfig(true, true, "bo");
		  BreakIterator boundary = config.TROMBONE_WORD_BREAK_ITERATOR;
		  String text = "ཆུ་ཡོད་མ་རེད། ཁང་པ་བརྗེ་བོ་བརྒྱབ་དང་ལབ་ཀྱི་འདུག་ར། ཁང་པ་བརྗེ་བོ་བརྒྱབ་ན་ང་ཚོ་འདིའི་རྒྱབ་ལོགས་འདི་ལ། ཁང་པ་ཉི་མ་ཁ་ཤས་ཤིག་ལ་མི་སླེབས་པ་ཡོད་ལབ་ཡིན་པ one་two";
		  boundary.setText(text);

	     int start = boundary.first();
	     for (int end = boundary.next();
	          end != BreakIterator.DONE;
	          start = end, end = boundary.next()) {
	          System.out.println(text.substring(start,end));
	     }
	  }
}
