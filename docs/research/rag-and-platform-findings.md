# RAG and platform findings (Spring AI 1.1.2 / Boot 3.5 / Security 6.x)

Researched 2026-10-01. Primary sources only. Where the docs page and the tagged source (`v1.1.2`) disagreed, the source is cited and the conflict noted.
Source root for code: `https://github.com/spring-projects/spring-ai/blob/v1.1.2/` (abbreviated `SAI/`).

## 1. Attaching retrieval to a ChatClient

**Answer.** Two options in 1.1.x:
- `QuestionAnswerAdvisor` (simple naive RAG). Package in 1.1.2 source is `org.springframework.ai.chat.client.advisor.vectorstore` (module `spring-ai-advisors-vector-store`). The docs page lists `org.springframework.ai.advisor`; that does not match the tagged source, so trust the source.
- `RetrievalAugmentationAdvisor` (modular RAG: query transformers, retriever, augmenter). Package `org.springframework.ai.rag.advisor`; needs the `spring-ai-rag` artifact. Retriever is `org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever`; augmenter `ContextualQueryAugmenter` (`allowEmptyContext(true)` to answer with no hits).
- `QuestionAnswerAdvisor` builder: `builder(VectorStore).searchRequest(SearchRequest).promptTemplate(PromptTemplate).build()`. Per-request filter via param `QuestionAnswerAdvisor.FILTER_EXPRESSION` (= `"qa_filter_expression"`). For `RetrievalAugmentationAdvisor` the param is `VectorStoreDocumentRetriever.FILTER_EXPRESSION`.
- `SearchRequest.builder()`: `query`, `topK` (default 4), `similarityThreshold` (default 0.0 = accept all), `similarityThresholdAll()`, `filterExpression(String|Filter.Expression)`.

**Code shape** (from docs):
```java
var qa = QuestionAnswerAdvisor.builder(vectorStore)
    .searchRequest(SearchRequest.builder().similarityThreshold(0.8d).topK(6).build())
    .build();
chatClient.prompt().advisors(qa).user(q)
    .advisors(a -> a.param(QuestionAnswerAdvisor.FILTER_EXPRESSION, "categoryId == 3"))
    .call().chatResponse();

Advisor rag = RetrievalAugmentationAdvisor.builder()
    .documentRetriever(VectorStoreDocumentRetriever.builder()
        .similarityThreshold(0.50).vectorStore(vectorStore).build())
    .build();
```

**Sources**
- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html
- https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- SAI/advisors/spring-ai-advisors-vector-store/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java
- SAI/spring-ai-rag/src/main/java/org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java

## 2. VectorStore: delete, metadata, chunking, pgvector schema

**Answer.**
- `VectorStore` has `add(List<Document>)`, `delete(List<String> idList)`, `delete(Filter.Expression)`, and default `delete(String filterExpression)` (verified in source, VectorStore.java lines 62/69/79). Delete by metadata, e.g. `vectorStore.delete("blogId == 42")` (docs example uses string values: `"docId == 'AIML-001' AND version == '1.0'"`). Docs show "delete old version then add new" as the versioning pattern; this is the update/delete-blog pattern.
- Metadata: `new Document(text, Map.of("blogId", ..., "categoryId", ..., "authorId", ...))`. Filter syntax: `==, !=, >, >=, <, <=, AND/&&, OR/||, in [..], nin [..], not(..), is null`. Type of the value in the filter should match how it was stored (numeric vs string); docs do not spell this out for pgvector (unverified beyond the examples).
- `TokenTextSplitter.builder()...build()`; `splitter.apply(List<Document>)`. Defaults: chunkSize 800 tokens, minChunkSizeChars 350, minChunkLengthToEmbed 5, maxNumChunks 10000, keepSeparator true. Original metadata is copied to every chunk. The docs page shows `withChunkSize(..)` style builder methods (docs say constructors are deprecated); exact builder method names not re-verified in source.
- pgvector: `initialize-schema` is opt-in, default `false` (`CommonVectorStoreProperties`, `private boolean initializeSchema = false`). When true it runs `CREATE EXTENSION IF NOT EXISTS vector/hstore/uuid-ossp` and creates the table + index if absent. `PgVectorStore.DEFAULT_ID_TYPE = PgIdType.UUID`; id column is `uuid DEFAULT uuid_generate_v4()`; delete-by-id does `UUID.fromString(id)`, so ids passed to `delete(List<String>)` must be UUID strings. `spring.ai.vectorstore.pgvector.id-type` exists (enum UUID, TEXT, INTEGER, SERIAL, BIGSERIAL) in source (field `idType`; the property path follows the field name, not shown in the docs page I fetched).
- Existing table is NOT altered: if `vector_store` exists with a different `vector(N)`, `initialize-schema` does not change it (CREATE TABLE IF NOT EXISTS). Dimensions come from `spring.ai.vectorstore.pgvector.dimensions`, else from `EmbeddingModel.dimensions()`, else 1536.
- Deleting by filter uses `metadata::jsonb @@ '<jsonpath>'` (PgVectorStore.doDelete(Filter.Expression)).
- Docs warn `remove-existing-vector-store-table` (default false) drops the table on startup.

