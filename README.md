# Sistema Arquivo Nuvem

Plataforma de armazenamento em nuvem com possibilidade de usar dispositivos
pessoais (PCs) como **nós de armazenamento** adicionais. Backend em Spring Boot,
frontend em React/Vite.

- **Backend:** `cloudsystem/` — Spring Boot 4 / Java 17, PostgreSQL, JPA, JWT, WebSocket (STOMP).
- **Frontend:** `frontend/` — React 19 + Vite, CSS puro, `react-router` v8.
- **Documentação e modelos:** `docs/` e `frontend/src/docs/`.

## Estrutura

```
cloudsystem/   API REST, domínio, persistência, nós de armazenamento
frontend/      SPA (landing, autenticação, dashboard)
backend/       scripts SQL de referência (modelo relacional legado)
docs/          requisitos, histórias de utilizador, documentação
```

## Pré-requisitos

- Java 17+
- Node.js 20+
- PostgreSQL 14+

## Backend — configuração e arranque

Os segredos **não** vivem no repositório. Crie `cloudsystem/.env` a partir do
exemplo e preencha os valores:

```bash
cd cloudsystem
cp .env.example .env      # editar e preencher
```

Variáveis obrigatórias:

| Variável | Descrição |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Ligação PostgreSQL |
| `JWT_SECRET` | Segredo HMAC256 (≥ 32 caracteres). Gere com `openssl rand -base64 48` |
| `CLOUD_MASTER_KEY` | Chave AES (Base64, 16/24/32 bytes). Gere com `openssl rand -base64 32` |

> ⚠️ Mudar `CLOUD_MASTER_KEY` torna **ilegíveis** os ficheiros já armazenados.

Opcionais (email OTP):

| Variável | Descrição |
|---|---|
| `MAIL_ENABLED` | `true` para enviar OTP real |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP |
| `MAIL_DEV_LOG_OTP` | `true` para ver o OTP no log em dev (nunca em produção) |

Arrancar:

```bash
cd cloudsystem
./mvnw spring-boot:run        # porta 5000
```

## Frontend — configuração e arranque

```bash
cd frontend
cp .env.example .env          # ajustar VITE_API_URL se necessário
npm install
npm run dev                   # Vite em http://localhost:5173
npm run build                 # build de produção
npm run lint                  # ESLint
```

### Variáveis de ambiente

| Variável | Default | Descrição |
|---|---|---|
| `VITE_API_URL` | `http://localhost:5000` | URL base da API |

## Segurança

- **Nunca** committar `.env`, `JWT_SECRET`, `CLOUD_MASTER_KEY` ou credenciais de BD.
- Rotacione periodicamente os segredos. Se algum foi exposto, considere-o
  comprometido (ver histórico do git) e substitua-o.
- Tokens de acesso de terceiros (ex.: GitHub) usados localmente devem ser
  revogados após testes.

## Testes

- Backend: `cd cloudsystem && ./mvnw test`
- Frontend: ainda **sem** framework de testes configurado (a adicionar).

## Documentação

- `docs/requisitos_sistema_armazenamento_nuvem.pdf` — requisitos funcionais e não-funcionais (RF01–RF30, RNF01–RNF07).
- `docs/historias_usuario.pdf` — épicos e histórias de utilizador.
- `docs/OTP_EMAIL_SETUP.md` — configuração do envio de OTP por email.
- `frontend/src/docs/IDENTIDADE_VISUAL.md` — regras normativas de UI/UX.
- `frontend/src/docs/Modelos_Sidebar/SIDEBAR_ARQUITETURA.md` — arquitetura de navegação.
- `frontend/src/docs/RegrasDeTecnologias.md` — regras de implementação de componentes.
