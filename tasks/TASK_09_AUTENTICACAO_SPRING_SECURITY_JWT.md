# 🎯 TASK 09: Autenticação e Segurança (Spring Security + JWT)

## 📌 Contexto e Problema
Com o deploy público, os endpoints da API REST (`/api/recipes/**`) precisam ser restritos para evitar acessos não autorizados e consumo abusivo da API do Google Gemini. Além disso, o usuário precisa de login e senha para gerenciar seu próprio caderno de receitas pelo celular.

---

## 🎯 Objetivos da Task
1. Adicionar dependências do `spring-boot-starter-security` e biblioteca JWT (`jjwt` ou `nimbus-jose-jwt`).
2. Criar a entidade `AppUser` (id, email, passwordHash, name, roles, telegramChatId) e repositório `AppUserRepository`.
3. Criar serviço de autenticação com geração e validação de tokens JWT (`JwtService` / `TokenProvider`).
4. Criar controller `/api/auth`:
   - `POST /api/auth/register` (registro com hash BCrypt).
   - `POST /api/auth/login` (autenticação e retorno do token JWT + dados do usuário).
   - `GET /api/auth/me` (perfil do usuário logado).
5. Configurar `SecurityFilterChain`:
   - Permitir acesso público aos assets estáticos (`/`, `/index.html`, `/manifest.json`, `/icons/**`, `/sw.js`).
   - Permitir acesso público às rotas de autenticação (`/api/auth/**`).
   - Bloquear requisições não autenticadas em `/api/recipes/**` exigindo cabeçalho `Authorization: Bearer <token>`.
   - Garantir que o bot do Telegram continue funcionando independentemente das sessões HTTP.
6. Criar tela de Login / Registro visual no modal do frontend `index.html` com persistência do token no `localStorage`.

---

## 📂 Arquivos Envolvidos
- `ChefBot_Telegram/build.gradle` (adicionar security e JWT)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/model/AppUser.java` (criar)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/repository/AppUserRepository.java` (criar)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/security/SecurityConfig.java` (criar)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/security/JwtAuthFilter.java` (criar)
- `ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/AuthController.java` (criar)
- `ChefBot_Telegram/src/main/resources/static/index.html` (tela de login/registro)
- `ChefBot_Telegram/src/test/java/ChefBot_Telegram/controller/AuthControllerTest.java` (criar)

---

## 🧪 Critérios de Aceitação
- Teste unitário de login com senha correta e incorreta.
- Requisições sem token para `/api/recipes` retornam HTTP 401 Unauthorized.
- Requisições com token válido no header retornam HTTP 200 OK.