**Code shape**
```java
Document d = new Document(chunkText, Map.of("blogId", id, "authorId", aid));
vectorStore.add(splitter.apply(List.of(d)));
vectorStore.delete("blogId == " + id);   // or Filter.Expression via FilterExpressionBuilder
```

**Sources**
- https://docs.spring.io/spring-ai/reference/api/vectordbs.html
- https://docs.spring.io/spring-ai/reference/api/vectordbs/pgvector.html
- https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html
- SAI/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/VectorStore.java
- SAI/spring-ai-vector-store/src/main/java/org/springframework/ai/vectorstore/properties/CommonVectorStoreProperties.java
- SAI/vector-stores/spring-ai-pgvector-store/src/main/java/org/springframework/ai/vectorstore/pgvector/PgVectorStore.java
- SAI/auto-configurations/vector-stores/spring-ai-autoconfigure-vector-store-pgvector/src/main/java/org/springframework/ai/vectorstore/pgvector/autoconfigure/PgVectorStoreProperties.java

## 3. Google GenAI embeddings and models

**Answer.**
- Property names (verified in `GoogleGenAiTextEmbeddingProperties`, prefix `spring.ai.google.genai.embedding.text`, with an `options` field): the working form is `spring.ai.google.genai.embedding.text.options.model` and `...text.options.dimensions` (the project already uses `.options.model`). The docs page lists them without `.options` (`...text.model`, `...text.dimensions`); this conflicts with the source's `getOptions()` binding. Treat the `.options.` form as verified from source; the docs form is unverified. Connection: `spring.ai.google.genai.embedding.api-key`.
- Spring AI 1.1.2 default model is `text-embedding-004` (768 dims); its `GoogleGenAiTextEmbeddingModelName` enum only knows `text-embedding-004` and `text-multilingual-embedding-002`. The model option is a free `String`, so another id can be passed; the docs do not list `gemini-embedding-001`/`-2` as supported for Spring AI 1.1.2 (unverified whether it works end to end). Dimensions option maps to `outputDimensionality` in the request (GoogleGenAiTextEmbeddingModel.java line ~146).
- Google's deprecations table: `text-embedding-004` shutdown **January 14, 2026** (replacement `gemini-embedding-2`); that date is already past as of 2026-10-01. `gemini-embedding-001`: released July 14, 2025, shutdown **May 14, 2028**, replacement `gemini-embedding-2`. `gemini-embedding-2`: released April 22, 2026, no shutdown announced. `embedding-001` shut down Oct 30, 2025.
- Dimensions (Google embeddings doc): both models default to 3072 output dims, truncation allowed, recommended 768, 1536, 3072 (gemini-embedding-001 range 128-3072). `gemini-embedding-001` requires manual normalisation for non-3072 dims; `gemini-embedding-2` auto-normalises. Embedding spaces of the two models are not comparable and switching means re-embedding all data (my fetch summary stated this; the raw page confirms 001 needs manual normalisation).
- Practical: to keep `vector(768)` set dimensions=768 with a gemini-embedding model; any model change requires re-embedding all stored vectors.
- `gemini-2.5-flash`: Google lists release June 17, 2025, "No shutdown date announced". Table also notes access to 2.5 models is being limited to users who have used them before (per my fetch summary; the raw table row confirms no shutdown date). Spring AI chat property is `spring.ai.google.genai.chat.options.model` per source (`GoogleGenAiChatProperties` has `options`); docs page shows `spring.ai.google.genai.chat.model`.

