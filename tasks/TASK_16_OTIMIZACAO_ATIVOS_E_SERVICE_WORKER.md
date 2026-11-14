# TASK 16: Otimização de Ativos Estáticos e Service Worker Offline

## 📌 Contexto & Objetivo
Eliminar a dependência de CDNs externas bloqueantes no `<head>` (`cdn.tailwindcss.com` e `unpkg.com/lucide`) que congelam o carregamento inicial da página e causam atraso de renderização perceptível em redes lentas.

---

## 🛠️ Especificações Técnicas

### 1. Desacoplamento de CDNs Críticas
- **Tailwind CSS:**
  - Substituir o CDN dinâmico `cdn.tailwindcss.com` (que interpreta e compila estilos via JavaScript no cliente a cada abertura) por CSS otimizado e minificado servido localmente em `static/css/tailwind-custom.css` ou pré-compilado.
- **Lucide Icons:**
  - Salvar o arquivo standalone `lucide.min.js` em `static/js/lucide.min.js` em vez de buscar remotamente em `unpkg.com` a cada requisição.

### 2. Estratégia de Cache no Service Worker (`sw.js`)
- Atualizar `src/main/resources/static/sw.js` com estratégia **Cache-First** para recursos estáticos essenciais:
  - `/index.html`
  - `/manifest.json`
  - Arquivos CSS e JS locais
  - Ícones da aplicação (`/icons/icon-192.png`, `/icons/icon-512.png`)
- Versão de cache explícita (`const CACHE_NAME = 'chefbot-v2';`) para garantir invalidação automática ao implantar novas versões.

### 3. Fontes e Metadados do PWA
- Configurar `font-display: swap` para prevenir telas brancas enquanto fontes são carregadas.
- Assegurar cabeçalhos de cache HTTP adequados via Spring Boot (`Cache-Control: max-age=86400`) para recursos sob `/static/`.

---

## ✅ Critérios de Aceite
- [ ] O carregamento inicial da página não faz chamadas obrigatórias para CDNs externas para desenhar a interface.
- [ ] Em rede 3G ou modo avião, a casca da aplicação (App Shell) carrega instantaneamente.
- [ ] Redução comprovada no tempo de First Contentful Paint (FCP).
