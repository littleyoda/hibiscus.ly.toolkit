package de.open4me.ly.webscraper.runner.base;

import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

import de.open4me.ly.webscraper.runner.Runner.ResultSets;

public abstract class Engine {


	public boolean isinit = false;
	public void init() {
		isinit = true;
	}

	public abstract void close();
	
	public abstract void setCfg(String group, String group2);

	public abstract void open(ResultSets r, String openurl);

	public abstract void set(ResultSets r, String group, String group2);

	public abstract void click(ResultSets r, String rest);

	public abstract void extract(ResultSets r, String selector, String split);

	public abstract void download(ResultSets r, String rest, String charset);

	public abstract boolean assertexists(ResultSets r, String rest);

	public abstract void enrichWithDebuginfo(ResultSets r);

	public boolean isInit() {
		return isinit;
	}

	public abstract void downloadfromurl(ResultSets r, String url);

	public abstract int count(ResultSets r, String rest);

	public abstract List<ImmutablePair<String,String>>  getOptions(ResultSets r, String selector);
	
	public abstract void setOptionByText(ResultSets r, String selector, String optiontext);

	public abstract void closeWindow();

	public abstract void submit(String selector);

}
