package com.tesis.webscraping.service;

import java.util.List;
import java.util.Map;

import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

import com.tesis.webscraping.model.Ley;
import com.tesis.webscraping.model.RegistroTabla;

@Service
public class ScraperService {

	//private final HtmlUnitScraper htmlUnitScraper;
	private final SeleniumScraperService seleniumScraperService;
    //private final ProductoRepository productoRepository;
	
	public ScraperService(SeleniumScraperService seleniumScraperService) {
		this.seleniumScraperService = seleniumScraperService;
	}
	
	public List<RegistroTabla> extraerLeyes(String url, String rangoMin, String rangoMax) {
		List<RegistroTabla> leyes = seleniumScraperService.obtenerLeyesDesdeWeb(url, rangoMin, rangoMax);
		
		return leyes;
	}
	
	public List<Ley> todasLeyesV2(String url, WebDriver driver, String rangoMin, String rangoMax) {
		List<Ley> leyes = seleniumScraperService.obtenerTodasLeyesV2(url, driver, rangoMin, rangoMax);
		
		return leyes;
	}
	
	public Map<String, String> scrapingSecondPage(WebDriver driver, String url){
		
		Map<String, String> urlsMap = seleniumScraperService.scrapingSecondPage(driver, url);
		
		return urlsMap;
	}
	
	public Map<String, Object> getDetailLow(WebDriver driver, String url){
		
		Map<String, Object> urlsMap = seleniumScraperService.extraerDetalleProyectoLey(driver, url);
		
		return urlsMap;
	}
	
	public Map<String, Object> scrapingThirdPage(WebDriver driver, String url){
		
		Map<String, Object> urlsMap = seleniumScraperService.scrapingThirdPage(driver, url);
		
		return urlsMap;
	}
}
