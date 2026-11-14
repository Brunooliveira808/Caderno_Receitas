# TASK 13: Sistema de Seguidores, Perfil de Chefs e Descoberta

## 📌 Contexto & Objetivo
Implementar o grafo social do ChefBot, permitindo seguir outros chefs, buscar perfis e visualizar a página pública de um cozinheiro com suas estatísticas e receitas públicas.

---

## 🛠️ Especificações Técnicas

### 1. Entidade `UserFollow`
- Criar a entidade `UserFollow` com a tabela `user_follows`:
  - `id`: Chave primária auto-incremento.
  - `follower`: `@ManyToOne` referenciando `AppUser` (quem está seguindo).
  - `following`: `@ManyToOne` referenciando `AppUser` (quem está sendo seguido).
  - `created_at`: Data e hora em que começou a seguir.
  - Constraint de unicidade: `(follower_id, following_id)`.
- Atualizar [`DatabaseMigrationConfig.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/config/DatabaseMigrationConfig.java) com `CREATE TABLE IF NOT EXISTS user_follows (...)`.

### 2. Repositórios e Métodos JPA
- `UserFollowRepository`:
  - `boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);`
  - `void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);`
  - `long countByFollowingId(Long userId);` (contagem de seguidores)
  - `long countByFollowerId(Long userId);` (contagem de seguidos)
  - `List<AppUser> findFollowingByFollowerId(Long followerId);`

### 3. DTOs de Perfil
- Criar `UserProfileDTO`:
  - `String username`
  - `String name`
  - `long recipesCount`
  - `long followersCount`
  - `long followingCount`
  - `boolean isFollowing` (se o usuário que fez a requisição já segue este chef)
  - `boolean isSelf` (se é o próprio perfil logado)

### 4. Controller e Endpoints Sociais (`UserController`)
- `GET /api/users/{username}`:
  - Retorna dados públicos do perfil e estatísticas.
- `GET /api/users/{username}/recipes`:
  - Retorna todas as receitas marcadas como `isPublic = true` do usuário especificado.
- `POST /api/users/{username}/follow`:
  - Toggle inteligente: se já segue, dá unfollow; se não segue, cria a relação de seguidor.
- `GET /api/users/search?query=...`:
  - Busca chefs por `@username` ou nome exibido.
- `GET /api/recipes/explore?query=...`:
  - Retorna feed público de receitas com busca global.

---

## ✅ Critérios de Aceite
- [ ] Usuário não pode seguir a si próprio.
- [ ] Endpoint de perfil retorna as contagens exatas de receitas públicas e seguidores.
- [ ] Busca de usuários filtra corretamente por username case-insensitive.
- [ ] Teste unitário e de integração para follow/unfollow.
