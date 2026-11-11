# Plano de Tasks: Melhorias de Captação de Áudio/Texto e Entendimento da IA (Gemini) [CONCLUÍDO]

> [!NOTE]
> Todas as 5 Tasks planejadas foram integralmente implementadas no projeto e cobertas por testes automatizados com JUnit 5 e Mockito.

```mermaid
flowchart LR
    T1["✅ Task 1: DTO + Normalização"] --> T2["✅ Task 2: GeminiService (Prompt, MIME, Payload)"]
    T2 --> T3["✅ Task 3: Controller + Edição IA Web"]
    T3 --> T4["✅ Task 4: Frontend Web (Áudio + UI)"]
    T4 --> T5["✅ Task 5: Suíte de Testes JUnit"]
```

---

## Task 1: Evolução do `RecipeResponseDTO` e Normalização das Categorias Oficiais

### Objetivo
Adicionar o campo `transcription` (transcrição literal do áudio/texto) ao [`RecipeResponseDTO.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/dto/RecipeResponseDTO.java) mantendo um construtor de compatibilidade para não quebrar chamadas existentes, e garantir que [`RecipeService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/RecipeService.java) normalize categorias para as 6 opções oficiais da interface: `café da manhã`, `almoço`, `lanche`, `jantar`, `ceia`, `sobremesa`.

### Arquivos a Modificar
1. [`src/main/java/ChefBot_Telegram/dto/RecipeResponseDTO.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/dto/RecipeResponseDTO.java)
2. [`src/main/java/ChefBot_Telegram/service/RecipeService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/RecipeService.java)

### Passo a Passo de Implementação

#### 1.1. Atualizar `RecipeResponseDTO.java`
Substitua o conteúdo de [`RecipeResponseDTO.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/dto/RecipeResponseDTO.java) para incluir `transcription` e um construtor sobrecarregado de 9 argumentos (para manter compatibilidade com código e testes antigos):

```java
package ChefBot_Telegram.dto;

import java.util.List;

public record RecipeResponseDTO(
    Boolean isRecipe,
    String refusalReason,
    String title,
    String category,
    String sourceAuthor,
    Integer prepTimeMinutes,
    Integer portions,
    String transcription,
    List<IngredientDTO> ingredients,
    List<InstructionDTO> instructions
) {
    // Construtor de compatibilidade sem transcription
    public RecipeResponseDTO(
        Boolean isRecipe,
        String refusalReason,
        String title,
        String category,
        String sourceAuthor,
        Integer prepTimeMinutes,
        Integer portions,
        List<IngredientDTO> ingredients,
        List<InstructionDTO> instructions
    ) {
        this(isRecipe, refusalReason, title, category, sourceAuthor, prepTimeMinutes, portions, null, ingredients, instructions);
    }
}
```

#### 1.2. Adicionar normalizador de categoria em `RecipeService.java`
Em [`RecipeService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/RecipeService.java), adicione o método utilitário `normalizeCategory(String rawCategory)` e use-o ao salvar (`saveRecipeFromDTO`) e atualizar (`updateRecipeFromDTO` e `updateRecipeManual`):

