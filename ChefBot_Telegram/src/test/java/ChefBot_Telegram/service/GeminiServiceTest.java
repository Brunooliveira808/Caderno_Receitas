package ChefBot_Telegram.service;

import ChefBot_Telegram.config.RestClientConfig;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

class GeminiServiceTest {

    private GeminiService geminiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new RestClientConfig().objectMapper();
        RestClient restClient = RestClient.builder().build();
        geminiService = new GeminiService(restClient, objectMapper);
    }

    @Test
    @DisplayName("Deve limpar marcadores de markdown do JSON de forma segura")
    void shouldCleanMarkdownFences() {
        String markdownJson = "```json\n{\"title\": \"Bolo de Milho\"}\n```";
        String cleaned = GeminiService.cleanJsonContent(markdownJson);
        assertEquals("{\"title\": \"Bolo de Milho\"}", cleaned);

        String rawJson = "{\"title\": \"Pudim\"}";
        assertEquals(rawJson, GeminiService.cleanJsonContent(rawJson));
    }

    @Test
    @DisplayName("Deve fazer o parse de resposta da Gemini API com candidatos válidos")
    void shouldParseGeminiResponseSuccessfully() throws Exception {
        String geminiApiResponseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{\\n  \\"isRecipe\\": true,\\n  \\"refusalReason\\": null,\\n  \\"title\\": \\"Bolo de Cenoura da Vovó\\",\\n  \\"category\\": \\"Sobremesa\\",\\n  \\"sourceAuthor\\": \\"Vovó Ana\\",\\n  \\"prepTimeMinutes\\": 45,\\n  \\"portions\\": 8,\\n  \\"ingredients\\": [\\n    {\\n      \\"name\\": \\"Cenoura\\",\\n      \\"quantity\\": \\"3\\",\\n      \\"unit\\": \\"unidades\\",\\n      \\"notes\\": \\"raladas\\"\\n    }\\n  ],\\n  \\"instructions\\": [\\n    {\\n      \\"stepNumber\\": 1,\\n      \\"description\\": \\"Bata as cenouras no liquidificador\\"\\n    }\\n  ]\\n}\\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        RecipeResponseDTO dto = geminiService.parseGeminiResponse(geminiApiResponseBody);

        assertNotNull(dto);
        assertTrue(dto.isRecipe());
        assertEquals("Bolo de Cenoura da Vovó", dto.title());
        assertEquals("Sobremesa", dto.category());
        assertEquals("Vovó Ana", dto.sourceAuthor());
        assertEquals(45, dto.prepTimeMinutes());
        assertEquals(8, dto.portions());
        assertEquals(1, dto.ingredients().size());
        assertEquals("Cenoura", dto.ingredients().get(0).name());
        assertEquals("3", dto.ingredients().get(0).quantity());
        assertEquals(1, dto.instructions().size());
        assertEquals(1, dto.instructions().get(0).stepNumber());
        assertEquals("Bata as cenouras no liquidificador", dto.instructions().get(0).description());
    }

    @Test
    @DisplayName("Deve interpretar recusa quando o texto não for receita culinária")
    void shouldHandleNonRecipeResponse() throws Exception {
        String geminiApiResponseBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"isRecipe\\": false, \\"refusalReason\\": \\"Mensagem não contém uma receita culinária.\\", \\"title\\": null, \\"ingredients\\": [], \\"instructions\\": []}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        RecipeResponseDTO dto = geminiService.parseGeminiResponse(geminiApiResponseBody);

        assertNotNull(dto);
        assertFalse(dto.isRecipe());
        assertEquals("Mensagem não contém uma receita culinária.", dto.refusalReason());
        assertNull(dto.title());
    }

    @Test
    @DisplayName("Deve sanitizar formatos de áudio removendo parâmetros de codec e aceitando formatos comuns")
    void shouldSanitizeAudioMimeTypesCorrectly() {
        assertEquals("audio/webm", GeminiService.sanitizeAudioMimeType("audio/webm;codecs=opus"));
        assertEquals("audio/mp4", GeminiService.sanitizeAudioMimeType("audio/mp4; codecs=mp4a.40.2"));
        assertEquals("audio/mp4", GeminiService.sanitizeAudioMimeType("audio/x-m4a"));
        assertEquals("audio/mp3", GeminiService.sanitizeAudioMimeType("audio/mpeg"));
        assertEquals("audio/ogg", GeminiService.sanitizeAudioMimeType("audio/ogg;codecs=opus"));
        assertEquals("audio/ogg", GeminiService.sanitizeAudioMimeType(null));
        assertEquals("audio/ogg", GeminiService.sanitizeAudioMimeType(""));
    }

    @Test
    @DisplayName("Deve fazer o parse da resposta contendo transcrição de áudio e categoria oficial")
    void shouldParseTranscriptionAndCategory() throws Exception {
        String responseWithTranscript = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"isRecipe\\": true, \\"title\\": \\"Café com Pão de Queijo\\", \\"category\\": \\"café da manhã\\", \\"transcription\\": \\"Bata os ovos com o queijo e asse por 20 minutos\\", \\"ingredients\\":[], \\"instructions\\":[]}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        RecipeResponseDTO dto = geminiService.parseGeminiResponse(responseWithTranscript);

        assertNotNull(dto);
        assertTrue(dto.isRecipe());
        assertEquals("Café com Pão de Queijo", dto.title());
        assertEquals("café da manhã", dto.category());
        assertEquals("Bata os ovos com o queijo e asse por 20 minutos", dto.transcription());
    }

    @Test
    @DisplayName("Deve montar payloads com system_instruction nativo e temperature 0.1")
    void shouldBuildPayloadsWithSystemInstructionAndLowTemperature() {
        var textPayload = geminiService.buildTextPayload("Receita de tapioca");
        assertNotNull(textPayload.get("system_instruction"));
        var genConfig = (java.util.Map<?, ?>) textPayload.get("generationConfig");
        assertEquals("application/json", genConfig.get("responseMimeType"));
        assertEquals(0.1, genConfig.get("temperature"));

        var audioPayload = geminiService.buildAudioPayload(new byte[]{1, 2, 3}, "audio/webm;codecs=opus");
        assertNotNull(audioPayload.get("system_instruction"));
    }

    @Test
    @DisplayName("Deve lançar exceção se candidates vier vazio da Gemini API")
    void shouldThrowExceptionWhenCandidatesEmpty() {
        String emptyResponse = "{\"candidates\": []}";

        Exception exception = assertThrows(RuntimeException.class, () -> {
            geminiService.parseGeminiResponse(emptyResponse);
        });

        assertTrue(exception.getMessage().contains("não retornou nenhuma resposta válida"));
    }
}
