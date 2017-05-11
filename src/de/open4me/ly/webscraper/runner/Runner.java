package de.open4me.ly.webscraper.runner;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.Page;

import de.open4me.ly.webscraper.runner.htmlunit.HUEngine;
import de.open4me.ly.webscraper.runner.phantomjsdriver.PjsEngine;
import de.open4me.ly.webscraper.utils.Parsing;
import de.willuhn.logging.Logger;

public abstract class Runner {

	public class ResultSets {

		public String command;
		public String[] parts;
		public String action;
		public Page page;
		public String txt;
		public String htmlcode;
		public String output;
		public URL url;
		public Exception e;

		public String toString() {
			return command;
		}

	}

	private String[] codelines;

	public Runner() {

	}

	public void addVar(String key, String value) {
		variables.put(key, value);
	}
	public Runner(String code)  {
		setCode(code);
	}

	public void setCode(String code) {
		codelines = code.split("\n");
	}

	public void setCode(String[] code) {
		codelines = code;
	}

	protected Map<String, String> variables = new HashMap<String, String>(); 

	static Pattern befehlMitKlammer = Pattern.compile("([a-zA-Z]*)" + Pattern.quote("(") + "(.*)" + Pattern.quote(")"));
	static Pattern textMitAnfuehrungstriche = Pattern.compile("\"(.*?)\".*");
	Pattern removeattribut = Pattern.compile("\"(.*)\" from (.*)");
	private ArrayList<ResultSets> results;
	private ArrayList<Page> downloads = new ArrayList<Page>();
	Engine engine;
	
	public boolean run()  {
		results = new ArrayList<ResultSets>(); 

		ResultSets r = null;
		try {
			int nr = 0;
			for (String origline : codelines) {
				setProgress(nr * (100/codelines.length), origline);
				nr++;
				if (origline.trim().isEmpty() || origline.startsWith("#")) {
					continue;
				}
				System.out.println("Executing: " + origline);
				String codeline = enrich(origline);
				String[] parts = codeline.split(" ");
				r = new ResultSets();
				r.command = codeline;
				r.parts = parts;
				results.add(r);
				String rest = codeline.substring(parts[0].length() + 1);
				switch (parts[0].toLowerCase()) {
				case "engine":
					switch (rest.toLowerCase()) {
						case "htmlunit": engine = new HUEngine(); break;
						case "phantomjsdriver": engine = new PjsEngine(); break;
						default:
							throw new IllegalStateException("Unbekannte Engine: " + rest);
					}
					break;
				case "cfg":
					Pattern cfgPat = Pattern.compile("(.*)=(.*)");
					Matcher m = cfgPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig1: " + rest);
					}
					getEngine().setCfg(m.group(1), m.group(2));
					break;
				case "open":
					String openurl = Parsing.extractTextAusAnf(parts[1]);
					r.action = "Open " + openurl;
					getEngine().open(r, openurl);
					break;

				case "set":
					Pattern setPat = Pattern.compile("(.*) to value (.*)");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					String value = Parsing.extractTextAusAnf(m.group(2));
					getEngine().set(r, m.group(1), value);
					break;

				case "click":
					getEngine().click(r, rest);
					break;

				case "extract":
					setPat = Pattern.compile("(.*) split by (.*)");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					getEngine().extract(r, m.group(1), m.group(2));
					
					downloads.add(r.page);
					break;

				case "download":
					getEngine().download(r, rest);
					downloads.add(r.page);
					break;
				case "downloadfromurl":
					getEngine().downloadfromurl(r, rest);
					downloads.add(r.page);
					break;
//				case "removeattribute":
//					m = removeattribut.matcher(rest);
//					if (!m.matches()) {
//						throw new IllegalStateException("Befehl ist ungültig");
//					}
//					String attrName = m.group(1);
//					String get = m.group(2);
//					getEngine().removeAttribute(r, attrName, get);
//					break;

				case "assertexists":
					String fehlermeldung = Parsing.extractTextAusAnf(rest);
					rest = rest.substring(fehlermeldung.length() + 2 + 1); // +2 wegen den fehlenden Anführungsstrichen in fehlermeldung
					if (rest.trim().isEmpty()) {
						throw new IllegalStateException("Zweiter Teil des Befehles nicht gefunden! " + codeline);
					}
					if (!getEngine().assertexists(r, rest)) {
						throw new IllegalStateException(fehlermeldung);
					}
					results.remove(r); // Assertexists werden nicht gespeichert
					break;
				case "if":
					setPat = Pattern.compile("(exists) (.*) goto (.*)");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					break;
				default:
					throw new IllegalStateException("Unbekannter Befehl: " + codeline);
				}
				if (r.e != null) {
					r.e.printStackTrace();
					throw r.e;
				}
				//Thread.sleep(3000);
				getEngine().enrichWithDebuginfo(r);
			}
			setProgress(100, "Finish");
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
			Logger.error("", e);
			r.e = e;
			setProgress(100, e.toString());
			return false;
		}
		finish();
		return true;

	}

	private Engine getEngine() {
		if (engine == null) {
			engine = new HUEngine();
		}
		if (!engine.isinit) {
			engine.init();
		}
		return engine;
	}

	private String enrich(String codeline) {
		if (codeline.contains("${")) {
			for (Entry<String, String> var : variables.entrySet()) {
				String key = "${" + var.getKey() + "}";
				if (codeline.contains(key)) {
					String value = var.getValue();
					if (value == null || value.isEmpty()) {
						throw new IllegalStateException("Variable " + var.getKey() + " ist leer! Bitte Feld füllen!");
					}
					codeline = codeline.replace(key, value);
				}
			}
		}
		return codeline;
	}

	
	protected void finish() {

	}

	protected void setProgress(int i, String codeline) {

	}

	public ArrayList<ResultSets> getResults() {
		return results;
	}


	public ArrayList<Page> getDownloads() {
		return downloads;
	}

	public void setDownloads(ArrayList<Page> downloads) {
		this.downloads = downloads;
	}

	public void setInfo(HashMap<String, String> info) {
		variables = info;
	}

}
