package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.dto.RecipeUpdateRequestDTO;
import ChefBot_Telegram.dto.RecipeWebDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.model.User;
import ChefBot_Telegram.service.GeminiService;
import ChefBot_Telegram.service.RecipeService;
import ChefBot_Telegram.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private static final Logger log = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;
    private final GeminiService geminiService;
    private final UserService userService;

    public RecipeController(
            RecipeService recipeService,
            GeminiService geminiService,
            UserService userService) {
        this.recipeService = recipeService;
        this.geminiService = geminiService;
        this.userService = userService;
    }

    /**
     * Lista as receitas pertencentes ao usuário autenticado, com filtro por categoria e busca textual inteligente.
     */
    @GetMapping
    public List<RecipeWebDTO> getAllRecipes(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {
        if (user == null) {
            return List.of();
        }

        User recipeUser = userService.getOrCreateRecipeUser(user);
        List<Recipe> recipes = recipeService.searchUserRecipes(recipeUser.getTelegramChatId(), q, category);

        return recipes.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Detalha uma receita pertencente ao usuário autenticado por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecipeWebDTO> getRecipeById(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Long id) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User recipeUser = userService.getOrCreateRecipeUser(user);
        return recipeService.findRecipeByUserAndId(recipeUser.getTelegramChatId(), id)
            .map(recipe -> ResponseEntity.ok(toDTO(recipe)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cria receita a partir de texto vinculada ao usuário autenticado.
     */
    @PostMapping("/from-text")
    public ResponseEntity<?> createFromText(
            @AuthenticationPrincipal AppUser user,
            @RequestBody Map<String, String> payload) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String text = payload.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Texto não informado."));
        }

        try {
            RecipeResponseDTO dto = geminiService.extractRecipeFromText(text);
            if (Boolean.FALSE.equals(dto.isRecipe())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", dto.refusalReason() != null ? dto.refusalReason() : "Não foi detectada uma receita culinária válida."));
            }

            User recipeUser = userService.getOrCreateRecipeUser(user);
            Recipe saved = recipeService.saveRecipeFromDTO(recipeUser.getTelegramChatId(), user.getName(), dto, text);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (Exception e) {
            log.error("Erro ao processar receita por texto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao processar com a IA: " + e.getMessage()));
        }
    }

    /**
     * Cria receita a partir de áudio vinculada ao usuário autenticado.
     */
    @PostMapping(value = "/from-audio", consumes = "multipart/form-data")
    public ResponseEntity<?> createFromAudio(
            @AuthenticationPrincipal AppUser user,
            @RequestParam("audio") MultipartFile audioFile) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo de áudio vazio."));
        }

        try {
            byte[] bytes = audioFile.getBytes();
            String mimeType = audioFile.getContentType() != null ? audioFile.getContentType() : "audio/webm";

            RecipeResponseDTO dto = geminiService.extractRecipeFromAudio(bytes, mimeType);
            if (Boolean.FALSE.equals(dto.isRecipe())) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", dto.refusalReason() != null ? dto.refusalReason() : "Não foi identificada uma receita culinária no áudio."));
            }

            String transcriptToSave = (dto.transcription() != null && !dto.transcription().isBlank())
                ? dto.transcription()
                : "Gravação de áudio Web";

            User recipeUser = userService.getOrCreateRecipeUser(user);
            Recipe saved = recipeService.saveRecipeFromDTO(recipeUser.getTelegramChatId(), user.getName(), dto, transcriptToSave);
            return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
        } catch (Exception e) {
            log.error("Erro ao processar receita por áudio: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao processar áudio com a IA: " + e.getMessage()));
        }
    }

    /**
     * Atualiza uma receita do usuário usando IA.
     */
    @PostMapping("/{id}/ai-update")
    public ResponseEntity<?> updateRecipeWithAi(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String instructions = payload.get("instructions");
        if (instructions == null || instructions.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Instruções de alteração não informadas."));
        }

        User recipeUser = userService.getOrCreateRecipeUser(user);
        Optional<Recipe> existingOpt = recipeService.findRecipeByUserAndId(recipeUser.getTelegramChatId(), id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Recipe existing = existingOpt.get();
            RecipeResponseDTO updatedDto = geminiService.updateRecipe(existing, instructions);
            Recipe updated = recipeService.updateRecipeFromDTO(recipeUser.getTelegramChatId(), id, updatedDto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar receita com IA: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao atualizar com IA: " + e.getMessage()));
        }
    }

    /**
     * Atualiza uma receita manualmente verificando a posse do usuário.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRecipe(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Long id,
            @RequestBody RecipeUpdateRequestDTO dto) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User recipeUser = userService.getOrCreateRecipeUser(user);
        Optional<Recipe> existingOpt = recipeService.findRecipeByUserAndId(recipeUser.getTelegramChatId(), id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Recipe updated = recipeService.updateRecipeManual(id, dto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar receita: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Exclui uma receita verificando a posse do usuário.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRecipe(
            @AuthenticationPrincipal AppUser user,
            @PathVariable Long id) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        User recipeUser = userService.getOrCreateRecipeUser(user);
        Optional<Recipe> existingOpt = recipeService.findRecipeByUserAndId(recipeUser.getTelegramChatId(), id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        boolean deleted = recipeService.deleteRecipeById(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    private RecipeWebDTO toDTO(Recipe recipe) {
        List<IngredientDTO> ings = recipe.getIngredients().stream()
            .map(i -> new IngredientDTO(i.getName(), i.getQuantity(), i.getUnit(), i.getNotes()))
            .collect(Collectors.toList());

        List<InstructionDTO> insts = recipe.getInstructions().stream()
            .map(i -> new InstructionDTO(i.getStepNumber(), i.getInstructionText()))
            .collect(Collectors.toList());

        return new RecipeWebDTO(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getCategory(),
            recipe.getSourceAuthor(),
            recipe.getPrepTimeMinutes(),
            recipe.getPortions(),
            recipe.getRawTranscript(),
            ings,
            insts
        );
    }
}
