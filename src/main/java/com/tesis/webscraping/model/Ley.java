package com.tesis.webscraping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "leyes")
public class Ley {

	@Id
	private String id;
	private String norma; // LEY - RESOLUCION LEGISLATIVA ...
	private int numero;
	private String publicacion;
	private String denominacion;
	private String linkSegundaPagina;
	private String linkTerceraPagina;
	private String periodoParlamentario;
	private String legislatura;
	private String fechaPresentacion; // 2022 - 2025
	private String proponente;
	private String titulo;
	private String sumilla;
	private String observaciones;
	private List<String> autorPrincipal;
	private List<String> coautores;
	private List<String> adhrentes;
	private String grupoParlamentario;
	private List<String> comisiones;
	private String ultimoEstado;
	private String linkTextoNormaLegal;
	private String linkFichaTecnica;
	
	
	
}
