package com.tesis.webscraping.service;

import java.util.ArrayList;
import java.util.List;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.tesis.webscraping.model.Ley;

public class HtmlUnitScraper {

	
	public List<Ley> obtenerLeyesDesdeWeb(String url){
		
		List<Ley> leyes = new ArrayList<>();
		
		try(final WebClient webClient = new WebClient(BrowserVersion.CHROME)) {
			webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setCssEnabled(false);

            HtmlPage page = webClient.getPage(url);
            
            
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		return null;
	}
}
