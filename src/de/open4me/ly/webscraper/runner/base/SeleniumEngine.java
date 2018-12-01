package de.open4me.ly.webscraper.runner.base;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import ch.racic.selenium.helper.download.FileData;
import ch.racic.selenium.helper.download.SeleniumDownloadHelper;
import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.open4me.ly.webscraper.runner.phantomjsdriver.PjsUtils;
import de.open4me.ly.webscraper.utils.Parsing;
import de.open4me.ly.webscraper.utils.StringPage;

public abstract class SeleniumEngine extends Engine {

	protected WebDriver driver;
	protected String language;
	
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
	public boolean assertexists(ResultSets r, String selector) {
		List<?> elements = getElements(selector);
		return (elements.size() > 0);
	}

	@Override
	public int count(ResultSets r, String selector) {
		List<?> elements = getElements(selector);
		return elements.size();
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
			String text = Parsing.extractTextAusAnf(m.group(2));
			return sc.findElements(By.partialLinkText(text));
		default:
			throw new IllegalStateException("Befehl " + m.group(1) + " ist unbekannt!");
		}
	}

	public Select getSelectionElement(String selector) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Element not found! " + selector);
		}
		return new Select(elements.get(0));
	}

	@Override
	public void click(ResultSets r, String rest) {
		TreeSet<String> knowntabs = new TreeSet<String>();
		knowntabs.addAll(driver.getWindowHandles());
		
		List<WebElement> elements = getElements(rest);
		if (elements.size() == 0) {

			throw new IllegalStateException("Kein Element gefunden! " + rest);
		}
		WebElement x = PjsUtils.getEnabledVisibled(elements);
		if (x == null) {
			throw new IllegalStateException("Kein Element (Visible & Enabled) gefunden!" + rest + " " + elements);
		}
		x.click();
		PjsUtils.waitForJSandJQueryToLoad(driver);
		if (driver.getWindowHandles().size() > knowntabs.size()) {
			System.out.println("#new Tabs: " + (driver.getWindowHandles().size() - knowntabs.size()));
			for (String tabs : driver.getWindowHandles()) {
				if (!knowntabs.contains(tabs)) {
					driver.switchTo().window(tabs);
					break;
				}
			}
		}
	}

	@Override
	public void open(ResultSets r, String openurl) {
		driver.get(openurl);
		PjsUtils.waitForJSandJQueryToLoad(driver);
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
	public void setCfg(String key, String value) {
		switch (key.toLowerCase()) {
		case "language":
			language = value;
			break;
		default:
			throw new IllegalStateException("Befehl ist ungültig. Diese Einstellung ist unbekannt: " + key);
		}
	}

	@Override
	public void closeWindow() {
		driver.close();
		String windowName = driver.getWindowHandles().toArray(new String[0])[0];
		driver.switchTo().window(windowName);
	}

	@Override
	public void download(ResultSets r, String selector, String charset) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden! " + selector);
		}
		WebElement x = elements.get(0);
		if ("form".equals(x.getTagName())) {
			handleFormDownloads(r, selector, x);
		} else if ("input".equals(x.getTagName()) && "submit".equals(x.getAttribute("type"))) {
			WebElement form = getForm(x); 
			handleFormDownloads(r, selector, form);
		} else if ("a".equals(x.getTagName())) {
			String url = x.getAttribute("href");
			if (url == null) {
				throw new IllegalStateException("href attribute missing");
			}
			downloadfromurl(r, url, charset);
		} else {
			throw new IllegalStateException("Not supported element: " + x.getTagName());
		}

	}

	private void handleFormDownloads(ResultSets r, String selector, WebElement form)  {
		Map<String, String> param = extractParamter(form);

		boolean isPost = "post".equalsIgnoreCase(form.getAttribute("method"));
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
			downloadfromurl(r, url.toString(), null);
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

	/**
	 * Verwandelt eine Parameter-Liste in die Post-Request-Notation
	 * @param param
	 * @return
	 */
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

	/**
	 * Iteriert über ein Form Element und extrahiert aus allen Input-Tag die relevanten Informationen
	 * @param form
	 * @return
	 */
	private Map<String, String> extractParamter(WebElement form) {
		Map<String,String> params = new LinkedHashMap<>();		
		for (WebElement xx : getElements(form, "getbyxpath(\".//input\")")) {
			if ("radio".equals(xx.getAttribute("type"))) {
				if (!"true".equals(xx.getAttribute("checked"))) {
					continue;
				}
			} 
			params.put(xx.getAttribute("name"), xx.getAttribute("value"));
		}
		return params;
	}

	@Override
	public void downloadfromurl(ResultSets r, String url, String charset) {
		try {
			if (charset == null) {
				charset = "UTF-8";
			}
			SeleniumDownloadHelper sdlh;
			sdlh = new SeleniumDownloadHelper(driver);
			FileData testFileData = sdlh.getFileFromUrlRaw(new URL(url));
			r.txt = new String(testFileData.getData(), charset);
			r.page = new StringPage(r.txt);
		} catch (IOException e) {
			r.e = e;
		}
	}




	@Override
	public void submit(String selector) {
		List<WebElement> elements = getElements(selector);
		if (elements.size() == 0) {
			throw new IllegalStateException("Kein Element gefunden! " + selector);
		}
		WebElement x = elements.get(0);
		x.submit();
	}

	
	
}
