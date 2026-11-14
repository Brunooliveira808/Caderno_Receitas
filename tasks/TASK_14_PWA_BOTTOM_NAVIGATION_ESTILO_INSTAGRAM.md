# TASK 14: Frontend PWA Estilo Instagram - Bottom Navigation Bar & UX

## 📌 Contexto & Objetivo
Transformar a experiência do PWA em um aplicativo mobile-first com padrão Instagram: navegação por barra fixa inferior com 3 abas principais, exibição contextual de botões de ação e interação fluida de exploração e cópia de receitas.

---

## 🎨 Arquitetura de Navegação

### Barra Inferior Fixa (Bottom Navigation Bar)
Posicionada na base da tela com 3 ícones principais:
1. **🔍 Explorar (Esquerda):** Feed comunitário de receitas públicas e busca de chefs/pratos.
2. **📖 Meu Caderno (Centro):** Tela principal com as receitas do usuário logado.
3. **👤 Meu Perfil (Direita):** Estatísticas sociais, dados da conta e opções de logout.

---

## 🛠️ Especificações de Telas & Comportamento

### 1. Aba Central: "Meu Caderno"
- **Botão `[➕ Adicionar Receita]`:**
  - Posicionado em destaque **acima da listagem do caderno**.
  - **Regra de exibição:** Fica visível **apenas** quando a aba "Meu Caderno" estiver selecionada. Desaparece nas abas Explorar e Perfil.
- **Card de Receita Pessoal:**
  - Badge clicável para alternar status: `🌐 Pública` / `🔒 Privada`.
  - Indicação discreta caso tenha sido copiada: *"Inspirada em @chef_origem"*.
  - Ações: Ver detalhes, Editar e Excluir.

### 2. Aba Esquerda: "Explorar"
- **Barra de Busca Multifuncional:**
  - Seletor de modo de busca: **[Receitas]** ou **[Chefs (@user)]**.
  - Busca em tempo real com debounce (300ms).
- **Feed da Comunidade:**
  - Cards de receitas públicas postadas por outros chefs.
  - Exibição de `@autor` com avatar de iniciais colorido.
  - Botão de ação direta em cada card: **📥 "Puxar para o meu caderno"**.
- **Visualização de Perfil de Outro Chef:**
  - Ao clicar no `@autor`, abre a visão de perfil do chef:
    - Cabeçalho com foto/iniciais, nome e `@username`.
    - Contadores: *Receitas* | *Seguidores* | *Seguindo*.
    - Botão dinâmico: **"Seguir"** (amarelo) ou **"Seguindo"** (cinza).
    - Grid apenas com as receitas públicas daquele chef.

### 3. Aba Direita: "Meu Perfil"
- Visualização do perfil próprio:
  - Cabeçalho social com contadores de seguidores, pessoas seguidas e receitas públicas.
  - Opções da conta (e-mail, nome, redefinição de senha).
  - Botão vermelho destacado para **Sair da Conta (Logout)**.

---

## ✅ Critérios de Aceite
- [ ] Barra inferior responsiva, fixa no rodapé e otimizada para iOS Safe Area (`env(safe-area-inset-bottom)`).
- [ ] O botão de adicionar receita só aparece na aba do meio (Caderno).
- [ ] Ação de "Puxar para o meu caderno" copia a receita instantaneamente com feedback visual (Toast de sucesso) e atualiza o caderno.
- [ ] Transição rápida e sem recarregamento de página entre as 3 abas.
