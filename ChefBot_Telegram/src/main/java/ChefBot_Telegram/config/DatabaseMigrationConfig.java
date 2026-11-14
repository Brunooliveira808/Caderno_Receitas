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

                log.info("Migração defensiva concluída com sucesso no PostgreSQL!");
            } catch (Exception e) {
                log.warn("Nota na migração de schema: {}", e.getMessage());
            }
        };
    }
}
