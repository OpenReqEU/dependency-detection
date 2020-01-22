package com.gessi.dependency_detection.repository;

import com.gessi.dependency_detection.entity.RequirementEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RequirementRepository extends CrudRepository<RequirementEntity, String> {

    @Query("FROM RequirementEntity WHERE projectId = ?1")
    List<RequirementEntity> findAllByProject(String projectId);

}
