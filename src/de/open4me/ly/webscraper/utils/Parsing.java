package de.open4me.ly.webscraper.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parsing {

	public static Pattern befehlMitKlammer = Pattern.compile("([a-zA-Z]*)" + Pattern.quote("(") + "(.*)" + Pattern.quote(")"));
	public static Pattern textMitAnfuehrungstriche = Pattern.compile("\"(.*?)\".*");

	public static String extractTextAusAnf(String s) {
		Matcher m = textMitAnfuehrungstriche.matcher(s);
		if (!m.matches()) {
			throw new IllegalStateException("String nicht im Format '\"text\"' Text:" + s);
		}
		return m.group(1);

	}

}
