package ChefBot_Telegram.controller;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeWebDTO;
import ChefBot_Telegram.dto.UserProfileDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.service.RecipeService;
import ChefBot_Telegram.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final RecipeService recipeService;

    public UserController(UserService userService, RecipeService recipeService) {
        this.userService = userService;
        this.recipeService = recipeService;
    }

    /**
     * Retorna os dados do perfil público de um chef por @username.
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> getChefProfile(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable String username) {
        try {
            UserProfileDTO profile = userService.getChefProfile(currentUser, username);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Retorna as receitas públicas de um determinado chef.
     */
    @GetMapping("/{username}/recipes")
    public ResponseEntity<?> getChefPublicRecipes(@PathVariable String username) {
        try {
            List<Recipe> recipes = userService.getChefPublicRecipes(username);
            List<RecipeWebDTO> dtoList = recipes.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Alterna o estado de seguir/deixar de seguir (Follow / Unfollow).
     */
    @PostMapping("/{username}/follow")
    public ResponseEntity<?> toggleFollow(
            @AuthenticationPrincipal AppUser currentUser,
            @PathVariable String username) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean following = userService.toggleFollow(currentUser, username);
            return ResponseEntity.ok(Map.of(
                    "username", username,
                    "following", following,
                    "message", following ? "Agora você segue este chef!" : "Você deixou de seguir este chef."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Busca chefs por username ou nome.
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDTO>> searchChefs(
            @AuthenticationPrincipal AppUser currentUser,
            @RequestParam(required = false, defaultValue = "") String query) {
        List<UserProfileDTO> results = userService.searchChefs(currentUser, query);
        return ResponseEntity.ok(results);
    }

    private RecipeWebDTO toDTO(Recipe recipe) {
        List<IngredientDTO> ings = recipe.getIngredients().stream()
                .map(i -> new IngredientDTO(i.getName(), i.getQuantity(), i.getUnit(), i.getNotes()))
                .collect(Collectors.toList());

        List<InstructionDTO> insts = recipe.getInstructions().stream()
                .map(i -> new InstructionDTO(i.getStepNumber(), i.getInstructionText()))
                .collect(Collectors.toList());

        String author = recipeService.resolveAuthorUsername(recipe);

        return new RecipeWebDTO(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getCategory(),
                recipe.getSourceAuthor(),
                recipe.getPrepTimeMinutes(),
                recipe.getPortions(),
                recipe.getRawTranscript(),
                recipe.isPublic(),
                author,
                recipe.getForkedFromUsername(),
                recipe.getForkCount(),
                recipe.getTags(),
                ings,
                insts
        );
    }
}
