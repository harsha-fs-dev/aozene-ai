package com.dubpilot.backend.repository;

import com.dubpilot.backend.entity.Project;
import com.dubpilot.backend.entity.ProjectStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByStage(ProjectStage stage);

}