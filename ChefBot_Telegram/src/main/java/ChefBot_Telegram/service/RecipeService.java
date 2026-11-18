package ChefBot_Telegram.service;

import ChefBot_Telegram.dto.IngredientDTO;
import ChefBot_Telegram.dto.InstructionDTO;
import ChefBot_Telegram.dto.RecipeResponseDTO;
import ChefBot_Telegram.dto.RecipeUpdateRequestDTO;
import ChefBot_Telegram.model.AppUser;
import ChefBot_Telegram.model.Ingredient;
import ChefBot_Telegram.model.Instruction;
import ChefBot_Telegram.model.Recipe;
import ChefBot_Telegram.model.User;
import ChefBot_Telegram.repository.AppUserRepository;
import ChefBot_Telegram.repository.RecipeRepository;
import ChefBot_Telegram.repository.UserRepository;
import ChefBot_Telegram.util.TelegramHtmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

    public static final List<String> VALID_CATEGORIES = List.of(
        "café da manhã", "almoço", "lanche", "jantar", "ceia", "sobremesa", "bebida", "petisco", "molho"
    );

    private static final Set<String> STOPWORDS = Set.of(
        "de", "da", "do", "das", "dos", "com", "sem", "para", "em", "e", "ou", "a", "o", "as", "os", "um", "uma", "uns", "umas", "no", "na", "nos", "nas"
    );

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final AppUserRepository appUserRepository;

    public RecipeService(UserRepository userRepository, RecipeRepository recipeRepository) {
        this(userRepository, recipeRepository, null);
    }

    @Autowired
    public RecipeService(UserRepository userRepository,
                         RecipeRepository recipeRepository,
                         AppUserRepository appUserRepository) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Normaliza a categoria vinda da IA ou do usuário para uma das opções canônicas.
     */
    public String normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return "lanche";
        }
        String norm = normalizeText(rawCategory);
        if (norm.contains("cafe") || norm.contains("manha") || norm.contains("desjejum")) return "café da manhã";
        if (norm.contains("almoco") || norm.contains("prato principal")) return "almoço";
        if (norm.contains("janta")) return "jantar";
        if (norm.contains("ceia")) return "ceia";
        if (norm.contains("molho") || norm.contains("pesto") || norm.contains("vinagrete") || norm.contains("maionese") || norm.contains("pate") || norm.contains("chutney")) return "molho";
        if (norm.contains("bebida") || norm.contains("suco") || norm.contains("drink") || norm.contains("cha") || norm.contains("vitamina") || norm.contains("smoothie") || norm.contains("coquetel")) return "bebida";
        if (norm.contains("petisco") || norm.contains("tira gosto") || norm.contains("aperitivo") || norm.contains("tira-gosto") || norm.contains("boteco") || norm.contains("canapé")) return "petisco";
        if (norm.contains("sobremesa") || norm.contains("doce") || norm.contains("bolo") || norm.contains("pudim") || norm.contains("torta doce")) return "sobremesa";
        if (norm.contains("lanche") || norm.contains("salgado") || norm.contains("sanduiche")) return "lanche";

        for (String valid : VALID_CATEGORIES) {
            if (normalizeText(valid).equals(norm)) {
                return valid;
            }
        }
        return rawCategory.trim().toLowerCase();
    }

    /**
     * Salva uma receita extraída pela IA para o usuário informado.
     * Inclui proteção contra duplicidade acidental recente (janela de 30 segundos).
     */
    @Transactional
    public Recipe saveRecipeFromDTO(Long telegramChatId, String userName, RecipeResponseDTO dto, String rawTranscript) {
        User user = userRepository.findByTelegramChatId(telegramChatId)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setTelegramChatId(telegramChatId);
                newUser.setName(userName != null ? userName : "Usuário");
                return userRepository.save(newUser);
            });

        String targetTitle = (dto.title() != null && !dto.title().isBlank()) ? dto.title().trim() : "Receita sem título";

        // Trava de idempotência recente (30 segundos)
        List<Recipe> existingUserRecipes = recipeRepository.findByUserTelegramChatId(telegramChatId);
        if (!existingUserRecipes.isEmpty()) {
            java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusSeconds(30);
            for (Recipe r : existingUserRecipes) {
                if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff)) {
                    if (normalizeText(r.getTitle()).equals(normalizeText(targetTitle))) {
                        log.info("Receita duplicada recente detectada ('{}'). Retornando existente ID #{}", targetTitle, r.getId());
                        return r;
                    }
                }
            }
        }

        Recipe recipe = new Recipe();
        recipe.setUser(user);
        recipe.setTitle(targetTitle);
        recipe.setCategory(normalizeCategory(dto.category()));
        recipe.setSourceAuthor(dto.sourceAuthor());
        recipe.setPrepTimeMinutes(dto.prepTimeMinutes());
        recipe.setPortions(dto.portions());

        if (dto.transcription() != null && !dto.transcription().isBlank()) {
            recipe.setRawTranscript(dto.transcription().trim());
        } else {
            recipe.setRawTranscript(rawTranscript);
        }

        if (dto.ingredients() != null) {
            for (IngredientDTO ingDTO : dto.ingredients()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setName(ingDTO.name());
                ingredient.setQuantity(ingDTO.quantity());
                ingredient.setUnit(ingDTO.unit());
                ingredient.setNotes(ingDTO.notes());
                recipe.addIngredient(ingredient);
            }
        }

        if (dto.instructions() != null) {
            for (InstructionDTO instDTO : dto.instructions()) {
                Instruction instruction = new Instruction();
                instruction.setStepNumber(instDTO.stepNumber());
                instruction.setInstructionText(instDTO.description());
                recipe.addInstruction(instruction);
            }
        }

        return recipeRepository.save(recipe);
    }

    /**
     * Atualiza uma receita existente com base no retorno da IA.
     */
    @Transactional
    public Recipe updateRecipeFromDTO(Long telegramChatId, Long recipeId, RecipeResponseDTO dto) {
        Recipe recipe = recipeRepository.findByUserTelegramChatIdAndId(telegramChatId, recipeId)
            .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada para este usuário."));

        if (dto.title() != null && !dto.title().isBlank()) {
            recipe.setTitle(dto.title().trim());
        }
        if (dto.category() != null) {
            recipe.setCategory(dto.category().isBlank() ? null : normalizeCategory(dto.category()));
        }
        if (dto.sourceAuthor() != null) {
            recipe.setSourceAuthor(dto.sourceAuthor().isBlank() ? null : dto.sourceAuthor().trim());
        }
        if (dto.prepTimeMinutes() != null) {
            recipe.setPrepTimeMinutes(dto.prepTimeMinutes() > 0 ? dto.prepTimeMinutes() : null);
        }
        if (dto.portions() != null) {
            recipe.setPortions(dto.portions() > 0 ? dto.portions() : null);
        }

        if (dto.ingredients() != null && !dto.ingredients().isEmpty()) {
            recipe.getIngredients().clear();
            for (IngredientDTO ingDTO : dto.ingredients()) {
                if (ingDTO.name() != null && !ingDTO.name().isBlank()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setName(ingDTO.name().trim());
                    ingredient.setQuantity(ingDTO.quantity());
                    ingredient.setUnit(ingDTO.unit());
                    ingredient.setNotes(ingDTO.notes());
                    recipe.addIngredient(ingredient);
                }
            }
        }

        if (dto.instructions() != null && !dto.instructions().isEmpty()) {
            recipe.getInstructions().clear();
            int step = 1;
            for (InstructionDTO instDTO : dto.instructions()) {
                if (instDTO.description() != null && !instDTO.description().isBlank()) {
                    Instruction instruction = new Instruction();
                    instruction.setStepNumber(instDTO.stepNumber() != null ? instDTO.stepNumber() : step);
                    instruction.setInstructionText(instDTO.description().trim());
                    recipe.addInstruction(instruction);
                    step++;
                }
            }
        }

        return recipeRepository.saveAndFlush(recipe);
    }

    /**
     * Atualização manual enviada pelo usuário via formulário web.
     */
    @Transactional
    public Recipe updateRecipeManual(Long id, RecipeUpdateRequestDTO dto) {
        Recipe recipe = recipeRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Receita não encontrada com ID: " + id));

        if (dto.title() != null && !dto.title().isBlank()) {
            recipe.setTitle(dto.title().trim());
        }
        if (dto.category() != null) {
            recipe.setCategory(dto.category().isBlank() ? null : normalizeCategory(dto.category()));
        }
        if (dto.sourceAuthor() != null) {
            recipe.setSourceAuthor(dto.sourceAuthor().isBlank() ? null : dto.sourceAuthor().trim());
        }
        if (dto.prepTimeMinutes() != null) {
            recipe.setPrepTimeMinutes(dto.prepTimeMinutes() > 0 ? dto.prepTimeMinutes() : null);
        }
        if (dto.portions() != null) {
            recipe.setPortions(dto.portions() > 0 ? dto.portions() : null);
        }

        if (dto.ingredients() != null) {
            recipe.getIngredients().clear();
            for (IngredientDTO ingDTO : dto.ingredients()) {
                if (ingDTO.name() != null && !ingDTO.name().isBlank()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setName(ingDTO.name().trim());
                    ingredient.setQuantity(ingDTO.quantity());
                    ingredient.setUnit(ingDTO.unit());
                    ingredient.setNotes(ingDTO.notes());
                    recipe.addIngredient(ingredient);
                }
            }
        }

        if (dto.instructions() != null) {
            recipe.getInstructions().clear();
            int step = 1;
            for (InstructionDTO instDTO : dto.instructions()) {
                if (instDTO.description() != null && !instDTO.description().isBlank()) {
                    Instruction instruction = new Instruction();
                    instruction.setStepNumber(instDTO.stepNumber() != null ? instDTO.stepNumber() : step);
                    instruction.setInstructionText(instDTO.description().trim());
                    recipe.addInstruction(instruction);
                    step++;
                }
            }
        }

        return recipeRepository.saveAndFlush(recipe);
    }

    @Transactional
    public boolean deleteRecipe(Long telegramChatId, Long recipeId) {
        Optional<Recipe> recipeOpt = recipeRepository.findByUserTelegramChatIdAndId(telegramChatId, recipeId);
        if (recipeOpt.isPresent()) {
            recipeRepository.delete(recipeOpt.get());
            return true;
        }
        return false;
    }

    @Transactional
    public boolean deleteRecipeById(Long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Recipe> findRecipesByChatId(Long telegramChatId) {
        return recipeRepository.findByUserTelegramChatId(telegramChatId);
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> findRecipeByUserAndId(Long telegramChatId, Long recipeId) {
        return recipeRepository.findByUserTelegramChatIdAndId(telegramChatId, recipeId);
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> findRecipeById(Long id) {
        return recipeRepository.findById(id);
    }

    public List<String> extractSearchTokens(String query) {
        if (query == null) return List.of();
        String normalized = normalizeText(query);
        String[] words = normalized.split("[^a-z0-9]+");
        List<String> tokens = new ArrayList<>();
        for (String w : words) {
            if (!w.isBlank() && !STOPWORDS.contains(w)) {
                tokens.add(w);
            }
        }
        return tokens.isEmpty() && !normalized.isBlank() ? List.of(normalized.trim()) : tokens;
    }

    private boolean textMatchesToken(String text, String token) {
        if (text == null || token == null || token.isBlank()) return false;
        String normalizedText = normalizeText(text);
        if (normalizedText.contains(token)) return true;
        if (token.endsWith("s") && token.length() > 3) {
            String singular = token.substring(0, token.length() - 1);
            if (normalizedText.contains(singular)) return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Recipe> searchRecipes(Long telegramChatId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Recipe> allUserRecipes = recipeRepository.findByUserTelegramChatId(telegramChatId);
        return filterAndScoreRecipes(allUserRecipes, query);
    }

    @Transactional(readOnly = true)
    public List<Recipe> searchUserRecipes(Long telegramChatId, String query, String category) {
        List<Recipe> recipes = recipeRepository.findByUserTelegramChatId(telegramChatId);

        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("todos")) {
            String normCategory = normalizeText(category);
            recipes = recipes.stream()
                .filter(r -> r.getCategory() != null && normalizeText(r.getCategory()).contains(normCategory))
                .toList();
        }

        if (query == null || query.isBlank()) {
            return recipes;
        }

        return filterAndScoreRecipes(recipes, query);
    }

    private List<Recipe> filterAndScoreRecipes(List<Recipe> sourceRecipes, String query) {
        if (sourceRecipes == null || sourceRecipes.isEmpty()) {
            return List.of();
        }

        String cleanQuery = query.trim();
        List<String> tokens = extractSearchTokens(cleanQuery);
        String normalizedFullQuery = normalizeText(cleanQuery);

        Long explicitId = null;
        String numeric = cleanQuery.startsWith("#") ? cleanQuery.substring(1).trim() : cleanQuery;
        if (numeric.matches("^\\d+$")) {
            try {
                explicitId = Long.parseLong(numeric);
            } catch (NumberFormatException ignored) {}
        }

        record ScoredRecipe(Recipe recipe, int score) {}
        List<ScoredRecipe> scored = new ArrayList<>();

        for (Recipe r : sourceRecipes) {
            int score = 0;

            if (explicitId != null && r.getId().equals(explicitId)) {
                score += 100;
            }

            String normalizedTitle = normalizeText(r.getTitle());
            if (!normalizedFullQuery.isBlank() && normalizedTitle.contains(normalizedFullQuery)) {
                score += 25;
            }

            int titleTokensMatched = 0;

            for (String token : tokens) {
                if (textMatchesToken(r.getTitle(), token)) {
                    score += 10;
                    titleTokensMatched++;
                }
                if (r.getCategory() != null && textMatchesToken(r.getCategory(), token)) {
                    score += 5;
                }
                if (r.getSourceAuthor() != null && textMatchesToken(r.getSourceAuthor(), token)) {
                    score += 5;
                }
                if (r.getIngredients() != null) {
                    for (Ingredient ing : r.getIngredients()) {
                        if (textMatchesToken(ing.getName(), token) || 
                            (ing.getNotes() != null && textMatchesToken(ing.getNotes(), token))) {
                            score += 4;
                            break;
                        }
                    }
                }
                if (r.getInstructions() != null) {
                    for (Instruction inst : r.getInstructions()) {
                        if (textMatchesToken(inst.getInstructionText(), token)) {
                            score += 2;
                            break;
                        }
                    }
                }
            }

            if (!tokens.isEmpty() && titleTokensMatched == tokens.size()) {
                score += 15;
            }

            if (score > 0) {
                scored.add(new ScoredRecipe(r, score));
            }
        }

        return scored.stream()
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .map(ScoredRecipe::recipe)
                .toList();
    }

    public String normalizeText(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase()
            .trim();
    }

    public String formatRecipeForTelegram(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("🍽️ <b>").append(TelegramHtmlUtil.escapeHtml(recipe.getTitle()))
          .append("</b> <code>(ID: #").append(recipe.getId()).append(")</code>\n\n");

        if (recipe.getSourceAuthor() != null && !recipe.getSourceAuthor().isBlank()) {
            sb.append("👤 <b>Ensinado por:</b> ").append(TelegramHtmlUtil.escapeHtml(recipe.getSourceAuthor())).append("\n");
        }
        if (recipe.getCategory() != null && !recipe.getCategory().isBlank()) {
            sb.append("📂 <b>Categoria:</b> ").append(TelegramHtmlUtil.escapeHtml(recipe.getCategory())).append("\n");
        }
        if (recipe.getPrepTimeMinutes() != null) {
            sb.append("⏱️ <b>Tempo:</b> ").append(recipe.getPrepTimeMinutes()).append(" min");
        }
        if (recipe.getPortions() != null) {
            sb.append(" | 👥 <b>Rendimento:</b> ").append(recipe.getPortions()).append(" porções");
        }
        sb.append("\n\n");

        sb.append("🛒 <b>Ingredientes:</b>\n");
        if (recipe.getIngredients().isEmpty()) {
            sb.append("• Nenhum ingrediente especificado.\n");
        } else {
            for (Ingredient ing : recipe.getIngredients()) {
                sb.append("• ");
                if (ing.getQuantity() != null && !ing.getQuantity().isBlank()) {
                    sb.append(TelegramHtmlUtil.escapeHtml(ing.getQuantity())).append(" ");
                }
                if (ing.getUnit() != null && !ing.getUnit().isBlank()) {
                    sb.append(TelegramHtmlUtil.escapeHtml(ing.getUnit())).append(" de ");
                }
                sb.append(TelegramHtmlUtil.escapeHtml(ing.getName()));
                if (ing.getNotes() != null && !ing.getNotes().isBlank()) {
                    sb.append(" (").append(TelegramHtmlUtil.escapeHtml(ing.getNotes())).append(")");
                }
                sb.append("\n");
            }
        }
        sb.append("\n");

        sb.append("👩‍🍳 <b>Modo de Preparo:</b>\n");
        if (recipe.getInstructions().isEmpty()) {
            sb.append("1. Nenhum passo especificado.\n");
        } else {
            for (Instruction inst : recipe.getInstructions()) {
                sb.append(inst.getStepNumber()).append(". ")
                  .append(TelegramHtmlUtil.escapeHtml(inst.getInstructionText())).append("\n");
            }
        }

        return sb.toString();
    }

    public String formatRecipeSummary(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append("• <code>[#").append(recipe.getId()).append("]</code> <b>")
          .append(TelegramHtmlUtil.escapeHtml(recipe.getTitle())).append("</b>");
        if (recipe.getCategory() != null && !recipe.getCategory().isBlank()) {
            sb.append(" <i>(").append(TelegramHtmlUtil.escapeHtml(recipe.getCategory())).append(")</i>");
        }
        if (recipe.getPrepTimeMinutes() != null) {
            sb.append(" ⏱️ ").append(recipe.getPrepTimeMinutes()).append("m");
        }
        return sb.toString();
    }

    public String resolveAuthorUsername(Recipe recipe) {
        if (recipe == null || recipe.getUser() == null) {
            return null;
        }
        if (appUserRepository == null) {
            return recipe.getUser().getName();
        }
        Long chatId = recipe.getUser().getTelegramChatId();
        if (chatId == null) {
            return recipe.getUser().getName();
        }

        if (chatId <= -1000000000L) {
            Long appUserId = -chatId - 1000000000L;
            return appUserRepository.findById(appUserId)
                .map(AppUser::getUsername)
                .orElse(recipe.getUser().getName());
        }

        return appUserRepository.findByTelegramChatId(chatId)
            .map(AppUser::getUsername)
            .orElse(recipe.getUser().getName());
    }
}
