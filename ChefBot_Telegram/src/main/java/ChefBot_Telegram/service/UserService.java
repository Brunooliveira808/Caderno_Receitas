package ChefBot_Telegram.service;

import ChefBot_Telegram.dto.UserProfileDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.model.User;
import ChefBot_Telegram.model.UserFollow;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserFollowRepository;
import ChefBot_Telegram.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final UserFollowRepository userFollowRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(
            AppUserRepository appUserRepository,
            UserRepository userRepository,
            RecipeRepository recipeRepository,
            UserFollowRepository userFollowRepository) {
        this.appUserRepository = appUserRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.userFollowRepository = userFollowRepository;
    }

    /**
     * Retorna ou cria a entidade User (relacionada a Recipes) correspondente ao AppUser autenticado.
     * Se o AppUser tiver telegramChatId, utiliza esse telegramChatId. Caso contrário, gera um ID virtual negativo único.
     */
    @Transactional
    public User getOrCreateRecipeUser(AppUser appUser) {
        Long targetChatId = (appUser.getTelegramChatId() != null)
                ? appUser.getTelegramChatId()
                : (-1000000000L - appUser.getId());

        // Se o usuário ainda não tem receitas próprias, mas existem receitas legadas (chatId 999999999L),
        // migra automaticamente essas receitas para este usuário!
        Long legacyChatId = 999999999L;
        List<Recipe> legacyRecipes = recipeRepository.findByUserTelegramChatId(legacyChatId);

        User user = userRepository.findByTelegramChatId(targetChatId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setTelegramChatId(targetChatId);
                    newUser.setName(appUser.getName());
                    return userRepository.save(newUser);
                });

        if (!legacyRecipes.isEmpty()) {
            for (Recipe r : legacyRecipes) {
                r.setUser(user);
            }
            recipeRepository.saveAll(legacyRecipes);
        }

        return user;
    }

    /**
     * Gera um código de vinculação temporário de 6 dígitos no formato "CHEF-XXXX" válido por 15 minutos.
     */
    @Transactional
    public String generateLinkCode(Long appUserId) {
        AppUser user = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        int codeNumber = 1000 + secureRandom.nextInt(9000);
        String code = "CHEF-" + codeNumber;

        user.setLinkCode(code);
        user.setLinkCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        appUserRepository.save(user);

        return code;
    }

    /**
     * Processa a vinculação a partir do comando do Telegram: `/vincular CHEF-XXXX`.
     * Transfere as receitas da conta web desvinculada para o novo chatId compartilhado.
     */
    @Transactional
    public String linkTelegramAccount(Long telegramChatId, String telegramUserName, String linkCode) {
        if (linkCode == null || linkCode.isBlank()) {
            return "❌ Por favor, informe o código gerado no App. Exemplo: <code>/vincular CHEF-1234</code>";
        }

        String normalizedCode = linkCode.trim().toUpperCase();
        Optional<AppUser> appUserOpt = appUserRepository.findByLinkCode(normalizedCode);

        if (appUserOpt.isEmpty()) {
            return "❌ Código de vinculação inválido ou expirado. Gere um novo código no aplicativo.";
        }

        AppUser appUser = appUserOpt.get();
        if (appUser.getLinkCodeExpiresAt() != null && appUser.getLinkCodeExpiresAt().isBefore(LocalDateTime.now())) {
            appUser.setLinkCode(null);
            appUser.setLinkCodeExpiresAt(null);
            appUserRepository.save(appUser);
            return "❌ Este código de vinculação expirou. Por favor, gere um novo código no app.";
        }

        // Verifica se o telegramChatId já pertence a outro usuário
        Optional<AppUser> existingWithChat = appUserRepository.findByTelegramChatId(telegramChatId);
        if (existingWithChat.isPresent() && !existingWithChat.get().getId().equals(appUser.getId())) {
            return "⚠️ Este Telegram já está vinculado à conta: <b>" + existingWithChat.get().getEmail() + "</b>.";
        }

        // Identifica o User virtual antigo da Web (se existia)
        Long oldVirtualChatId = -1000000000L - appUser.getId();
        Optional<User> oldVirtualUserOpt = userRepository.findByTelegramChatId(oldVirtualChatId);

        // Busca ou cria o User com o telegramChatId real
        User realUser = userRepository.findByTelegramChatId(telegramChatId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setTelegramChatId(telegramChatId);
                    newUser.setName(telegramUserName != null ? telegramUserName : appUser.getName());
                    return userRepository.save(newUser);
                });

        // Se existiam receitas criadas pelo usuário virtual na Web, migra para o realUser
        if (oldVirtualUserOpt.isPresent()) {
            User oldVirtualUser = oldVirtualUserOpt.get();
            List<Recipe> oldRecipes = recipeRepository.findByUserTelegramChatId(oldVirtualChatId);
            for (Recipe r : oldRecipes) {
                r.setUser(realUser);
            }
            recipeRepository.saveAll(oldRecipes);
        }

        // Atualiza o AppUser
        appUser.setTelegramChatId(telegramChatId);
        appUser.setLinkCode(null);
        appUser.setLinkCodeExpiresAt(null);
        appUserRepository.save(appUser);

        return "🎉 <b>Conta vinculada com sucesso!</b>\n\nOlá, <b>" + appUser.getName() + "</b> (" + appUser.getEmail() + ")!\n" +
               "Agora todas as receitas que você mandar aqui no Telegram aparecerão no seu App do celular, e as criadas no App estarão disponíveis aqui!";
    }

    /**
     * Alterna o estado de seguir/deixar de seguir (toggle follow) entre dois chefs.
     * Retorna true se agora está seguindo, false se deu unfollow.
     */
    @Transactional
    public boolean toggleFollow(AppUser follower, String targetUsername) {
        if (follower.getUsername().equalsIgnoreCase(targetUsername.trim())) {
            throw new IllegalArgumentException("Você não pode seguir a si mesmo.");
        }

        AppUser targetUser = appUserRepository.findByUsernameIgnoreCase(targetUsername.trim())
                .orElseThrow(() -> new IllegalArgumentException("Chef não encontrado com username: " + targetUsername));

        Optional<UserFollow> existingFollow = userFollowRepository.findByFollowerIdAndFollowingId(follower.getId(), targetUser.getId());
        if (existingFollow.isPresent()) {
            userFollowRepository.delete(existingFollow.get());
            return false;
        } else {
            UserFollow follow = new UserFollow(follower, targetUser);
            userFollowRepository.save(follow);
            return true;
        }
    }

    /**
     * Retorna o perfil público e métricas sociais de um chef.
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getChefProfile(AppUser currentUser, String username) {
        AppUser target = appUserRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Chef não encontrado com username: " + username));

        boolean isSelf = (currentUser != null && currentUser.getId().equals(target.getId()));
        boolean isFollowing = false;
        if (currentUser != null && !isSelf) {
            isFollowing = userFollowRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), target.getId());
        }

        long followersCount = userFollowRepository.countByFollowingId(target.getId());
        long followingCount = userFollowRepository.countByFollowerId(target.getId());

        // Receitas públicas do usuário
        Long targetChatId = (target.getTelegramChatId() != null)
                ? target.getTelegramChatId()
                : (-1000000000L - target.getId());
        long recipesCount = recipeRepository.countByUserTelegramChatIdAndIsPublicTrue(targetChatId);

        return new UserProfileDTO(
                target.getUsername(),
                target.getName(),
                recipesCount,
                followersCount,
                followingCount,
                isFollowing,
                isSelf
        );
    }

    /**
     * Retorna a lista de receitas públicas de um chef.
     */
    @Transactional(readOnly = true)
    public List<Recipe> getChefPublicRecipes(String username) {
        AppUser target = appUserRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Chef não encontrado com username: " + username));

        Long targetChatId = (target.getTelegramChatId() != null)
                ? target.getTelegramChatId()
                : (-1000000000L - target.getId());

        return recipeRepository.findByUserTelegramChatIdAndIsPublicTrue(targetChatId);
    }

    /**
     * Busca chefs por username ou nome.
     */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> searchChefs(AppUser currentUser, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<AppUser> found = appUserRepository.searchChefs(query.trim());
        return found.stream()
                .map(target -> getChefProfile(currentUser, target.getUsername()))
                .toList();
    }
}