```java
    public static final List<String> VALID_CATEGORIES = List.of(
        "café da manhã", "almoço", "lanche", "jantar", "ceia", "sobremesa"
    );

    /**
     * Normaliza a categoria vinda da IA ou do usuário para uma das 6 categorias oficiais.
     */
    public String normalizeCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            return "lanche";
        }
        String norm = normalizeText(rawCategory);
        if (norm.contains("cafe") || norm.contains("manha") || norm.contains("desjejum")) return "café da manhã";
        if (norm.contains("almoco") || norm.contains("prato principal")) return "almoço";
        if (norm.contains("janta")) return "jantar";
        if (norm.contains("ceia")) return "ceia";
        if (norm.contains("sobremesa") || norm.contains("doce") || norm.contains("bolo") || norm.contains("pudim") || norm.contains("torta doce")) return "sobremesa";
        if (norm.contains("lanche") || norm.contains("salgado") || norm.contains("bebida") || norm.contains("petisco")) return "lanche";
        // Se já bater com alguma categoria válida, retorna ela em minúsculo
        for (String valid : VALID_CATEGORIES) {
            if (normalizeText(valid).equals(norm)) {
                return valid;
            }
        }
        return rawCategory.trim().toLowerCase();
    }
```
- No método `saveRecipeFromDTO` (linha 53 de [`RecipeService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/RecipeService.java)):
  - Altere `recipe.setCategory(dto.category());` para `recipe.setCategory(normalizeCategory(dto.category()));`.
  - Se `dto.transcription()` não for nulo nem vazio, e `rawTranscript` for genérico (`"[Áudio gravado]"`, `"Gravação de áudio Web"` ou `null`), salve `dto.transcription().trim()` em `recipe.setRawTranscript(...)`.

### Verificação da Task 1
Execute:
```bash
./gradlew test
```

---

## Task 2: Refinamento do `GeminiService` (Sanitização de MIME, `system_instruction`, Temperatura e Regras de Fala Informal)

### Objetivo
Melhorar drasticamente o entendimento de áudio e texto no [`GeminiService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/GeminiService.java):
1. Criar o método `sanitizeAudioMimeType(String mimeType)` para limpar sufixos como `audio/webm;codecs=opus` -> `audio/webm` (evitando erro 400 na API do Gemini em celulares Android/iOS).
2. Atualizar o `SYSTEM_PROMPT` para:
   - Exigir que `category` seja **exatamente** uma das 6 opções: `"café da manhã"`, `"almoço"`, `"lanche"`, `"jantar"`, `"ceia"` ou `"sobremesa"`.
   - Preencher `"transcription"` com a transcrição fiel do áudio (ou cópia limpa do texto).
   - Tratar **autocorreções de fala** (ex: *"coloca 2... quer dizer, 3 xícaras de trigo"* -> registrar `3`).
   - Tratar **medidas informais brasileiras** (ex: *"um fio de azeite"*, *"uma pitada"*, *"meio copo americano"*, *"a gosto"*).
3. Estruturar o payload da API usando `"system_instruction"` separado de `"contents"` e `"generationConfig"` com `"temperature": 0.1` e `"responseMimeType": "application/json"`.

### Arquivo a Modificar
- [`src/main/java/ChefBot_Telegram/service/GeminiService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/GeminiService.java)

### Passo a Passo de Implementação

#### 2.1. Adicionar `sanitizeAudioMimeType(String mimeType)` em `GeminiService.java`
Adicione o método package-private estático para facilitar testes unitários:

```java
    static String sanitizeAudioMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "audio/ogg";
        }
        String clean = mimeType.split(";")[0].trim().toLowerCase();
        return switch (clean) {
            case "audio/mp4", "audio/x-m4a", "audio/m4a", "video/mp4" -> "audio/mp4";
            case "audio/mpeg", "audio/mp3" -> "audio/mp3";
            case "audio/wav", "audio/x-wav", "audio/wave" -> "audio/wav";
            case "audio/aac" -> "audio/aac";
            case "audio/webm", "video/webm" -> "audio/webm";
            case "audio/ogg", "application/ogg", "audio/opus" -> "audio/ogg";
            default -> clean.startsWith("audio/") ? clean : "audio/webm";
        };
    }
