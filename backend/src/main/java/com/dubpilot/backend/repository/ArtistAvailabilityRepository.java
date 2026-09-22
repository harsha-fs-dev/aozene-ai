package com.dubpilot.backend.repository;

import com.dubpilot.backend.entity.ArtistAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtistAvailabilityRepository extends JpaRepository<ArtistAvailability, Long> {

    List<ArtistAvailability> findByArtistIdOrderByIdDesc(Long artistId);

}