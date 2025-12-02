package com.tesis.webscraping.service;

import org.openqa.selenium.WebDriver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tesis.webscraping.factory.WebDriverFactory;
import com.tesis.webscraping.model.ErrorScraping;
import com.tesis.webscraping.model.Historial;
import com.tesis.webscraping.model.Ley;
import com.tesis.webscraping.repository.IErrorScrapingRepo;
import com.tesis.webscraping.repository.IHistorialRepository;
import com.tesis.webscraping.repository.ILeyRepository;
import com.tesis.webscraping.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

@Service
public class LeyOrquestadorService {

	private final WebDriverFactory driverFactory;
    private final ILeyRepository leyRepository;
    private final IHistorialRepository historialRepository;
    private final ScraperService scraperService;
    private final IErrorScrapingRepo errorScrapingRepo;
    
    // Use a local, bounded, named ThreadPoolExecutor per batch (Option A)
    Semaphore semaphore = new Semaphore(2); // ajustar según concurrencia deseada
    Random random = new Random();
    
    private static final org.slf4j.Logger log = LogUtil.getLogger(LeyOrquestadorService.class);
    
    //@Autowired
    public LeyOrquestadorService(WebDriverFactory driverFactory, ILeyRepository leyRepository, IHistorialRepository historialRepository, ScraperService scraperService, IErrorScrapingRepo errorScrapingRepo) {
        this.driverFactory = driverFactory;
    	this.leyRepository = leyRepository;
        this.scraperService = scraperService;
        this.historialRepository = historialRepository;
        this.errorScrapingRepo = errorScrapingRepo;
    }
    
    @Transactional
    public List<Ley> ejecutarScrapingYGuardar(String url, String rangoMin, String rangoMax) {

        validarParametros(url, rangoMin, rangoMax);
        
        WebDriver driver = null;

        try {
            log.info("Obteniendo leyes desde URL: {}, Rango: {} - {}", url, rangoMin, rangoMax);
            
            driver = driverFactory.createDriver();

            List<Ley> leyes = scraperService.todasLeyesV2(url, driver, rangoMin, rangoMax);

            if (leyes.isEmpty()) {
                log.warn("No se encontraron leyes para guardar");
                return Collections.emptyList();
            }

            // Guardar la última ley obtenida
            historialRepository.save(cotaSuperior(leyes));

            log.info("Guardando {} leyes en la base de datos...", leyes.size());

            return leyRepository.saveAll(leyes);

        } catch (Exception e) {
            log.error("Error en ejecutarScrapingYGuardar: {}", e.getMessage(), e);
            throw new RuntimeException("Error en scraping o guardado de leyes", e);
        }
    }
    
	public Historial cotaSuperior(List<Ley> leyes) {
	        
        Ley ultimaLey = leyes.isEmpty() ? null : leyes.get(leyes.size() - 1);
        
        Historial registro = Historial.builder()
                .norma(ultimaLey.getNorma())
                .numero(ultimaLey.getNumero())
                .publicacion(ultimaLey.getPublicacion())
                .denominacion(ultimaLey.getDenominacion())
                .linkSegundaPagina(ultimaLey.getLinkSegundaPagina())
                .build();
        
        return registro;
	}

    private void validarParametros(String url, String rangoMin, String rangoMax) {
        if (url == null || url.isBlank())
            throw new IllegalArgumentException("URL no puede ser nula o vacía");

        if (rangoMin == null || rangoMax == null)
            throw new IllegalArgumentException("El rango no puede ser nulo");
    }


    public List<Ley> listAllLeyes(){
    	
    	try {
			log.info("Obteniendo todas las leyes de la base de datos...");
			
			List<Ley> leyes = leyRepository.findAll();
	        return leyes;
	        
		} catch (Exception e) {
			log.error("Error al obtener todas las leyes: {}", e.getMessage(), e);
			throw new RuntimeException("Error al obtener todas las leyes", e);
		}
        
    }
    
    public List<ErrorScraping> listAllScrapingError(){
    	
    	try {
			log.info("Obteniendo todas las leyes con error de scraping de la 2da Pagina...");
			
			List<ErrorScraping> leyesErrorScraping2do = errorScrapingRepo.findAll();
	        return leyesErrorScraping2do;
	        
		} catch (Exception e) {
			log.error("Error al obtener todas las leyes con error de scraping de la 2da Pagina: {}", e.getMessage());
			throw new RuntimeException("Error al obtener todas las leyes con error de scraping de la 2da Pagina", e);
		}
        
    }
    