**Sources**
- https://ai.google.dev/gemini-api/docs/deprecations
- https://ai.google.dev/gemini-api/docs/embeddings
- https://docs.spring.io/spring-ai/reference/api/embeddings/google-genai-embeddings-text.html
- https://docs.spring.io/spring-ai/reference/api/chat/google-genai-chat.html
- SAI/auto-configurations/models/spring-ai-autoconfigure-model-google-genai/src/main/java/org/springframework/ai/model/google/genai/autoconfigure/embedding/GoogleGenAiTextEmbeddingProperties.java
- SAI/models/spring-ai-google-genai-embedding/src/main/java/org/springframework/ai/google/genai/text/GoogleGenAiTextEmbeddingModelName.java

## 4. Citations / sources in RAG answers

**Answer.** Both advisors put the retrieved `List<Document>` on the response:
- `QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS` = `"qa_retrieved_documents"`: set in the advisor context and copied into `ChatResponse` metadata (`chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, ...)`).
- `RetrievalAugmentationAdvisor.DOCUMENT_CONTEXT` = `"rag_document_context"`: same behaviour.
- Read via `chatClient...call().chatResponse().getMetadata().get(KEY)` or `.chatClientResponse().context().get(KEY)`. Each `Document` carries `getMetadata()` (blogId etc.) and `getScore()` (if the store sets it). Map `blogId` metadata to blog posts to return citations; dedupe across chunks.

**Sources**
- SAI/advisors/spring-ai-advisors-vector-store/src/main/java/org/springframework/ai/chat/client/advisor/vectorstore/QuestionAnswerAdvisor.java (lines 56, 119, 146)
- SAI/spring-ai-rag/src/main/java/org/springframework/ai/rag/advisor/RetrievalAugmentationAdvisor.java (lines 63, 143, 173)
- https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html

## 5. Spring Security 6.x: current user, ownership checks, rate limiting

**Answer.**
- Controller: `@AuthenticationPrincipal CustomUser u` (resolved by `AuthenticationPrincipalArgumentResolver`, registered by `@EnableWebSecurity`); SpEL `expression` supported; wrap in a `@CurrentUser` meta-annotation to isolate the dependency. For services, the docs approach is method security with parameters passed from the controller, or `SecurityContextHolder` (not re-verified here).
- Method security: `@EnableMethodSecurity`; `@PreAuthorize("@authz.decide(#root)")` with a `@Component("authz")` bean, or `#param` references (needs `@P`/`@Param`/`-parameters`); `@PostAuthorize("returnObject.owner == authentication.name")`. A bean ownership check like `@PreAuthorize("@blogSecurity.isOwner(#id, authentication)")` follows the same bean-reference form (the docs show `@authz.decide(#root)`; the exact ownership variant is my application of it).
- Rate limiting / lockout: not found in the Spring Security reference (the "Protection Against Exploits" index lists only CSRF, headers, HTTP, HttpFirewall). Spring Security provides `UserDetails.isAccountNonLocked()` and `LockedException`, but the lockout counting logic is not provided (API docs state locking is not implemented in `DaoAuthenticationProvider`; counting would be custom, e.g. via `AuthenticationFailureBadCredentialsEvent`; event approach unverified in docs). OWASP: tie counters to the account, not IP, define threshold/window/duration, beware lockout-as-DoS, add throttling/CAPTCHA/MFA.

**Code shape**
```java
@GetMapping("/me") User me(@AuthenticationPrincipal CustomUser u) {...}
@PreAuthorize("@authz.decide(#root)") // from docs
```

**Sources**
- https://docs.spring.io/spring-security/reference/servlet/integrations/mvc.html
- https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html
- https://docs.spring.io/spring-security/reference/servlet/exploits/index.html
- https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/authentication/LockedException.html
- https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html

## 6. Boot 3.5 config/profiles and Pageable

**Answer.**
- Files: `application.properties` plus `application-{profile}.properties` (profile file overrides); activate via `spring.profiles.active`. Placeholders `${name}` and `${name:default}` resolve against any property source, including OS env vars. Env vars use relaxed binding (`SPRING_DATASOURCE_PASSWORD`). Optional external file: `spring.config.import=optional:file:./dev.properties`; Kubernetes: `optional:configtree:/etc/config/`. OS env vars outrank config files in precedence.
- Paging: repository method `Page<T> findAll(Pageable)` (`PagingAndSortingRepository`), `PageRequest.of(page, size)` (zero-based). Derived queries may also take `Pageable` and return `Page`/`Slice`/`List`. (Slice/derived-query details not shown on the page fetched.)

**Sources**
- https://docs.spring.io/spring-boot/reference/features/external-config.html
- https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html

