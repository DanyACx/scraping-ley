package com.tesis.webscraping.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tesis.webscraping.model.Historial;

@Repository
public interface IHistorialRepository extends MongoRepository<Historial, String> {

}
