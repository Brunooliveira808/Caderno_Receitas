# TASK 14: PWA Bottom Navigation Bar Estilo Instagram

## 📌 Contexto & Objetivo
Implementar a nova arquitetura de navegação mobile-first com barra fixa inferior (Bottom Navigation Bar) contendo 3 abas principais e a exibição contextual das receitas.

---

## 🛠️ Especificações Técnicas

### 1. Barra Inferior Fixa (Bottom Navigation)
- Fixada no rodapé da aplicação, estilizada com Tailwind CSS.
- **3 Abas:**
  1. **🔍 Explorar (Esquerda):** Feed da comunidade e busca de chefs/receitas.
  2. **🍳 Minhas Receitas (Centro):** Caderno de receitas pessoais.
  3. **👤 Meu Perfil (Direita):** Dados do usuário, estatísticas sociais e logout.
- Indicador visual ativo (cor amarela/destaque na aba selecionada).

### 2. Aba Central: "Minhas Receitas"
- **Botão `[➕ Adicionar Receita]`:**
  - Posicionado no topo, logo acima da lista de receitas.
  - **Regra:** Visível exclusivamente nesta aba central.
- **Card de Receita Pessoal:**
  - Exibe Categoria principal e chips de tags (`[🌱 Vegano]`, `[⏱️ Rápida]`).
  - Badge clicável para alternar status: `🌐 Pública` / `🔒 Privada`.
  - Se for copiada, exibe: *"Inspirada em @autor"*.
  - Ações: Ver detalhes, Editar e Excluir.

### 3. Aba Esquerda: "Explorar"
- Feed de receitas públicas postadas pela comunidade.
- Cada card exibe o `@autor` e o botão: **📥 "Puxar para Minhas Receitas"**.
- Ao clicar em "Puxar", chama a API `/fork`, dá feedback visual e adiciona à aba central.

---

## ✅ Critérios de Aceite
- [ ] Transição suave entre as 3 abas sem recarregar a página.
- [ ] O botão `[➕ Adicionar Receita]` aparece somente na aba "Minhas Receitas".
- [ ] Botão "Puxar" clona a receita e atualiza a contagem na tela.
- [ ] Barra inferior respeita a área segura de celulares (iOS/Android).