## 7. Flyway baseline and Testcontainers

**Answer.**
- Flyway on Boot 3.5: needs `org.flywaydb:flyway-core` plus the DB module `org.flywaydb:flyway-database-postgresql`; migrations default to `classpath:db/migration`, `V<VERSION>__<NAME>.sql`. For an existing Hibernate-created schema: `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=1` (property names from the Boot docs page; note the page fetched was Boot 4.x, but these two properties are also in `spring.flyway.*` in 3.x; 3.5 reference list not re-checked). Flyway: baselineOnMigrate defaults false; it baselines a non-empty schema with no history table, applying only migrations above the baseline version; warning that it removes the safety net against migrating the wrong DB. Boot docs recommend using Flyway alone, i.e. set `spring.jpa.hibernate.ddl-auto=none` (or `validate`). So V1 should be a snapshot of the current schema (applied only on fresh DBs), existing DBs get baselined at 1.
- Testcontainers: add `spring-boot-testcontainers` (test) and `org.testcontainers:postgresql` (+ `junit-jupiter`). `@ServiceConnection` on a `@Container static PostgreSQLContainer<?>` (3.5 docs import `org.testcontainers.containers.PostgreSQLContainer`; TC 2.x moved it to `org.testcontainers.postgresql`). pgvector image: Testcontainers Postgres module documents `new PostgreSQLContainer("pgvector/pgvector:pg16")`. Boot docs: for custom images use `@ServiceConnection(name = "postgres")`. Whether `pgvector/pgvector` needs `asCompatibleSubstituteFor("postgres")` with a typed `PostgreSQLContainer`: the TC page shows pgvector without it, postgis/timescale with it; unverified for `@ServiceConnection` detection (typed containers are matched by container class, per the Boot docs, so it should be fine).
- Spring AI's own Testcontainers module (`spring-ai-spring-boot-testcontainers`) has no pgvector-specific connection details, so use the plain Postgres path.

**Code shape**
```java
@Testcontainers @SpringBootTest
class T {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("pgvector/pgvector:pg16");
}
```

**Sources**
- https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html
- https://docs.spring.io/spring-boot/how-to/data-initialization.html
- https://documentation.red-gate.com/flyway/reference/configuration/flyway-namespace/flyway-baseline-on-migrate-setting
- https://docs.spring.io/spring-boot/3.5/reference/testing/testcontainers.html
- https://java.testcontainers.org/modules/databases/postgres/
- https://docs.spring.io/spring-ai/reference/api/testcontainers.html

## Surprises / things that contradict the audit report or the current pom/properties

1. `text-embedding-004` (set in application.properties) was shut down by Google on Jan 14, 2026; today is 2026-10-01, so embeddings will fail unless it is replaced. Spring AI 1.1.2 still defaults to it and its enum only knows 004/multilingual-002. Replacement per Google: `gemini-embedding-2` (or `gemini-embedding-001`, shutdown May 14, 2028).
2. Switching embedding model means re-embedding everything; `vector_store` is `vector(768)`: set dimensions to 768 explicitly on the new model (default is 3072) and keep `pgvector.dimensions=768`. `initialize-schema` will not alter an existing table.
3. `gemini-2.5-flash` has no shutdown date, but Google is limiting access to 2.5 models to prior users and recommends newer Flash models for new projects.
4. `initialize-schema=true` in the properties: Spring AI default is false (opt-in); the store creates `uuid-ossp`, so vector ids are UUIDs and any `delete(List<String>)` must pass UUID strings; per-blog cleanup is better done with `delete("blogId == ...")`.
5. Docs page vs source mismatches: QuestionAnswerAdvisor package, and property names with/without `.options.`. The project's `...embedding.text.options.model` and `...chat.options.model` match source.
6. pom has both `spring-ai-google-genai` and the two starters (chat + embedding starters already pull the model modules); `spring-ai-rag` is not in the pom (needed only for `RetrievalAugmentationAdvisor`); no Flyway or Testcontainers dependencies exist yet, and `ddl-auto=update` must change to `none`/`validate` when Flyway is adopted. (pom grep only; not a full pom review.)
7. Spring Security provides no rate limiting and no lockout counter; those must be custom (or a library, not covered by primary docs here).
8. Some Boot docs pages resolve to Boot 4.x content by default (e.g. `spring-boot-starter-flyway`, TC 2.x packages); use the `/3.5/` URLs for this project.