    public void updateLinksSecondPageV2() {

            List<Ley> todas = leyRepository
                    .findByLinkTextoNormaLegalIsNullAndLinkFichaTecnicaIsNull();


            if (todas.isEmpty()) {
                log.info("No hay más leyes pendientes para Scraping de 2da página.");
                
            } else {
            	
            	List<Ley> actualizadas = new ArrayList<>();

                for (Ley ley : todas) {
                    try {
                        // pausa opcional para no saturar al servidor
                    	int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg
                        Thread.sleep(delay);

                        Ley procesada = procesarLeySecuencial(ley, true);
                        if (procesada != null) {
                            actualizadas.add(procesada);
                        }

                    } catch (Exception e) {
                        log.error("Error procesando ley {}: {}", ley.getNumero(), e.getMessage());

                        registrarError(ley, e.getMessage());
                    }
                }

                if (!actualizadas.isEmpty()) {
                    leyRepository.saveAll(actualizadas);
                }

                log.info("Proceso completo. Total final procesado: {}", actualizadas.size());
            }
    }

    
    /**
     * Cada hilo crea y destruye su propio WebDriver.
     * Esto evita el problema de referencias obsoletas (StaleElementReferenceException).
     */
    private Ley procesarLeySecuencial(Ley ley, boolean registrarError) {

        WebDriver driver = null;

        try {
            driver = driverFactory.createDriver();

            Map<String, String> urls = scraperService.scrapingSecondPage(driver, ley.getLinkSegundaPagina());

            ley.setLinkTextoNormaLegal(urls.get("linkTextoNormaLegal"));
            ley.setLinkFichaTecnica(urls.get("linkFichaTecnica"));
            ley.setLinkTerceraPagina(urls.get("linkTerceraPagina"));

            return ley;

        } catch (Exception e) {
        	
        	if(registrarError) {
        		registrarError(ley, e.getMessage());
        	}
        	
            return null;

        } finally {
            if (driver != null) driver.quit();
        }
    }
    
    @SuppressWarnings("unchecked")
	private Ley procesarLeySecuencialThirdPage(Ley ley, boolean registrarError) {

        WebDriver driver = null;

        try {
            driver = driverFactory.createDriver();

            Map<String, Object> urls = scraperService.scrapingThirdPage(driver, ley.getLinkTerceraPagina());

            ley.setPeriodoParlamentario(urls.get("periodoParlamentario").toString());
            ley.setLegislatura(urls.get("legislatura").toString());
            ley.setFechaPresentacion(urls.get("fechaPresentacion").toString());
            ley.setProponente(urls.get("proponente").toString());
            ley.setTitulo(urls.get("titulo").toString());
            ley.setSumilla(urls.get("sumilla").toString());
            ley.setObservaciones(urls.get("observaciones").toString());
            ley.setAutorPrincipal((List<String>)urls.get("autorPrincipal"));
            ley.setCoautores((List<String>)urls.get("coautores"));
            ley.setAdhrentes((List<String>)urls.get("adhrentes"));
            ley.setGrupoParlamentario(urls.get("grupoParlamentario").toString());
            ley.setComisiones((List<String>)urls.get("comisiones"));
            ley.setUltimoEstado(urls.get("ultimoEstado").toString());

            return ley;

        } catch (Exception e) {
        	
        	if(registrarError) {
        		registrarError(ley, e.getMessage());
        	}
        	
            return null;

        } finally {
            if (driver != null) driver.quit();
        }
    }
    
    private void registrarError(Ley ley, String mensaje) {
        ErrorScraping error = new ErrorScraping();
        error.setNumeroLey(ley.getNumero());
        error.setUrl(ley.getLinkSegundaPagina());
        error.setMensaje(mensaje);

        errorScrapingRepo.save(error);
    }
    
