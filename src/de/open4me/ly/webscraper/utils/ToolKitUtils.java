package de.open4me.ly.webscraper.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import de.willuhn.logging.Logger;


public class ToolKitUtils {

	static public BigDecimal betrag2BigDecimal(String value) throws ParseException {
		String l1 = "de";
		String l2 = "DE";
		char kommaZeichen = '\0';
		for (int idx = value.length() - 1; idx > 0; idx--) {
			char c = value.charAt(idx);
			if (!Character.isDigit(c)) {
				kommaZeichen = c;
				break;
			}
		}
		switch (kommaZeichen) {
			case '\0':
			case ',':
				// Use Default
				break;
			case '.':
				l1 = "en";
				l2 = "US";
				break;
			default:
				Logger.error("Unbekanntes Zeichen in einer Zahl gefunden: " + kommaZeichen);
		}
		return betrag2BigDecimal(value, l1, l2);
	}

	static public BigDecimal betrag2BigDecimal(String value, String formatlocale, String formatlocale2 ) throws ParseException {
		if (value.startsWith("+")) {
			value = value.substring(1);
		}
        Locale in_ID = new Locale(formatlocale,formatlocale2);
        DecimalFormat nf = (DecimalFormat)NumberFormat.getInstance(in_ID);
        nf.setParseBigDecimal(true);
        return (BigDecimal) nf.parse(value);
	}
	
	  // Zerlegt einen String intelligent in max. 27 Zeichen lange Stücke
	  public static String[] parse(String line)
	  {
	    if (line == null || line.length() == 0)
	      return new String[0];
	    List<String> out = new ArrayList<String>();
	    String rest = line.trim();
	    int lastpos = 0;
	    while (rest.length() > 0) {
	    	if (rest.length() < 28) {
	    		out.add(rest);
	    		rest = "";
	    		continue;
	    	}
	    	int pos = rest.indexOf(' ', lastpos + 1);
	    	boolean zulang = (pos > 28) || pos == -1;
	    	// 1. Fall: Durchgehender Text mit mehr als 27 Zeichen ohne Space
	    	if (lastpos == 0 && zulang) {
	    		out.add(rest.substring(0, 27));
	    		rest = rest.substring(27).trim();
	    		continue;
	    	} 
	    	// 2. Fall Wenn der String immer noch passt, weitersuchen
	    	if (!zulang) {
	    		lastpos = pos;
	    		continue;
	    	}
	    	// Bis zum Space aus dem vorherigen Schritt den String herausschneiden
	    	out.add(rest.substring(0, lastpos));
	    	rest = rest.substring(lastpos + 1).trim();
	    	lastpos = 0;
	    }
	    return out.toArray(new String[0]);
	  }


//	public static void main(String [] args)
//	{
//		String[] s = new String[]{
//				"1", "1 2", "123456789012345678901234567890",
//				"123456789012345678901234567 890",
//				"1234567890123456789012345678 90",
//				"123456789012345678901234567 890",
//				"123456789012345678901234567 890 123456789012345678901234567 3342",
//				};
//		for (String t : s) {
//			System.out.println(t + ": " + Arrays.toString(parse(t)));
//		}
//	}

		
		/**
		 * - Tausender Punkte entfernen
		 * - Komma durch Punkt ersetzen
		 * @param s
		 * @return
		 */
		public static float string2float(String s) {
			try {
				return Float.parseFloat(s.replace(".", "").replace(",", "."));
			} catch (Exception e) {
				throw new RuntimeException("Cannot convert " + s + " to float");
			}

		}
	
}
