# 🚀 Guia de Deploy em Nuvem do ChefBot Culinário

Este guia explica como colocar o **ChefBot Culinário** em produção na nuvem de forma gratuita ou de baixíssimo custo (Render ou Railway), permitindo que você e sua família acessem o aplicativo Web/PWA no celular e o Bot do Telegram 24 horas por dia.

---

## 📋 Pré-requisitos
1. Repositório no GitHub com o código da aplicação.
2. Chave de API do **Google Gemini** ([Google AI Studio](https://aistudio.google.com/)).
3. Token do Bot do Telegram criado com o **@BotFather**.

---

## 🌟 Opção 1: Deploy no Render (Recomendado)

O [Render](https://render.com) oferece suporte nativo a Docker e bancos de dados PostgreSQL gerenciados gratuitos.

### 1. Criar o Banco PostgreSQL no Render
1. Acesse o dashboard do Render e clique em **New +** > **PostgreSQL**.
2. Preencha:
   - **Name**: `chefbot-db`
   - **Database**: `chefbot`
   - **User**: `chefbot`
   - **Region**: Oregon (US West) ou Frankfurt (EU)
   - **Plan**: Free
3. Clique em **Create Database**.
4. Copie a **Internal Database URL** (ou as credenciais individuais: Host, Database, User, Password).

### 2. Criar o Web Service
1. No dashboard, clique em **New +** > **Web Service**.
2. Conecte o seu repositório GitHub (`ChefBot_Telegram`).
3. Configure o serviço:
   - **Root Directory**: `ChefBot_Telegram`
   - **Runtime**: `Docker`
   - **Plan**: Free
4. Na aba **Environment Variables**, adicione as seguintes variáveis:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `jdbc:postgresql://<HOST>:<PORT>/chefbot` (adicione `jdbc:` na frente da URL do Postgres)
   - `DATABASE_USERNAME`: usuário do PostgreSQL
   - `DATABASE_PASSWORD`: senha do PostgreSQL
   - `GEMINI_API_KEY`: sua chave do Google Gemini
   - `GEMINI_MODEL`: `gemini-3.1-flash-lite`
   - `TELEGRAM_BOT_TOKEN`: seu token do Telegram
   - `JWT_SECRET`: uma chave secreta aleatória com pelo menos 32 caracteres
5. Clique em **Create Web Service**.
6. O Render fará o build do Dockerfile e gerará uma URL HTTPS (ex: `https://chefbot-telegram.onrender.com`).

---

## 🚂 Opção 2: Deploy no Railway

O [Railway](https://railway.app) permite subir com 1 clique usando Docker e PostgreSQL com provisionamento automático:

1. Acesse o Railway e crie um **New Project**.
2. Adicione um **PostgreSQL Database**.
3. No mesmo projeto, adicione um **GitHub Repo** selecionando `ChefBot_Telegram`.
4. Em **Variables** do serviço da aplicação, adicione:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `jdbc:${{Postgres.DATABASE_URL}}`
   - `DATABASE_USERNAME`: `${{Postgres.PGUSER}}`
   - `DATABASE_PASSWORD`: `${{Postgres.PGPASSWORD}}`
   - `GEMINI_API_KEY`: sua chave do Google Gemini
   - `TELEGRAM_BOT_TOKEN`: seu token do Telegram
   - `JWT_SECRET`: chave secreta para assinatura dos tokens JWT
5. Em **Settings** > **Networking**, clique em **Generate Domain** para gerar a URL pública HTTPS.

---

## 📱 Como instalar o App no Celular após o Deploy

1. Abra a URL HTTPS gerada (ex: `https://chefbot.onrender.com`) no navegador do celular (Chrome no Android ou Safari no iOS).
2. **Android**: Toque no botão **"Instalar"** que aparecerá no topo ou clique nos 3 pontinhos do Chrome > **"Instalar aplicativo"**.
3. **iOS (iPhone)**: No Safari, toque no ícone de compartilhamento (quadrado com seta para cima) > **"Adicionar à Tela de Início"**.
4. Crie sua conta com e-mail e senha no aplicativo.
5. Em **Minha Conta**, clique em **"Sincronizar com Telegram"** para gerar seu código `CHEF-XXXX`.
6. No Telegram, abra a conversa com seu bot e envie `/vincular CHEF-XXXX`.
7. Pronto! Seu caderno de receitas agora funciona em tempo real pelo celular e pelo Telegram!
