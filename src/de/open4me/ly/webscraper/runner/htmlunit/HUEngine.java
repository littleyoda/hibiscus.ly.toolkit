package de.open4me.ly.webscraper.runner.htmlunit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ImmediateRefreshHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import de.open4me.ly.webscraper.runner.Engine;
import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.open4me.ly.webscraper.utils.Parsing;
import de.open4me.ly.webscraper.utils.StringPage;

public class HUEngine extends Engine {

	private WebClient webClient;
	private HtmlPage page = null;
	private int jswait = 0;

	@Override
	public void init() {
		super.init();
		webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
	}

	@Override
	public void setCfg(String key, String value) {
		switch (key.toLowerCase()) {
		case "browser":
			webClient.getBrowserVersion().setUserAgent(value);
			break;
		case "waitforjavascript":
			jswait = Integer.parseInt(value) * 1000;
			break;
		case "javascript":
			webClient.getOptions().setJavaScriptEnabled(Boolean.parseBoolean(value));
			break;
		case "css":
			webClient.getOptions().setCssEnabled(Boolean.parseBoolean(value));
			break;
		case "ajax":
			if (Boolean.parseBoolean(value)) {
				webClient.setAjaxController(new NicelyResynchronizingAjaxController());
			} 
			break;
		case "language":
			webClient.getBrowserVersion().setBrowserLanguage(value);
			webClient.getBrowserVersion().setSystemLanguage(value);
			webClient.getBrowserVersion().setUserLanguage(value);
			break;
		case "redirect":
			webClient.getOptions().setRedirectEnabled(Boolean.parseBoolean(value));
			break;
		case "refresh":
			if ("true".equals(value.toLowerCase())) {
				webClient.setRefreshHandler(new ThreadedRefreshHandler());
			} else if ("immediately".equals(value.toLowerCase())) {
				webClient.setRefreshHandler(new ImmediateRefreshHandler());
			}
			break;
		case "showversion":
			break;

		default:
			throw new IllegalStateException("Befehl ist ungültig. Diese Einstellung ist unbekannt: " + key);
		}
	}

	@Override
	public void open(ResultSets r, String openurl) {
		try {
			page = webClient.getPage(openurl);
			if (jswait > 0) {
				webClient.waitForBackgroundJavaScript(jswait);
			}
			r.page = page.cloneNode(true);
			r.url = page.getUrl();
		} catch (FailingHttpStatusCodeException | IOException e) {
			e.printStackTrace();
			r.e = e;
		}
	}

	
	@SuppressWarnings("rawtypes")
	public static List<?> getElements(HtmlPage page, String string) {
		Matcher m = Parsing.befehlMitKlammer.matcher(string);
		if (!m.matches()) {
			throw new IllegalStateException("Befehl ist ungültig. Klammerstrukt passt nicht. >" + string + "<");
		}
		switch (m.group(1).toLowerCase()) {
		case "getbyname":
			return page.getElementsByName(Parsing.extractTextAusAnf(m.group(2)));
		case "getbyid":
			Object o = page.getElementById(Parsing.extractTextAusAnf(m.group(2)));
			if (o == null) {
				return new ArrayList();
			}
			return Arrays.asList(new Object[] { o });
		case "getbyxpath":
			return page.getByXPath(Parsing.extractTextAusAnf(m.group(2)));
		case "getbytext":
			String text = Parsing.extractTextAusAnf(m.group(2));
			List<HtmlElement> elements = getAllHtmlElements(page);
			for (Iterator i = elements.listIterator(); i.hasNext();) {
				HtmlElement element = (HtmlElement) i.next();
				if (element.getChildElementCount() > 0 || !element.getTextContent().equals(text)) {
					i.remove();						
				}
			}
			return elements; 
		default:
			throw new IllegalStateException("Befehl " + m.group(1) + " ist unbekannt!");
		}
	}
	
	private static List<HtmlElement> getAllHtmlElements(HtmlPage page) {
		List<HtmlElement> out = new ArrayList<HtmlElement>();
		for (Object o : page.getByXPath("//*")) {
			if (!(o instanceof HtmlElement)) {
				continue;
			}
			out.add((HtmlElement) o);
		}
		return out;

	}

	@Override
	public void set(ResultSets r, String selector, String value) {
		List<?> elements = getElements(page, selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Element " + selector + " nicht gefunden");
		}
		for (Object x : elements) {
//			String value = extractTextAusAnf(m.group(2));
			if (x instanceof HtmlInput) {
				((HtmlInput) x).setAttribute("value", value);
			} else if (x instanceof HtmlSelect) {
				((HtmlSelect) x).setSelectedAttribute(value, true);
			} else {
				throw new IllegalStateException("Element nicht vom Typ HtmlInput.\n" + x.getClass() + "\n" + x.toString() +"\n" + elements);
			}
		}
		r.action = "Set " + elements +" to '" +  value + "'";
		r.page = page.cloneNode(true);
		r.url = page.getUrl();
	}

