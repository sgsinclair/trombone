/**
 * 
 */
package org.voyanttools.trombone.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.QueryParser;

/**
 * @author sgs
 *
 */
public class JsoupHelper {
	
	private static Pattern AttributeSelector = Pattern.compile("@(\\p{L}+)\\s*$");

	/**
	 * 
	 */
	public JsoupHelper() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Get a single string for all elements matching the selector. There's special
	 * treatment of the selector when it ends with \@attribute in which case the
	 * attribute's value is used instead (since there's no way with selectors to grab
	 * the value directly). Note that only distinct/unique values are kept.
	 * @param element the Jsoup element to be queried
	 * @param selector a CSS-type selector
	 * @return
	 */
	public static String getQueryValue(Element element, String selector) {
		String[] values = getQueryValues(element, selector);
		return values !=null ? Arrays.asList(values).stream().distinct().collect(Collectors.joining()) : null;
	}
	
	/**
	 * Get the string values for all elements matching the selector. There's special
	 * treatment of the selector when it ends with \@attribute in which case the
	 * attribute's value is used instead (since there's no way with selectors to grab
	 * the value directly)
	 * @param element the Jsoup element to be queried
	 * @param selector a CSS-type selector
	 * @return
	 */
	public static String[] getQueryValues(Element element, String selector) {
		String select = selector.trim();
		Matcher matcher = AttributeSelector.matcher(select);
		if (matcher.find()) {
			select = select.substring(0, matcher.start()).trim();
			String attr = matcher.group(1);
			return element.select(select).eachAttr(attr).toArray(new String[0]);
		} else {
			return element.select(select).eachText().toArray(new String[0]);
		}
	}
	
	public static void main(String[] args) {
		Evaluator e = QueryParser.parse("title, title@test");
		System.err.println(e);
	}

}
