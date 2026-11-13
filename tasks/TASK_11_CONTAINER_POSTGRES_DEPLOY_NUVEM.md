# 🎯 TASK 11: Containerização, PostgreSQL e Deploy em Nuvem (Render / Railway)

## 📌 Contexto e Problema
Para que o bot e o PWA fiquem disponíveis 24 horas por dia no celular sem depender do computador pessoal ligado, a aplicação deve ser empacotada em container Docker e configurada para banco de dados relacional em nuvem (PostgreSQL) com suporte a HTTPS automático.

---

## 🎯 Objetivos da Task
1. Criar `Dockerfile` multi-stage:
   - Stage 1: Build do projeto com Gradle e JDK 21.
   - Stage 2: Imagem de execução enxuta com OpenJDK JRE (Alpine ou Eclipse Temurin).
2. Criar `docker-compose.yml` local para testes com PostgreSQL.
3. Criar profile `application-prod.properties`:
   - Configurações para conexão via variáveis de ambiente (`DATABASE_URL`, `TELEGRAM_BOT_TOKEN`, `GEMINI_API_KEY`).
   - Dialeto PostgreSQL e driver configurados.
4. Criar documentação passo a passo de deploy:
   - Configuração no **Render** ou **Railway** (Web Service + PostgreSQL grátis).
   - Injeção segura de segredos e chaves de API nas variáveis de ambiente da nuvem.

---

## 📂 Arquivos Envolvidos
- `Dockerfile` (criar na raiz ou no módulo do backend)
- `docker-compose.yml` (criar)
- `ChefBot_Telegram/src/main/resources/application-prod.properties` (criar)
- `docs/DEPLOY_GUIDE.md` (criar guia detalhado)

---

## 🧪 Critérios de Aceitação
- Build do container Docker executado com sucesso: `docker build -t chefbot .`.
- Aplicação sobe localmente conectada a um banco PostgreSQL via Docker Compose.
- Variáveis de ambiente substituem devidamente chaves locais sem expor credenciais no Git.
