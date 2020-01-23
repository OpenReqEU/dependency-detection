package com.gessi.dependency_detection.repository;

import com.gessi.dependency_detection.entity.Dependency;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface DependencyRepository extends CrudRepository<Dependency, String> {

    @Query("FROM Dependency WHERE (fromid = ?1 AND toid = ?2 OR fromid = ?2 AND toid = ?1) AND project_id = ?3 AND analysis_id = ?4")
    List<Dependency> findByIds(String fromid, String toid, String projectId, Long analysisId);

    @Query("FROM Dependency WHERE analysis_id = ?1")
    List<Dependency> findByAnalysis(Long analysisId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Dependency WHERE analysis_id = ?1")
    void deleteByAnalysisId(Long analysisId);
}
