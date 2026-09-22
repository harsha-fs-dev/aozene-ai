package com.dubpilot.backend.repository;

import com.dubpilot.backend.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findByProjectId(Long projectId);

    boolean existsByProjectIdAndEpisodeNumber(Long projectId, Integer episodeNumber);

}