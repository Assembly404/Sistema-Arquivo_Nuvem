# Análise de Melhorias — Sistema Arquivo Nuvem

> **Objetivo:** identificar, função a função, **funcionalidades a adicionar ou melhorar**, com base no cruzamento entre os requisitos oficiais (`docs/requisitos_sistema_armazenamento_nuvem.pdf`, RF01–RF30 / RNF01–RNF07) e o código realmente implementado em `cloudsystem/` (backend) e `frontend/` (SPA React).
>
> **Data:** 2026-10-12
> **Base:** leitura do código-fonte atual (controllers, serviços, entidades, páginas React, configuração, CI). Complementa — e corrige — o `docs/PLANO_COMPLETUDE.md` onde este diverge do código.

---

## 1. Metodologia

1. Extração dos requisitos funcionais e não funcionais do PDF oficial.
2. Inventário do backend: 20 controllers, ~25 serviços, 20 entidades, 11 repositórios, 12 DTOs de entrada, 15 de saída.
3. Inventário do frontend: páginas, componentes, estado (`AuthProvider`), cliente HTTP (`axios.js`) e configuração Vite.
4. Verificação de CI (`.github/workflows/ci.yml`), dependências (`pom.xml`, `package.json`) e configuração (`application.properties`).
5. Classificação de cada item: **✅ Implementado · ⚠️ Parcial · ❌ Ausente**, seguida de melhoria proposta, critérios de aceite e esforço.

**Legenda de esforço:** 🟢 Baixo (≤1 dia) · 🟡 Médio (2–4 dias) · 🔴 Alto (>4 dias).

---

## 2. Sumário executivo

O **núcleo funcional está sólido**: autenticação JWT, CRUD de ficheiros/pastas com lixeira e versionamento, cifra AES‑GCM (container `.dbs`), partilhas + links públicos, arquitetura de nós/agente local, heartbeat e replicação síncrona. As lacunas concentram-se em **5 eixos**:

| Eixo | Risco | Exemplos |
|---|---|---|
| **Segurança de acesso** | 🔴 Alto | Sem 2FA (RF04), sem bloqueio por tentativas, suspensão lida do JWT e não da BD |
| **Integridade de dados** | 🔴 Alto | Exclusão de conta deixa órfãos; `updateContent`/`copy-to-space` fogem à quota |
| **Completude de RF** | 🟡 Médio | `offlineBehavior=CACHE` guardado mas nunca usado (RF24); sem audit log (RF29) |
| **Robustez/observabilidade** | 🟡 Médio | Sem testes; CI provavelmente falha; sem Actuator; sem WebSocket |
| **UX/produto** | 🟢 Baixo | Sem preview, ordenação, paginação; placeholders em Perfil/Configurações/Suporte |

**Top 6 correções críticas (P0):** (1) testes + perfil de teste, (2) rate‑limit/bloqueio de login, (3) suspensão efetiva a partir da BD, (4) purga real na exclusão de conta, (5) fechar fugas de quota, (6) validação de tipos no upload.

---

## 3. Matriz de cobertura de requisitos

### 3.1 Funcionais (RF01–RF30)

