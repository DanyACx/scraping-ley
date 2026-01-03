package com.tesis.webscraping.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tesis.webscraping.model.Congresista;
import com.tesis.webscraping.service.CongresistaOrquestadorService;

@RestController
@RequestMapping("/api/congresista")
public class CongresistaController {
	
	private final CongresistaOrquestadorService congresistaOrquestadorService;
	
	public CongresistaController(CongresistaOrquestadorService congresistaOrquestadorService) {
		this.congresistaOrquestadorService = congresistaOrquestadorService;
	}

	@PostMapping("/scrapingAndSave")
    public List<Congresista> guardarCongresistas() {
    	
        return congresistaOrquestadorService.ejecutarScrapingYGuardarCongresistas();
    }
	
	@PostMapping("/update-FTcongresistas")
    public ResponseEntity<String> updateLinksSecondPage() {
		congresistaOrquestadorService.getDataFichaTecnica();
        return ResponseEntity.ok("Scraping completado - FT Congresistas");
    }
	
	@GetMapping("/ListAllCongresistas")
    public List<Congresista> todasLeyes() {
    	
        return congresistaOrquestadorService.listAllCongresistas();
    }
	
	@DeleteMapping("/delete-all-congresistas")
    public ResponseEntity<String> eliminarTodasLasLeyes() {
		congresistaOrquestadorService.deleteAllCongresistas();
        return ResponseEntity.ok("Se eliminaron todos los documentos de la colección 'congresistas'.");
    }
	
}
