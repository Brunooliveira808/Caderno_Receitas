# 📖 Caderno de Receitas — Assistente Culinário Inteligente com IA

> **Aplicativo web moderno (PWA) para organização e extração de receitas culinárias potencializado por Inteligência Artificial Multimodal (Google Gemini).**

---

### 🌐 Acesse a Aplicação Online (Produção)

A aplicação está hospedada e ativa no Render. Você pode acessar diretamente pelo link ou escanear o QR Code abaixo com a câmera do seu celular:

<div align="center">

[![Acessar Caderno de Receitas](https://img.shields.io/badge/Acessar_Caderno_de_Receitas-https%3A%2F%2Fchefbot--zekr.onrender.com-brightgreen?style=for-the-badge&logo=render)](https://chefbot-zekr.onrender.com)

<br/>

<a href="https://chefbot-zekr.onrender.com">
  <img src="./qrcode.png" alt="QR Code Caderno de Receitas" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
</a>

<p><em>Aponte a câmera do celular para abrir o Web App instantaneamente</em></p>

</div>

---

![Status](https://img.shields.io/badge/Status-Produção%20Ativa-brightgreen?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21%20%2F%2025-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x%20%2F%204.x-brightgreen?style=for-the-badge&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?style=for-the-badge&logo=postgresql)
![Google Gemini](https://img.shields.io/badge/Google%20Gemini%20AI-Multimodal-purple?style=for-the-badge&logo=googlegemini)
![PWA](https://img.shields.io/badge/Frontend-PWA%20Mobile--First-red?style=for-the-badge&logo=pwa)

---

## 📌 Sobre o Projeto

O **Caderno de Receitas** é uma aplicação desenvolvida para eliminar o atrito de cadastrar e organizar receitas culinárias. Em vez de preencher formulários manuais longos, o usuário pode simplesmente **gravar uma nota de voz pelo microfone** ou **colar um texto informal**.

Através de engenharia de prompt estruturada com a IA Multimodal do **Google Gemini**, a aplicação interpreta o conteúdo, extrai o título com fidelidade, categoriza os ingredientes com suas respectivas medidas, enumera os passos de preparo, estima tempos e porções, e organiza tudo de forma padronizada no caderno pessoal do usuário.

---

## 🚀 Principais Funcionalidades

- 🎙️ **Captura e Extração por Voz (Áudio):** Grave instruções pelo microfone do navegador; a IA analisa o áudio e estrutura a receita completa automaticamente.
- ✍️ **Extração por Texto Livre:** Cole anotações informais ou mensagens de bate-papo (ex: *"bolo de cenoura: 3 cenouras, 2 xícaras de farinha..."*) e a IA organiza em ficha técnica culinária.
- 🔐 **Autenticação Segura & Stateless:** Cadastro, login com E-mail ou @Username, recuperação de senha com tokens e autenticação baseada em JWT com senhas criptografadas em BCrypt.
- 🔍 **Busca em Tempo Real & Filtros:** Busca textual normalizada e filtros instantâneos por categorias (Café da manhã, Almoço, Jantar, Sobremesas, etc.).
- ✏️ **Edição e Gestão Completa:** Atualização manual de campos, exclusão e visualização detalhada em modal.
- 📲 **Instalável no Smartphone (PWA Mobile-First):** Suporte a tela cheia, manifest mobile e tema personalizado.

---

## 🛠️ Arquitetura e Tecnologias

```
├── Backend (Monólito Modular em Camadas)
│   ├── Controller (Mapeamento REST HTTP & Validações de Entrada)
│   ├── Service (Regras de negócio, manipulação de receitas e IA)
│   ├── Security (Spring Security 6, JWT Stateless, BCryptPasswordEncoder)
│   └── Repository (Spring Data JPA / Hibernate)
├── Persistência de Dados
│   ├── Produção: PostgreSQL Gerenciado (Render)
│   └── Dev / Testes: H2 Database em memória/arquivo local
├── Integração de IA
│   └── Google Gemini Generative AI (via Spring RestClient)
└── Frontend
    └── PWA Vanilla (HTML5, Tailwind CSS, Lucide Icons, MediaRecorder API)
```

---

## 💻 Como Executar Localmente

### Pré-requisitos
- **Java JDK 21+** instalado.
- **Git** instalado.

### 1. Clonar o Repositório
```bash
git clone https://github.com/Brunooliveira808/Caderno_Receitas.git
cd Caderno_Receitas
```

### 2. Configurar Variáveis de Ambiente
Crie ou edite o arquivo `ChefBot_Telegram/src/main/resources/application.properties`:
```properties
# Google Gemini API Key (Obtenha em: https://aistudio.google.com/app/apikey)
gemini.api.key=SUA_CHAVE_GEMINI_AQUI

# Banco H2 Local (Gerado automaticamente em ./data)
spring.datasource.url=jdbc:h2:file:./data/chefbotdb;AUTO_SERVER=TRUE
```

### 3. Rodar a Suíte de Testes
```bash
cd ChefBot_Telegram
./gradlew test
```
*Todos os testes unitários e de integração rodam com banco em memória e mocks, sem dependências externas.*

### 4. Iniciar o Servidor
```bash
./gradlew bootRun
```
Acesse no seu navegador: **`http://localhost:8080`**

---

## ☁️ Deploy em Produção (Docker + Render)

O projeto possui um **Multi-stage Dockerfile** otimizado:
* **Stage 1 (Builder):** Compila o projeto com Gradle e gera o JAR executável (`bootJar`).
* **Stage 2 (Runtime):** Utiliza imagem leve `eclipse-temurin:21-jre-alpine` com usuário não-root por segurança.
* A inicialização ativa automaticamente o perfil `prod` conectando ao PostgreSQL via variável `DATABASE_URL`.

---

## 👤 Autor

Desenvolvido por **[Bruno Oliveira](https://github.com/Brunooliveira808)**.
