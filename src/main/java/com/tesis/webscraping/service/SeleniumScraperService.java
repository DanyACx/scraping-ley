package com.tesis.webscraping.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import com.tesis.webscraping.model.Ley;
import com.tesis.webscraping.model.RegistroTabla;
import com.tesis.webscraping.util.UtilitarioScraping;

@Component
public class SeleniumScraperService {
	
	// Timeouts recomendados — ajusta según la página objetivo
   // private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);
   // private static final Duration ELEMENT_WAIT_TIMEOUT = Duration.ofSeconds(15);
    //private static final Duration POLLING_INTERVAL = Duration.ofMillis(500);
	WebDriver driver = null;

	public List<RegistroTabla> obtenerLeyesDesdeWeb(String url, String rangoMin, String rangoMax) {
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");

		WebDriver driver = null;
		
		List<RegistroTabla> registroTabla = new ArrayList<>();

		try {
			ChromeOptions options = new ChromeOptions();
            //options.addArguments("--headless"); // sin interfaz
            //options.addArguments("--no-sandbox");
            //options.addArguments("--disable-dev-shm-usage");
			
			driver = new ChromeDriver(options);
            driver.get(url);
            
            // Esperar que cargue el formulario
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")));
            
            // Llenar el formulario
            WebElement cotaInferior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaI"));
            cotaInferior.clear(); // Limpia el campo si tiene texto previo
            cotaInferior.sendKeys(rangoMin); // Escribe el texto
            //cotaInferior.selectByVisibleText(rangoMin);
            
            WebElement cotaSuperior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaF"));
            cotaSuperior.clear(); // Limpia el campo si tiene texto previo
            cotaSuperior.sendKeys(rangoMax); // Escribe el texto
            //cotaSuperior.selectByVisibleText(rangoMax);
            
            // Clic en el botón Buscar
            driver.findElement(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")).click();
            
            // Esperar los resultados
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle")));
            
            // Extraer las leyes de la tabla
            WebElement tabla = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle"));
            List<WebElement> filas = tabla.findElements(By.tagName("tr"));
            
            for (WebElement fila : filas) {
            	List<WebElement> columnas = fila.findElements(By.tagName("td"));
            	
            	if (columnas.size() >= 4) {
                    String norma = columnas.get(0).getText();
                    int numero = Integer.parseInt(columnas.get(1).getText());
                    String publicacion = columnas.get(2).getText();
                    String denominacion = columnas.get(3).getText();
                    
                    WebElement enlace = columnas.get(1).findElement(By.tagName("a"));
                    
                    String href = enlace.getAttribute("href");
                    
                 // Decodifica los caracteres especiales
                    String hrefDecodificado = UtilitarioScraping.decodificarURL(href);
                    System.out.println("href: " + href);
                    String hrefUrl = hrefDecodificado.substring(hrefDecodificado.indexOf("'") + 1, hrefDecodificado.lastIndexOf("'"));

                   
                    System.out.println(norma + "   -   " + denominacion);
                    
                    RegistroTabla registro = RegistroTabla.builder()
                    		.norma(norma)
                    		.numero(numero)
                    		.publicacion(publicacion)
                    		.denominacion(denominacion)
                    		.linkExpedienteVirtual(hrefUrl)
                    		.build();
                    		
                    registroTabla.add(registro);
                    
                }
            }
            

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (driver != null) {
                driver.quit();
            }
		}

		return registroTabla;
	}
	
	public List<RegistroTabla> obtenerTodasLeyes(String url, String rangoMin, String rangoMax){
		
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");
		//WebDriver driver = null;
		List<RegistroTabla> leyesAll = new ArrayList<>();
		
		try {
			ChromeOptions options = new ChromeOptions();
			driver = new ChromeDriver(options);
            driver.get(url);
            
            // Esperar que cargue el formulario
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")));
            
            // Llenar el formulario
            WebElement cotaInferior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaI"));
            cotaInferior.clear(); // Limpia el campo si tiene texto previo
            cotaInferior.sendKeys(rangoMin); // Escribe el texto
			
            WebElement cotaSuperior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaF"));
            cotaSuperior.clear(); // Limpia el campo si tiene texto previo
            cotaSuperior.sendKeys(rangoMax); // Escribe el texto
            
            // Clic en el botón Buscar
            driver.findElement(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")).click();
            
            // Esperar los resultados
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle")));
            
            WebElement paginacion = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_LblNroPagina"));
            String numero = paginacion.getText();
            
            int cantidadPaginas = UtilitarioScraping.cantidadPaginas(numero);
            
            for(int i=0; i<cantidadPaginas; i++) {
            	boolean flag = true;
            	List<RegistroTabla> aux = new ArrayList<>();
            	
            	if(i == cantidadPaginas-1)
            		flag = false;
            	
            	aux = leyesUnaPagina(flag);
            	leyesAll.addAll(aux);
            	
            	if(i != cantidadPaginas-1) {
            		WebElement botonSiguiente = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_ImgBtnSiguiente"));
            		UtilitarioScraping.tiempoPaginacion();
            		botonSiguiente.click();
            	}
            }
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (driver != null) {
                driver.quit();
            }
		}
		
		return leyesAll;
	}
	
	public List<Ley> obtenerTodasLeyesV2(String url, String rangoMin, String rangoMax){
		
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");
		//WebDriver driver = null;
		List<Ley> leyesAll = new ArrayList<>();
		
		try {
			ChromeOptions options = new ChromeOptions();
			driver = new ChromeDriver(options);
            driver.get(url);
            
            // Esperar que cargue el formulario
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")));
            
            // Llenar el formulario
            WebElement cotaInferior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaI"));
            cotaInferior.clear(); // Limpia el campo si tiene texto previo
            cotaInferior.sendKeys(rangoMin); // Escribe el texto
			
            WebElement cotaSuperior = driver.findElement(By.id("ctl00_ContentPlaceHolder1_TxtNroNormaF"));
            cotaSuperior.clear(); // Limpia el campo si tiene texto previo
            cotaSuperior.sendKeys(rangoMax); // Escribe el texto
            
            // Clic en el botón Buscar
            driver.findElement(By.id("ctl00_ContentPlaceHolder1_BtnConsultar")).click();
            
            // Esperar los resultados
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle")));
            
            WebElement paginacion = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_LblNroPagina"));
            String numero = paginacion.getText();
            
            int cantidadPaginas = UtilitarioScraping.cantidadPaginas(numero);
            
            for(int i=0; i<cantidadPaginas; i++) {
            	boolean flag = true;
            	List<Ley> aux = new ArrayList<>();
            	
            	if(i == cantidadPaginas-1)
            		flag = false;
            	
            	aux = leyesUnaPaginaV2(flag);
            	leyesAll.addAll(aux);
            	
            	if(i != cantidadPaginas-1) {
            		WebElement botonSiguiente = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_ImgBtnSiguiente"));
            		UtilitarioScraping.tiempoPaginacion();
            		botonSiguiente.click();
            	}
            }
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (driver != null) {
                driver.quit();
            }
		}
		
		return leyesAll;
	}
	
	public List<RegistroTabla> leyesUnaPagina(boolean ultimaPag) {
		//System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");

		List<RegistroTabla> registroTabla = new ArrayList<>();

		try {
			
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle")));
            
            if(ultimaPag)
            	wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_ImgBtnSiguiente")));
            
            // Extraer las leyes de la tabla
            WebElement tabla = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle"));
            List<WebElement> filas = tabla.findElements(By.tagName("tr"));
            
            for (WebElement fila : filas) {
            	List<WebElement> columnas = fila.findElements(By.tagName("td"));
            	
            	if (columnas.size() >= 4) {
                    String norma = columnas.get(0).getText();
                    int numero = Integer.parseInt(columnas.get(1).getText());
                    String publicacion = columnas.get(2).getText();
                    String denominacion = columnas.get(3).getText();
                    
                    WebElement enlace = columnas.get(1).findElement(By.tagName("a"));
                    
                    String href = enlace.getAttribute("href");
                    
                    // Decodifica los caracteres especiales
                    String hrefDecodificado = UtilitarioScraping.decodificarURL(href);
                    String hrefUrl = hrefDecodificado.substring(hrefDecodificado.indexOf("'") + 1, hrefDecodificado.lastIndexOf("'"));

                    RegistroTabla registro = RegistroTabla.builder()
                    		.norma(norma)
                    		.numero(numero)
                    		.publicacion(publicacion)
                    		.denominacion(denominacion)
                    		.linkExpedienteVirtual(hrefUrl)
                    		.build();
                    		
                    registroTabla.add(registro);
                    
                }
            }

		} catch (Exception e) {
			e.printStackTrace();
		} 

		return registroTabla;
	}
	
	public List<Ley> leyesUnaPaginaV2(boolean ultimaPag) {
		//System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");

		List<Ley> registroTabla = new ArrayList<>();

		try {
			
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle")));
            
            if(ultimaPag)
            	wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("ctl00_ContentPlaceHolder1_GwDetalle_ctl23_ImgBtnSiguiente")));
            else
            	Thread.sleep(5000);
            
            // Extraer las leyes de la tabla
            WebElement tabla = driver.findElement(By.id("ctl00_ContentPlaceHolder1_GwDetalle"));
            List<WebElement> filas = tabla.findElements(By.tagName("tr"));
            
            for (WebElement fila : filas) {
            	List<WebElement> columnas = fila.findElements(By.tagName("td"));
            	
            	if (columnas.size() >= 4) {
                    String norma = columnas.get(0).getText();
                    int numero = Integer.parseInt(columnas.get(1).getText());
                    String publicacion = columnas.get(2).getText();
                    String denominacion = columnas.get(3).getText();
                    
                    WebElement enlace = columnas.get(1).findElement(By.tagName("a"));
                    
                    String href = enlace.getAttribute("href");
                    
                    // Decodifica los caracteres especiales
                    String hrefDecodificado = UtilitarioScraping.decodificarURL(href);
                    String hrefUrl = hrefDecodificado.substring(hrefDecodificado.indexOf("'") + 1, hrefDecodificado.lastIndexOf("'"));

                    Ley registro = Ley.builder()
                    		.norma(norma)
                    		.numero(numero)
                    		.publicacion(publicacion)
                    		.denominacion(denominacion)
                    		.linkSegundaPagina(hrefUrl)
                    		.build();
                    		
                    registroTabla.add(registro);
                    
                }
            }

		} catch (Exception e) {
			e.printStackTrace();
		} 

		return registroTabla;
	}
	
