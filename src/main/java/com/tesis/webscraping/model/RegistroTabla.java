package com.tesis.webscraping.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistroTabla {

	private String norma;
	private int numero;
	private String publicacion;
	private String denominacion;
	private String linkExpedienteVirtual;
}
