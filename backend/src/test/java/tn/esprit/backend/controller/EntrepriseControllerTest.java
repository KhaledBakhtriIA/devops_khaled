package tn.esprit.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.service.IEntrepriseService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test de la couche web uniquement (pas de base de données) :
 * le service est remplacé par un mock.
 */
@WebMvcTest(EntrepriseController.class)
class EntrepriseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    IEntrepriseService entrepriseService;

    private Entreprise entreprise(Long id, String nom) {
        Entreprise e = new Entreprise();
        e.setId(id);
        e.setNom(nom);
        e.setAdresse("Tunis");
        return e;
    }

    @Test
    void getAll_returnsJsonList() throws Exception {
        when(entrepriseService.getAllEntreprises()).thenReturn(List.of(entreprise(1L, "Esprit")));

        mockMvc.perform(get("/entreprise/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Esprit"));
    }

    @Test
    void getById_returnsEntreprise() throws Exception {
        when(entrepriseService.getEntrepriseById(1L)).thenReturn(entreprise(1L, "Esprit"));

        mockMvc.perform(get("/entreprise/get/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void add_postsJsonAndReturnsSavedEntity() throws Exception {
        when(entrepriseService.addEntreprise(any(Entreprise.class))).thenReturn(entreprise(1L, "Esprit"));

        mockMvc.perform(post("/entreprise/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Esprit\",\"adresse\":\"Tunis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void delete_callsService() throws Exception {
        mockMvc.perform(delete("/entreprise/delete/1"))
                .andExpect(status().isOk());

        verify(entrepriseService).deleteEntreprise(1L);
    }
}
