package com.wecti.api.service;

import com.wecti.api.domain.Palestrante;
import com.wecti.api.dto.NovoPalestranteRequest;
import com.wecti.api.exception.RecursoNaoEncontradoException;
import com.wecti.api.repository.PalestranteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PalestranteService {

    private final PalestranteRepository palestranteRepository;

    public PalestranteService(PalestranteRepository palestranteRepository) {
        this.palestranteRepository = palestranteRepository;
    }

    public List<Palestrante> listar() {
        return palestranteRepository.findAll();
    }

    public Palestrante buscarPorId(UUID id) {
        return palestranteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Palestrante nao encontrado: " + id));
    }

    public Palestrante criar(NovoPalestranteRequest request) {
        Palestrante palestrante = Palestrante.builder()
                .nome(request.nome())
                .bio(request.bio())
                .build();
        return palestranteRepository.save(palestrante);
    }
}
