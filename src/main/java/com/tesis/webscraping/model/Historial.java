package com.tesis.webscraping.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "historial")
public class Historial {

	@Id
	private String id;
	private String norma; // LEY - RESOLUCION LEGISLATIVA ...
	private int numero;
	private String publicacion;
	private String denominacion;
	private String linkSegundaPagina;
	@CreatedDate
	private Date fechaRegistro;
}
