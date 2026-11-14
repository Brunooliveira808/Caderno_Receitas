# TASK 15: Modo Cozinha & Compartilhamento Social

## 📌 Contexto & Objetivo
Adicionar funcionalidades de usabilidade avançada para quem está cozinhando e compartilhamento nativo de receitas em redes sociais.

---

## 🛠️ Especificações Técnicas

### 1. Botão e Modo Cozinha (Display Ativo & Checklists)
- Dentro do modal de detalhes da receita, adicionar o botão destacado: **👨‍🍳 "Modo Cozinha"**.
- Ao abrir o Modo Cozinha:
  - Layout simplificado em tela cheia com tipografia grande e legível de longe.
  - Ativação do **Screen Wake Lock API** (`navigator.wakeLock.request('screen')`) para impedir que a tela do celular apague/bloqueie enquanto o usuário cozinha.
  - Ingredientes com checkboxes interativos que riscam o texto ao tocar (para conferência dos itens separados).
  - Passos de instrução em cards grandes sequenciais (estilo carrossel com botões "Anterior" e "Próximo Passo").

### 2. Compartilhamento Nativo (Web Share API)
- Botão **"Compartilhar"** no card e no detalhe da receita.
- Utiliza a **Web Share API** (`navigator.share`):
  - No celular: abre a bandeja nativa (WhatsApp, Telegram, Instagram Stories, Copiar Link).
  - No desktop: copia automaticamente o link formatado para a área de transferência com Toast de confirmação.

---

## ✅ Critérios de Aceite
- [ ] O celular não desliga a tela enquanto o Modo Cozinha estiver aberto (com fallback gracioso se não suportado).
- [ ] Checkboxes de ingredientes riscam visualmente e mantêm o estado durante a sessão.
- [ ] Botão de compartilhar funciona no mobile e copia link no desktop.
