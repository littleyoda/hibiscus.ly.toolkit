package de.open4me.ly.webscraper.runner.htmlunit;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.BrowserVersion.BrowserVersionBuilder;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.ImmediateRefreshHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.open4me.ly.webscraper.runner.base.Engine;
import de.open4me.ly.webscraper.utils.Parsing;
import de.open4me.ly.webscraper.utils.StringPage;

public class HUEngine extends Engine {

	private WebClient webClient;
	private HtmlPage page = null;
	private int jswait = 0;
	private Map<String, String> cfgs;

	@Override
	public void init() {
		super.init();
		cfgs = new HashMap<String, String>();
	}


	private WebClient getwebClient() {
		if (webClient != null) {
			return webClient;
		}
		BrowserVersionBuilder bv = new BrowserVersion.BrowserVersionBuilder(BrowserVersion.BEST_SUPPORTED);
		if (cfgs.containsKey("language")) {
			String value = cfgs.get("language");
			bv = bv.setBrowserLanguage(value).setSystemLanguage(value).setUserLanguage(value);
			cfgs.remove("language");
		}
		if (cfgs.containsKey("browser")) {
			String value = cfgs.get("browser");
			bv = bv.setUserAgent(value);
			cfgs.remove("browser");
		}
		webClient = new WebClient(bv.build());
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		webClient.getOptions().setJavaScriptEnabled(true);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.setCssErrorHandler(new SilentCssErrorHandler());
		for (Entry<String, String> x : cfgs.entrySet()) {
			applyCfgs(x.getKey(), x.getValue());
		}
		cfgs = null;
		return webClient;
	}

	@Override
	public void setCfg(String key, String value) {
		switch (key.toLowerCase()) {
		case "browser":
		case "waitforjavascript":
		case "javascript":
		case "css":
		case "ajax":
		case "language":
		case "redirect":
		case "refresh":
		case "showversion":
			if (cfgs != null) {
				// Config-File zwischenspeichern
				cfgs.put(key, value);
			} else {
				// Webclient ist bereits instanziert worden. Config direkt setzten 
				applyCfgs(key, value);
			}
			break;
		default:
			throw new IllegalStateException("Befehl ist ungültig. Diese Einstellung ist unbekannt: " + key);
		}
	}

	public void applyCfgs(String key, String value) {
		switch (key.toLowerCase()) {
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
			throw new IllegalStateException("Befehl ist ungültig. Diese Einstellung ist unbekannt oder wurde zu spät gesendet: " + key);
		}
	}

	@Override
	public void open(ResultSets r, String openurl) {
		try {
			page = getwebClient().getPage(openurl);
			if (jswait > 0) {
				getwebClient().waitForBackgroundJavaScript(jswait);
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
			List<?> list = getElements((HtmlElement) zeile, split);
			if (list.size() == 0) {
				continue;
			}
			out += i;
			for (Object spalten : list) {
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
	public void download(ResultSets r, String rest, String charset) {
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
			if (p instanceof UnexpectedPage) {
				UnexpectedPage u = (UnexpectedPage) p;
				if (charset == null) {
					r.txt = u.getWebResponse().getContentAsString();
				} else {
					Charset c = Charset.forName(charset);
					r.txt = u.getWebResponse().getContentAsString(c);
				}
				r.page = new StringPage(r.txt);
			}
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

	@Override
	public void closeWindow() {
		throw new IllegalStateException("Nicht implementiert");
	}

	public void fixdh4096() {
		// Extract all Cipher Suites
		SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket soc;
		try {
			soc = (SSLSocket) factory.createSocket();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Fix DH4096 konnte nicht ausgeführt werden." + e.getMessage());
		}
		String[] protocols = soc.getEnabledCipherSuites();

		// Remove all DH Cipher Suites
		ArrayList<String> list = new ArrayList<>(Arrays.asList(protocols));
		List<String> newlist = list.stream()
				.filter(p -> !(p.contains("_DHE_") ))
				.collect(Collectors.toList());
		// Set the filtered list as default for HTMLUnit
		getwebClient().getOptions().setSSLClientCipherSuites(newlist.toArray(new String[newlist.size()]));

	}


	@Override
	public void submit(String selector) {
		throw new IllegalStateException("Nicht implementiert");
	}

}
