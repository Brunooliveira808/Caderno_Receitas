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
class UserSyncTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @BeforeEach
    void setUp() {
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve gerar código de vinculação e sincronizar receitas criadas na Web e no Telegram")
    void shouldGenerateLinkCodeAndSyncWebAndTelegram() {
        // 1. Cadastra usuário na Web (ainda sem telegramChatId)
        AppUser appUser = new AppUser("chef.bruno@teste.com", "hash123", "Bruno");
        appUser = appUserRepository.save(appUser);

        // 2. Usuário cria receita no App da Web
        User recipeUser = userService.getOrCreateRecipeUser(appUser);
        RecipeResponseDTO webDto = new RecipeResponseDTO(
            true, null, "Risoto de Cogumelos", "almoço", "Bruno", 35, 2, "Risoto",
            List.of(new IngredientDTO("Arroz arbóreo", "1", "xícara", null)),
            List.of(new InstructionDTO(1, "Refogue e adicione o caldo lentamente"))
        );
        recipeService.saveRecipeFromDTO(recipeUser.getTelegramChatId(), appUser.getName(), webDto, "Risoto Web");

        // 3. Usuário gera código no App
        String code = userService.generateLinkCode(appUser.getId());
        assertNotNull(code);
        assertTrue(code.startsWith("CHEF-"));

        // 4. Usuário envia comando no Telegram
        Long telegramChatId = 123456789L;
        String linkResult = userService.linkTelegramAccount(telegramChatId, "BrunoTelegram", code);
        assertTrue(linkResult.contains("Conta vinculada com sucesso"));

        // 5. Verifica se as receitas criadas antes da vinculação agora pertencem ao chatId do Telegram
        List<Recipe> syncedRecipes = recipeRepository.findByUserTelegramChatId(telegramChatId);
        assertEquals(1, syncedRecipes.size());
        assertEquals("Risoto de Cogumelos", syncedRecipes.get(0).getTitle());

        // 6. Usuário dita nova receita no Telegram
        RecipeResponseDTO tgDto = new RecipeResponseDTO(
            true, null, "Suco de Laranja Natural", "bebida", "Bruno", 5, 1, "Suco",
            List.of(new IngredientDTO("Laranja", "3", "unidades", null)),
            List.of(new InstructionDTO(1, "Esprema as laranjas"))
        );
        recipeService.saveRecipeFromDTO(telegramChatId, "BrunoTelegram", tgDto, "Suco Telegram");

        // 7. Ambas as receitas devem ser visíveis no caderno do usuário
        List<Recipe> allUserRecipes = recipeRepository.findByUserTelegramChatId(telegramChatId);
        assertEquals(2, allUserRecipes.size());
    }

    @Test
    @DisplayName("Deve isolar receitas entre diferentes usuários (Multi-tenant)")
    void shouldIsolateRecipesBetweenDifferentUsers() {
        // Usuário 1
        AppUser user1 = appUserRepository.save(new AppUser("user1@teste.com", "hash", "User 1"));
        User rUser1 = userService.getOrCreateRecipeUser(user1);

        // Usuário 2
        AppUser user2 = appUserRepository.save(new AppUser("user2@teste.com", "hash", "User 2"));
        User rUser2 = userService.getOrCreateRecipeUser(user2);

        // Salva receita para o Usuário 1
        RecipeResponseDTO dto1 = new RecipeResponseDTO(
            true, null, "Lasanha do User 1", "almoço", "U1", 60, 4, "Lasanha",
            List.of(), List.of()
        );
        recipeService.saveRecipeFromDTO(rUser1.getTelegramChatId(), user1.getName(), dto1, "Lasanha");

        // Usuário 2 busca e não deve ver a receita do Usuário 1
        List<Recipe> user2Recipes = recipeService.searchUserRecipes(rUser2.getTelegramChatId(), null, "Todos");
        assertTrue(user2Recipes.isEmpty());

        // Usuário 1 busca e vê apenas a sua receita
        List<Recipe> user1Recipes = recipeService.searchUserRecipes(rUser1.getTelegramChatId(), null, "Todos");
        assertEquals(1, user1Recipes.size());
        assertEquals("Lasanha do User 1", user1Recipes.get(0).getTitle());
    }
}
