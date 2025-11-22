package com.tesis.webscraping.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tesis.webscraping.model.Historial;
import com.tesis.webscraping.model.Ley;
import com.tesis.webscraping.repository.IHistorialRepository;
import com.tesis.webscraping.repository.ILeyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

@Service
public class LeyOrquestadorService {

	private final ILeyRepository leyRepository;
	private final IHistorialRepository historialRepository;
	private final ScraperService scraperService;
	
	private final ExecutorService executorService = Executors.newFixedThreadPool(2); // 5 hilos, sugiere 15
	Semaphore semaphore = new Semaphore(2); // máximo 3 llamadas activas a la vez, sugiere 10
    Random random = new Random();
	
	//@Autowired
    public LeyOrquestadorService(ILeyRepository leyRepository, IHistorialRepository historialRepository, ScraperService scraperService) {
        this.leyRepository = leyRepository;
        this.scraperService = scraperService;
        this.historialRepository = historialRepository;
    }
	
	public List<Ley> ejecutarScrapingYGuardar(String url, String rangoMin, String rangoMax) {
		
		List<Ley> leyes = scraperService.todasLeyesV2(url, rangoMin, rangoMax);
		Historial cotaSuperior = cotaSuperior(leyes);
		historialRepository.save(cotaSuperior); // para guardar la ultima ley
		
        return leyRepository.saveAll(leyes);
    }
	
	public void deleteAllLeyes() {
        leyRepository.deleteAll();
    }
	
	public void deleteAllHistorial() {
		historialRepository.deleteAll();
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
	
	public List<Ley> listAllLeyes(){
		
		List<Ley> leyes = leyRepository.findAll();
		
		return leyes;
	}
	
	public void registrarLinksDocs() {

        int totalProcesados = 0;

        while (true) {
            //Trae solo los pendientes, sin paginación
            List<Ley> leyesPendientes =
                    leyRepository.findTop10ByLinkTextoNormaLegalIsNullAndLinkFichaTecnicaIsNull();

            if (leyesPendientes.isEmpty()) {
                System.out.println("No hay más documentos pendientes.");
                break;
            }

            List<Future<Ley>> futures = new ArrayList<>();

            for (Ley ley : leyesPendientes) {
                try {
                    semaphore.acquire();
                    int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg

                    futures.add(executorService.submit(() -> {
                        try {
                            Thread.sleep(delay);
                            //return procesarLey(ley);
                            return procesarLeyConWebDriverIndependiente(ley);
                        } finally {
                            semaphore.release();
                        }
                    }));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
                    System.err.println("Error procesando ley: " + e.getMessage());
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                totalProcesados += actualizadas.size();
                System.out.println("Lote actualizado. Total procesados: " + totalProcesados);
            }

            // Repite hasta que ya no haya pendientes
        }

        executorService.shutdown();
        System.out.println("Proceso completo. Total documentos procesados: " + totalProcesados);
    }
	
	/*private Ley procesarLey(Ley ley) { // tarda en promedio 4 segundos
        try {
        	Map<String, String> urlsMap = scraperService.scrapingSecondPage(ley.getLinkSegundaPagina());
            ley.setLinkTextoNormaLegal(urlsMap.get("linkTextoNormaLegal"));
            ley.setLinkFichaTecnica(urlsMap.get("linkFichaTecnica"));
            ley.setLinkTerceraPagina(urlsMap.get("linkTerceraPagina"));
            return ley;
            
        } catch (Exception e) {
            System.err.println("Error al obtener info de ley " + ley.getNumero() + ": " + e.getMessage());
            return null;
        }
    }*/
	
	
	/**
     * Cada hilo crea y destruye su propio WebDriver.
     * Esto evita el problema de referencias obsoletas (StaleElementReferenceException).
     */
	private Ley procesarLeyConWebDriverIndependiente(Ley ley) {
        WebDriver driver = null;
        try {
            // Crear un driver independiente por hilo
        	ChromeOptions options = new ChromeOptions();
			options.addArguments("--headless");
			options.addArguments("--disable-extensions");
			
            driver = new ChromeDriver(options);

            Map<String, String> urlsMap = scraperService.scrapingSecondPage(driver, ley.getLinkSegundaPagina());

            ley.setLinkTextoNormaLegal(urlsMap.get("linkTextoNormaLegal"));
            ley.setLinkFichaTecnica(urlsMap.get("linkFichaTecnica"));
            ley.setLinkTerceraPagina(urlsMap.get("linkTerceraPagina"));
            return ley;

        } catch (Exception e) {
            System.err.println("Error en ley " + ley.getNumero() + ": " + e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                driver.quit(); // Cierra el navegador al terminar
            }
        }
    }
	
	public void registrarDetalleLey() {

        int totalProcesados = 0;

        while (true) {
            //Trae solo los pendientes, sin paginación
            List<Ley> leyesDetallePendientes =
                    leyRepository.findTop10ByPeriodoParlamentarioIsNullAndTituloIsNull();

            if (leyesDetallePendientes.isEmpty()) {
                System.out.println("No hay más documentos pendientes.");
                break;
            }

            List<Future<Ley>> futures = new ArrayList<>();

            for (Ley ley : leyesDetallePendientes) {
                try {
                    semaphore.acquire();
                    int delay = 4000 + random.nextInt(6000); // delay entre 4–10 seg

                    futures.add(executorService.submit(() -> {
                        try {
                            Thread.sleep(delay);
                            //return procesarLey(ley);
                            return procesarDetalleLeyConWebDriverIndependiente(ley);
                        } finally {
                            semaphore.release();
                        }
                    }));

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
                    System.err.println("Error procesando leyDetalle: " + e.getMessage());
                }
            }

            if (!actualizadas.isEmpty()) {
                leyRepository.saveAll(actualizadas);
                totalProcesados += actualizadas.size();
                System.out.println("Lote actualizado. Total procesados: " + totalProcesados);
            }

            // Repite hasta que ya no haya pendientes
        }

        executorService.shutdown();
        System.out.println("Proceso completo. Total documentos procesados: " + totalProcesados);
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
            System.err.println("Error en leyDetalle " + ley.getNumero() + ": " + e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                driver.quit(); // Cierra el navegador al terminar
            }
        }
    }
	
	
}
