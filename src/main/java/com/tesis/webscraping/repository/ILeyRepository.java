package com.tesis.webscraping.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tesis.webscraping.model.Ley;

@Repository
public interface ILeyRepository extends MongoRepository<Ley, String> {

	Page<Ley> findByLinkTextoNormaLegalIsNullAndLinkFichaTecnicaIsNull(Pageable pageable);
	List<Ley> findTop10ByLinkTextoNormaLegalIsNullAndLinkFichaTecnicaIsNull();
	List<Ley> findTop10ByPeriodoParlamentarioIsNullAndTituloIsNull();
}
