package ChefBot_Telegram.service;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.model.User;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RecipeSocialForkTest {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private UserService userService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve alternar a visibilidade de uma receita com sucesso")
    void shouldToggleVisibility() {
        AppUser chefA = appUserRepository.save(new AppUser("chefa@chef.com", "chefa", "pass123", "Chef A"));
        User userChefA = userService.getOrCreateRecipeUser(chefA);

        RecipeResponseDTO dto = new RecipeResponseDTO(
            true, null, "Risoto de Cogumelos", "almoço", "Chef A", 40, 2,
            List.of(new IngredientDTO("Arroz Arbóreo", "200", "g", null)),
            List.of(new InstructionDTO(1, "Refogue e adicione o caldo"))
        );

        Recipe created = recipeService.saveRecipeFromDTO(userChefA.getTelegramChatId(), chefA.getName(), dto, "Texto");
        assertTrue(created.isPublic(), "A receita deve nascer pública por padrão");

        // Alterna para privada
        Recipe toggled = recipeService.toggleVisibility(userChefA.getTelegramChatId(), created.getId());
        assertFalse(toggled.isPublic(), "A visibilidade deve ser alterada para privada");

        // Alterna de volta para pública
        Recipe toggledBack = recipeService.toggleVisibility(userChefA.getTelegramChatId(), created.getId());
        assertTrue(toggledBack.isPublic(), "A visibilidade deve voltar para pública");
    }

    @Test
    @DisplayName("Deve clonar (fork) receita pública incrementando contador e criando cópia independente")
    void shouldForkPublicRecipeSuccessfully() {
        // Usuário 1: Criador original
        AppUser chefOriginal = appUserRepository.save(new AppUser("original@chef.com", "cheforiginal", "pass123", "Chef Original"));
        User userOriginal = userService.getOrCreateRecipeUser(chefOriginal);

        // Usuário 2: Quem fará o fork
        AppUser chefCopier = appUserRepository.save(new AppUser("copier@chef.com", "chefcopier", "pass123", "Chef Copiador"));
        User userCopier = userService.getOrCreateRecipeUser(chefCopier);

        RecipeResponseDTO dto = new RecipeResponseDTO(
            true, null, "Lasanha Bolonhesa", "almoço", "Chef Original", 60, 6, null,
            true, "cheforiginal", null, 0L, List.of("Massa", "Tradicional"),
            List.of(new IngredientDTO("Massa", "500", "g", null), new IngredientDTO("Molho", "300", "g", null)),
            List.of(new InstructionDTO(1, "Monte as camadas"), new InstructionDTO(2, "Asse por 40 min"))
        );

        Recipe original = recipeService.saveRecipeFromDTO(userOriginal.getTelegramChatId(), chefOriginal.getName(), dto, "Texto original");
        original.setTags(List.of("Massa", "Tradicional"));
        original = recipeRepository.save(original);

        assertEquals(0, original.getForkCount());

        // Executa o Fork
        Recipe forked = recipeService.forkRecipe(userCopier, original.getId());

        // Validações na cópia clonada
        assertNotNull(forked.getId());
        assertNotEquals(original.getId(), forked.getId(), "IDs devem ser distintos");
        assertEquals("cheforiginal", forked.getForkedFromUsername(), "Deve registrar o @username do autor original");
        assertEquals("Lasanha Bolonhesa", forked.getTitle());
        assertEquals("almoço", forked.getCategory());
        assertEquals(2, forked.getIngredients().size());
        assertEquals(2, forked.getInstructions().size());
        assertEquals(List.of("Massa", "Tradicional"), forked.getTags());
        assertEquals(0, forked.getForkCount(), "O fork novo começa com 0 forks");
        assertEquals(userCopier.getId(), forked.getUser().getId(), "A receita pertence agora ao usuário que copiou");

        // Validações na receita original
        Recipe reloadedOriginal = recipeRepository.findById(original.getId()).orElseThrow();
        assertEquals(1, reloadedOriginal.getForkCount(), "O forkCount da receita original deve ser incrementado");
    }

    @Test
    @DisplayName("Não deve permitir fork de receita privada")
    void shouldRejectForkOfPrivateRecipe() {
        AppUser chefA = appUserRepository.save(new AppUser("secret@chef.com", "chefsecret", "pass123", "Chef Secret"));
        User userChefA = userService.getOrCreateRecipeUser(chefA);

        AppUser chefB = appUserRepository.save(new AppUser("curious@chef.com", "chefcurious", "pass123", "Chef Curioso"));
        User userChefB = userService.getOrCreateRecipeUser(chefB);

        RecipeResponseDTO dto = new RecipeResponseDTO(
            true, null, "Molho Secreto da Vó", "molho", "Família", 15, 4,
            List.of(new IngredientDTO("Ingrediente Secreto", "100", "g", null)),
            List.of(new InstructionDTO(1, "Misture tudo"))
        );

        Recipe privateRecipe = recipeService.saveRecipeFromDTO(userChefA.getTelegramChatId(), chefA.getName(), dto, "Texto");
        privateRecipe.setPublic(false);
        recipeRepository.save(privateRecipe);

        assertThrows(IllegalStateException.class, () -> {
            recipeService.forkRecipe(userChefB, privateRecipe.getId());
        }, "Deve lançar exceção ao tentar fazer fork de receita privada");
    }
}
