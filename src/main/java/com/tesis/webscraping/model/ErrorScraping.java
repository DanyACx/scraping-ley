package com.tesis.webscraping.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Document(collection = "errores_scraping")
public class ErrorScraping {
	
	@Id
    private String id;
    private int numeroLey;
    private String url;
    private String mensaje;
    @Builder.Default
    private int estado = 1;
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();

}