| RF | Requisito | Estado | Observação |
|---|---|---|---|
| RF01 | Cadastro de utilizador | ✅ | `AuthController.register` |
| RF02 | Login | ✅ | JWT 24h fixas |
| RF03 | Recuperação/redefinição de senha | ✅ | OTP 6 dígitos, 15 min, 5 tentativas |
| RF04 | Autenticação em duas etapas (2FA) | ❌ | Sem TOTP, sem campo `totpCode` |
| RF05 | Edição de perfil (nome, email, senha) | ⚠️ | Nome/sobrenome e senha sim; **email não editável** |
| RF06 | Exclusão de conta | ⚠️ | Só cascade BD; órfãos no disco e link público |
| RF07 | Armazenar ficheiros | ✅ | Upload multipart |
| RF08 | Gerir pastas (CRUD/mover/eliminar) | ✅ | Anti‑ciclo e lixeira recursiva |
| RF09 | Compactação no upload | ⚠️ | **Implementada** (DBS2), mas doc diz removida |
| RF10 | Descompactação no download | ⚠️ | Idem |
| RF11 | Busca por nome/tipo/data | ⚠️ | Backend completo; UI só busca por nome, sem ordenar/paginar |
| RF12 | Exclusão reversível (lixeira) | ✅ | Retenção 30 dias + purge agendado |
| RF13 | Versionamento | ✅ | Snapshot/restauro, contador de versão |
| RF14 | Partilhar ficheiros/pastas | ✅ | `ShareService` |
| RF15 | Permissões (leitura/edição) | ✅ | VIEW/EDIT + herança de pasta |
| RF16 | Link público | ✅ | Token opaco, senha, expiração, revogação |
| RF17 | Configurar dispositivo pessoal | ✅ | Pairing + agente `.jar` |
| RF17.1 | Pasta do dispositivo | ✅ | `localPath` |
| RF17.2 | Espaço a disponibilizar | ✅ | `totalSpaceBytes` |
| RF17.3 | Agente/cliente local | ✅ | `cloudsystem-node.jar`, `NodeHttpServer` |
| RF17.4 | Vinculação segura | ✅ | Pairing code + device token cifrado |
| RF17.5 | Nomear/vários dispositivos | ✅ | `name` |
| RF17.6 | Ver/gerir lista (ativar/desativar/remover) | ✅ | `NodeController` |
| RF17.7 | Revogar com migração | ✅ | `removeDevice(migrate=true)` |
| RF17.8 | Validar requisitos mínimos | ⚠️ | Valida SO + espaço livre; **sem teste de conectividade** |
| RF18 | Dois espaços unificados | ✅ | |
| RF19 | Ver consumo por espaço | ✅ | `/storage/summary` |
| RF20 | Escolher destino no upload | ✅ | SYSTEM/DEVICE/MANUAL |
| RF21 | Mover/copiar entre espaços | ⚠️ | Copiar não verifica quota |
| RF22 | Política padrão | ✅ | SYSTEM_FIRST/DEVICE_FIRST/MANUAL |
| RF23 | Status online/offline | ✅ | Heartbeat + monitor |
| RF24 | Comportamento offline | ⚠️ | `CACHE` guardado, **não implementado** |
| RF25 | Limitar cota por utilizador | ✅ | Admin define; upload valida |
| RF26 | Notificar próximo do limite | ⚠️ | Só 80%, só no upload, sem anti‑spam; sem 95% |
| RF27 | Sincronizar múltiplos dispositivos | ⚠️ | **Fator de réplica = 1**; sem redundância/quórum |
| RF28 | Ver status de sincronização | ✅ | `/nodes/{id}/sync-status` |
| RF29 | Registar logs de ações | ❌ | Sem `AuditLog` |
| RF30 | Notificações de eventos | ⚠️ | Tipos existem; sem WebSocket; `NODE_ONLINE` nunca emitido |

> ⚠️ **Nota de numeração:** comentários no código referem RF33/RF35 e o `PLANO_COMPLETUDE.md` usa RF01–RF35, mas o PDF oficial lista **RF01–RF30**. Alinhar a numeração dos comentários/docs com o PDF.

### 3.2 Não funcionais (RNF)

| RNF | Requisito | Estado | Observação |
|---|---|---|---|
| RNF01 | Cifra em trânsito e repouso | ⚠️ | Repouso AES‑GCM ✅; agente local comunica em **HTTP:8081** |
| RNF02 | Múltiplas plataformas (Win/Mac/Linux/Android/iOS) | ⚠️ | Agente Win/Mac/Linux; **sem apps móveis** |
| RNF03 | Disponibilidade ≥ 99% | ⚠️ | Fator de réplica 1 → ponto único |
| RNF04 | Escalabilidade | ⚠️ | Escala vertical; sem sharding/particionamento |
| RNF05 | Tempo máximo de upload/download | ⚠️ | Sem limites nem métricas |
| RNF06 | LGPD/GDPR | ⚠️ | Sem auditoria, exportação de dados ou consentimento |
| RNF07 | Agente com consumo mínimo | ⚠️ | Não medido |

---

## 4. Análises detalhadas

### A. Autenticação e conta

#### A1. 2FA / TOTP (RF04) — ❌ Ausente
- **Estado:** não há `totpSecret`, endpoint `/auth/2fa/*` nem `totpCode` em `LoginRequest`. `pom.xml` sem biblioteca TOTP. OTP existe **apenas** na recuperação de senha (`PasswordResetService`).
- **Melhoria:**
  1. `User.totpSecret` (nullable) + `User.twoFactorEnabled`.
  2. `TotpService` (HMAC‑SHA1, janela 30s, tolerância ±1) ou `com.warrenstrange:googleauth`.
  3. `POST /auth/2fa/setup` (gera secret + `otpauth://`), `POST /auth/2fa/verify`, `POST /auth/2fa/disable`, `GET /auth/2fa/status`.
  4. `LoginResponse` devolve `2faRequired: true` quando ativo; `LoginRequest.totpCode` obrigatório nessa fase.
  5. UI: campo no `Login.jsx`; ativar/desativar em `Settings.jsx`/`Profile.jsx` com QR.
