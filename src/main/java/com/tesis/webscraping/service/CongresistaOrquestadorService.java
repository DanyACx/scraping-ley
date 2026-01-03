package com.tesis.webscraping.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tesis.webscraping.factory.WebDriverFactory;
import com.tesis.webscraping.model.Congresista;
import com.tesis.webscraping.repository.ICongresistaRepository;
import com.tesis.webscraping.util.LogUtil;

@Service
public class CongresistaOrquestadorService {

	private final WebDriverFactory driverFactory;
    private final ICongresistaRepository congresistaRepository;
    private final ScraperService scraperService;
    
    private static final org.slf4j.Logger log = LogUtil.getLogger(CongresistaOrquestadorService.class);
    
    @Value("${url.web.congresistas}")
    private String urlWebCongresistas;
    
    Random random = new Random();
    
    public CongresistaOrquestadorService(WebDriverFactory driverFactory, ICongresistaRepository congresistaRepository, ScraperService scraperService) {
        this.driverFactory = driverFactory;
    	this.congresistaRepository = congresistaRepository;
        this.scraperService = scraperService;
    }
    
    @Transactional
    public List<Congresista> ejecutarScrapingYGuardarCongresistas() {

        validarParametros(urlWebCongresistas);
        
        WebDriver driver = null;

        try {
            log.info("Obteniendo leyes desde URL: {}", urlWebCongresistas);
            
            driver = driverFactory.createDriver();

            List<Congresista> congresistas = scraperService.extraerCongresistas(urlWebCongresistas, driver);

            if (congresistas.isEmpty()) {
                log.warn("No se encontraron congresistas para guardar");
                return Collections.emptyList();
            }

            log.info("Guardando {} congresistas en la base de datos...", congresistas.size());

            return congresistaRepository.saveAll(congresistas);

        } catch (Exception e) {
            log.error("Error en ejecutarScrapingYGuardar-congresistas: {}", e.getMessage(), e);
            throw new RuntimeException("Error en scraping o guardado de congresistas", e);
        }
    }
    
    private void validarParametros(String url) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL no puede ser nula o vacía");
    }
    
    public List<Congresista> listAllCongresistas(){
    	
    	try {
			log.info("Obteniendo todas los congresistas de la base de datos...");
			
			List<Congresista> leyes = congresistaRepository.findAll();
	        return leyes;
	        
		} catch (Exception e) {
			log.error("Error al obtener todos los congresistas: {}", e.getMessage(), e);
			throw new RuntimeException("Error al obtener todos los congresistas", e);
		}
        
    }
    
    public void deleteAllCongresistas() {
        
        try {
        	congresistaRepository.deleteAll();
            log.info("Se eliminaron todos los documentos de la colección 'congresistas'.");
        } catch (Exception e) {
            log.error("Error al eliminar documentos de 'congresistas'", e);
            throw new RuntimeException("Error al eliminar documentos de 'congresistas'", e);
        }
        
    }
    
    public void getDataFichaTecnica() {

        List<Congresista> todas = congresistaRepository.findByVotacionIsNull();

        if (todas.isEmpty()) {
            log.info("No hay más congresistas pendientes para Scraping de Ficha Tecnica");
            
        } else {
        	
        	List<Congresista> actualizadas = new ArrayList<>();

            for (Congresista congresista : todas) {
                try {
                    // pausa opcional para no saturar al servidor
                	int delay = 2000 + random.nextInt(2000); // delay entre 4–10 seg
                    Thread.sleep(delay);

                    Congresista procesada = procesarCongresistaSecuencial(congresista);
                    
                    if (procesada != null) {
                        actualizadas.add(procesada);
                    }

                } catch (Exception e) {
                    log.error("Error procesando FT de congresista {}: {}", congresista.getNombreCompleto(), e.getMessage());
                    //registrarError(ley, e.getMessage());
                }
            }

            if (!actualizadas.isEmpty()) {
            	congresistaRepository.saveAll(actualizadas);
            }

            log.info("Proceso completo. Total final procesado: {}", actualizadas.size());
        }
    }
    
    private Congresista procesarCongresistaSecuencial(Congresista congresista) {

        WebDriver driver = null;

        try {
            driver = driverFactory.createDriver();

            Map<String, String> dataFT = scraperService.scrapingCongresistaFichaTecnica(driver, congresista.getEnlaceFichaTecnica());

            congresista.setVotacion(Integer.parseInt(dataFT.get("votacion").replace(",", "")));
            congresista.setFechaInicio(dataFT.get("fechaInicio"));
            congresista.setFechaFin(dataFT.get("fechaFin"));
            congresista.setPartidoPolitico(dataFT.get("partidoPolitico"));
            congresista.setBancada(dataFT.get("bancada"));
            congresista.setDistritoElectoral(dataFT.get("distritoElectoral"));
            congresista.setCondicion(dataFT.get("condicion"));

            return congresista;

        } catch (Exception e) {
        	
        	/*if(registrarError) {
        		registrarError(ley, e.getMessage());
        	}*/
        	
            return null;

        } finally {
            if (driver != null) driver.quit();
        }
    }
    
}
