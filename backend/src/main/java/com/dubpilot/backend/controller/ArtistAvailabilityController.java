package com.dubpilot.backend.controller;

import com.dubpilot.backend.dto.AvailabilityCreateRequest;
import com.dubpilot.backend.entity.Artist;
import com.dubpilot.backend.entity.ArtistAvailability;
import com.dubpilot.backend.repository.ArtistAvailabilityRepository;
import com.dubpilot.backend.repository.ArtistRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app", "https://aozene-pt8vv9zy3-harsha-d199.vercel.app"})
public class ArtistAvailabilityController {

    private final ArtistAvailabilityRepository availabilityRepository;
    private final ArtistRepository artistRepository;

    public ArtistAvailabilityController(ArtistAvailabilityRepository availabilityRepository,
                                        ArtistRepository artistRepository) {
        this.availabilityRepository = availabilityRepository;
        this.artistRepository = artistRepository;
    }

    @GetMapping
    public List<ArtistAvailability> getAllAvailability() {
        return availabilityRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtistAvailability> getAvailabilityById(@PathVariable Long id) {
        return availabilityRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ArtistAvailability> createAvailability(@Valid @RequestBody AvailabilityCreateRequest request) {
        Optional<Artist> artistOptional = artistRepository.findById(request.getArtistId());

        if (artistOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ArtistAvailability availability = new ArtistAvailability();
        availability.setArtist(artistOptional.get());
        availability.setDate(request.getDate());
        availability.setStartTime(request.getStartTime());
        availability.setEndTime(request.getEndTime());

        ArtistAvailability saved = availabilityRepository.save(availability);
        return ResponseEntity.status(201).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id) {
        if (!availabilityRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        availabilityRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}