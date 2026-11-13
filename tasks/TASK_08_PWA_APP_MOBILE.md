# 🎯 TASK 08: Transformação em PWA (Progressive Web App) Mobile-First

## 📌 Contexto e Problema
A interface web (`index.html`) já foi desenhada no estilo mobile-first com Tailwind CSS e componentes táteis. No entanto, ao ser aberta no navegador do celular, exibe barras de endereço e de navegação do browser, não permite instalação na tela inicial como app independente e não possui suporte a cache offline de assets essenciais.

---

## 🎯 Objetivos da Task
1. Criar o arquivo `manifest.json` com identidade visual culinária (nome, descrição, cores temáticas, ícones e `display: standalone`).
2. Criar ícones SVG/PNG para diferentes densidades de tela (192x192 e 512x512).
3. Criar um `sw.js` (Service Worker) para cache dos recursos estáticos essenciais (HTML, ícones, Tailwind).
4. Integrar o registro do Service Worker e o manifesto no cabeçalho do `index.html`.
5. Adicionar botão e prompt visual "Instalar Aplicativo" para facilitar a instalação no Android (Chrome) e instrução no iOS (Safari).

---

## 📂 Arquivos Envolvidos
- `ChefBot_Telegram/src/main/resources/static/manifest.json` (criar)
- `ChefBot_Telegram/src/main/resources/static/sw.js` (criar)
- `ChefBot_Telegram/src/main/resources/static/icons/` (criar ícones)
- `ChefBot_Telegram/src/main/resources/static/index.html` (atualizar meta tags e script PWA)

---

## 🔨 Instruções Passo a Passo

### Passo 1: Criar `manifest.json`
```json
{
  "name": "ChefBot Culinário",
  "short_name": "ChefBot",
  "description": "Seu Caderno Inteligente de Receitas com IA",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#FDFCF7",
  "theme_color": "#F59E0B",
  "orientation": "portrait-primary",
  "icons": [
    {
      "src": "/icons/icon-192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any maskable"
    },
    {
      "src": "/icons/icon-512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any maskable"
    }
  ]
}
```

### Passo 2: Criar `sw.js`
Implementar cache-first para recursos estáticos locais e network-first para chamadas da API `/api/**`.

### Passo 3: Atualizar `index.html`
- Adicionar tags `<link rel="manifest" href="/manifest.json">`.
- Adicionar `<meta name="theme-color" content="#F59E0B">`.
- Adicionar `<meta name="apple-mobile-web-app-capable" content="yes">`.
- Registrar o Service Worker via JavaScript.

---

## 🧪 Critérios de Aceitação
- No Chrome/Safari móvel, aparece a opção "Instalar Aplicativo" ou "Adicionar à Tela de Início".
- Ao abrir o app instalado, ele abre em tela cheia (sem barra de URL).