	@Override
	public void click(ResultSets r, String rest) {
		List<?> elements = getElements(page, rest);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + rest);
		}
		Object x = elements.get(0);
		if (!(x instanceof HtmlElement)) {
			throw new IllegalStateException("Element nicht vom Typ HtmlInput");
		}
		HtmlElement e = ((HtmlElement) x);
		r.action = "Click on first of the following list " + elements;
		try {
			page = e.click();
			r.page = page.cloneNode(true);
			r.url = page.getUrl();
		} catch (IOException e1) {
			r.e = e1;
		}
	}

	@Override
	public void extract(ResultSets r, String selector, String split) {
		List<?> elements = getElements(page, selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + selector);
		}
		String out = "";
		int i = 0;
		for (Object zeile : elements) {
			out += i;
			for (Object spalten : getElements((HtmlElement) zeile, split)) {
				String c = ((HtmlElement) spalten).getTextContent();
				c = c.trim().replace('\r', ' ').replace('\t', ' ').replace('\n', ' ').replaceAll("\"", "'").trim();
				out +=  ",\"" + c + "\"";
			}
			out += System.lineSeparator();
			i++;
		}
		r.output = out;
		r.txt = out;
		r.page = new StringPage(out);
	}
	

	public static List<?> getElements(HtmlElement page, String string) {
		Matcher m = Parsing.befehlMitKlammer.matcher(string);
		if (!m.matches()) {
			throw new IllegalStateException("Befehl ist ungültig. Klammerstrukt passt nicht. >" + string + "<");
		}
		switch (m.group(1).toLowerCase()) {
		//			case "getbyname":
		//				return page.getElementsByAttribute("name", extractTextAusAnf(m.group(2)));
		//			case "getbyid":
		//				Object o = page.getht.getElementById(extractTextAusAnf(m.group(2)));
		//				if (o == null) {
		//					return new ArrayList();
		//				}
		//				return Arrays.asList(new Object[] { o });
		case "getbyxpath":
			return page.getByXPath(Parsing.extractTextAusAnf(m.group(2)));
			//			case "getbytext":
			//				String text = extractTextAusAnf(m.group(2));
			//				List<HtmlElement> elements = getAllHtmlElements(page);
			//				for (Iterator i = elements.listIterator(); i.hasNext();) {
			//					HtmlElement element = (HtmlElement) i.next();
			//					if (element.getChildElementCount() > 0 || !element.getTextContent().equals(text)) {
			//						i.remove();						
			//					}
			//				}
			//				return elements; 
		default:
			throw new IllegalStateException("Befehl " + m.group(1) + " ist für HtmlElemente unbekannt!");
		}
	}

	@Override
	public void download(ResultSets r, String rest) {
		List<?> elements = getElements(page, rest);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + rest);
		}
		Object dl = elements.get(0);
		if (!(dl instanceof HtmlElement)) {
			throw new IllegalStateException("Element nicht vom Typ HtmlInput");
		}
		r.action = "Click on first of the following list " + elements;
		try {
			Page p;
			p = ((HtmlElement) dl).click();
			r.page = p;
			r.url = p.getUrl();
		} catch (IOException e) {
			r.e = e;
		}
	}

	@Override
	public boolean assertexists(ResultSets r, String rest) {
		List<?> elements = getElements(page, rest);
//		Logger.error("assertExists: " + fehlermeldung + " " + rest + " " + elements.toString());
		return (elements.size() > 0);
	}

	@Override
	public int count(ResultSets r, String rest) {
		List<?> elements = getElements(page, rest);
//		Logger.error("assertExists: " + fehlermeldung + " " + rest + " " + elements.toString());
		return elements.size();
	}
	
	
	@Override
	public void enrichWithDebuginfo(ResultSets r) {
		if (page != null) {
			r.htmlcode = page.asXml();
			if (r.txt == null) {
				r.txt = page.asText();
			}
		}
	}

	@Override
	public void downloadfromurl(ResultSets r, String url) {
		throw new IllegalStateException("Nicht implementiert");
	}

	@Override
	public void close() {
		try {
			if (webClient != null) {
				webClient.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<ImmutablePair<String,String>>  getOptions(ResultSets r, String selector) {
		throw new IllegalStateException("Nicht implementiert");
	}

	
	@Override
	public void setOptionByText(ResultSets r, String selector, String optiontext) {
		throw new IllegalStateException("Nicht implementiert");
	}

//	@Override
//	public void removeAttribute(ResultSets r, String attrName, String get) {
//		List<?> elements = getElements(page, get);
//		for (Object o : elements) {
//			if (!(o instanceof HtmlElement)) {
//				throw new IllegalStateException("Element nicht vom Typ HtmlElement.\n");
//			}
//			((HtmlInput) o).removeAttribute(attrName);
//		}
//	}

	
	
}