	public Map<String, String> scrapingSecondPage(String url){ // debe tomar entre 3 y 5 seg para cada URL
		
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");
		//WebDriver driver = null;
		
		Map<String, String> urlsMap = new HashMap<>(); 
		
		try {
			ChromeOptions options = new ChromeOptions();
			driver = new ChromeDriver(options);
            driver.get(url);
            
            // Esperar que cargue la pagina
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("box_right_int")));
            
            // Buscar el primer elemento <li> dentro del contenedor
            WebElement primerElemento = driver.findElement(By.cssSelector("#box_right_int ul li:first-child"));
            
            // Obtener el valor del atributo 'class'
            String clase = primerElemento.getAttribute("class");
            System.out.println("Clase del primer elemento: " + clase);
            
            String linkTextoNormaLegal;
            String linkFichaTecnica;
            String linkTerceraPagina;
            
            if("btnpress".equals(clase)) {
            	WebElement iframe1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2")));
            	
            	linkTextoNormaLegal = UtilitarioScraping.decodificarURL(iframe1.getAttribute("src"));
            	
            	// Esperar que aparezca el link con el texto "Ficha técnica"
            	WebElement enlaceFichaTecnica = wait.until(
            	    ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Ficha técnica')]"))
            	);
            	
            	UtilitarioScraping.tiempoClic();
            	
            	enlaceFichaTecnica.click();
            	
            	WebElement iframe2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2"))); // ejemplo si aparece un iframe
            	linkFichaTecnica = UtilitarioScraping.decodificarURL(iframe2.getAttribute("src"));
            	
            	
            	WebElement enlaceExpediente = wait.until(
                			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Expediente del Proyecto de Ley')]"))
                	);
            	
            	String onclickValue = enlaceExpediente.getAttribute("onclick");
            	// Extraer la URL usando una expresión regular simple:
            	Pattern pattern = Pattern.compile("window\\.open\\('([^']+)'");
            	Matcher matcher = pattern.matcher(onclickValue);
            	
            	String href = "";
            	
            	if (matcher.find()) 
            	    href = matcher.group(1);
            	   
            	linkTerceraPagina = UtilitarioScraping.decodificarURL(href);

            	
            }else {
            	
            	linkTextoNormaLegal = null;
            	WebElement enlaceFichaTecnica = wait.until(
            			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Ficha técnica')]"))
                	);
            	
            	UtilitarioScraping.tiempoClic();
            	
            	enlaceFichaTecnica.click();
            	
            	WebElement iframe2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2"))); // ejemplo si aparece un iframe
            	linkFichaTecnica = UtilitarioScraping.decodificarURL(iframe2.getAttribute("src"));
            	
            	WebElement enlaceExpediente = wait.until(
                			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Expediente del Proyecto de Ley')]"))
                	);
            	
            	String onclickValue = enlaceExpediente.getAttribute("onclick");
            	
            	// Extraer la URL usando una expresión regular simple:
            	Pattern pattern = Pattern.compile("window\\.open\\('([^']+)'");
            	Matcher matcher = pattern.matcher(onclickValue);
            	
            	String href = "";
            	
            	if (matcher.find()) 
            	    href = matcher.group(1);

            	linkTerceraPagina = UtilitarioScraping.decodificarURL(href);
            	
            }

