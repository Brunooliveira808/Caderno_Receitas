# TASK 12: Modelo de Dados Social, Visibilidade e Fork de Receitas

## 📌 Contexto & Objetivo
Evoluir o modelo de dados de receitas e usuários para transformar o ChefBot em uma rede social culinária (PWA First), permitindo que receitas sejam públicas ou privadas e possam ser clonadas ("puxadas para o caderno") como snapshots independentes.

---

## 🛠️ Especificações Técnicas

### 1. Alterações na Entidade `Recipe`
- **Campo `isPublic` (boolean):**
  - Define se a receita aparece no feed e buscas da comunidade (`true`) ou apenas no caderno pessoal do autor (`false`). Padrão: `true`.
- **Campo `forkedFromUsername` (varchar 50, nullable):**
  - Armazena o `@username` do criador original quando a receita foi clonada a partir de outro chef.
- **Migração defensiva:**
  - Atualizar [`DatabaseMigrationConfig.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/config/DatabaseMigrationConfig.java) para executar `ALTER TABLE recipes ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT TRUE;` e `ALTER TABLE recipes ADD COLUMN IF NOT EXISTS forked_from_username VARCHAR(50);`.

### 2. DTOs
- Atualizar [`RecipeResponseDTO.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/dto/RecipeResponseDTO.java) e [`RecipeWebDTO.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/dto/RecipeWebDTO.java) para conter:
  - `Boolean isPublic`
  - `String authorUsername` (autor da receita)
  - `String forkedFromUsername` (se foi inspirada/clonada)

### 3. Regras de Negócio em `RecipeService`
- **Método `toggleVisibility(Long userId, Long recipeId)`:**
  - Inverte o status de `isPublic` (autorizada apenas pelo dono da receita).
- **Método `forkRecipe(AppUser currentUser, Long recipeId)`:**
  - Busca a receita pública alvo.
  - Valida se é pública (ou pertence a outro usuário).
  - Cria uma nova instância de `Recipe` associada ao `currentUser`.
  - Duplica ingredientes e instruções em snapshot limpo.
  - Define `forkedFromUsername` com o `@username` do autor da receita original.
  - Salva no banco e retorna o DTO da nova receita no caderno do usuário atual.

### 4. Endpoints REST em `RecipeController`
- `PATCH /api/recipes/{id}/visibility`: Alterna visibilidade pública/privada.
- `POST /api/recipes/{id}/fork`: Realiza a cópia da receita para o perfil do usuário logado.

---

## ✅ Critérios de Aceite
- [ ] Colunas criadas no PostgreSQL sem quebrar dados existentes.
- [ ] Testes de unidade cobrindo o fork com duplicata independente (se o autor original deletar a receita, a receita clonada continua intacta).
- [ ] Teste de autorização garantindo que apenas o dono pode alternar a visibilidade da própria receita.
