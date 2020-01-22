package com.gessi.dependency_detection.repository;

import com.gessi.dependency_detection.entity.Dependency;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DependencyRepository extends CrudRepository<Dependency, String> {

    @Query("FROM Dependency WHERE fromid = ?1 AND toid = ?2 OR fromid = ?2 AND toid = ?1")
    List<Dependency> findByIds(String fromid, String toid);

}
