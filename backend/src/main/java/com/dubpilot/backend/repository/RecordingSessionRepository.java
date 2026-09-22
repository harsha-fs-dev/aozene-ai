package com.dubpilot.backend.repository;

import com.dubpilot.backend.entity.RecordingSession;
import com.dubpilot.backend.entity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordingSessionRepository extends JpaRepository<RecordingSession, Long> {

    List<RecordingSession> findByArtistIdAndStatus(Long artistId, SessionStatus status);

    List<RecordingSession> findByEpisodeId(Long episodeId);

    List<RecordingSession> findByArtistId(Long artistId);

}