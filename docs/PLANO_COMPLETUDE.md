# Plano de Completude — Sistema Arquivo Nuvem

> **Objetivo:** levar o sistema de ~75-80% para **100% dos requisitos funcionais (RF01-RF35)** + **pronto para produção**.
>
> **Data:** 2026-10-12
> **Estado atual:** MVP funcional — backend e frontend implementados, sem testes automatizados, sem 2FA, sem cache offline real.
>
> **Decisão de 2026-10-12:** o sistema **não usa compressão**. Os dados são cifrados com AES-GCM e armazenados diretamente no container `.dbs`. RF09/RF10 ficam fora de âmbito.

---

## Índice

1. [Fase 0 — Fundação: Testes Automatizados](#fase-0--fundação-testes-automatizados)
2. [Fase 1 — Segurança de Acesso](#fase-1--segurança-de-acesso)
3. [Fase 2 — Completude Funcional (RF em falta)](#fase-2--completude-funcional-rf-em-falta)
4. [Fase 3 — Notificações e Tempo Real](#fase-3--notificações-e-tempo-real)
5. [Fase 4 — Infraestrutura de Produção](#fase-4--infraestrutura-de-produção)
6. [Fase 5 — Validação Final](#fase-5--validação-final)

---

## Fase 0 — Fundação: Testes Automatizados

> **Porquê primeiro:** sem testes, todas as fases seguintes são refactor cego. É a fundação que permite evoluir sem medo.

### 0.1 Backend — testes unitários (JUnit 5 + Mockito)

**Ficheiros a criar em `cloudsystem/src/test/java/...`:**

| Teste | O que valida |
|---|---|
| `ContainerServiceTest` | Cifra/descifra AES-GCM, magic `DBS1`, checksum SHA-256, wrap/unwrap de chave |
| `FileServiceTest` | Upload, rename, move, softDelete/restore, versioning (snapshot + restore), moveToSpace/copyToSpace |
| `FolderServiceTest` | CRUD, anti-ciclo (mover pasta para dentro de si), lixeira recursiva, nomes duplicados |
| `ShareServiceTest` | createOrUpdate, herança por pasta, revogação, permissão VIEW/EDIT |
| `ReplicationServiceTest` | Seleção de nós com espaço, fator 3, PENDING→SYNCED/FAILED, deleteReplicas |
| `PasswordResetServiceTest` | Geração OTP, expiração 15 min, 5 tentativas, reset token uso único |
| `PublicLinkServiceTest` | Criação, expiração, senha, revogação, download anónimo |
| `NodeManagementServiceTest` | Registo com pairing code, validação de requisitos, enable/disable, remove com migração |
| `NotificationServiceTest` | Criação, marcar lida, contagem de não lidas |
| `TrashCleanupServiceTest` | Purge automático após retenção |

**Meta:** ≥ 80% de cobertura nos serviços.

### 0.2 Backend — testes de integração (`@SpringBootTest` + `TestRestTemplate`)

| Teste | O que valida |
|---|---|
| `AuthControllerIT` | Registo → login → acesso a `/users/me` → logout |
| `FileControllerIT` | Upload → listar → download → rename → move → delete → restore |
| `FolderControllerIT` | Criar → listar → renomear → mover → apagar → restaurar |
| `ShareControllerIT` | Partilhar → aceder como recipiente → revogar |
| `PublicLinkIT` | Criar link → aceder sem auth → revogar |
| `NodeControllerIT` | Pairing code → registar → heartbeat → listar → remover |
| `AdminUserControllerIT` | CRUD admin, guardas do último ADMIN |

**Nota:** usar `@AutoConfigureMockMvc` ou `TestRestTemplate` com perfil `test` (H2 ou Testcontainers com PostgreSQL).

### 0.3 Frontend — testes (Vitest + Testing Library)

**Dependências a adicionar:** `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `jsdom`.

| Teste | O que valida |
|---|---|
| `Login.test.jsx` | Render, submissão, erro, redirect |
| `MyFiles.test.jsx` | Listar, upload, filtro, partilhar |
| `Storage.test.jsx` | Resumo, política, quota |
| `sidebarConfig.test.js` | `filterMenuByPermissions` (itens secret/conditional) |
| `RequireAuth.test.jsx` | Redirect sem token |

### 0.4 Testes E2E (opcional, Cypress ou Playwright)

- Fluxo completo: registar → login → upload → partilhar → aceder como outro utilizador → download.

---

## Fase 1 — Segurança de Acesso

### 1.1 Bloqueio de login após 5 tentativas (RF do login)

**Problema:** hoje não há rate-limiting nem bloqueio por tentativas.

**Solução:**
- Nova entidade `LoginAttempt` (email, ip, timestamp, success) ou usar cache (`ConcurrentHashMap` com TTL).
- No `AuthenticationManager` customizado ou num `AuthenticationFailureHandler`:
  - Registar falha.
  - Se ≥ 5 falhas em 15 min → lançar `LockedException` com mensagem genérica.
- No sucesso, limpar tentativas.
- Mensagem genérica: "Credenciais inválidas ou conta temporariamente bloqueada."

**Ficheiros:**
- `entity/LoginAttempt.java` (ou `service/LoginAttemptService.java`)
- `configuration/CustomAuthenticationFailureHandler.java`
- `application.properties`: `cloud.security.max-attempts=5`, `cloud.security.lockout-minutes=15`

### 1.2 2FA no login (RF04)

**Problema:** hoje só há OTP na recuperação de senha.

**Solução (TOTP — Google Authenticator):**
- Nova entidade `User.totpSecret` (String, nullable).
- Endpoint `POST /auth/2fa/setup` → devolve secret + QR code (ou URL otpauth://).
- Endpoint `POST /auth/2fa/verify` → valida código de 6 dígitos.
- No login: se `totpSecret != null`, exigir campo `totpCode` no `LoginRequest`.
- Biblioteca: `com.warrenstrange:googleauth` ou implementar TOTP manualmente (HMAC-SHA1, 30s window).

**Ficheiros:**
- `entity/User.java` — campo `totpSecret`
- `dto/request/LoginRequest.java` — campo opcional `totpCode`
- `service/TotpService.java` — gerar secret, validar código, gerar QR
- `controller/AuthController.java` — endpoints `/auth/2fa/setup`, `/auth/2fa/verify`
- `frontend/src/pages/Login.jsx` — campo extra quando 2FA ativo
- `frontend/src/pages/dashboard/Settings.jsx` — ativar/desativar 2FA

### 1.3 Expiração de sessão por inatividade

**Problema:** o JWT dura 24h sem inatividade.

**Solução:**
- Adicionar claim `lastActivity` ao JWT (ou usar Redis com TTL).
- Filtro que renova `lastActivity` a cada request.
- Se inatividade > 30 min → 401.
- Config: `cloud.security.inactivity-timeout-minutes=30`.

**Ficheiros:**
- `configuration/TokenConfig.java` — claim `lastActivity`
- `configuration/SecurityFilter.java` — validar inatividade
- `application.properties` — timeout

### 1.4 Rate limiting global

- Adicionar `Bucket4j` ou filtro simples com `ConcurrentHashMap<String, RateLimiter>`.
- Limites: 100 req/min por IP para API geral, 10 req/min para `/auth/login`.

---

## Fase 2 — Completude Funcional (RF em falta)

### 2.1 Alerta de quota próxima (RF32)

**Problema:** não há notificação quando o utilizador se aproxima do limite.

**Solução:**
- No `FileService.uploadFile` e `updateContent`, após gravar:
  - Calcular `used / quota`.
  - Se ≥ 80% → `NotificationService.notify(user, QUOTA_WARNING, ...)`.
  - Se ≥ 95% → `NotificationService.notify(user, QUOTA_CRITICAL, ...)`.
- Evitar spam: só notificar uma vez por limiar (guardar flag no `User` ou na notificação).

**Ficheiros:**
- `service/FileService.java` — verificar quota após upload
- `entity/NotificationType.java` — novos valores `QUOTA_WARNING`, `QUOTA_CRITICAL`
- `service/NotificationService.java` — já existe, só chamar

### 2.2 Comportamento offline CACHE real (RF30)

**Problema:** hoje `offlineBehavior` é guardado mas não há cache. O `DeviceOfflineException` é lançado sem fallback.

**Solução:**
- Quando um ficheiro é lido com sucesso, guardar cópia no **nó de sistema** (cache).
- Se `offlineBehavior == CACHE` e o nó do ficheiro está offline:
  - Tentar ler do nó de sistema (cache).
  - Se existir → devolver com header `X-Cache: HIT`.
- Se `DENY` → manter comportamento atual (`DeviceOfflineException`).

**Ficheiros:**
- `service/FileService.java` — `readCurrentContainer` com fallback para cache
- `service/SystemNodeService.java` — garantir que o nó de sistema existe
- `service/ReplicationService.java` — ao ler com sucesso, replicar para o nó de sistema se ainda não estiver lá
- `frontend/src/pages/dashboard/Settings.jsx` — UI para escolher DENY/CACHE

### 2.3 Validação de tipos de ficheiro no upload

**Problema:** hoje aceita qualquer `mimeType`.

**Solução:**
- Lista de tipos permitidos (configurável): imagens, documentos, vídeo, áudio, código, outros.
- Rejeitar tipos perigosos: `.exe`, `.bat`, `.sh`, `.js`, `.html` (ou validar por magic bytes).
- Config: `cloud.upload.allowed-mime-types=...`

**Ficheiros:**
- `service/ContainerService.java` — validar `mimeType` no `prepare`
- `application.properties` — lista de tipos permitidos

### 2.4 Auditoria de ações admin

**Problema:** não há log de quem mudou o quê.

**Solução:**
- Nova entidade `AuditLog` (adminId, ação, target, timestamp, IP).
- Interceptor ou `@Aspect` que regista: createUser, changeRole, deleteUser, suspend, setQuota, setNodeQuota.
- Endpoint `GET /admin/audit` (só ADMIN).

**Ficheiros:**
- `entity/AuditLog.java`
- `repository/AuditLogRepository.java`
- `service/AuditService.java`
- `controller/AdminAuditController.java`
- `aspect/AuditAspect.java` (ou chamar manualmente nos controllers)

---

## Fase 3 — Notificações e Tempo Real

### 3.1 Notificações de heartbeat/replicação (RF35)

**Problema:** hoje só `ShareService` gera notificações. Heartbeat e replicação não notificam.

**Solução:**
- No `HeartbeatMonitorService.checkAllNodes`:
  - Se um nó transita ONLINE → OFFLINE → notificar o dono.
  - Se OFFLINE → ONLINE → notificar o dono.
- No `ReplicationService.replicateFile`:
  - Se uma réplica falha → notificar o dono do ficheiro.
  - Se todas as réplicas falham → notificar com urgência.

**Ficheiros:**
- `service/HeartbeatMonitorService.java` — chamar `NotificationService`
- `service/ReplicationService.java` — chamar `NotificationService`
- `entity/NotificationType.java` — `NODE_OFFLINE`, `NODE_ONLINE`, `REPLICA_FAILED`

### 3.2 Sync status via WebSocket (RF33/RF34)

**Problema:** o backend tem STOMP configurado (`WebSocketConfig`) mas o frontend não usa.

**Solução:**
- Endpoint STOMP `/ws-nodes` já existe.
- Tópicos:
  - `/topic/nodes/{userId}` — atualizações de estado dos nós.
  - `/topic/files/{userId}` — atualizações de sync de ficheiros.
- No `HeartbeatMonitorService` e `ReplicationService`, publicar mensagens nos tópicos.
- Frontend: usar `@stomp/stompjs` + `sockjs-client` para subscrever e atualizar a UI em tempo real.

**Ficheiros:**
- `configuration/WebSocketConfig.java` — já existe, pode precisar de ajustes
- `service/HeartbeatMonitorService.java` — publicar no tópico
- `service/ReplicationService.java` — publicar no tópico
- `frontend/package.json` — adicionar `@stomp/stompjs`, `sockjs-client`
- `frontend/src/api/websocket.js` — cliente STOMP
- `frontend/src/pages/dashboard/Storage.jsx` — subscrever e atualizar UI
- `frontend/src/pages/dashboard/Notifications.jsx` — badge real com contagem

### 3.3 Badge de notificações no sidebar

**Problema:** `badge` está `null` em todos os nós do `sidebarConfig.js`.

**Solução:**
- No `DashboardLayout`, já busca `getUnreadNotificationCount`.
- Passar o valor para a sidebar e mostrar badge no item "Notificações".

**Ficheiros:**
- `frontend/src/components/header/sidebarConfig.js` — item notificações com `badge: "count"`
- `frontend/src/components/header/sidebar.jsx` — renderizar badge
- `frontend/src/pages/dashboard/DashboardLayout.jsx` — passar `unreadCount` para a sidebar

---

## Fase 4 — Infraestrutura de Produção

### 4.1 Docker + Docker Compose

**Ficheiros a criar:**
- `cloudsystem/Dockerfile` — multi-stage build (Maven → JRE 17).
- `frontend/Dockerfile` — Node 20 → nginx.
- `docker-compose.yml` — serviços: `app`, `db` (PostgreSQL 14), `mail` (MailHog), `frontend`.
- `.dockerignore` para ambos.

### 4.2 Configuração de produção

**Ficheiros:**
- `cloudsystem/src/main/resources/application-prod.properties`:
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.jpa.show-sql=false`
  - CORS restritivo (só domínio de produção)
  - `app.mail.enabled=true`
- `frontend/.env.production` — `VITE_API_URL` para domínio real.

### 4.3 Health checks / actuator

- Adicionar `spring-boot-starter-actuator`.
- Endpoints `/actuator/health`, `/actuator/metrics`.
- Custom health indicator para o nó de sistema e para a base de dados.

### 4.4 Backup da base de dados

- Script `backup.sh` com `pg_dump` diário.
- Ou usar `docker-compose` com volume + cron.

### 4.5 CI/CD

**Ficheiro:** `.github/workflows/ci.yml`
- Lint (ESLint + Checkstyle).
- Testes (Maven + Vitest).
- Build (Docker).
- Deploy (opcional: SSH para servidor).

### 4.6 HTTPS / reverse proxy

- `nginx` ou `traefik` com Let's Encrypt.
- Configurar `VITE_API_URL` para `https://api.arquivonuvem.com`.

---

## Fase 5 — Validação Final

### 5.1 Checklist de aceitação

- [ ] Todos os testes passam (`mvn test`, `npm test`).
- [ ] Cobertura de testes ≥ 80% no backend.
- [ ] RF01-RF35 todos implementados e testados (exceto RF09/RF10 — decisão de produto).
- [ ] 2FA funciona (setup + login + desativar).
- [ ] Bloqueio de login após 5 tentativas.
- [ ] Alerta de quota a 80% e 95%.
- [ ] Cache offline funciona (CACHE).
- [ ] Notificações de heartbeat/replicação.
- [ ] WebSocket atualiza UI em tempo real.
- [ ] Badge de notificações no sidebar.
- [ ] Docker Compose sobe todos os serviços.
- [ ] Health checks respondem.
- [ ] CI/CD passa.

### 5.2 Testes manuais (E2E)

1. Registar → login → upload → download → rename → move → delete → restore.
2. Criar pasta → mover ficheiro → apagar pasta → restaurar.
3. Partilhar com outro utilizador → aceder → revogar.
4. Criar link público → aceder sem login → revogar.
5. Registar nó → heartbeat → remover com migração.
6. Ativar 2FA → login com código → desativar.
7. Exceder quota → verificar notificação.
8. Desligar nó → tentar aceder → verificar cache (CACHE) ou erro (DENY).

---

## Resumo de Fases

| Fase | Nome | Est. esforço | Bloqueador? |
|---|---|---|---|
| 0 | Testes automatizados | 3-4 dias | 🔴 Sim |
| 1 | Segurança de acesso | 2-3 dias | 🔴 Sim |
| 2 | Completude funcional | 2-3 dias | 🟡 Importante |
| 3 | Notificações e tempo real | 2 dias | 🟡 Importante |
| 4 | Infraestrutura de produção | 2-3 dias | 🟢 Produção |
| 5 | Validação final | 1 dia | — |

**Total estimado:** ~12-18 dias de trabalho focado.

---

## Notas

- A ordem das fases é **intencional**: testes primeiro (Fase 0) para que as fases seguintes possam ser feitas com confiança.
- A Fase 1 (segurança) é crítica porque o sistema lida com dados pessoais.
- A Fase 4 (infraestrutura) pode ser feita em paralelo com as Fases 2-3 por outra pessoa.
- **Decisão de 2026-10-12:** compressão removida. O container `.dbs` é o único formato de armazenamento (AES-GCM direto, sem compressão).
- O sistema já tem uma base sólida: a arquitetura de nós, replicação e cifra estão bem implementadas.
