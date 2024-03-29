package de.open4me.ly.webscraper.runner.chromejsdriver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map.Entry;

import de.derrichter.finance.websync.connector.ChromeDriverWebClientInit;
import de.open4me.ly.webscraper.runner.Runner.ResultSets;
import de.open4me.ly.webscraper.runner.base.SeleniumEngine;
import de.open4me.ly.webscraper.utils.StringPage;
import de.willuhn.jameica.system.Application;

public class CEngine extends SeleniumEngine {

	private static File chromeDownloadsDir;
	
	@Override
	public void init(HashMap<String, String> cfg) {
		super.init(cfg);
		try {
			setChromeDriverPaths();
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException("ChromeDriver konnte nicht initalisiert werden!" + e.getMessage());
		}
		try {
			boolean appSysProxyUse = Application.getConfig().getUseSystemProxy();
			String appProxyHost = Application.getConfig().getProxyHost();
			int appProxyPort = Application.getConfig().getProxyPort();
			String appHttpsProxyHost = Application.getConfig().getHttpsProxyHost();
			int appHttpsProxyPort = Application.getConfig().getHttpsProxyPort();
			boolean show = !Boolean.parseBoolean(cfg.getOrDefault("show", "false"));
			cfg.remove("show");
			driver = ChromeDriverWebClientInit.connConfig(false, true, true, ExternalLogger.class, WebProgressMonitor.class, appSysProxyUse, appProxyHost, appProxyPort, appHttpsProxyHost, appHttpsProxyPort, false, "", "", show);
		}	
		catch (Exception webClientError) {
			throw new IllegalStateException("ChromeDriver konnte nicht initalisiert werden!" + webClientError.getMessage());
		}
		for (Entry<String, String> x : cfg.entrySet()) {
			setCfg(x.getKey(), x.getValue());
		}
	}

	
	public static void setChromeDriverPaths() throws Exception {

		try {
			/*
			 * ***************************************************************************
			 * ChromeDriver für Finance.WebSync vorbereiten ...
			 */
			//Logger.info("Bereite ChromeDriver vor und definiere Pfade ...");
			String osname = System.getProperty("os.name");
			String osarch = System.getProperty("os.arch");
			//Logger.info("OS-Info: " + osname + " " + osarch);

			String pluginspath = Application.getConfig().getUserPluginDir().getAbsolutePath();
			String chromeDriverSubsource = File.separator + "hibiscus.scripting.chromedriver" + File.separator + "bin" + File.separator;
			File chromeDriverBinaryPath = new File(pluginspath + chromeDriverSubsource);
			if (!chromeDriverBinaryPath.exists()) {

				pluginspath = Application.getConfig().getSystemPluginDir().getAbsolutePath();
				chromeDriverBinaryPath = new File(pluginspath + chromeDriverSubsource);
				if (!chromeDriverBinaryPath.exists()) {

					throw new Exception("ChromeDriver-Binary-Pfad konnte nicht gefunden werden! Installieren Sie ChromeDriver im Verzeichnis 'plugins'");
				}
			}
			String chromedriversource = pluginspath + chromeDriverSubsource;

			String osbinarypath = "";
			String osUserPath = System.getProperty("user.home");
			String osChromiumDir = null;
			String osChromiumBinary = null;

			if (osname.contains("Mac")) {
				osbinarypath = "macosx" + File.separator + "chromedriver";
				osChromiumDir = File.separator + "Users" + File.separator + "Shared" + File.separator + "Chrome-Headless" + File.separator;
				osChromiumBinary = osChromiumDir + "chrome-mac/Chromium.app/Contents/MacOS/Chromium";
			}
			else if (osname.contains("Win")) {
				osbinarypath = "windows" + File.separator + "chromedriver.exe";
				osChromiumDir = "C:" + File.separator + "ProgramData" + File.separator + "Chrome-Headless" + File.separator;
				osChromiumBinary = osChromiumDir + "chrome-win32\\chrome.exe";
			}
			else if (osname.contains("Linux") && osarch.contains("386")) {
				throw new Exception("Chrome/Chromium und daher auch ChromeDriver werden unter 32-Bit-Linux nicht mehr angeboten! Wechseln Sie zu 64-Bit.");
			}
			else if (osname.contains("Linux") && (osarch.contains("64") || osarch.contains("amd64"))) {
				osbinarypath = "linux64" + File.separator + "chromedriver";
				osChromiumDir = osUserPath + File.separator + ".chrome-headless" + File.separator;
				osChromiumBinary = osChromiumDir + "chrome-linux/chrome";
			}
			else {
				new Exception("OS konnte nicht ermittelt werden!");
			}

			File chromeDriverBinary = new File(chromedriversource + osbinarypath);
			System.setProperty("chromedriver.binary.path", chromeDriverBinary.getCanonicalPath());

			File chromeDriverLog = new File(Application.getConfig().getWorkDir() + File.separator + "chromedriver.log");
			System.setProperty("chromedriver.logfile.path", chromeDriverLog.getCanonicalPath());
			// System.setProperty("chromedriver.logfile.path", System.getProperties().getProperty("java.io.tmpdir") + File.separator + "chromedriverdriver" + File.separator + "chromedriver.log");

			chromeDownloadsDir = new File(System.getProperty("java.io.tmpdir") + File.separator + "chromedriver-downloads");
			System.setProperty("chrome.downloads.path", chromeDownloadsDir.getCanonicalPath());

			File chromiumRootFolder = new File(osChromiumDir);
			System.setProperty("chromium.root.path", chromiumRootFolder.getCanonicalPath());
			File chromiumBinary = new File(osChromiumBinary);
			System.setProperty("chromium.binary.path", chromiumBinary.getCanonicalPath());

			System.out.println("Def Working-Path to ChromeDriver-Binary is: " + chromeDriverBinary.getCanonicalPath());
			System.out.println("Def Working-Path to ChromeDriver-Log is: " + chromeDriverLog.getCanonicalPath());
			System.out.println("Def Working-Path to Chrome Downloads-Directory is: " + chromeDownloadsDir.getCanonicalPath());
			System.out.println("Def Working-Path to Chromium-RootDir is: " + chromiumRootFolder.getCanonicalPath());
			System.out.println("Def Working-Path to Chromium-Binary is: " + chromiumBinary.getCanonicalPath());
			/* *************************************************************************** */
		}
		catch (Exception error) {

			System.out.println("SetChromeDriverPaths fehlerhaft:\n" + error);
			//throw new Exception("SetChromeDriverPaths fehlerhaft: " + error.getMessage());
		}
	}
	
	@Override
	public void download(ResultSets r, String selector, String charset) {
		if (selector.equals("download_Directory")) {
			File[] x = chromeDownloadsDir.listFiles();
			if (charset == null) {
				charset = "UTF-8";
			}
			if (x.length == 1) {
				try {
					String content = new String (Files.readAllBytes(x[0].toPath()), Charset.forName(charset));
					r.txt = content;
					r.page = new StringPage(r.txt);
					x[0].delete();
				} catch (IOException e) {
					throw new IllegalStateException(e);
				} 
			} else {
				throw new IllegalStateException("Zuviele Dateien im Download-Verzeichnis (" + x.length + ")");
			}
			return;
		}
		super.download(r,  selector, charset);
	}
	
}
