package ChefBot_Telegram.repository;

import ChefBot_Telegram.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByUserTelegramChatId(Long telegramChatId);

    Optional<Recipe> findByUserTelegramChatIdAndId(Long telegramChatId, Long id);

    List<Recipe> findByUserTelegramChatIdAndIsPublicTrue(Long telegramChatId);

    long countByUserTelegramChatIdAndIsPublicTrue(Long telegramChatId);

    @Query("SELECT r FROM Recipe r WHERE r.isPublic = true ORDER BY r.createdAt DESC")
    List<Recipe> findAllPublicRecipes();

    @Query("SELECT r FROM Recipe r WHERE r.isPublic = true AND (" +
           "LOWER(r.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(r.sourceAuthor) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY r.createdAt DESC")
    List<Recipe> searchPublicRecipes(@Param("query") String query);
}


