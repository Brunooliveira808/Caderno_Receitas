package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.dto.RecipeWebDTO;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserRepository;
import ChefBot_Telegram.service.GeminiService;
import ChefBot_Telegram.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class RecipeControllerTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private GeminiService geminiService;

    private RecipeController recipeController;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        recipeController = new RecipeController(recipeService, geminiService);
    }

    @Test
    @DisplayName("Deve extrair áudio, normalizar categoria para sobremesa e salvar transcrição literal")
    void shouldSaveTranscriptionFromAudioWhenProvidedByAi() {
        RecipeResponseDTO mockDto = new RecipeResponseDTO(
            true,
            null,
            "Bolo de Cenoura",
            "Doces e Sobremesas",
            "Tia Maria",
            40,
            6,
            "Bata as cenouras com ovos e asse por 40 minutos",
            List.of(new IngredientDTO("Cenoura", "3", "unidades", null)),
            List.of(new InstructionDTO(1, "Misture tudo"))
        );

        Mockito.when(geminiService.extractRecipeFromAudio(any(byte[].class), any(String.class)))
            .thenReturn(mockDto);

        MockMultipartFile audioFile = new MockMultipartFile(
            "audio",
            "gravacao.webm",
            "audio/webm;codecs=opus",
            new byte[]{1, 2, 3, 4}
        );

        ResponseEntity<?> response = recipeController.createFromAudio(audioFile);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof RecipeWebDTO);

        RecipeWebDTO created = (RecipeWebDTO) response.getBody();
        assertEquals("Bolo de Cenoura", created.title());
        assertEquals("sobremesa", created.category());
        assertEquals("Bata as cenouras com ovos e asse por 40 minutos", created.rawTranscript());
    }

    @Test
    @DisplayName("Deve atualizar receita usando o novo endpoint de IA da Web (/api/recipes/{id}/ai-update)")
    void shouldUpdateRecipeViaAiEndpoint() {
        RecipeResponseDTO initialDto = new RecipeResponseDTO(
            true, null, "Pão de Queijo", "café da manhã", "Vó", 30, 4,
            List.of(new IngredientDTO("Polvilho", "500", "g", null)),
            List.of(new InstructionDTO(1, "Asse a 180C"))
        );
        Recipe initialRecipe = recipeService.saveRecipeFromDTO(999999999L, "Web Chef", initialDto, "Texto");

        RecipeResponseDTO updatedDto = new RecipeResponseDTO(
            true, null, "Pão de Queijo", "café da manhã", "Vó", 45, 8,
            List.of(new IngredientDTO("Polvilho", "500", "g", null)),
            List.of(new InstructionDTO(1, "Asse a 180C"))
        );
        Mockito.when(geminiService.updateRecipe(any(Recipe.class), eq("aumente para 8 porções e 45 min")))
            .thenReturn(updatedDto);

        ResponseEntity<?> response = recipeController.updateRecipeWithAi(
            initialRecipe.getId(),
            Map.of("instructions", "aumente para 8 porções e 45 min")
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        RecipeWebDTO updated = (RecipeWebDTO) response.getBody();
        assertNotNull(updated);
        assertEquals(45, updated.prepTimeMinutes());
        assertEquals(8, updated.portions());
    }

    @Test
    @DisplayName("Deve retornar status 422 quando o áudio não contiver receita")
    void shouldRejectNonRecipeAudio() {
        RecipeResponseDTO refusalDto = new RecipeResponseDTO(
            false,
            "Não foi identificada uma receita culinária.",
            null, null, null, null, null, null,
            List.of(), List.of()
        );

        Mockito.when(geminiService.extractRecipeFromAudio(any(byte[].class), any(String.class)))
            .thenReturn(refusalDto);

        MockMultipartFile audioFile = new MockMultipartFile(
            "audio",
            "conversa.webm",
            "audio/webm",
            new byte[]{9, 9, 9}
        );

        ResponseEntity<?> response = recipeController.createFromAudio(audioFile);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
    }
}
