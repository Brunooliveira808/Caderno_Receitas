# TASK 17: Cache em Memória no Spring Boot e Otimização de Consultas (Backend)

## 📌 Contexto & Objetivo
Reduzir drasticamente o tempo de resposta das APIs para menos de 10ms, evitando viagens constantes e repetidas ao banco de dados PostgreSQL hospedado no Render em Oregon (EUA).

---

## 🛠️ Especificações Técnicas

### 1. Habilitação do Spring Cache
- Adicionar anotação `@EnableCaching` na classe principal `ChefBotTelegramApplication.java`.
- Configurar dependência e gerenciador de cache com Caffeine ou Spring ConcurrentMapCache:
  - Cache `exploreRecipes`: feed público de receitas.
  - Cache `chefProfiles`: dados e métricas públicas de chefs (`/api/users/{username}`).
  - Cache `userRecipes`: caderno de receitas do usuário autenticado.

### 2. Anotações de Cache nos Serviços
- Em `RecipeService` e `UserService`:
  - `@Cacheable(value = "exploreRecipes", key = "#query != null ? #query : 'all'")`: no método de busca/listagem do Explorar.
  - `@Cacheable(value = "chefProfiles", key = "#username")`: no detalhe público de chef.
  - `@CacheEvict(value = {"exploreRecipes", "userRecipes"}, allEntries = true)`: disparado ao salvar, atualizar, excluir ou duplicar (fork) receita.
  - `@CacheEvict(value = "chefProfiles", allEntries = true)`: disparado ao seguir ou deixar de seguir um chef.

### 3. Otimização de Consultas JPA (Prevenção de N+1)
- Em `RecipeRepository`, utilizar `JOIN FETCH` explícito ou `@EntityGraph` para carregar ingredientes e instruções em conjunto para visualizações completas, eliminando múltiplas queries pontuais ao banco na nuvem.

---

## ✅ Critérios de Aceite
- [ ] Endpoints de leitura (`/api/recipes/explore`, `/api/users/{username}`) respondem em menos de 15ms em chamadas consecutivas.
- [ ] Invalidação automática e imediata do cache quando uma receita é alterada ou criada.
- [ ] Testes unitários e de integração passam validando o comportamento de `@Cacheable` e `@CacheEvict`.
