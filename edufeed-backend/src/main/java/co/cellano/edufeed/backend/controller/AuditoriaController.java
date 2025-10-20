package co.cellano.edufeed.backend.controller;

import co.cellano.edufeed.backend.dto.AuditoriaDto;
import co.cellano.edufeed.backend.mapper.AuditoriaMapper;
import co.cellano.edufeed.backend.repository.AuditoriaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaController(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping
    public List<AuditoriaDto> list() {
        return auditoriaRepository.findAll().stream().map(AuditoriaMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaDto> get(@PathVariable UUID id) {
        return auditoriaRepository.findById(id)
                .map(a -> ResponseEntity.ok(AuditoriaMapper.toDto(a)))
                .orElse(ResponseEntity.notFound().build());
    }
}
