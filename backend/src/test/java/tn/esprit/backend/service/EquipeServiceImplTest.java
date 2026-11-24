package tn.esprit.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.entity.Equipe;
import tn.esprit.backend.entity.Projet;
import tn.esprit.backend.repository.EntrepriseRepository;
import tn.esprit.backend.repository.EquipeRepository;
import tn.esprit.backend.repository.ProjetRepository;
import tn.esprit.backend.service.impl.EquipeServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeServiceImplTest {

    @Mock
    EquipeRepository equipeRepository;
    @Mock
    EntrepriseRepository entrepriseRepository;
    @Mock
    ProjetRepository projetRepository;

    @InjectMocks
    EquipeServiceImpl equipeService;

    private Equipe equipe(Long id) {
        Equipe e = new Equipe();
        e.setId(id);
        e.setNom("Equipe " + id);
        e.setSpecialite("DevOps");
        e.setProjets(new ArrayList<>());
        return e;
    }

    @Test
    void addAndUpdateEquipe_delegateToRepository() {
        Equipe e = equipe(1L);
        when(equipeRepository.save(e)).thenReturn(e);

        assertSame(e, equipeService.addEquipe(e));
        assertSame(e, equipeService.updateEquipe(e));
        verify(equipeRepository, times(2)).save(e);
    }

    @Test
    void deleteEquipe_deletesById() {
        equipeService.deleteEquipe(3L);
        verify(equipeRepository).deleteById(3L);
    }

    @Test
    void getEquipeById_returnsNullWhenMissing() {
        when(equipeRepository.findById(9L)).thenReturn(Optional.empty());
        assertNull(equipeService.getEquipeById(9L));
    }

    @Test
    void getAllEquipes_and_byEntreprise() {
        when(equipeRepository.findAll()).thenReturn(List.of(equipe(1L), equipe(2L)));
        when(equipeRepository.findByEntrepriseId(7L)).thenReturn(List.of(equipe(1L)));

        assertEquals(2, equipeService.getAllEquipes().size());
        assertEquals(1, equipeService.getEquipesByEntreprise(7L).size());
    }

    @Test
    void assignEquipeToEntreprise_setsEntreprise() {
        Equipe e = equipe(1L);
        Entreprise ent = new Entreprise();
        ent.setId(10L);
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(entrepriseRepository.findById(10L)).thenReturn(Optional.of(ent));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe result = equipeService.assignEquipeToEntreprise(1L, 10L);

        assertEquals(10L, result.getEntreprise().getId());
    }

    @Test
    void assignEquipeToProjet_addsProjet() {
        Equipe e = equipe(1L);
        Projet p = new Projet();
        p.setId(20L);
        when(equipeRepository.findById(1L)).thenReturn(Optional.of(e));
        when(projetRepository.findById(20L)).thenReturn(Optional.of(p));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipe result = equipeService.assignEquipeToProjet(1L, 20L);

        assertEquals(1, result.getProjets().size());
        assertEquals(20L, result.getProjets().get(0).getId());
    }
}