- **Aceite:** login com 2FA ativo exige código válido; código expirado/errado → 401 genérico; segredo gravado cifrado.
- **Esforço:** 🟡 Médio.

#### A2. Bloqueio por tentativas + rate limiting — ❌ Ausente
- **Estado:** sem `LoginAttempt`/limiter; só a recuperação de senha limita tentativas.
- **Melhoria:** `LoginAttemptService` (email+IP, TTL 15 min) + `AuthenticationFailureHandler`; bloquear após 5 falhas; limpar no sucesso. Rate limiting global (Bucket4j ou filtro em memória): ~100 req/min por IP, 10/min em `/auth/login`. Config: `security.max-attempts=5`, `security.lockout-minutes=15`.
- **Aceite:** 6ª tentativa → `423/429` com mensagem genérica; admin não consegue enumerar emails.
- **Esforço:** 🟡 Médio.

#### A3. Suspensão efetiva (from DB) — ⚠️ Bug de segurança
- **Estado:** `SecurityFilter` (l.41‑48) lê o claim `suspended` **do JWT**, não da BD. O comentário afirma "re‑verificar a cada pedido", mas um utilizador suspenso mantém acesso até 24h.
- **Melhoria:** no filtro, carregar estado do utilizador (cache curto) e rejeitar se `suspended`; ou incluir versão de token/`jti` invalidável. Alternativa mínima: verificar `suspended` na BD a cada pedido (com cache de curta duração).
- **Aceite:** suspender utilizador → próximo pedido devolve 401/403 imediatamente.
- **Esforço:** 🟢 Baixo.

#### A4. Sessão: inatividade, refresh e logout — ❌ Ausente
- **Estado:** JWT fixo de 24h, stateless, sem `lastActivity`, sem refresh token, sem endpoint de logout.
- **Melhoria:** claim `lastActivity`/`iat` + timeout de inatividade configurável (ex. 30 min); refresh token rotativo; lista de revogação (`jti`) em BD/cache; endpoint `POST /auth/logout`.
- **Aceite:** inatividade > timeout → 401; logout invalida o refresh.
- **Esforço:** 🟡 Médio.

#### A5. Edição de email no perfil (RF05) — ⚠️ Parcial
- **Estado:** `UpdateMeRequest` só aceita `name`/`surname`; email é read‑only na UI.
- **Melhoria:** permitir alteração de email com re‑verificação por OTP (reutilizar `PasswordResetService`); garantir unicidade.
- **Esforço:** 🟢 Baixo.

#### A6. Exclusão de conta com limpeza real (RF06) — ⚠️ Órfãos
- **Estado:** `UserController.deleteMe` (l.80) faz apenas `repository.delete(user)`. Existem cascades JPA para ficheiros/pastas/nós/shares, **mas**:
  - não remove os `.dbs` físicos dos nós de armazenamento;
  - não invoca `PurgeService` (não limpa versões locais nem links públicos do ficheiro);
  - `PublicLink.owner` é `@ManyToOne(nullable=false)` **sem cascade** e `User` não tem coleção de links → **pode violar FK e falhar**.
- **Melhoria:** orquestrar exclusão: para cada ficheiro do utilizador → `PurgeService.purgeFile` (revoga links, apaga réplicas e versões); apagar links públicos e nós (com limpeza de disco); só depois apagar o `User`. Envolver em transação única com melhor‑esforço registado.
- **Aceite:** apagar conta com ficheiros versionados + links + dispositivos não deixa linhas, ficheiros nem links órfãos.
- **Esforço:** 🟢 Baixo.

---

### B. Ficheiros e pastas

#### B1. Quota furada em operações alternativas — ⚠️ Crítico
- **Estado:** a quota só é validada no upload (`FileController` l.65‑70). **Não** é validada em:
  - `PUT /files/{id}/content` → `FileService.updateContent` (l.108‑128);
  - `POST /files/{id}/copy-to-space` → `FileService.copyToSpace` (l.224‑244).
- **Melhoria:** extrair `assertQuota(user, deltaBytes)` e chamar em `updateContent` (delta = novo − antigo) e `copyToSpace` (delta = tamanho da cópia). Unificar a contabilização (`originalSizeBytes` na cota vs `storedSizeBytes` no dashboard).
- **Aceite:** substituir/copiar conteúdo que exceda a cota → 400 com mensagem clara; nunca excede a quota.
- **Esforço:** 🟢 Baixo.

