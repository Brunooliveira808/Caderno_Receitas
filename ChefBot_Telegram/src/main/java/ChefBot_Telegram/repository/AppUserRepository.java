package ChefBot_Telegram.repository;

import ChefBot_Telegram.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    Optional<AppUser> findByUsernameIgnoreCase(String username);
    Optional<AppUser> findByTelegramChatId(Long telegramChatId);
    Optional<AppUser> findByLinkCode(String linkCode);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUsernameIgnoreCase(String username);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM AppUser u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<AppUser> searchChefs(@org.springframework.data.repository.query.Param("query") String query);
}
