package com.tesis.webscraping.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tesis.webscraping.model.Congresista;

@Repository
public interface ICongresistaRepository extends MongoRepository<Congresista, String>{

	List<Congresista> findByVotacionIsNull();
}