#### B2. Alerta de quota com 2 níveis e anti‑spam (RF26) — ⚠️ Parcial
- **Estado:** `FileController` l.86‑95 notifica só aos 80%, só no upload, e pode repetir.
- **Melhoria:** 80% (`QUOTA_WARNING`) e 95% (`QUOTA_CRITICAL`); guardar limiar já notificado (flags no `User` ou tipo+período); verificar também em `updateContent`/`copyToSpace`.
- **Aceite:** cada limiar notifica uma vez por ciclo; notificação ≤1 por upload.
- **Esforço:** 🟢 Baixo.

#### B3. Comportamento offline CACHE (RF24) — ⚠️ Guardado, não usado
- **Estado:** `offlineBehavior` é persistido (`User`, `StorageController` l.109‑126) mas **nenhum código o lê**. `FileService.readCurrentContainer` (l.349‑362) lança sempre `DeviceOfflineException` quando não há réplica online.
- **Melhoria:**
  1. Ao ler com sucesso, garantir/atualizar uma cópia no nó de sistema (cache).
  2. Em `readCurrentContainer`, se `offlineBehavior == CACHE` e o nó do ficheiro está offline → tentar a réplica do nó de sistema; devolver com `X-Cache: HIT`.
  3. Se `DENY` → manter o comportamento atual. Se não houver cache → `DeviceOfflineException` com mensagem explícita.
- **Aceite:** com CACHE e nó offline mas réplica no sistema → download funciona; com DENY → erro claro.
- **Esforço:** 🟡 Médio.

#### B4. Validação de tipos de ficheiro no upload — ❌ Ausente
- **Estado:** `ContainerService.prepare` aceita qualquer `MultipartFile`/`mimeType`.
- **Melhoria:** lista configurável de MIME permitidos (`cloud.upload.allowed-mime-types`) + validação por *magic bytes* (não confiar no cliente); bloquear executáveis (`exe/bat/sh/js/html`) e denylist opcional; mensagem 400 clara.
- **Aceite:** tipo proibido → 400; ficheiro legítimo reconhecido pelo conteúdo, não pela extensão.
- **Esforço:** 🟢 Baixo.

#### B5. Busca: ordenação, paginação e filtros na UI (RF11) — ⚠️ Parcial
- **Estado:** backend suporta `search/mime/from/to` (`FileMetadataRepository.search`); a UI só busca por nome (via `?q=`), **sem ordenação nem paginação**.
- **Melhoria:** adicionar `sort`/`order` e `page`/`size` no repositório e endpoint; UI com seletor de ordenação (nome/data/tamanho), filtro por tipo/data e paginação (ou scroll infinito).
- **Aceite:** ordenar por qualquer coluna e paginar sem carregar toda a pasta.
- **Esforço:** 🟡 Médio.

#### B6. Preview de ficheiros — ❌ Ausente
- **Estado:** apenas ícones por tipo; download direto. Sem visualizador (imagem/PDF/texto/vídeo/áudio).
- **Melhoria:** modal de preview com `responseType: blob` + `URL.createObjectURL`; tipos suportados: imagens, PDF (embed), texto/código, áudio/vídeo (tag nativa). Fallback para download.
- **Aceite:** clicar num ficheiro abre preview sem descarregar para disco.
- **Esforço:** 🟡 Médio.

#### B7. Upload: progresso por bytes e retoma — ⚠️ Parcial
- **Estado:** `uploadFile` aceita `onUploadProgress`, mas `MyFiles.jsx` não o liga (progresso só por contagem `done/total`). Sem upload resumível no HTTP.
- **Melhoria:** ligar `onUploadProgress` (percentagem por ficheiro); considerar upload *chunked*/resumível (tus) para ficheiros grandes; limite de tamanho configurável.
- **Aceite:** barra mostra % real; falha de rede pode retomar.
- **Esforço:** 🟡 Médio (progresso) / 🔴 Alto (resumível).

#### B8. "Esvaziar lixeira" e confirmação de purge — ⚠️ UX
- **Estado:** `Trash.jsx` só faz purge item a item e **sem diálogo de confirmação**.
- **Melhoria:** endpoint `DELETE /files/trash` e `DELETE /folders/trash` (esvaziar) + modal de confirmação no purge definitivo.
- **Aceite:** esvaziar lixeira remove tudo com confirmação; ação irreversível confirmada.
- **Esforço:** 🟢 Baixo.

