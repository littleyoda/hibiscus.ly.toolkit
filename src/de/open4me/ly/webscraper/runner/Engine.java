package de.open4me.ly.webscraper.runner;

import de.open4me.ly.webscraper.runner.Runner.ResultSets;

public abstract class Engine {


	public boolean isinit = false;
	public void init() {
		isinit = true;
	}

	public abstract void setCfg(String group, String group2);

	public abstract void open(ResultSets r, String openurl);

	public abstract void set(ResultSets r, String group, String group2);

	public abstract void click(ResultSets r, String rest);

	public abstract void extract(ResultSets r, String selector, String split);

	public abstract void download(ResultSets r, String rest);

	public abstract boolean assertexists(ResultSets r, String rest);

	public abstract void enrichWithDebuginfo(ResultSets r);

	public boolean isInit() {
		return isinit;
	}

	public abstract void downloadfromurl(ResultSets r, String url);

	

}
