package com.tesis.webscraping.repository;


import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.tesis.webscraping.model.ErrorScraping;

public interface IErrorScrapingRepo extends MongoRepository<ErrorScraping, String> {
	
	@Query("{ 'estado': 1 }")
	List<ErrorScraping> findLeyesActivas();

}