#### B9. Compressão: alinhar doc/código (RF09/RF10)
- **Estado:** `ContainerService` implementa DBS2 com compressão (NONE/DEFLATE/GZIP) e `POST /files/{id}/compress|decompress` existem. O `PLANO_COMPLETUDE.md` afirma que a compressão foi removida.
- **Melhoria:** decidir e documentar — manter (então atualizar o plano e adicionar testes) ou remover (então retirar endpoints/UI). Verificar também a coerência `cloud.container.magic=DBS1` vs `MAGIC_V2=DBS2`.
- **Esforço:** 🟢 Baixo.

---

### C. Partilha e links públicos

#### C1. Permissões além de VIEW/EDIT — ⚠️ Melhoria
- **Estado:** apenas `VIEW`/`EDIT`; `AdminPermissions.jsx` é **só leitura** (não edita a matriz).
- **Melhoria:** avaliar papel adicional (ex. comentar/upload em pasta partilhada); tornar a matriz de permissões efetivamente editável, com persistência e auditoria.
- **Esforço:** 🟡 Médio.

#### C2. Links públicos: limites e gestão — ⚠️ Melhoria
- **Estado:** link opaco com senha/expiração/contador de acessos; sem limite de downloads, sem log de acessos por IP, sem "lista de links por ficheiro" na UI.
- **Melhoria:** limite opcional de downloads; registo de acessos (data/IP) para auditoria; visão central de todos os links do utilizador.
- **Esforço:** 🟢 Baixo.

#### C3. `vercel.json` engole `/s/:token`
- **Estado:** o rewrite SPA `→ /index.html` pode servir a app em vez do tratamento do link público, conforme o routing.
- **Melhoria:** garantir rewrite que preserve `/s/*` (o `BrowserRouter` resolve, mas confirmar em produção) e testar link público no domínio Vercel.
- **Esforço:** 🟢 Baixo.

---

### D. Nós de armazenamento e sincronização

#### D1. Fator de réplica = 1 / sem redundância (RF27, RNF03) — ⚠️ Alto
- **Estado:** `ReplicationService.resolveTargets` (l.109‑116) devolve **sempre uma única** lista de 1 nó; `handleNodeAck` (l.266‑274) nunca é chamado.
- **Melhoria:** fator de replicação configurável (ex. 3): distribuir réplicas por múltiplos nós (sistema + dispositivos disponíveis), estados PENDING→SYNCED/FAILED reais, reconciliação de nós que voltam online, e *repair* de contagem mínima. Definir quórum mínimo para leitura.
- **Aceite:** nó que cai não torna o ficheiro indisponível; contagem de réplicas convergente.
- **Esforço:** 🔴 Alto.

#### D2. Validação de conectividade no registo (RF17.8) — ⚠️ Parcial
- **Estado:** `NodeManagementService.validateRequirements` valida SO e espaço livre, mas não testa conectividade real com o agente.
- **Melhoria:** após registo, *probe* imediato ao `NodeHttpServer` (health) e só marcar ONLINE se responder; registar `agentVersion`.
- **Esforço:** 🟢 Baixo.

#### D3. Reconciliação de espaço em nós — ⚠️ Melhoria
- **Estado:** `refreshUsedSpace` só incrementa em novas réplicas; em `deleteReplicasExcept` só o nó de sistema recalcula (`refreshSystemUsage`); dispositivos não reconciliam ao apagar.
- **Melhoria:** reconciliar `usedSpaceBytes` dos dispositivos a partir do agente (endpoint de stats) em cada heartbeat/sync.
- **Esforço:** 🟡 Médio.

#### D4. Sync assíncrono/tempo real (RF28, RF30) — ⚠️ Parcial
- **Estado:** replicação é síncrona; sem WebSocket para progresso de sync; UI faz polling.
- **Melhoria:** publicar eventos de sync (STOMP) e/ou reconciliar em *background* (fila) para não bloquear o request HTTP.
- **Esforço:** 🔴 Alto (com D1).

---

### E. Quotas

| Item | Estado | Melhoria |
|---|---|---|
| Cota por utilizador (RF25) | ✅ | Manter; garantir validação em **todas** as escritas (ver B1) |
| Cota por dispositivo/nó | ✅ | Manter; reconciliar uso (D3) |
| Contabilização consistente | ⚠️ | Unificar `originalSizeBytes` (cota) × `storedSizeBytes` (dashboard) |
| Aviso de limite (RF26) | ⚠️ | Dois limiares + anti‑spam (ver B2) |

---

### F. Notificações e tempo real

