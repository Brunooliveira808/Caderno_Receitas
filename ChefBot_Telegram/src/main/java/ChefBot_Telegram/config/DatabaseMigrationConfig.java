package ChefBot_Telegram.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Bean
    @Order(1)
    public CommandLineRunner migrateDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("Iniciando checagem/migração defensiva do schema do PostgreSQL...");
            try {
                // 1. Garantir que a coluna username existe em app_users
                jdbcTemplate.execute(
                    "ALTER TABLE app_users ADD COLUMN IF NOT EXISTS username VARCHAR(50);"
                );

                // 2. Preencher usernames nulos baseado no prefixo do email
                jdbcTemplate.execute(
                    "UPDATE app_users SET username = LOWER(REGEXP_REPLACE(SPLIT_PART(email, '@', 1), '[^a-zA-Z0-9_.]', '', 'g')) WHERE username IS NULL OR username = '';"
                );

                // 3. Garantir unicidade de username (se não for nulo)
                jdbcTemplate.execute(
                    "DO $$ BEGIN " +
                    "  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_app_users_username') THEN " +
                    "    ALTER TABLE app_users ADD CONSTRAINT uk_app_users_username UNIQUE (username); " +
                    "  END IF; " +
                    "END $$;"
                );

                // 4. Garantir que a tabela password_reset_tokens existe
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                    "  id BIGSERIAL PRIMARY KEY," +
                    "  token VARCHAR(100) NOT NULL UNIQUE," +
                    "  app_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE," +
                    "  expires_at TIMESTAMP NOT NULL," +
                    "  used BOOLEAN NOT NULL DEFAULT FALSE" +
                    ");"
                );

                // 5. Garantir campos sociais em recipes (is_public, forked_from_username, fork_count)
                jdbcTemplate.execute(
                    "ALTER TABLE recipes ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT TRUE;"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE recipes ADD COLUMN IF NOT EXISTS forked_from_username VARCHAR(50);"
                );
                jdbcTemplate.execute(
                    "ALTER TABLE recipes ADD COLUMN IF NOT EXISTS fork_count BIGINT NOT NULL DEFAULT 0;"
                );

                // 6. Garantir tabela de tags para receitas (recipe_tags)
                jdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS recipe_tags (" +
                    "  recipe_id BIGINT NOT NULL REFERENCES recipes(id) ON DELETE CASCADE," +
                    "  tag VARCHAR(50) NOT NULL" +
                    ");"
                );

                log.info("Migração defensiva concluída com sucesso no PostgreSQL!");
            } catch (Exception e) {
                log.warn("Nota na migração de schema: {}", e.getMessage());
            }
        };
    }
}
