package de.open4me.ly.webscraper.utils;

import org.htmlunit.TextPage;

public class StringPage extends TextPage {

	private String content;

	public StringPage(String s) {
		super(null, null);
		this.content = s;
	}
	
	@Override
	public String getContent() {
		return content;
	}
	
	@Override
	public String toString() {
		return content;
	}
}