#### F1. WebSocket real (RF30) — ❌ Desativado
- **Estado:** `WebSocketConfig` é um *placeholder* comentado ("WebSocket disabled"); `pom.xml` sem `spring-boot-starter-websocket`; UI faz polling de 60s do contador de não lidas.
- **Melhoria:** ativar STOMP (`/ws-nodes`), tópicos `/topic/nodes/{userId}` e `/topic/files/{userId}`; publicar em heartbeat/replicação; `@stomp/stompjs` + fallback para polling.
- **Aceite:** badge e estado de nós atualizam sem refresh.
- **Esforço:** 🟡 Médio.

#### F2. Produtores de notificação incompletos — ⚠️ Parcial
- **Estado:** `NODE_ONLINE` definido e nunca emitido; sem notificação de `DEGRADED`; heartbeat só notifica OFFLINE.
- **Melhoria:** emitir ONLINE ao transitar OFFLINE→ONLINE; notificar degradação; `QUOTA_CRITICAL`.
- **Esforço:** 🟢 Baixo.

#### F3. Preferências de notificação — ❌ Ausente
- **Estado:** `Settings.jsx` diz "A gestão detalhada estará disponível em breve."
- **Melhoria:** opt‑in por tipo (partilhas, sync, quota, dispositivos) e canal (in‑app/email); persistir por utilizador.
- **Esforço:** 🟡 Médio.

#### F4. Badge no sidebar — ✅ (verificar `conditional`)
- **Estado:** `DashboardLayout` injeta badge real no contador; **mas** `filterMenuByPermissions` só trata `visibility === "secret"`, ignorando `"conditional"` (item Lixeira aparece sempre).
- **Melhoria:** corrigir a lógica para tratar `"conditional"` (esconder/desabilitar com tooltip quando sem permissão).
- **Esforço:** 🟢 Baixo.

---

### G. Administração, auditoria e dashboard

#### G1. Audit log (RF29) — ❌ Ausente
- **Estado:** sem `AuditLog`; mutações de admin não são registadas.
- **Melhoria:** entidade `AuditLog` (ator, ação, alvo, timestamp, IP, antes/depois); `@Aspect` ou chamadas explícitas em createUser/changeRole/delete/suspend/setQuota/setNodeQuota/createShare/delete; endpoint `GET /admin/audit` (ADMIN) com filtros e paginação.
- **Aceite:** cada ação sensível gera registo imutável e consultável.
- **Esforço:** 🟡 Médio.

#### G2. Matriz de permissões editável — ⚠️ Melhoria
- **Estado:** `PermissionService` tem matriz estática; `AdminPermissions.jsx` só visualiza; endpoints não usam `hasPermission` (só role).
- **Melhoria:** persistir a matriz (BD), tornar a UI editável, e aplicar `@PreAuthorize`/checks de permissão fina nos endpoints (hoje é só `ROLE_ADMIN`/`ROLE_USER`).
- **Esforço:** 🟡 Médio.

#### G3. Dashboard: séries temporais reais — ⚠️ Stub
- **Estado:** `DashboardService.buildUsageOverTime` repete os mesmos totais em "hoje/semana/mês".
- **Melhoria:** agregações reais por período (guardar snapshots diários ou usar `uploadedAt`).
- **Esforço:** 🟡 Médio.

#### G4. Observabilidade — ❌ Desativada
- **Estado:** `management.endpoints.enabled=false`; só `/api/health`. `ddl-auto=update` e `show-sql=true` em qualquer ambiente.
- **Melhoria:** ativar Actuator (`/actuator/health` com indicators de BD/nó de sistema, `/actuator/metrics`); criar `application-prod.properties` com `ddl-auto=validate`, `show-sql=false`, CORS restritivo; separar logs e adicionar correlação de pedidos.
- **Esforço:** 🟢 Baixo.

---

### H. Infraestrutura, testes e qualidade

#### H1. Testes automatizados — ❌ Crítico
- **Estado:** só `CloudsystemApplicationTests.contextLoads()`. Sem testes unitários/integração/frontend. O CI (`./mvnw verify`) tende a **falhar** porque `CLOUD_MASTER_KEY`/`JWT_SECRET` são validados no arranque e não há perfil de teste.
- **Melhoria:**
  1. `application-test.properties` com chaves de teste válidas + H2/Testcontainers.
  2. Testes de serviço (cifra, ficheiro, pasta, partilha, replicação, OTP, links, nós).
  3. Testes de integração (auth, ficheiro, pasta, share, link, nó, admin).
  4. Frontend: Vitest + Testing Library (login, MyFiles, sidebarConfig, RequireAuth).
- **Aceite:** `mvn test` e `npm test` verdes no CI; cobertura de serviços ≥ 80%.
- **Esforço:** 🔴 Alto.

