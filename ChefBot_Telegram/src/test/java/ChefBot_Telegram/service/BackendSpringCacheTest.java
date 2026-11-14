package ChefBot_Telegram.service;

import ChefBot_Telegram.dto.UserProfileDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserFollowRepository;
import ChefBot_Telegram.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BackendSpringCacheTest {

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

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        if (cacheManager.getCache("exploreRecipes") != null) cacheManager.getCache("exploreRecipes").clear();
        if (cacheManager.getCache("chefProfiles") != null) cacheManager.getCache("chefProfiles").clear();
        userFollowRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cachear em memória o feed público de explorar e invalidar após nova receita")
    void shouldCacheExploreRecipesAndEvictOnNewRecipe() {
        AppUser chef = appUserRepository.save(new AppUser("cache@chef.com", "chefcache", "123456", "Chef Cache"));
        var recipeUser = userService.getOrCreateRecipeUser(chef);

        // 1. Primeira chamada ao feed: deve buscar do banco e gravar no cache
        List<Recipe> initialFeed = recipeService.getPublicRecipes(null);
        assertTrue(initialFeed.isEmpty());

        // Verifica que o cache agora contém a entrada 'all'
        assertNotNull(cacheManager.getCache("exploreRecipes"));
        assertNotNull(cacheManager.getCache("exploreRecipes").get("all"));

        // 2. Salva uma receita via service (que possui @CacheEvict)
        var dto = new ChefBot_Telegram.dto.RecipeResponseDTO(
            true, null, "Pão de Queijo Cache", "lanche", "Chef Cache", 30, 4, List.of(), List.of()
        );
        recipeService.saveRecipeFromDTO(recipeUser.getTelegramChatId(), chef.getName(), dto, "Texto");

        // O cache deve ter sido invalidado (evicted)
        assertNull(cacheManager.getCache("exploreRecipes").get("all"));

        // 3. Nova busca: deve recarregar a nova receita e re-popular o cache
        List<Recipe> updatedFeed = recipeService.getPublicRecipes(null);
        assertEquals(1, updatedFeed.size());
        assertNotNull(cacheManager.getCache("exploreRecipes").get("all"));
    }

    @Test
    @DisplayName("Deve cachear o perfil do chef e invalidar após toggleFollow")
    void shouldCacheChefProfileAndEvictOnToggleFollow() {
        AppUser chefA = appUserRepository.save(new AppUser("chefa@test.com", "chefa", "123456", "Chef A"));
        AppUser chefB = appUserRepository.save(new AppUser("chefb@test.com", "chefb", "123456", "Chef B"));

        // 1. Busca perfil de Chef A (armazena no cache)
        UserProfileDTO p1 = userService.getChefProfile(chefB, "chefa");
        assertEquals(0, p1.followersCount());
        assertFalse(p1.isFollowing());

        assertNotNull(cacheManager.getCache("chefProfiles"));
        String cacheKey = "chefb:chefa";
        assertNotNull(cacheManager.getCache("chefProfiles").get(cacheKey));

        // 2. Chef B segue Chef A (dispara @CacheEvict em chefProfiles)
        userService.toggleFollow(chefB, "chefa");

        // O cache deve ter sido limpo
        assertNull(cacheManager.getCache("chefProfiles").get(cacheKey));

        // 3. Nova busca do perfil: reflete o novo status de seguidor
        UserProfileDTO p2 = userService.getChefProfile(chefB, "chefa");
        assertEquals(1, p2.followersCount());
        assertTrue(p2.isFollowing());
    }
}
