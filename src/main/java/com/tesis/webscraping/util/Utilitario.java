package com.tesis.webscraping.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Utilitario {

	public static String formatFechaSoloDia(Date fecha) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
	    return fecha.toInstant()
	            .atZone(ZoneId.systemDefault())
	            .toLocalDate()
	            .format(formatter)
	            .toString();
	}
	
	public static String formatoFecha(LocalDate fecha) {
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		
	    return LocalDate.now().format(formatter);
	}
}