#### H2. CI/CD — ⚠️ Parcial
- **Estado:** `.github/workflows/ci.yml` corre backend (`mvnw verify`) e frontend (`npm ci && lint && build`).
- **Melhoria:** adicionar testes frontend, *build* de Docker, verificação de formatação e (opcional) deploy; cache de dependências.
- **Esforço:** 🟢 Baixo.

#### H3. Configuração/perfis — ⚠️
- **Estado:** `NodeAgentApplication` aponta para `application-node.properties` **inexistente**; várias chaves `cloud.node.*` só têm defaults em código.
- **Melhoria:** criar `application-node.properties` e `application-prod.properties`; documentar todas as chaves.
- **Esforço:** 🟢 Baixo.

#### H4. CORS e segurança de produção — ⚠️
- **Estado:** origens fixas em `SecurityConfig`; a origem Vercel tem **trailing slash** (`"https://debesis-tau.vercel.app/"`) que tende a não corresponder ao `Origin` enviado pelo browser.
- **Melhoria:** remover a barra final; tornar as origens configuráveis por env; restringir `allowedHeaders`/métodos em produção.
- **Esforço:** 🟢 Baixo.

#### H5. RNF02/RNF06 — produto
- **Melhoria:** apps móveis (ou PWA — ver I5) para Android/iOS; exportação de dados do utilizador ("baixar os meus dados"), política de retenção e registo de consentimento para LGPD/GDPR.
- **Esforço:** 🔴 Alto.

---

### I. Frontend / UX

| # | Item | Estado | Melhoria | Esforço |
|---|---|---|---|---|
| I1 | Interceptor de resposta (401/refresh/logout) | ❌ | `axios.js` só adiciona `Bearer`; falta tratar 401 → renovar/deslogar | 🟢 |
| I2 | Ordenação/paginação na UI | ❌ | Ver B5 | 🟡 |
| I3 | Preview de ficheiros | ❌ | Ver B6 | 🟡 |
| I4 | Placeholders | ⚠️ | Perfil→aba Armazenamento real; Configurações→notificações; Suporte com formulário/tickets; `NotFound.jsx` com link | 🟢 |
| I5 | Dark mode + i18n + PWA/offline | ❌ | Tema (já há tokens em `exemplo.css`), i18n (strings PT hardcoded), PWA/service worker | 🔴 |
| I6 | BottomNav reativo + `adminOnly` | ⚠️ | `isMobile` calculado uma vez (não reage a resize); flag `adminOnly` não aplicada | 🟢 |
| I7 | Limpeza de código morto | ⚠️ | Remover `Home.jsx`, `exemplo.jsx/css`, `Placeholder.jsx`, `dropdown.jsx`, `LandingPage/Navbar.jsx`, `components/header/Navbar.jsx`, ícone `react-icon` | 🟢 |
| I8 | Progresso real de upload | ⚠️ | Ligar `onUploadProgress` (ver B7) | 🟡 |
| I9 | Acessibilidade | ✅ | Já extensa (`aria-*`, foco, teclado) — manter e cobrir com testes | — |

---

## 5. Backlog priorizado

| Prio | ID | Melhoria | RF/RNF | Esforço |
|---|---|---|---|---|
| P0 | H1 | Testes automatizados + perfil de teste (destrava o CI) | — | 🔴 |
| P0 | A2 | Rate limiting + bloqueio de login | RNF01 | 🟡 |
| P0 | A3 | Suspensão efetiva a partir da BD | RNF01 | 🟢 |
| P0 | A6 | Exclusão de conta com purga real | RF06 | 🟢 |
| P0 | B1 | Fechar fugas de quota (`updateContent`, `copyToSpace`) | RF25 | 🟢 |
| P0 | B4 | Validação de tipos de ficheiro no upload | RNF01 | 🟢 |
| P0 | H4 | Corrigir CORS (trailing slash) e tornar configurável | RNF01 | 🟢 |
| P1 | A1 | 2FA / TOTP | RF04 | 🟡 |
| P1 | A4 | Sessão: inatividade + refresh + logout | RF02 | 🟡 |
| P1 | B3 | Comportamento offline CACHE real | RF24 | 🟡 |
| P1 | B2 | Alerta de quota 80%/95% com anti‑spam | RF26 | 🟢 |
| P1 | G1 | Audit log de ações | RF29 | 🟡 |
| P1 | F1 | WebSocket/STOMP para notificações e sync | RF30/RF28 | 🟡 |
| P1 | F2/F4 | Notificações ONLINE/DEGRADED + corrigir `conditional` no sidebar | RF30 | 🟢 |
| P1 | G4/H3 | Actuator + perfis `prod`/`node` | RNF03/RNF04 | 🟢 |
| P2 | B5/I2 | Busca: ordenação e paginação (backend + UI) | RF11 | 🟡 |
| P2 | B6/I3 | Preview de ficheiros | — | 🟡 |
| P2 | D1/D4 | Replicação com fator >1 / quórum / sync assíncrono | RF27/RNF03 | 🔴 |
| P2 | G2 | Matriz de permissões editável + permissões finas | RF15 | 🟡 |
| P2 | A5 | Email editável no perfil (com OTP) | RF05 | 🟢 |
| P2 | B8 | Esvaziar lixeira + confirmação de purge | RF12 | 🟢 |
| P2 | I4 | Substituir placeholders (Perfil/Configurações/Suporte/NotFound) | — | 🟢 |
| P2 | D2/D3 | Probe de conectividade no registo + reconciliação de espaço | RF17.8/RF23 | 🟢 |
| P3 | F3 | Preferências de notificação | RF30 | 🟡 |
| P3 | G3 | Séries temporais no dashboard | — | 🟡 |
| P3 | B9 | Alinhar compressão doc/código | RF09/RF10 | 🟢 |
| P3 | I1/I6/I7 | Interceptor 401, BottomNav reativo, limpeza de código morto | — | 🟢 |
| P3 | H5/I5 | PWA/apps móveis + LGPD/GDPR + i18n + dark mode | RNF02/RNF06 | 🔴 |

