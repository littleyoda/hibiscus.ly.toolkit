package de.open4me.ly.webscraper.runner;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.gargoylesoftware.htmlunit.Page;

import de.open4me.ly.webscraper.runner.base.Engine;
import de.open4me.ly.webscraper.runner.chromejsdriver.CEngine;
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
			String jumpto = "";
			while (nr < codelines.length) {
				String origline = codelines[nr].trim();
				setProgress(nr * (100/codelines.length), origline);
				if (origline.trim().isEmpty() || origline.startsWith("#") || origline.startsWith(":")) {
					nr++;
					continue;
				}
				System.out.println("Executing: " + origline);
				String codeline = enrich(origline);
				String[] parts = codeline.split(" ");
				r = new ResultSets();
				r.command = codeline;
				r.parts = parts;
				results.add(r);
				String rest = "";
				if (codeline.length() > (parts[0].length() + 1)) {
					rest = codeline.substring(parts[0].length() + 1);
				}
				switch (parts[0].toLowerCase()) {
				case "engine":
					switch (rest.toLowerCase()) {
					case "htmlunit": engine = new HUEngine(); break;
					case "phantomjsdriver": engine = new PjsEngine(); break;
					case "chromedriver": engine = new CEngine(); break;
					default:
						throw new IllegalStateException("Unbekannte Engine: " + rest);
					}
					break;
				case "var":
					Pattern varPat = Pattern.compile("(.*)=(.*)");
					Matcher mvar = varPat.matcher(rest);
					if (!mvar.matches()) {
						throw new IllegalStateException("Befehl ist ungültig1: " + rest);
					}
					addVar(mvar.group(1), mvar.group(2));
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
					setPat = Pattern.compile("(.*) split by (.*) as \"(.*?)\"");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						setPat = Pattern.compile("(.*) split by (.*)");
						m = setPat.matcher(rest);
					}
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					if (m.groupCount() == 3) {
						System.out.println(m.group(3));
					}
					getEngine().extract(r, m.group(1), m.group(2));
					downloads.add(r.page);
					break;

				case "download":
					String charset = null;
					String xpath = null;
					setPat = Pattern.compile("(.*) charset \"(.*?)\"");
					m = setPat.matcher(rest);
					if (m.matches()) {
						xpath = m.group(1);
						charset = m.group(2);
					} else {
						xpath = rest;
					}
					getEngine().download(r, xpath, charset);
					System.out.println(r.page.toString());
					downloads.add(r.page);
					break;

				case "ask":
					setPat = Pattern.compile("\"(.*?)\" (.*)");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					System.out.println(m.group(1));
					System.out.println(m.group(2));
					List<ImmutablePair<String, String>> pp = getEngine().getOptions(r, m.group(2));
					ImmutablePair<String, String> auswahl = askFeedback(m.group(1), pp);
					engine.setOptionByText(r, m.group(2), auswahl.right);
					break;

				case "downloadfromurl":
					getEngine().downloadfromurl(r, rest);
					downloads.add(r.page);
					break;

				case "closewindow":
					getEngine().closeWindow();
					break;
					
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
					
				case "assertnotexists":
					fehlermeldung = Parsing.extractTextAusAnf(rest);
					rest = rest.substring(fehlermeldung.length() + 2 + 1); // +2 wegen den fehlenden Anführungsstrichen in fehlermeldung
					if (rest.trim().isEmpty()) {
						throw new IllegalStateException("Zweiter Teil des Befehles nicht gefunden! " + codeline);
					}
					if (getEngine().assertexists(r, rest)) {
						throw new IllegalStateException(fehlermeldung);
					}
					results.remove(r); // Assertexists werden nicht gespeichert
					break;

				case "sleep":
					Thread.sleep(5000);
					break;

				case "submit":
					getEngine().submit(rest);
					break;
					
				case "if":
					setPat = Pattern.compile("(exists|not exists) (.*) goto (.*)");
					m = setPat.matcher(rest);
					if (!m.matches()) {
						throw new IllegalStateException("Befehl ist ungültig");
					}
					int count = getEngine().count(r, m.group(2));
					r.txt = "Count: " + count;
					boolean jump;
					switch (m.group(1)) {
					case "exists": jump = (count > 0); break;
					case "not exists": jump = (count == 0); break;
					default: throw new IllegalStateException("Unbekannte If-Bedingung: " + m.group(1));
					}
					if (jump) {
						jumpto = m.group(3);
					}
					break;
					
				case "fix":
					switch (rest.toLowerCase()) {
					case "dh4096":
						if (engine instanceof HUEngine) {
							((HUEngine) engine).fixdh4096();
						} else {
							throw new IllegalStateException("Fix wird für diese Engine nicht unterstützt." + rest);
						}
						break;
					default:
						throw new IllegalStateException("Unbekannter Fix" + rest);
					}
					break;

				default:
					throw new IllegalStateException("Unbekannter Befehl: " + codeline);
				}
				//Thread.sleep(3000);
				getEngine().enrichWithDebuginfo(r);
				if (jumpto.isEmpty()) {
					nr++;
				} else {
					String sprungmarke = ":" + jumpto.toLowerCase();
					nr = -1;
					for (int i = 0; i < codelines.length; i++) {
						if (sprungmarke.equals(codelines[i].toLowerCase().trim())) {
							nr = i + 1;
							break;
						}
					}
					jumpto = "";
					if (nr == -1) {
						throw new IllegalStateException("Spruchmarke " + sprungmarke + " nicht gefunden!");
					}
				}
				if (r.e != null) {
					r.e.printStackTrace();
					throw r.e;
				}
			}
			setProgress(100, "Finish");
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
			Logger.error("", e);
			r.e = e;
			setProgress(100, e.toString());
		}
		finally {
			finish();
		}
		return r.e == null;

	}


	private Engine getEngine() {
		// default
		if (engine == null) {
			engine = new HUEngine();
		}
		// Init wenn nötig
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

	public ImmutablePair<String, String> askFeedback(String configName, List<ImmutablePair<String, String>> pp) {
		throw new IllegalStateException("AskFeedback nicht implementiert");

	}

}
