package com.tesis.webscraping.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

public class UtilitarioScraping {

	public static String decodificarURL(String urlCodificada) {
		
		return URLDecoder.decode(urlCodificada, StandardCharsets.UTF_8);
	}
	
	
	public static int cantidadPaginas(String texto) {
		
		Matcher matcher = Pattern.compile("\\d+").matcher(texto);

		int contador = 0;
		String totalPaginas = null;

		while (matcher.find()) {
		    contador++;
		    if (contador == 2) { // segundo número
		        totalPaginas = matcher.group();
		        break;
		    }
		}
		
		return Integer.parseInt(totalPaginas);
	}
	
	public static void tiempoPaginacion() {
		Random random = new Random();
		// Generamos un número aleatorio entre 3000 y 9000 ms
        int tiempoEspera = 3000 + random.nextInt(6000);
        System.out.println("Esperando " + tiempoEspera + " ms antes de ir a la siguiente página...");
        
        try {
            Thread.sleep(tiempoEspera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // buena práctica
        }
        
        //return tiempoEspera;
	}
	
	public static void tiempoClic() {
		Random random = new Random();
		
        int tiempoEspera = 2000 + random.nextInt(4000);
        
        try {
            Thread.sleep(tiempoEspera);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // buena práctica
        }
        
        //return tiempoEspera;
	}
}