---

## 6. Roadmap sugerido (por fases)

- **Fase 0 — Fundação (P0):** testes + perfil de teste → destrava o CI; corrigir CORS.
- **Fase 1 — Segurança (P0/P1):** rate limiting/bloqueio, suspensão a partir da BD, 2FA, sessão/refresh/logout, validação de tipos.
- **Fase 2 — Integridade (P0/P1):** exclusão de conta com purga, fecho de fugas de quota, alerta 80/95%.
- **Fase 3 — Completude (P1/P2):** offline CACHE, audit log, WebSocket, notificações em falta, Actuator/perfis.
- **Fase 4 — Experiência (P2):** busca completa, preview, lixeira, placeholders, permissões editáveis.
- **Fase 5 — Escala/produto (P2/P3):** replicação > 1, sync assíncrono, PWA/móvel, LGPD/GDPR, i18n, dark mode.

---

## 7. Riscos e divergências a corrigir (checklist curto)

- [ ] `SecurityFilter` promete re‑verificação de suspensão que **não** acontece → A3.
- [ ] `PLANO_COMPLETUDE.md` diz que a compressão foi removida, mas ela está **implementada** → B9.
- [ ] `application.properties` com `ddl-auto=update` e `show-sql=true` → G4.
- [ ] `cloud.container.magic=DBS1` vs `MAGIC_V2=DBS2` → confirmar intenção (DBS1 = legado) → B9.
- [ ] `NodeAgentApplication` referencia `application-node.properties` inexistente → H3.
- [ ] CORS Vercel com trailing slash → H4.
- [ ] `PublicLink.owner` sem cascade pode partir a exclusão de conta → A6.
- [ ] Numeração RF nos comentários (RF33/RF35) não bate com o PDF (RF01–RF30) → alinhar docs.

---

## 8. Anexo — ficheiros de referência

**Backend (`cloudsystem/src/main/java/com/assemblyproject/cloudsystem/`)**
`configuration/{SecurityFilter,SecurityConfig,TokenConfig,WebSocketConfig}.java` · `controller/{Auth,File,Folder,Share,PublicLink,PublicAccess,Node,Storage,User,AdminUser,AdminPermissions,Dashboard,Notification}Controller.java` · `service/{FileService,FolderService,ReplicationService,NodeManagementService,StorageNodeService,SystemNodeService,HeartbeatMonitorService,ContainerService,PublicLinkService,PasswordResetService,NotificationService,PurgeService,TrashCleanupService}.java` · `entity/{User,PublicLink,NotificationType,FileMetadata}.java` · `resources/application.properties`

**Frontend (`frontend/src/`)**
`api/axios.js` · `context/AuthProvider.jsx` · `components/header/{sidebarConfig.js,sidebar.jsx}` · `pages/dashboard/{MyFiles,Storage,Settings,Profile,Support,Trash,Notifications,BottomNav,DashboardLayout}.jsx` · `pages/{Login,Register,ForgotPassword,PublicAccess,NotFound}.jsx`

**Projeto:** `docs/PLANO_COMPLETUDE.md` · `docs/COMOUSAR.md` · `.github/workflows/ci.yml` · `frontend/package.json` · `cloudsystem/pom.xml`
