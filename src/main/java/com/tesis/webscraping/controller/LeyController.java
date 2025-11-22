package com.tesis.webscraping.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tesis.webscraping.model.Ley;
import com.tesis.webscraping.service.LeyOrquestadorService;

@RestController
@RequestMapping("/api/ley")
public class LeyController {

	private final LeyOrquestadorService leyOrquestadorService;
	
	public LeyController(LeyOrquestadorService leyOrquestadorService) {
		this.leyOrquestadorService = leyOrquestadorService;
	}
	
	@PostMapping("/scrapingAndSave")
    public List<Ley> guardarLeyes(
            @RequestParam String url, @RequestParam String rangoMin, @RequestParam String rangoMax) {
    	
        return leyOrquestadorService.ejecutarScrapingYGuardar(url, rangoMin, rangoMax);
    }
	
	@DeleteMapping("/delete-all-leyes")
    public ResponseEntity<String> eliminarTodasLasLeyes() {
		leyOrquestadorService.deleteAllLeyes();
        return ResponseEntity.ok("Se eliminaron todos los documentos de la colección 'leyes'.");
    }
	
	@DeleteMapping("/delete-allHistory")
    public ResponseEntity<String> eliminarTodoHistorial() {
		leyOrquestadorService.deleteAllHistorial();
        return ResponseEntity.ok("Se eliminaron todos los documentos de la colección 'Historial'.");
    }
	
	@GetMapping("/ListAllLeyes")
    public List<Ley> todasLeyes() {
    	
        return leyOrquestadorService.listAllLeyes();
    }
	
	@PostMapping("/update-links-secondPage")
    public ResponseEntity<String> updateLinksSecondPage() {
		leyOrquestadorService.registrarLinksDocs();
        return ResponseEntity.ok("Proceso de actualización iniciado - 2da Page");
    }
	
	@PostMapping("/update-detail-low")
    public ResponseEntity<String> updateDetailLow() {
		leyOrquestadorService.registrarDetalleLey();
        return ResponseEntity.ok("Proceso de actualización iniciado - Detalle Ley");
    }
	
}
