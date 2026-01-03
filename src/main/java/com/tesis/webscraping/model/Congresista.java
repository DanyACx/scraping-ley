package com.tesis.webscraping.model;

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
@Document(collection = "congresistas")
public class Congresista {

	@Id
	private String id;
	private String nombreCompleto;
	private String enlaceFichaTecnica;
	private Integer votacion;
	@Builder.Default
	private String periodoParlamentario = "2021-2026";
	private String fechaInicio;
	private String fechaFin;
	private String partidoPolitico;
	private String bancada;
	private String distritoElectoral;
	private String condicion;
	
}
