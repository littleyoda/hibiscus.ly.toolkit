package de.open4me.ly.webscraper.runner.phantomjsdriver;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.Select;

import ch.racic.selenium.helper.download.FileData;
import ch.racic.selenium.helper.download.SeleniumDownloadHelper;
import de.open4me.ly.webscraper.runner.Engine;
import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.open4me.ly.webscraper.utils.Parsing;
import de.open4me.ly.webscraper.utils.StringPage;

public class PjsEngine extends Engine {

	private PhantomJSDriver driver;
	private DesiredCapabilities caps;
	private String agentname = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:53.0) Gecko/20100101 Firefox/53.0";
	private String language;

	@Override
	public void init() {
		super.init();
		caps = new DesiredCapabilities();
		caps.setJavascriptEnabled(true);
		caps.setCapability("phantomjs.page.settings.userAgent", agentname);
		if (language != null) {
			caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Accept-Language", language);
		}
		caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, "/home/sven/devel/hibiscus/hibiscus.scripting.phantomjsdriver/lib/bin/linux64/phantomjs");
		driver = new org.openqa.selenium.phantomjs.PhantomJSDriver(caps);
	}

	@Override
	public void setCfg(String key, String value) {
		switch (key.toLowerCase()) {
		case "browser":
			agentname = value;
			break;
		case "language":
			language = value;
			break;
		default:
			throw new IllegalStateException("Befehl ist ungültig. Diese Einstellung ist unbekannt: " + key);
		}
	}

	@Override
	public void open(ResultSets r, String openurl) {
		driver.get(openurl);
		PjsUtils.waitForJSandJQueryToLoad(driver);
		//		try {
		//			Thread.sleep(1000);
		//			r.url = new URL(driver.getCurrentUrl());
		//		} catch (MalformedURLException | InterruptedException e) {
		//			e.printStackTrace();
		//		}
	}

	@Override
	public void set(ResultSets r, String selector, String group2) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + selector);
		}
		WebElement x = elements.get(0);
		x.clear();
		x.sendKeys(group2);
	}

	@Override
	public void click(ResultSets r, String rest) {
		List<WebElement> elements = getElements(rest);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + rest);
		}
		WebElement x = PjsUtils.getEnabledVisibled(elements);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element (Visible & Enabled) gefunden!" + rest + " " + elements);
		}
		x.click();
		PjsUtils.waitForJSandJQueryToLoad(driver);
	}

	@Override
	public void extract(ResultSets r, String selector, String split) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + selector);
		}
		String out = "";
		int i = 0;
		for (WebElement zeile : elements) {
			out += i;
			for (WebElement spalten : getElements(zeile, split)) {
				String c = spalten.getText();
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

	@Override
	public void downloadfromurl(ResultSets r, String url) {
		try {
			SeleniumDownloadHelper sdlh;
			sdlh = new SeleniumDownloadHelper(driver);
			FileData testFileData = sdlh.getFileFromUrlRaw(new URL(url));
			r.txt = new String(testFileData.getData(), "UTF-8");
			r.page = new StringPage(r.txt);
		} catch (IOException e) {
			r.e = e;
		}
	}


	@Override
	public void download(ResultSets r, String selector) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden!" + selector);
		}
		WebElement x = elements.get(0);
		if ("input".equals(x.getTagName()) && "submit".equals(x.getAttribute("type"))) {
			handleFormDownloads(r, selector, x);
		} else if ("a".equals(x.getTagName())) {
			downloadfromurl(r, x.getAttribute("href"));
		} else {
			throw new IllegalStateException("Not supported element");
		}
		////		List<WebElement> elements = getElements(selector);
		////		if (elements.size() == 0) {
		////			throw new IllegalStateException("Kein Element gefunden!" + selector);
		////		}
		////		WebElement x = elements.get(0);
		//		String useragent = caps.getCapability("phantomjs.page.settings.userAgent").toString();
		//		FileDownloader down = new FileDownloader(driver, useragent, language);
		//		try {
		////			String out = down.downloadFile(x);
		//			String out = down.downloadUrl(selector);
		//			r.htmlcode = out;
		//			r.txt = out;
		//		} catch (Exception e) {
		//			r.e = e;
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//
		////		try {
		////			Page p;
		////			p = ((HtmlElement) dl).click();
		////			r.page = p;
		////			r.url = p.getUrl();
		////		} catch (IOException e) {
		////			r.e = e;
		////		}
		////		throw new IllegalStateException("Nicht implementiert");

	}

	private void handleFormDownloads(ResultSets r, String selector, WebElement x)  {
		// TODO Check if x is already a form-Element
		WebElement form = getForm(x); 
		Map<String, String> param = extractParamter(form);

		boolean isPost = "post".equalsIgnoreCase(form.getAttribute("method"));
//		String currentpage = driver.getCurrentUrl();
		String path = form.getAttribute("action");
		if (isPost) {
			downloadpost(r, path, param);
		} else {
			String paramString = paramToString(param);
			if (path.contains("?")) {
				path += "&" + paramString;
			} else {
				path += "?" + paramString;
			}
			URL url;
			try {
				url = new URL(driver.getCurrentUrl());
				url = new URL(url, path);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				throw new IllegalStateException("Ungültige URL Kombination: " + driver.getCurrentUrl() + " / " + path);
			}
			System.out.println("URL: " + url);
			downloadfromurl(r, url.toString());
		}
	}

	private void downloadpost(ResultSets r, String path, Map<String, String> param) {
		try {
			String paramString = paramToString(param);
			SeleniumDownloadHelper sdlh;
			sdlh = new SeleniumDownloadHelper(driver);
			FileData testFileData = sdlh.getFileFromUrlRaw(new URL(path), "POST", paramString);
			r.txt = new String(testFileData.getData(), "UTF-8");
			r.page = new StringPage(r.txt);
		} catch (IOException e) {
			r.e = e;
		}
	}

	private String paramToString(Map<String, String> param) {
		return param.entrySet().stream()
				.map(
						s-> { 
							try { 
								return URLEncoder.encode(s.getKey(), "UTF-8") + "=" + URLEncoder.encode(s.getValue(), "UTF-8"); 
							} catch (UnsupportedEncodingException e) { throw new IllegalStateException(e.getMessage()); } 
						}
						).collect(Collectors.joining("&"));
	}

	private WebElement getForm(WebElement x) {
		List<WebElement> elements = getElements(x, "getbyxpath(\".//ancestor::form\")");
		if (elements.size() == 0) {
			throw new IllegalStateException("Form not found!");
		}
		return elements.get(0);
	}

	private Map<String, String> extractParamter(WebElement form) {
		Map<String,String> params = new LinkedHashMap<>();		
		for (WebElement xx : getElements(form, "getbyxpath(\".//input\")")) {
			params.put(xx.getAttribute("name"), xx.getAttribute("value"));
		}
		return params;
	}

	@Override
	public boolean assertexists(ResultSets r, String selector) {
		List<?> elements = getElements(selector);
		return (elements.size() > 0);
	}

	@Override
	public int count(ResultSets r, String selector) {
		List<?> elements = getElements(selector);
		return elements.size();
	}

	@Override
	public void enrichWithDebuginfo(ResultSets r) {
		if (r.txt != null || r.page != null || r.e != null) {
			return;
		}
		WebElement element = driver.findElement(By.tagName("body"));
		if (element == null) {
			r.txt = "Body Element not found";
		} else {
			r.txt = element.getText();
		}
		r.htmlcode = driver.getPageSource();
	}

	public List<WebElement> getElements(String selector) {
		return getElements(driver, selector);
	}


	public List<WebElement> getElements(SearchContext sc, String selector) {
		Matcher m = Parsing.befehlMitKlammer.matcher(selector);
		if (!m.matches()) {
			throw new IllegalStateException("Befehl ist ungültig. Klammerstrukt passt nicht. >" + selector + "<");
		}
		switch (m.group(1).toLowerCase()) {
		case "getbyname":
			throw new IllegalStateException("Nicht implementiert");
			//			return page.getElementsByName(Parsing.extractTextAusAnf(m.group(2)));
		case "getbyid":
			return sc.findElements(By.id(Parsing.extractTextAusAnf(m.group(2))));
		case "getbyxpath":
			return sc.findElements(By.xpath(Parsing.extractTextAusAnf(m.group(2))));
		case "getbytext":
			throw new IllegalStateException("Nicht implementiert");
			//			String text = Parsing.extractTextAusAnf(m.group(2));
			//			List<HtmlElement> elements = getAllHtmlElements(page);
			//			for (Iterator i = elements.listIterator(); i.hasNext();) {
			//				HtmlElement element = (HtmlElement) i.next();
			//				if (element.getChildElementCount() > 0 || !element.getTextContent().equals(text)) {
			//					i.remove();						
			//				}
			//			}
			//			return elements; 
		default:
			throw new IllegalStateException("Befehl " + m.group(1) + " ist unbekannt!");
		}
	}

	@Override
	public void close() {
		try {
			if (driver != null) {
				driver.close();
				driver.quit();
			}
			driver = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<ImmutablePair<String,String>>  getOptions(ResultSets r, String selector) {
		Select dropdown = getSelectionElement(selector); 		
		List<ImmutablePair<String,String>> params = new ArrayList<ImmutablePair<String,String>>();
		for (WebElement w : dropdown.getOptions()) {
			params.add(ImmutablePair.of(w.getAttribute("value"), w.getText()));
		}
		return params;
	}

	@Override
	public void setOptionByText(ResultSets r, String selector, String optiontext) {
		Select dropdown = getSelectionElement(selector); 		
		for (WebElement w : dropdown.getOptions()) {
			if (w.getText().equals(optiontext)) {
				w.click();
				return;
			}
		}
		throw new IllegalStateException("Option " + optiontext + " nicht gefunden!");
	}


	public Select getSelectionElement(String selector) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Element not found! " + selector);
		}
		return new Select(elements.get(0));
	}
}
