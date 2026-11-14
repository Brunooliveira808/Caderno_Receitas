package ChefBot_Telegram.service;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.dto.UserProfileDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.model.User;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserFollowRepository;
import ChefBot_Telegram.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChefFollowAndProfileTest {

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

    @Autowired
    private UserFollowRepository userFollowRepository;

    @BeforeEach
    void setUp() {
        userFollowRepository.deleteAll();
        recipeRepository.deleteAll();
        userRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve alternar entre seguir e deixar de seguir outro chef (Toggle Follow)")
    void shouldToggleFollowCorrectly() {
        AppUser follower = appUserRepository.save(new AppUser("fogaça@chef.com", "fogaca", "pass123", "Henrique Fogaça"));
        AppUser target = appUserRepository.save(new AppUser("paola@chef.com", "paolacarossella", "pass123", "Paola Carosella"));

        // 1. Seguir
        boolean followed = userService.toggleFollow(follower, "paolacarossella");
        assertTrue(followed, "Deve retornar true ao seguir");
        assertEquals(1, userFollowRepository.countByFollowingId(target.getId()));
        assertEquals(1, userFollowRepository.countByFollowerId(follower.getId()));

        // 2. Deixar de seguir
        boolean unfollowed = userService.toggleFollow(follower, "paolacarossella");
        assertFalse(unfollowed, "Deve retornar false ao deixar de seguir");
        assertEquals(0, userFollowRepository.countByFollowingId(target.getId()));
        assertEquals(0, userFollowRepository.countByFollowerId(follower.getId()));
    }

    @Test
    @DisplayName("Não deve permitir que um usuário siga a si mesmo")
    void shouldNotAllowSelfFollowing() {
        AppUser self = appUserRepository.save(new AppUser("jacquin@chef.com", "jacquin", "pass123", "Érick Jacquin"));

        assertThrows(IllegalArgumentException.class, () -> {
            userService.toggleFollow(self, "jacquin");
        }, "Deve lançar exceção ao tentar seguir a si mesmo");
    }

    @Test
    @DisplayName("Deve retornar perfil com contagens exatas de seguidores, seguindo e receitas públicas")
    void shouldReturnAccurateProfileStats() {
        AppUser chefA = appUserRepository.save(new AppUser("chefa@chef.com", "chefa", "pass123", "Chef A"));
        AppUser chefB = appUserRepository.save(new AppUser("chefb@chef.com", "chefb", "pass123", "Chef B"));
        AppUser chefC = appUserRepository.save(new AppUser("chefc@chef.com", "chefc", "pass123", "Chef C"));

        User userChefA = userService.getOrCreateRecipeUser(chefA);

        // Chef A cria 2 receitas públicas e 1 privada
        RecipeResponseDTO pub1 = new RecipeResponseDTO(true, null, "Bolo de Fubá", "sobremesa", "A", 30, 8, List.of(), List.of());
        Recipe r1 = recipeService.saveRecipeFromDTO(userChefA.getTelegramChatId(), chefA.getName(), pub1, "Texto 1");
        r1.setPublic(true);
        recipeRepository.save(r1);

        RecipeResponseDTO pub2 = new RecipeResponseDTO(true, null, "Pão Caseiro", "lanche", "A", 60, 4, List.of(), List.of());
        Recipe r2 = recipeService.saveRecipeFromDTO(userChefA.getTelegramChatId(), chefA.getName(), pub2, "Texto 2");
        r2.setPublic(true);
        recipeRepository.save(r2);

        RecipeResponseDTO priv = new RecipeResponseDTO(true, null, "Receita Secreta", "almoço", "A", 45, 2, List.of(), List.of());
        Recipe r3 = recipeService.saveRecipeFromDTO(userChefA.getTelegramChatId(), chefA.getName(), priv, "Texto 3");
        r3.setPublic(false);
        recipeRepository.save(r3);

        // Chef B e Chef C seguem Chef A
        userService.toggleFollow(chefB, "chefa");
        userService.toggleFollow(chefC, "chefa");

        // Chef A segue Chef B
        userService.toggleFollow(chefA, "chefb");

        // Busca o perfil de Chef A a partir da visão de Chef B
        UserProfileDTO profile = userService.getChefProfile(chefB, "chefa");

        assertEquals("chefa", profile.username());
        assertEquals("Chef A", profile.name());
        assertEquals(2, profile.recipesCount(), "Deve contabilizar apenas receitas públicas");
        assertEquals(2, profile.followersCount(), "Deve ter 2 seguidores (Chef B e C)");
        assertEquals(1, profile.followingCount(), "Deve estar seguindo 1 chef (Chef B)");
        assertTrue(profile.isFollowing(), "Chef B deve ver que já segue Chef A");
        assertFalse(profile.isSelf(), "Não deve marcar como perfil próprio para Chef B");

        // Busca o perfil de Chef A pela própria visão (isSelf)
        UserProfileDTO selfProfile = userService.getChefProfile(chefA, "chefa");
        assertTrue(selfProfile.isSelf(), "Deve marcar isSelf = true para o próprio chef");
        assertFalse(selfProfile.isFollowing());
    }

    @Test
    @DisplayName("Deve buscar chefs por nome ou username")
    void shouldSearchChefs() {
        appUserRepository.save(new AppUser("gordon@chef.com", "ramsay", "pass123", "Gordon Ramsay"));
        appUserRepository.save(new AppUser("jamie@chef.com", "oliver", "pass123", "Jamie Oliver"));

        List<UserProfileDTO> byUsername = userService.searchChefs(null, "ram");
        assertEquals(1, byUsername.size());
        assertEquals("ramsay", byUsername.get(0).username());

        List<UserProfileDTO> byName = userService.searchChefs(null, "jamie");
        assertEquals(1, byName.size());
        assertEquals("oliver", byName.get(0).username());
    }
}
