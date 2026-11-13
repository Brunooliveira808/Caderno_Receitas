# 🎯 TASK 10: Multi-tenant e Sincronização Web + Telegram

## 📌 Contexto e Problema
Atualmente, as receitas criadas na Web utilizam um ID fixo mockado (`WEB_DEFAULT_CHAT_ID = 999999999L`). 
Para que a aplicação seja realmente multi-usuário e útil no dia a dia:
1. Cada usuário logado na Web/App deve ver e gerenciar apenas as suas próprias receitas.
2. Deve existir uma forma simples de vincular a conta Web ao Telegram, para que receitas ditadas pelo Telegram apareçam no App do celular e vice-versa.

---

## 🎯 Objetivos da Task
1. Refatorar `RecipeController` para extrair o usuário autenticado a partir do token JWT (`@AuthenticationPrincipal` ou `SecurityContextHolder`).
2. Vincular as receitas criadas pela Web ao `AppUser` logado.
3. Criar funcionalidade de Código de Vinculação (Link Token):
   - O usuário gera um código no app da Web (ex: `CHEF-7482`).
   - O usuário digita `/vincular CHEF-7482` no Telegram.
   - O bot associa o `telegramChatId` daquela conversa ao `AppUser` da Web.
   - A partir desse momento, todo o caderno de receitas fica 100% unificado e sincronizado entre o Telegram e o celular.
4. Criar testes integrados para o fluxo de vinculação e isolamento de receitas por usuário.

---

## 📂 Arquivos Envolvidos
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java`
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/UserService.java` (criar/atualizar)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/TelegramBotService.java` (adicionar comando `/vincular`)
- `ChefBot_Telegram/src/main/resources/static/index.html` (exibir perfil e código de vinculação)
- `ChefBot_Telegram/src/test/java/ChefBot_Telegram/service/UserSyncTest.java` (criar)

---

## 🧪 Critérios de Aceitação
- Usuário A não enxerga nem altera receitas do Usuário B.
- Após o comando `/vincular`, receitas criadas via Telegram aparecem imediatamente na listagem da Web do usuário.
