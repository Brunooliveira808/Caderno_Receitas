# TASK 15: Cache Local e Inicialização Instantânea no PWA (Instant Boot)

## 📌 Contexto & Objetivo
Eliminar a sensação de lentidão e o spinner branco ao abrir o aplicativo ou alternar abas, adotando o padrão **Stale-While-Revalidate (SWR)** no frontend. O app deve abrir imediatamente (0 segundos) com os dados previamente salvos no dispositivo e sincronizar em segundo plano de forma imperceptível.

---

## 🛠️ Especificações Técnicas

### 1. Camada de Cache Local no Frontend (`localStorage` / `CacheStorage`)
- Criar gerenciador de cache simples para receitas:
  - Chave: `chefbot_cache_my_recipes` (para "Minhas Receitas").
  - Chave: `chefbot_cache_explore` (para feed público).
  - Chave: `chefbot_cache_profile` (para estatísticas do perfil).
- Ao salvar dados vindos da API, gravar timestamp de atualização:
  ```json
  {
    "timestamp": 1731600000000,
    "data": [ ... ]
  }
  ```

### 2. Fluxo de Inicialização "Instant Boot" (Stale-While-Revalidate)
- Na função `fetchRecipes()`:
  1. **Passo 1 (Síncrono/Imediato):** Ler `chefbot_cache_my_recipes`. Se existir, invocar imediatamente `renderRecipes(cachedData)`. A tela renderiza sem mostrar nenhum loader.
  2. **Passo 2 (Assíncrono/Segundo Plano):** Disparar requisição `fetch('/api/recipes')` silenciosamente sem exibir spinner de tela inteira.
  3. **Passo 3 (Atualização Suave):** Quando a resposta chegar, comparar com o cache. Se houver diferença (novas receitas ou alterações), atualizar o cache e re-renderizar a lista suavemente. Se não houver alteração, não tocar no DOM.
- Aplicar o mesmo padrão em `fetchExploreRecipes()` com a chave `chefbot_cache_explore`.

### 3. Eliminação de Bloqueio do Loader
- Desativar o loader central (`#loading`) caso já existam receitas no cache ou na memória.
- Exibir apenas um pequeno indicador discreto e não bloqueante no topo (ex: barra de progresso fina ou ícone pulsando) quando uma sincronização estiver em andamento.

---

## ✅ Critérios de Aceite
- [ ] Ao recarregar a página, a tela não fica vazia nem exibe spinner central se houver cache prévio.
- [ ] O app abre instantaneamente com as últimas receitas visualizadas.
- [ ] Ao criar, editar ou excluir uma receita, o cache local correspondente é atualizado imediatamente.
- [ ] O app funciona em modo offline parcial (exibição de receitas já carregadas).