```

#### 2.2. Atualizar `SYSTEM_PROMPT` em `GeminiService.java`
Inclua no `SYSTEM_PROMPT`:
- Regra de **CATEGORIA OBRIGATÓRIA**: deve ser sempre uma destas 6 strings em minúsculas: `"café da manhã"`, `"almoço"`, `"lanche"`, `"jantar"`, `"ceia"`, `"sobremesa"`.
- Regra de **TRANSCRIÇÃO (`transcription`)**: para áudios, transcreva literalmente o que o usuário disse antes de extrair os campos.
- Regra de **AUTOCORREÇÃO E LINGUAGEM FALADA**: ignore hesitações (*"ééé"*, *"tipo assim"*) e aplique correções faladas pelo próprio locutor (*"duas colheres... não, três colheres"* -> usar `3`).
- Regra de **MEDIDAS CASEIRAS**: se o usuário disser *"uma pitada de sal"*, use `quantity="1"`, `unit="pitada"`, `name="Sal"`. Se disser *"um fio de azeite"*, use `quantity="1"`, `unit="fio"`, `name="Azeite"`.

#### 2.3. Adicionar suporte a Categorias no Fast-Path (`tryDirectFieldEdit`)
No método `tryDirectFieldEdit(Recipe existingRecipe, String updateInstructions)` em [`GeminiService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/GeminiService.java#L185), adicione também o matcher nº 5 para alterar a **Categoria** rapidamente sem gastar chamada de API:
```java
        // 5. Alterar Categoria
        java.util.regex.Matcher categoryMatcher = java.util.regex.Pattern
            .compile("(?i)^(?:mudar?\\s+(?:a\\s+)?)?categoria\\s*(?:para|pra|=|:|[eé])\\s*(.+)$")
            .matcher(trimmed);
        if (categoryMatcher.matches()) {
            String newCat = categoryMatcher.group(1).trim().toLowerCase();
            return Optional.of(new RecipeResponseDTO(
                true, null, existingRecipe.getTitle(), newCat,
                existingRecipe.getSourceAuthor(), existingRecipe.getPrepTimeMinutes(), existingRecipe.getPortions(),
                ingredientsDTO, instructionsDTO
            ));
        }
```

#### 2.4. Estruturar o Payload com `system_instruction` e `temperature: 0.1`
Crie métodos auxiliares package-private `buildTextPayload(String userText)` e `buildAudioPayload(byte[] audioBytes, String mimeType)` em [`GeminiService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/GeminiService.java) que montam o `Map<String, Object>`:

```java
    Map<String, Object> buildTextPayload(String userText) {
        return Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            ),
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", "Entrada do usuário:\n\"" + userText + "\"")
                ))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.1
            )
        );
    }

    Map<String, Object> buildAudioPayload(byte[] audioBytes, String mimeType) {
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        String cleanMime = sanitizeAudioMimeType(mimeType);

        return Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT))
            ),
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("inlineData", Map.of(
                        "mimeType", cleanMime,
                        "data", base64Audio
                    )),
                    Map.of("text", "Ouça o áudio com atenção, preencha o campo 'transcription' com o que foi falado e extraia a receita estruturada no formato JSON especificado.")
                ))
            ),
            "generationConfig", Map.of(
                "responseMimeType", "application/json",
                "temperature", 0.1
            )
        );
    }
