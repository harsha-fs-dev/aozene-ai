package com.dubpilot.backend.repository;

import com.dubpilot.backend.entity.ProductionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionNoteRepository extends JpaRepository<ProductionNote, Long> {

    List<ProductionNote> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<ProductionNote> findByEpisodeIdOrderByCreatedAtDesc(Long episodeId);

    List<ProductionNote> findTop10ByOrderByCreatedAtDesc();

}