package com.tesis.webscraping.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tesis.webscraping.model.RegistroTabla;
import com.tesis.webscraping.service.ScraperService;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

	private final ScraperService scraperService;
	
	public ScraperController(ScraperService scraperService) {
		this.scraperService = scraperService;
	}

    @GetMapping("/leyes")
    public List<RegistroTabla> scrapeProductos(
            @RequestParam String url, @RequestParam String rangoMin, @RequestParam String rangoMax) {
    	
        return scraperService.extraerLeyes(url, rangoMin, rangoMax);
    }
    
    @GetMapping("/AllLeyes")
    public List<RegistroTabla> todasLeyes(
            @RequestParam String url, @RequestParam String rangoMin, @RequestParam String rangoMax) {
    	
        return scraperService.todasLeyes(url, rangoMin, rangoMax);
    }
}
