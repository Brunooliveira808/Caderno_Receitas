# TASK 18: UI Otimista (Optimistic UI) e Feedback Assíncrono Fluido

## 📌 Contexto & Objetivo
Tornar as interações sociais e de alteração de estado no PWA instantâneas para o usuário, eliminando a espera de requisições de rede para atualizar a interface (padrão utilizado por Instagram e Twitter).

---

## 🛠️ Especificações Técnicas

### 1. Toggle de Seguir/Deixar de Seguir Otimista
- Ao clicar no botão `Seguir` / `Seguindo`:
  - **Reação Imediata (0ms):**
    - Alternar o texto e o estilo do botão instantaneamente na tela.
    - Incrementar ou decrementar a contagem de seguidores no card do chef e no perfil local.
  - **Requisição em Segundo Plano:**
    - Disparar `POST /api/users/{username}/follow` assincronamente.
  - **Tratamento de Falha (Rollback):**
    - Se a requisição falhar ou der timeout, reverter o estado visual para o anterior e exibir notificação discreta (Toast): *"Não foi possível atualizar. Verifique sua conexão."*

### 2. Toggle de Visibilidade Pública/Privada Otimista
- Ao clicar no badge `🌐 Pública` / `🔒 Privada` de uma receita:
  - O badge muda de cor e ícone imediatamente no card.
  - A API `PATCH /api/recipes/{id}/visibility` roda em background.
  - Em caso de erro, reverte o badge.

### 3. Ação "Puxar Receita" (Fork) com Micro-Interação
- Ao clicar em `📥 Puxar`:
  - Mudar o botão para `✅ Salva!` e desabilitar cliques repetidos instantaneamente.
  - Disparar a chamada para `/fork` em segundo plano.
  - Adicionar a receita duplicada imediatamente ao cache local de `Minhas Receitas` para que, ao alternar para a aba central, ela já esteja lá visível.

### 4. Sistema Leve de Notificações Toast
- Criar componente de Toast flutuante, discreto e auto-removível (duração de 2.5s) para substituir `alert(...)` invasivos do navegador.

---

## ✅ Critérios de Aceite
- [ ] O clique em seguir/deixar de seguir não apresenta atraso perceptível na interface.
- [ ] O toggle de visibilidade responde no milissegundo do toque.
- [ ] Falhas de rede revertem graciosamente o estado da tela (sem dessincronia).
- [ ] Nenhum `window.alert` bloqueante é disparado nas ações otimistas.
