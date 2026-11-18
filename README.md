# 👨‍🍳 ChefBot — Caderno Culinário Inteligente com IA

> **Assistente culinário inteligente potencializado por IA Multimodal (Google Gemini), disponível como Web App (PWA) e Bot do Telegram.**

---

### 🌐 Acesse a Aplicação Online (Produção)

A aplicação está hospedada e ativa no Render. Você pode acessar diretamente pelo link ou escanear o QR Code abaixo com a câmera do seu celular:

<div align="center">

[![Acessar ChefBot](https://img.shields.io/badge/Acessar_ChefBot-https%3A%2F%2Fchefbot--zekr.onrender.com-brightgreen?style=for-the-badge&logo=render)](https://chefbot-zekr.onrender.com)

<br/>

<a href="https://chefbot-zekr.onrender.com">
  <img src="./qrcode.png" alt="QR Code ChefBot" width="180" style="border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);" />
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

O **ChefBot** resolve o problema de perda e desorganização de receitas culinárias. Em vez de exigir o preenchimento manual de formulários longos e cansativos, o usuário pode simplesmente **gravar um áudio** ou **digitar livremente em linguagem natural**.

O motor com IA Multimodal do Google Gemini interpreta o conteúdo, extrai o título com fidelidade, categoriza os ingredientes com suas respectivas medidas, enumera os passos de preparo, calcula tempos e porções, e organiza tudo de forma estruturada no caderno pessoal do usuário.

A aplicação conta com **duas frentes integradas**:
1. **Web App (PWA Mobile-First):** Interface rica, rápida e instalável no celular, com autenticação JWT, busca instantânea, filtros por categoria e gravação de áudio direto no navegador.
2. **Telegram Bot:** Assistente complementar para quem prefere capturar notas de voz e consultar receitas diretamente no mensageiro.

---

## 🚀 Principais Funcionalidades

### 📱 Web App (PWA)
- 🎙️ **Captura por Voz com IA:** Grave instruções pelo microfone do navegador; a IA analisa o áudio e estrutura a receita completa automaticamente.
- ✍️ **Extração por Texto Livre:** Cole textos crus ou mensagens de WhatsApp ("bolo de cenoura: 3 cenouras, 2 xícaras de farinha...") e a IA organiza em ficha técnica.
- 🔐 **Autenticação Segura & Stateless:** Cadastro, login com E-mail ou @Username, recuperação de senha com tokens e autenticação baseada em JWT com senhas criptografadas em BCrypt.
- 🔍 **Busca em Tempo Real & Filtros:** Busca textual normalizada e filtros instantâneos por categorias (Café da manhã, Almoço, Jantar, Sobremesas, etc.).
- 📲 **Instalável no Smartphone (PWA):** Suporte a tela cheia, manifest mobile e tema personalizado.

### 🤖 Telegram Bot
- 🎙️ **Processamento Multimodal de Áudio & Voz:** Envie notas de voz gravadas no microfone do Telegram ou arquivos encaminhados (`.mp3`, `.ogg`).
- 🔍 **Motor de Busca Inteligente (`/buscar <termo>`):** Busca por termos nos títulos, ingredientes ou modo de preparo.
- ✏️ **Atualização Inteligente (`/atualizar`):** Modifique receitas existentes via linguagem natural (ex: `/atualizar 1 troque o açúcar por mel`).
- 📚 **Gestão do Caderno:** Comandos `/receitas`, `/deletar <ID>` e `/cancelar`.

---

## 🛠️ Arquitetura e Tecnologias

```
├── Backend (Monólito Modular em Camadas)
│   ├── Controller (Mapeamento HTTP & Validações de Entrada)
│   ├── Service (Regras de negócio, manipulação de receitas e IA)
│   ├── Security (Spring Security 6, JWT Stateless, BCryptPasswordEncoder)
│   └── Repository (Spring Data JPA / Hibernate)
├── Persistência
│   ├── Produção: PostgreSQL Gerenciado (Render)
│   └── Dev / Testes: H2 Database em memória/arquivo local
├── Integração de IA
│   └── Google Gemini Generative AI (via Spring RestClient moderno)
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
git clone https://github.com/Brunooliveira808/ChefBot_Telegram.git
cd ChefBot_Telegram
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
