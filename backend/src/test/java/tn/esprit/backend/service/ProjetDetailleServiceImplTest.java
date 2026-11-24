package tn.esprit.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.entity.Projet;
import tn.esprit.backend.entity.ProjetDetaille;
import tn.esprit.backend.repository.ProjetDetailleRepository;
import tn.esprit.backend.repository.ProjetRepository;
import tn.esprit.backend.service.impl.ProjetDetailleServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetDetailleServiceImplTest {

    @Mock
    ProjetDetailleRepository projetDetailleRepository;
    @Mock
    ProjetRepository projetRepository;

    @InjectMocks
    ProjetDetailleServiceImpl projetDetailleService;

    private ProjetDetaille pd(Long id) {
        ProjetDetaille pd = new ProjetDetaille();
        pd.setId(id);
        pd.setTechnologie("Spring Boot");
        pd.setCoutProvisoire(1500.0);
        pd.setDateDebut(LocalDate.of(2026, 1, 1));
        return pd;
    }

    @Test
    void addAndUpdate_delegateToRepository() {
        ProjetDetaille p = pd(1L);
        when(projetDetailleRepository.save(p)).thenReturn(p);

        assertSame(p, projetDetailleService.addProjetDetaille(p));
        assertSame(p, projetDetailleService.updateProjetDetaille(p));
    }

    @Test
    void delete_deletesById() {
        projetDetailleService.deleteProjetDetaille(6L);
        verify(projetDetailleRepository).deleteById(6L);
    }

    @Test
    void getById_and_getAll_and_byProjet() {
        when(projetDetailleRepository.findById(1L)).thenReturn(Optional.of(pd(1L)));
        when(projetDetailleRepository.findAll()).thenReturn(List.of(pd(1L), pd(2L)));
        when(projetDetailleRepository.findByProjetId(5L)).thenReturn(List.of(pd(1L)));

        assertEquals("Spring Boot", projetDetailleService.getProjetDetailleById(1L).getTechnologie());
        assertEquals(2, projetDetailleService.getAllProjetsDetailles().size());
        assertEquals(1, projetDetailleService.getProjetDetaillesByProjet(5L).size());
    }

    @Test
    void assignProjetDetailleToProjet_setsProjet() {
        ProjetDetaille p = pd(1L);
        Projet projet = new Projet();
        projet.setId(5L);
        when(projetDetailleRepository.findById(1L)).thenReturn(Optional.of(p));
        when(projetRepository.findById(5L)).thenReturn(Optional.of(projet));
        when(projetDetailleRepository.save(any(ProjetDetaille.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(5L, projetDetailleService.assignProjetDetailleToProjet(1L, 5L).getProjet().getId());
    }
}
