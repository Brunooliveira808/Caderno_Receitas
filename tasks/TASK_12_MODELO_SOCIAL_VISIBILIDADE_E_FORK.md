# TASK 12: Modelo de Dados Social, Visibilidade, Tags e Fork de Receitas

## 📌 Contexto & Objetivo
Evoluir o modelo de dados de receitas no backend para permitir controle de visibilidade (pública/privada), tags de destaque e a funcionalidade de duplicar (fork) receitas para o caderno pessoal "Minhas Receitas".

---

## 🛠️ Especificações Técnicas

### 1. Entidade `Recipe`
- **Campo `isPublic` (boolean):**
  - Define se a receita aparece na aba Explorar. Padrão: `true`.
- **Campo `forkedFromUsername` (String, max 50):**
  - Armazena o `@username` do criador original quando a receita for clonada.
- **Campo `forkCount` (long):**
  - Contador de quantas vezes esta receita foi salva por outros usuários. Padrão: `0`.
- **Coleção de Tags (`tags`):**
  - `@ElementCollection` de `String` (ex: `["Vegano", "Rápida", "Sem Glúten"]`).
- **Migração defensiva:**
  - Adicionar colunas e tabela de tags no [`DatabaseMigrationConfig.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/config/DatabaseMigrationConfig.java).

### 2. DTOs
- Atualizar `RecipeResponseDTO` e `RecipeWebDTO` com:
  - `Boolean isPublic`
  - `String authorUsername`
  - `String forkedFromUsername`
  - `Long forkCount`
  - `List<String> tags`

### 3. Regras de Negócio em `RecipeService`
- **`forkRecipe(AppUser currentUser, Long recipeId)`:**
  - Busca a receita pública alvo.
  - Incrementa `forkCount` na receita original.
  - Cria cópia independente para `currentUser` com `forkedFromUsername = autorOriginal`.
- **`toggleVisibility(Long userId, Long recipeId)`:**
  - Alterna entre público e privado (apenas o dono pode alterar).

### 4. Endpoints REST em `RecipeController`
- `POST /api/recipes/{id}/fork`: Duplica receita pública para Minhas Receitas.
- `PATCH /api/recipes/{id}/visibility`: Alterna público/privado.

---

## ✅ Critérios de Aceite
- [ ] Colunas e tabela de tags criadas no PostgreSQL sem perda de dados.
- [ ] Teste unitário do `fork` verificando isolamento e incremento do contador.
- [ ] Validação de segurança: apenas receitas públicas podem ser clonadas por terceiros.