            urlsMap.put("linkTextoNormaLegal", linkTextoNormaLegal);
            urlsMap.put("linkFichaTecnica", linkFichaTecnica);
            urlsMap.put("linkTerceraPagina", linkTerceraPagina);
            
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (driver != null) {
                driver.quit();
            }
		}
	
		return urlsMap;
	}
	
	public Map<String, String> scrapingSecondPage(WebDriver driver, String url){
		
		System.out.printf("[%s] Iniciando scraping de %s%n",
			    Thread.currentThread().getName(),
			    url);
		
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");
		
		Map<String, String> urlsMap = new HashMap<>(); 
		
		try {
            driver.get(url);
            
            // Esperar que cargue la pagina
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("box_right_int")));
            
            // Buscar el primer elemento <li> dentro del contenedor
            WebElement primerElemento = driver.findElement(By.cssSelector("#box_right_int ul li:first-child"));
            
            // Obtener el valor del atributo 'class'
            String clase = primerElemento.getAttribute("class");
            System.out.println("Clase del primer elemento: " + clase);
            
            String linkTextoNormaLegal;
            String linkFichaTecnica;
            String linkTerceraPagina;
            
            if("btnpress".equals(clase)) {
            	WebElement iframe1 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2")));
            	
            	linkTextoNormaLegal = UtilitarioScraping.decodificarURL(iframe1.getAttribute("src"));
            	
            	// Esperar que aparezca el link con el texto "Ficha técnica"
            	WebElement enlaceFichaTecnica = wait.until(
            	    ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Ficha técnica')]"))
            	);
            	
            	UtilitarioScraping.tiempoClic();
            	
            	enlaceFichaTecnica.click();
            	
            	WebElement iframe2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2"))); // ejemplo si aparece un iframe
            	linkFichaTecnica = UtilitarioScraping.decodificarURL(iframe2.getAttribute("src"));
            	
            	
            	WebElement enlaceExpediente = wait.until(
                			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Expediente del Proyecto de Ley')]"))
                	);
            	
            	String onclickValue = enlaceExpediente.getAttribute("onclick");
            	// Extraer la URL usando una expresión regular simple:
            	Pattern pattern = Pattern.compile("window\\.open\\('([^']+)'");
            	Matcher matcher = pattern.matcher(onclickValue);
            	
            	String href = "";
            	
            	if (matcher.find()) 
            	    href = matcher.group(1);
            	   
            	linkTerceraPagina = UtilitarioScraping.decodificarURL(href);

            	
            }else {
            	
            	linkTextoNormaLegal = null;
            	WebElement enlaceFichaTecnica = wait.until(
            			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Ficha técnica')]"))
                	);
            	
            	UtilitarioScraping.tiempoClic();
            	
            	enlaceFichaTecnica.click();
            	
            	WebElement iframe2 = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("windowO2"))); // ejemplo si aparece un iframe
            	linkFichaTecnica = UtilitarioScraping.decodificarURL(iframe2.getAttribute("src"));
            	
            	WebElement enlaceExpediente = wait.until(
                			ExpectedConditions.elementToBeClickable(By.xpath("//a[contains(text(),'Expediente del Proyecto de Ley')]"))
                	);
            	
            	String onclickValue = enlaceExpediente.getAttribute("onclick");
            	
            	// Extraer la URL usando una expresión regular simple:
            	Pattern pattern = Pattern.compile("window\\.open\\('([^']+)'");
            	Matcher matcher = pattern.matcher(onclickValue);
            	
            	String href = "";
            	
            	if (matcher.find()) 
            	    href = matcher.group(1);

            	linkTerceraPagina = UtilitarioScraping.decodificarURL(href);
            	
            }

            urlsMap.put("linkTextoNormaLegal", linkTextoNormaLegal);
            urlsMap.put("linkFichaTecnica", linkFichaTecnica);
            urlsMap.put("linkTerceraPagina", linkTerceraPagina);
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return urlsMap;
	}
	
	public Map<String, Object> extraerDetalleProyectoLey(WebDriver driver, String url) {
		
		System.out.printf("[%s] Iniciando scraping de %s%n",Thread.currentThread().getName(),url);
		System.setProperty("webdriver.chrome.driver", "./src/main/resources/chromedriver/chromedriver.exe");
		
		Map<String, Object> urlsMap = new HashMap<>(); 
		
		
		try {
				driver.get(url);
				WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
			    WebElement container = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("p-fieldset-0-content")));
	
			    // Utilidad para obtener texto de un campo simple (div -> <b>Etiqueta</b><br>valor)
			    Function<String, String> getCampoTexto = (String etiqueta) -> {
			        try {
			        	WebDriverWait localWait = new WebDriverWait(driver, Duration.ofSeconds(20));

			            // Espera a que el div con la etiqueta esté presente
			            WebElement div = localWait.until(ExpectedConditions.presenceOfElementLocated(
			                By.xpath(".//div[b[contains(.,'" + etiqueta + "')]]")
			            ));
			            
			            return div.getText().replace(etiqueta, "").trim();
			        } catch (NoSuchElementException e) {
			            return null;
			        }
			    };
	
			    // Campos simples
			    urlsMap.put("periodoParlamentario", getCampoTexto.apply("Periodo Parlamentario"));
			    urlsMap.put("legislatura", getCampoTexto.apply("Legislatura"));
			    urlsMap.put("fechaPresentacion", getCampoTexto.apply("Fecha de Presentación"));
			    urlsMap.put("proponente", getCampoTexto.apply("Proponente"));
			    urlsMap.put("titulo", getCampoTexto.apply("Título"));
			    urlsMap.put("sumilla", getCampoTexto.apply("Sumilla"));
			    urlsMap.put("observaciones", getCampoTexto.apply("Observaciones"));
			    urlsMap.put("grupoParlamentario", getCampoTexto.apply("Grupo Parlamentario"));
			    urlsMap.put("ultimoEstado", getCampoTexto.apply("Último Estado"));
	
			    // Autor principal
			    try {
			        WebElement divAutor = container.findElement(By.xpath(".//div[b[contains(.,'Autor Principal')]]"));
			        
			        List<WebElement> listas = divAutor.findElements(By.tagName("ul"));
			        
			        if (!listas.isEmpty()) {
			            List<WebElement> autores = divAutor.findElements(By.xpath(".//ul//a"));
			            
			            List<String> nombresAutores = autores.stream()
			                .map(a -> a.getText().trim())
			                .collect(Collectors.toList());
			            
			            urlsMap.put("autorPrincipal", nombresAutores);
			            
			        } else {
			        	List<String> listaConUnElemento = Arrays.asList("- -");
			        	urlsMap.put("autorPrincipal", listaConUnElemento);
			        }

			    } catch (NoSuchElementException e) {
			        urlsMap.put("autorPrincipal", Collections.emptyList());
			    }
	
			    // Coautores
			    try {
			        WebElement divCoautores = container.findElement(By.xpath(".//div[b[contains(.,'Coautores')]]"));
			        
			        List<WebElement> verMasLinks = divCoautores.findElements(By.xpath(".//a[contains(text(),'Ver más...')]"));
			        if (!verMasLinks.isEmpty()) {
			            WebElement verMas = verMasLinks.get(0);
			            
			            wait.until(ExpectedConditions.elementToBeClickable(verMas));
			            
			            verMas.click();
			            Thread.sleep(3000); // Puedes reemplazar con un WebDriverWait más fino si hay animación
			        }

			        List<WebElement> listas = divCoautores.findElements(By.tagName("ul"));

			        if (!listas.isEmpty()) {
			            List<WebElement> coautores = divCoautores.findElements(By.xpath(".//ul//a[not(contains(text(),'Ver más...'))]"));
			            
			            List<String> nombresCoautores = coautores.stream()
			                .map(a -> a.getText().trim())
			                .filter(texto -> !texto.isEmpty())
			                .collect(Collectors.toList());
			            
			            urlsMap.put("coautores", nombresCoautores);
			            
			        } else {
			        	List<String> listaConUnElemento = Arrays.asList("- -");
			        	urlsMap.put("coautores", listaConUnElemento);
			        }

			    } catch (NoSuchElementException e) {
			        urlsMap.put("coautores", Collections.emptyList());
			    } catch (InterruptedException e) {
			        Thread.currentThread().interrupt();
			    }
			    
			    // Adherentes
			    try {
			        WebElement divAdherentes = container.findElement(By.xpath(".//div[b[contains(.,'Adherentes')]]"));
			        
			        List<WebElement> listas = divAdherentes.findElements(By.tagName("ul"));
			        
			        if (!listas.isEmpty()) {
			            List<WebElement> adherentes = divAdherentes.findElements(By.xpath(".//ul//a"));
			            
			            List<String> nombresAdhrentes = adherentes.stream()
			                .map(a -> a.getText().trim())
			                .collect(Collectors.toList());
			            
			            urlsMap.put("adhrentes", nombresAdhrentes);
			            
			        } else {
			        	List<String> listaConUnElemento = Arrays.asList("- -");
			        	urlsMap.put("adhrentes", listaConUnElemento);
			        }

			    } catch (NoSuchElementException e) {
			        urlsMap.put("adhrentes", Collections.emptyList());
			    }
			    
			    // Comisiones
			    try {
			        WebElement divComisiones = container.findElement(By.xpath(".//div[b[contains(.,'Comisiones')]]"));
			        
			        List<WebElement> listas = divComisiones.findElements(By.tagName("ul"));
			        
			        if (!listas.isEmpty()) {
			            List<WebElement> comisiones = divComisiones.findElements(By.xpath(".//ul//span"));
			            
			            List<String> nombresComisiones = comisiones.stream()
			                .map(a -> a.getText().trim())
			                .collect(Collectors.toList());
			            
			            urlsMap.put("comisiones", nombresComisiones);
			            
			        } else {
			        	List<String> listaConUnElemento = Arrays.asList("- -");
			        	urlsMap.put("comisiones", listaConUnElemento);
			        }

			    } catch (NoSuchElementException e) {
			        urlsMap.put("comisiones", Collections.emptyList());
			    }
			    
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return urlsMap;
	}
	
	
	
}
