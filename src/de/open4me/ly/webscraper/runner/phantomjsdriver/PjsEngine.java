package de.open4me.ly.webscraper.runner.phantomjsdriver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

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
			elements = getElements(x, "getbyxpath(\".//ancestor::form\")");
			if (elements.size() == 0) {
				throw new IllegalStateException("Form not found!" + selector);
			}
			WebElement form = elements.get(0);
			System.out.println(form.getText() + " " + form.getTagName() + " ");
			List<WebElement> input = getElements(form, "getbyxpath(\".//input\")");
			String currentpage = driver.getCurrentUrl();
			String path = form.getAttribute("action");
			System.out.println(driver.getCurrentUrl());
			URIBuilder builder = null;
				try {
					builder = new URIBuilder(path);
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		//	builder.setPath(form.getAttribute("action"));
			for (WebElement xx : input) {
				builder = builder.addParameter(xx.getAttribute("name"), xx.getAttribute("value"));
				System.out.println(xx.getAttribute("name") + " " + xx.getAttribute("value"));
			}
	        System.out.println(builder.toString());
			System.out.println(elements);;
			downloadfromurl(r, builder.toString());
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

	@Override
	public boolean assertexists(ResultSets r, String selector) {
		List<?> elements = getElements(selector);
		return (elements.size() > 0);
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

	
}
