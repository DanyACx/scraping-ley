package com.tesis.webscraping.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LeyOrquestadorService {

    private final ILeyRepository leyRepository;
    private final IHistorialRepository historialRepository;
    private final ScraperService scraperService;
    private final IErrorScrapingRepo errorScrapingRepo;
    
    // Use a local, bounded, named ThreadPoolExecutor per batch (Option A)
    Semaphore semaphore = new Semaphore(2); // ajustar según concurrencia deseada
    Random random = new Random();
    
    private static final org.slf4j.Logger log = LogUtil.getLogger(LeyOrquestadorService.class);
    
    //@Autowired
    public LeyOrquestadorService(ILeyRepository leyRepository, IHistorialRepository historialRepository, ScraperService scraperService, IErrorScrapingRepo errorScrapingRepo) {
        this.leyRepository = leyRepository;
        this.scraperService = scraperService;
        this.historialRepository = historialRepository;
        this.errorScrapingRepo = errorScrapingRepo;
    }
    
    @Transactional
    public List<Ley> ejecutarScrapingYGuardar(String url, String rangoMin, String rangoMax) {

        validarParametros(url, rangoMin, rangoMax);

        try {
            log.info("Obteniendo leyes desde URL: {}, Rango: {} - {}", url, rangoMin, rangoMax);

            List<Ley> leyes = scraperService.todasLeyesV2(url, rangoMin, rangoMax);

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
    
    // registrarLinksDocs
    public void updateLinksSecondPage() {

        int totalProcesados = 0;

        while (true) {

            List<Ley> lote = leyRepository.findTop10ByLinkTextoNormaLegalIsNullAndLinkFichaTecnicaIsNull();

            if (lote.isEmpty()) {
                log.info("No hay más leyes pendientes.");
                break;
            }

            List<Ley> actualizadas = new ArrayList<>();

            for (Ley ley : lote) {
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
                totalProcesados += actualizadas.size();
                log.info("Lote de {} leyes actualizado. Total procesados: {}", actualizadas.size(), totalProcesados);
            }
        }

        log.info("Proceso completo. Total final procesado: {}", totalProcesados);
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

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-pdf-viewer");

        WebDriver driver = null;

        try {
            driver = new ChromeDriver(options);

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
            log.info("No hay más leyes pendientes para Scraping de 2da página.");
            
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
                   
                    /*Ley ley = Ley.builder()
                    		.numero(errorScraping.getNumeroLey())
                    		.linkSegundaPagina(errorScraping.getUrl())
                    		.build();
                    
                    registrarError(ley, e.getMessage());*/
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                errorScrapingRepo.saveAll(errorScrapingAux);
            }

            log.info("Proceso completo. Total final procesado (Fix de documentos - 2da pagina): {}", actualizadas.size());
        }

    }
    
    public void registrarDetalleLey() {

        int totalProcesados = 0;

        while (true) {
            //Trae solo los pendientes, sin paginación
            List<Ley> leyesDetallePendientes =
                    leyRepository.findTop10ByPeriodoParlamentarioIsNullAndTituloIsNull();

            if (leyesDetallePendientes.isEmpty()) {
                log.info("No hay más documentos pendientes para registrar detalle de ley.");
                break;
            }

            // Create a local bounded executor for this batch (Option A)
            int threads = 2; // ajustar según pruebas
            int queueCap = 50;
            AtomicInteger counter = new AtomicInteger(0);
            ThreadFactory namedFactory = r -> {
                Thread t = new Thread(r);
                t.setName("ley-detail-scraper-" + counter.incrementAndGet());
                return t;
            };

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    threads,
                    threads,
                    0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueCap),
                    namedFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

            List<Future<Ley>> futures = new ArrayList<>();

            for (Ley ley : leyesDetallePendientes) {
                try {
                    semaphore.acquire();
                    int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg

                    try {
                        futures.add(executor.submit(() -> {
                            try {
                                Thread.sleep(delay);
                                return procesarDetalleLeyConWebDriverIndependiente(ley);
                            } finally {
                                semaphore.release();
                            }
                        }));
                    } catch (RejectedExecutionException rex) {
                        semaphore.release();
                        log.error("Tarea rechazada al enviar detalle de ley {}", ley.getNumero(), rex);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Interrumpido al adquirir semáforo", e);
                }
            }

            List<Ley> actualizadas = new ArrayList<>();
            for (Future<Ley> future : futures) {
                try {
                    Ley leyActualizada = future.get();
                    if (leyActualizada != null) {
                        actualizadas.add(leyActualizada);
                    }
                } catch (Exception e) {
                    log.error("Error procesando leyDetalle: {}", e.getMessage(), e);
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                totalProcesados += actualizadas.size();
                log.info("Lote actualizado. Total procesados: {}", totalProcesados);
            }

            // shutdown local executor for this batch
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Forzando shutdown del executor local para registrarDetalleLey...");
                    List<Runnable> dropped = executor.shutdownNow();
                    log.warn("Tareas canceladas: {}", dropped.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }

            // Repite hasta que ya no haya pendientes
        }

        log.info("Proceso completo. Total documentos procesados: {}", totalProcesados);
    }
    
    @SuppressWarnings("unchecked")
    private Ley procesarDetalleLeyConWebDriverIndependiente(Ley ley) {
        WebDriver driver = null;
        try {
            // Crear un driver independiente por hilo
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-extensions");
            
            driver = new ChromeDriver(options);

            Map<String, Object> urlsMap = scraperService.getDetailLow(driver, ley.getLinkTerceraPagina());

            ley.setPeriodoParlamentario(urlsMap.get("periodoParlamentario").toString());
            ley.setLegislatura(urlsMap.get("legislatura").toString());
            ley.setFechaPresentacion(urlsMap.get("fechaPresentacion").toString());
            ley.setProponente(urlsMap.get("proponente").toString());
            ley.setTitulo(urlsMap.get("titulo").toString());
            ley.setSumilla(urlsMap.get("sumilla").toString());
            ley.setObservaciones(urlsMap.get("observaciones").toString());
            ley.setAutorPrincipal((List<String>)urlsMap.get("autorPrincipal"));
            ley.setCoautores((List<String>)urlsMap.get("coautores"));
            ley.setAdhrentes((List<String>)urlsMap.get("adhrentes"));
            ley.setGrupoParlamentario(urlsMap.get("grupoParlamentario").toString());
            ley.setComisiones((List<String>)urlsMap.get("comisiones"));
            ley.setUltimoEstado(urlsMap.get("ultimoEstado").toString());
            return ley;

        } catch (Exception e) {
            log.error("Error en leyDetalle {}: {}", ley.getNumero(), e.getMessage(), e);
            return null;
        } finally {
            if (driver != null) {
                driver.quit(); // Cierra el navegador al terminar
            }
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