    public void updateLinksSecondScrapingError() {

        List<ErrorScraping> todas = errorScrapingRepo.findLeyesActivas();


        if (todas.isEmpty()) {
            log.info("No hay más leyes pendientes para Fix - Scraping de 2da página.");
            
        } else {
        	
        	List<Ley> actualizadas = new ArrayList<>();
        	List<ErrorScraping> errorScrapingAux = new ArrayList<>();

            for (ErrorScraping errorScraping : todas) {
                try {
                    // pausa opcional para no saturar al servidor
                	int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg
                    Thread.sleep(delay);

                    Ley leyAux = leyRepository.findByNumero(errorScraping.getNumeroLey())
							.orElseThrow(() -> new RuntimeException("Ley no encontrada para número: " + errorScraping.getNumeroLey()));
                    
                    Ley procesada = procesarLeySecuencial(leyAux, false);
                    if (procesada != null) {
                        actualizadas.add(procesada);
                        
                        ErrorScraping aux = errorScraping.toBuilder()
                                .estado(0)
                                .build();
                        errorScrapingAux.add(aux);
                    }

                } catch (Exception e) {
                    log.error("Error procesando ley {}: {}", errorScraping.getNumeroLey(), e.getMessage());
                                      
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                errorScrapingRepo.saveAll(errorScrapingAux);
            }

            log.info("Proceso completo. Total final procesado (Fix de documentos - 2da pagina): {}", actualizadas.size());
        }

    }
    
    public void updateLinksThirdPageV2() {

        List<Ley> todas = leyRepository
                .findByTituloIsNullAndSumillaIsNull();


        if (todas.isEmpty()) {
            log.info("No hay más leyes pendientes para Scraping de 3ra página.");
            
        } else {
        	
        	List<Ley> actualizadas = new ArrayList<>();

            for (Ley ley : todas) {
                try {
                    // pausa opcional para no saturar al servidor
                	int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg
                    Thread.sleep(delay);

                    Ley procesada = procesarLeySecuencialThirdPage(ley, true);
                    if (procesada != null) {
                        actualizadas.add(procesada);
                    }

                } catch (Exception e) {
                    log.error("Error procesando ley {}: {}", ley.getNumero(), e.getMessage());

                    registrarError(ley, e.getMessage());
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
            }

            log.info("Proceso completo. Total final procesado: {}", actualizadas.size());
        }
    }
    
    public void updateLinksThridScrapingError() {

        List<ErrorScraping> todas = errorScrapingRepo.findLeyesActivas();


        if (todas.isEmpty()) {
            log.info("No hay más leyes pendientes para Fix - Scraping de 3ra página.");
            
        } else {
        	
        	List<Ley> actualizadas = new ArrayList<>();
        	List<ErrorScraping> errorScrapingAux = new ArrayList<>();

            for (ErrorScraping errorScraping : todas) {
                try {
                    // pausa opcional para no saturar al servidor
                	int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg
                    Thread.sleep(delay);

                    Ley leyAux = leyRepository.findByNumero(errorScraping.getNumeroLey())
							.orElseThrow(() -> new RuntimeException("Ley no encontrada para número: " + errorScraping.getNumeroLey()));
                    
                    Ley procesada = procesarLeySecuencialThirdPage(leyAux, false);
                    if (procesada != null) {
                        actualizadas.add(procesada);
                        
                        ErrorScraping aux = errorScraping.toBuilder()
                                .estado(0)
                                .build();
                        errorScrapingAux.add(aux);
                    }

                } catch (Exception e) {
                    log.error("Error procesando ley {}: {}", errorScraping.getNumeroLey(), e.getMessage());
                   
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                errorScrapingRepo.saveAll(errorScrapingAux);
            }

            log.info("Proceso completo. Total final procesado (Fix de documentos - 3ra pagina): {}", actualizadas.size());
        }

    }
    
    public void deleteAllLeyes() {
        
        try {
            leyRepository.deleteAll();
            log.info("Se eliminaron todos los documentos de la colección 'leyes'.");
        } catch (Exception e) {
            log.error("Error al eliminar documentos de 'leyes'", e);
            throw new RuntimeException("Error al eliminar documentos de 'leyes'", e);
        }
        
    }
    
    public void deleteAllHistorial() {
        
        try {
            historialRepository.deleteAll();
            log.info("Se eliminaron todos los documentos de la colección 'Historial'.");
        } catch (Exception e) {
            log.error("Error al eliminar documentos de 'Historial'", e);
            throw new RuntimeException("Error al eliminar documentos de 'Historial'", e);
        }
    }
    
    public void deleteAllErrorScraping() {
        
        try {
        	errorScrapingRepo.deleteAll();
            log.info("Se eliminaron todos los documentos de la colección 'ErrorScraping'.");
        } catch (Exception e) {
            log.error("Error al eliminar documentos de 'ErrorScraping'", e);
            throw new RuntimeException("Error al eliminar documentos de 'ErrorScraping'", e);
        }
    }

    
}