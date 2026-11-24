package tn.esprit.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.entity.Projet;
import tn.esprit.backend.repository.ProjetRepository;
import tn.esprit.backend.service.impl.ProjetServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjetServiceImplTest {

    @Mock
    ProjetRepository projetRepository;

    @InjectMocks
    ProjetServiceImpl projetService;

    private Projet projet(Long id) {
        Projet p = new Projet();
        p.setId(id);
        p.setSujet("Sujet " + id);
        return p;
    }

    @Test
    void addAndUpdateProjet_delegateToRepository() {
        Projet p = projet(1L);
        when(projetRepository.save(p)).thenReturn(p);

        assertSame(p, projetService.addProjet(p));
        assertSame(p, projetService.updateProjet(p));
    }

    @Test
    void deleteProjet_deletesById() {
        projetService.deleteProjet(4L);
        verify(projetRepository).deleteById(4L);
    }

    @Test
    void getProjetById_foundAndMissing() {
        when(projetRepository.findById(1L)).thenReturn(Optional.of(projet(1L)));
        when(projetRepository.findById(2L)).thenReturn(Optional.empty());

        assertEquals("Sujet 1", projetService.getProjetById(1L).getSujet());
        assertNull(projetService.getProjetById(2L));
    }

    @Test
    void getAllProjets_returnsRepositoryContent() {
        when(projetRepository.findAll()).thenReturn(List.of(projet(1L)));
        assertEquals(1, projetService.getAllProjets().size());
    }
}
