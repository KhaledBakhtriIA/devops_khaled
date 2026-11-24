package tn.esprit.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.repository.EntrepriseRepository;
import tn.esprit.backend.service.impl.EntrepriseServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceImplTest {

    @Mock
    EntrepriseRepository entrepriseRepository;

    @InjectMocks
    EntrepriseServiceImpl entrepriseService;

    private Entreprise entreprise(Long id, String nom) {
        Entreprise e = new Entreprise();
        e.setId(id);
        e.setNom(nom);
        e.setAdresse("Tunis");
        return e;
    }

    @Test
    void addEntreprise_savesAndReturnsEntity() {
        Entreprise e = entreprise(null, "Esprit");
        when(entrepriseRepository.save(e)).thenReturn(entreprise(1L, "Esprit"));

        Entreprise saved = entrepriseService.addEntreprise(e);

        assertEquals(1L, saved.getId());
        verify(entrepriseRepository).save(e);
    }

    @Test
    void updateEntreprise_savesEntity() {
        Entreprise e = entreprise(1L, "Esprit v2");
        when(entrepriseRepository.save(e)).thenReturn(e);

        assertEquals("Esprit v2", entrepriseService.updateEntreprise(e).getNom());
    }

    @Test
    void deleteEntreprise_deletesById() {
        entrepriseService.deleteEntreprise(5L);
        verify(entrepriseRepository).deleteById(5L);
    }

    @Test
    void getEntrepriseById_returnsEntityWhenFound() {
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise(1L, "Esprit")));
        assertEquals("Esprit", entrepriseService.getEntrepriseById(1L).getNom());
    }

    @Test
    void getEntrepriseById_returnsNullWhenMissing() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(entrepriseService.getEntrepriseById(99L));
    }

    @Test
    void getAllEntreprises_returnsRepositoryContent() {
        when(entrepriseRepository.findAll()).thenReturn(List.of(entreprise(1L, "A"), entreprise(2L, "B")));
        assertEquals(2, entrepriseService.getAllEntreprises().size());
    }
}