```

### Verificação da Task 2
Execute:
```bash
./gradlew test
```

---

## Task 3: Novos Endpoints de Edição com IA e Preservação de Transcrição no `RecipeController`

### Objetivo
1. Em [`RecipeController.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java), salvar a transcrição retornada pela IA (`dto.transcription()`) quando uma receita for criada por áudio.
2. Adicionar o endpoint `POST /api/recipes/{id}/ai-update` no [`RecipeController.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java) para permitir que o usuário edite uma receita pela Web usando linguagem natural (ex: *"troque o açúcar por mel e mude para 6 porções"*), reutilizando `geminiService.updateRecipe(...)`.

### Arquivo a Modificar
- [`src/main/java/ChefBot_Telegram/controller/RecipeController.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java)
- [`src/main/java/ChefBot_Telegram/service/TelegramBotService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/TelegramBotService.java)

### Passo a Passo de Implementação

#### 3.1. Salvar `dto.transcription()` no `RecipeController` e `TelegramBotService`
- Em [`RecipeController.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java#L116), altere:
```java
String transcriptToSave = (dto.transcription() != null && !dto.transcription().isBlank())
    ? dto.transcription()
    : "Gravação de áudio Web";
Recipe saved = recipeService.saveRecipeFromDTO(WEB_DEFAULT_CHAT_ID, WEB_DEFAULT_USER_NAME, dto, transcriptToSave);
```
- Em [`TelegramBotService.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/service/TelegramBotService.java#L184), faça o mesmo:
```java
String transcriptToSave = (dto.transcription() != null && !dto.transcription().isBlank())
    ? dto.transcription()
    : "[Áudio gravado]";
Recipe savedRecipe = recipeService.saveRecipeFromDTO(chatId, userName, dto, transcriptToSave);
```

#### 3.2. Criar endpoint `POST /api/recipes/{id}/ai-update` em `RecipeController.java`
Adicione no [`RecipeController.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/java/ChefBot_Telegram/controller/RecipeController.java):
```java
    /**
     * Atualiza uma receita existente usando instruções em linguagem natural via IA.
     */
    @PostMapping("/{id}/ai-update")
    public ResponseEntity<?> updateRecipeWithAi(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String instructions = payload.get("instructions");
        if (instructions == null || instructions.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Instruções de alteração não informadas."));
        }

        Optional<Recipe> existingOpt = recipeService.findRecipeById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            Recipe existing = existingOpt.get();
            RecipeResponseDTO updatedDto = geminiService.updateRecipe(existing, instructions);
            Long ownerChatId = existing.getUser() != null ? existing.getUser().getTelegramChatId() : WEB_DEFAULT_CHAT_ID;
            Recipe updated = recipeService.updateRecipeFromDTO(ownerChatId, id, updatedDto);
            return ResponseEntity.ok(toDTO(updated));
        } catch (Exception e) {
            log.error("Erro ao atualizar receita com IA: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao atualizar com IA: " + e.getMessage()));
        }
    }
```

### Verificação da Task 3
Execute:
```bash
./gradlew test
```

---

## Task 4: Melhorias de Captação de Áudio e Edição no Frontend ([`index.html`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/resources/static/index.html))

### Objetivo
1. **Detecção automática de MIME type de áudio** (`getSupportedAudioMimeType()`) compatível com Android (Chrome/Firefox) e iOS (Safari).
2. **Filtros de voz no microfone** (`echoCancellation: true`, `noiseSuppression: true`, `autoGainControl: true`) para reduzir barulho de cozinha.
3. **Upload de arquivo de áudio existente** (`<input type="file" accept="audio/*">`) para permitir enviar áudios salvos do WhatsApp/celular.
4. **Correção da edição manual de ingredientes** para preservar `quantity`, `unit` e `notes` (formato `Nome | Quantidade | Unidade | Notas` ou parser inteligente), além de um botão rápido de **"Edição Inteligente com IA"** dentro do modal de editar.

### Arquivo a Modificar
- [`src/main/resources/static/index.html`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/resources/static/index.html)

### Passo a Passo de Implementação

#### 4.1. Melhorar captura de microfone e MIME type dinâmico em `index.html`
Adicione a função `getSupportedAudioMimeType()` no `<script>` de [`index.html`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/resources/static/index.html):
```javascript
    function getSupportedAudioMimeType() {
      const types = [
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/ogg;codecs=opus',
        'audio/mp4',
        'audio/aac'
      ];
      if (typeof MediaRecorder === 'undefined') return '';
      for (const t of types) {
        if (MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(t)) {
          return t;
        }
      }
      return '';
    }
```
E em `toggleRecording()`:
- Solicite o stream com supressão de ruído:
```javascript
const stream = await navigator.mediaDevices.getUserMedia({
  audio: {
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl: true,
    channelCount: 1
  }
});
```
- Instancie o `MediaRecorder` usando o `mimeType` detectado e crie o `Blob` com `mediaRecorder.mimeType || 'audio/webm'`.
- Adicione abaixo do botão de gravar uma opção `<label>` para selecionar um arquivo de áudio do celular (`handleAudioFileUpload(event)`).

#### 4.2. Preservar Unidades e Notas no Modal de Edição + Campo de Edição Rápida por IA
No modal de edição (`#modal-edit`) em [`index.html`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/main/resources/static/index.html):
- Adicione um box no topo do modal de edição: **"✨ Pedir alteração para a IA"** com um `<input type="text" id="ai-edit-input" placeholder="Ex: Troque açúcar por mel e dobre a receita">` e botão **"Aplicar com IA"** que chama `POST /api/recipes/${id}/ai-update`.
- Na edição manual de ingredientes (`openEditModal` e `saveEditedRecipe`), formate cada linha como `Nome | Quantidade | Unidade | Observação` (ou suporte `Nome - Quantidade Unidade (Observação)`) para nunca perder `unit` nem `notes` ao salvar.

---

## Task 5: Criação e Atualização dos Testes Automatizados (Consistência Garantida)

### Objetivo
Garantir cobertura de testes unitários e de integração para todas as melhorias implementadas.

### Arquivos de Teste a Atualizar / Criar
1. Atualizar [`src/test/java/ChefBot_Telegram/service/GeminiServiceTest.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/test/java/ChefBot_Telegram/service/GeminiServiceTest.java)
2. Atualizar [`src/test/java/ChefBot_Telegram/service/GeminiServiceUpdatePromptTest.java`](file:///home/bruno/meusProjetos/ChefBot_Telegram/ChefBot_Telegram/src/test/java/ChefBot_Telegram/service/GeminiServiceUpdatePromptTest.java)
3. Criar `src/test/java/ChefBot_Telegram/controller/RecipeControllerTest.java`

### Casos de Teste Obrigatórios

#### 5.1. Em `GeminiServiceTest.java`
- `shouldSanitizeAudioMimeTypesCorrectly()`:
  - Verifica que `"audio/webm;codecs=opus"` vira `"audio/webm"`.
  - Verifica que `"audio/mp4; codecs=mp4a.40.2"` vira `"audio/mp4"`.
  - Verifica que `null` ou `""` vira `"audio/ogg"`.
- `shouldParseTranscriptionAndOfficialCategoryFromGeminiResponse()`:
  - Verifica que o JSON contendo `"transcription": "Bolo de fubá fácil..."` e `"category": "café da manhã"` é desserializado corretamente no `RecipeResponseDTO`.
- `shouldBuildAudioAndTextPayloadsWithSystemInstructionAndLowTemperature()`:
  - Verifica que `buildTextPayload(...)` e `buildAudioPayload(...)` contêm a chave `"system_instruction"`, `"generationConfig"` com `"temperature": 0.1` e `"responseMimeType": "application/json"`.

#### 5.2. Em `GeminiServiceUpdatePromptTest.java`
- `shouldUpdateCategoryViaFastPath()`:
  - Verifica que `"mudar a categoria para café da manhã"` atualiza a categoria via `tryDirectFieldEdit` sem chamar a rede.

#### 5.3. Novo `RecipeControllerTest.java`
- `shouldSaveTranscriptionFromAudioWhenProvidedByAi()`:
  - Mocka `GeminiService.extractRecipeFromAudio(...)` retornando uma receita válida com `transcription = "Receita falada no microfone"` e `category = "Sobremesa"`.
  - Chama `recipeController.createFromAudio(...)` com um `MockMultipartFile`.
  - Verifica que o status é `201 CREATED`, a categoria foi normalizada para `"sobremesa"` e `rawTranscript` foi salvo como `"Receita falada no microfone"`.
- `shouldUpdateRecipeViaAiEndpoint()`:
  - Cria uma receita no banco de teste, chama `recipeController.updateRecipeWithAi(id, Map.of("instructions", "mudar o tempo para 50 minutos"))` e verifica se `prepTimeMinutes` passa a ser `50`.

### Comando Final de Validação da Task 5
```bash
./gradlew clean test
```
Todos os testes devem passar sem erros (`BUILD SUCCESSFUL`).
