package de.open4me.ly.webscraper.runner.htmlunit;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

public class HUUtils {


	public static void setProxyCfg(WebClient webClient, String url)  {
		boolean useSystem = Application.getConfig().getUseSystemProxy();

		ProxyConfig pc = null;
		if (useSystem) {
			try {
				List<Proxy> proxies = ProxySelector.getDefault().select(new URI(url));
				Logger.info("Using system proxy settings: " + proxies);
				for (Proxy p : proxies) {
					if (p.type() == Proxy.Type.HTTP && p.address() instanceof InetSocketAddress) {
						pc = new ProxyConfig();
						InetSocketAddress addr = (InetSocketAddress) p.address();
						pc.setProxyHost(addr.getHostString());
						pc.setProxyPort(addr.getPort());
						webClient.getOptions().setProxyConfig(pc);
						Logger.info("Setting Proxy to " + pc);
						return;
					}
				}
				Logger.error("No default Proxy found");
			} catch (URISyntaxException e) {
				Logger.error("No default Proxy found", e);
			}
		} else {
			String host = Application.getConfig().getHttpsProxyHost();
			int port = Application.getConfig().getHttpsProxyPort();
			if (host != null && host.length() > 0 && port > 0) {
				pc = new ProxyConfig();
				pc.setProxyHost(host);
				pc.setProxyPort(port);
				webClient.getOptions().setProxyConfig(pc);
				Logger.info("Setting Proxy to " + pc);
				return;
			}
		}
		Logger.info("Keine gültige Proxy-Einstellunge gefunden. (" + useSystem + ")");
	}

	public static HtmlAnchor getLinksByLinkText(HtmlPage page, String search) {
		for (HtmlAnchor x : page.getAnchors()) {
			if (x.asText().contains(search)) {
				return x;
			}
		}
		return null;

	}

	public static void tabUntereinander2hash(HashMap<String, String> infos, HtmlTable tab, int idxname, int idxvalue) {
		for (HtmlTableRow row :tab.getRows()) {
			List<HtmlTableCell> cells = row.getCells();
			if (cells.size() < Math.max(idxname, idxvalue)) {
				Logger.info("Warnung. Ungültige Anzahl an Zellen: " + cells.size() + " " + row.asText());
				continue;
			}
			infos.put(cells.get(idxname).asText().toLowerCase(), cells.get(idxvalue).asText().trim());
		}
	}

	public static void tabNebeneinander2hash(HashMap<String, String> infos, HtmlTable tab) {
		List<HtmlTableRow> rows = tab.getRows();
		if (rows.size() < 2) {
			System.out.println("Warnung. Ungültige Anzahl an Zeilen: " + rows.toString());
			return;
		}
		List<HtmlTableCell> r1 = rows.get(0).getCells();
		for (int zeile = 1; zeile < rows.size(); zeile++) {
			List<HtmlTableCell> r2 = rows.get(zeile).getCells();
			if (r1.size() != r2.size()) {
				continue;
			}
			int missing=0;
			for (int i = 0; i < r1.size(); i++) {
				String header = r1.get(i).asText().toLowerCase();
				if ("".equals(header)) {
					header = "Missing" + missing;
					missing++;
				}
				infos.put(header, r2.get(i).asText().trim());
			}
		}
	}

	public static List<HtmlAnchor> getLinks(HtmlPage page) {
		ArrayList<HtmlAnchor> l = new ArrayList<HtmlAnchor>();
		for (DomElement e : page.getElementsByTagName("a")) {
			if (e instanceof HtmlAnchor) {
				HtmlAnchor ahref = (HtmlAnchor) e;
				l.add(ahref);
			}
		}
		return l;
	}


}
