package de.open4me.ly.webscraper.runner.phantomjsdriver;

import java.io.File;

import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import de.open4me.ly.webscraper.runner.base.SeleniumEngine;
import de.willuhn.jameica.plugin.Plugin;
import de.willuhn.jameica.system.Application;

public class PjsEngine extends SeleniumEngine {

	private DesiredCapabilities caps;
	private String agentname = "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:59.0) Gecko/20100101 Firefox/59.0";

	@Override
	public void init() {
		super.init();
		caps = new DesiredCapabilities();
		caps.setJavascriptEnabled(true);
		caps.setCapability("phantomjs.page.settings.userAgent", agentname);
		if (language != null) {
			caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Accept-Language", language);
		}
		for (Plugin x: Application.getPluginLoader().getInstalledPlugins()) {
			if ("hibiscus.scripting.phantomjsdriver".equals(x.getManifest().getName())) {
				System.out.println(x.getManifest().getName());
				System.out.println(x.getResources().getWorkPath());
				System.out.println(x.getManifest().getPluginDir());
				String osname = System.getProperty("os.name");
				String osarch = System.getProperty("os.arch");
				String subdir = "";
				if (osname.contains("Mac")) {
					subdir = "macosx" + File.separator + "phantomjs";
				}
				else if (osname.contains("Win")) {
					subdir = "windows" + File.separator + "phantomjs.exe";
				}
				else if (osname.contains("Linux") && osarch.contains("386")) {
					System.out.println("Chrome/Chromium und daher auch ChromeDriver werden unter 32-Bit-Linux nicht mehr angeboten! Wechseln Sie zu 64-Bit.");
				}
				else if (osname.contains("Linux") && (osarch.contains("64") || osarch.contains("amd64"))) {
					subdir = "linux64" + File.separator + "phantomjs";
				}
				else {
					new Exception("OS konnte nicht ermittelt werden!");
				}
				String dir = x.getManifest().getPluginDir() + File.separator + "bin" + File.separator+ subdir;
				File f = new File(dir);
				f.setExecutable(true);
				caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, dir);
			}
		}
		driver = new org.openqa.selenium.phantomjs.PhantomJSDriver(caps);
	}






	@Override
	public void setCfg(String key, String value) {
		switch (key.toLowerCase()) {
		case "browser":
			agentname = value;
		default:
			super.setCfg(key, value);;
		}
	}



}
