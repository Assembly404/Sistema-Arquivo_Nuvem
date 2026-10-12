# Como Usar o Sistema Arquivo Nuvem

> **Documentação única para desenvolvedores e utilizadores.**  
> Este documento cobre desde a instalação local até ao deploy em produção, passando por como os utilizadores devem interagir com a aplicação.

---

## Índice

1. [Pré-requisitos](#1-pré-requisitos)
2. [Quick Start — Como deixar a aplicação funcional em 10 minutos](#2-quick-start---como-deixar-a-aplicação-funcional-em-10-minutos)
3. [Para Desenvolvedores — Setup local e configuração](#3-para-desenvolvedores---setup-local-e-configuração)
   - 5.1 Backend (Spring Boot)
   - 5.2 Frontend (React + Vite)
   - 5.3 Variáveis de ambiente críticas
   - 5.4 Testes
4. [Para Utilizadores — Como usar a aplicação](#4-para-utilizadores---como-usar-a-aplicação)
   - 6.1 Registrar e login
   - 6.2 Área de trabalho (Dashboard)
   - 6.3 Gerir armazenamento (nuvem + nós pessoais)
   - 6.4 Partilha de ficheiros e pastas
   - 6.5 Links públicos
   - 6.6 Recuperação de senha
5. [Deploy em Produção](#5-deploy-em-produção)
   - 7.1 Backend no Render
   - 7.2 Frontend no Vercel
   - 7.3 Base de Dados PostgreSQL no Render
   - 7.4 Variáveis de ambiente no Render
6. [Segurança e Boas Práticas](#6-segurança-e-boas-práticas)
7. [Solucção de Problemas Comuns](#7-solucao-de-problemas-comuns)
8. [Histórico de alterações](#8-histórico-de-alterações)

---

## 1. Pré-requisitos

### Para Desenvolvedores (rodar localmente)

| Ferramenta | Versão Mínima | Porquê |
|---|---|---|
| **Java** | JDK 17 | O backend é Spring Boot 4 / Java 17 |
| **PostgreSQL** | 14+ | Base de dados do sistema |
| **Node.js** | 20+ | O frontend usa Vite + React 19 |
| **npm / yarn** | — | Para dependências do frontend |
| **openssl** | — | Para gerar `JWT_SECRET` e `CLOUD_MASTER_KEY` |
| **Git** | — | Para clonar o repositório |

### Para Utilizadores (aceder à aplicação)

| Requisito | Detalhe |
|---|---|
| **Navegador** | Chrome, Firefox, Edge ou Safari (últimas versões) |
| **Resolução** | 1366×768px mínimo (1920×1080px recomendado) |
| **Conexão** | Banda larga (para upload/download de ficheiros) |

---

## 2. Quick Start — Como deixar a aplicação funcional em 10 minutos

Este cenário instala apenas a **nuvem do sistema** (sem nós pessoais). Depois pode-se adicionar nós pessoais.

### Passo a passo:

```bash
# 1. Clonar o repositório (já está na pasta atual)
#    git clone <url-do-repositorio>
#    cd Sistema-Arquivo_Nuvem

# 2. Configurar o backend
cd cloudsystem
cp .env.example .env          # criar ficheiro a partir do exemplo
# Editar .env com os seguintes valores:
#   DB_URL=jdbc:postgresql://localhost:5432/storagearchivedb
#   DB_USERNAME=postgres
#   DB_PASSWORD=sua_senha_aqui
#   JWT_SECRET=$(openssl rand -base64 48)   <- gerar uma vez só
#   CLOUD_MASTER_KEY=$(openssl rand -base64 32) <- guardar para sempre!
#   MAIL_ENABLED=false                        <- manter false em dev

# 3. Criar a base de dados
createdb storagearchivedb   # ou usar pgAdmin/psql

# 4. Iniciar o backend
./mvnw spring-boot:run        # servidor subirá em http://localhost:5000

# 5. Configurar o frontend
cd ../frontend                # voltar ao root e entrar na pasta frontend
cp .env.example .env          # manter VITE_API_URL=http://localhost:5000
npm install
npm run dev                   # frontend subirá em http://localhost:5173

# 6. Aceder e registar
#    - Abrir http://localhost:5173 no navegador
#    - Clicar em "Registar"
#    - Preencher nome, e-mail e senha
#    - Primeiro utilizador criado será ADMIN automaticamente
#    - Fazer login e começar a usar

# Pronto! O sistema está funcional em modo "cloud-only".
# O armazenamento fica no disco do computador onde o backend roda.
```

---

## 3. Para Desenvolvedores — Setup local e configuração

### 3.1 Backend (Spring Boot)

| Passo | Comando |
|---|---|
| Entrar na pasta | `cd cloudsystem` |
| Copiar ficheiro de ambiente | `cp .env.example .env` |
| Editar `.env` com credenciais | — |
| Instalar dependências e correr | `./mvnw spring-boot:run` |
| Executar testes | `./mvnw test` |
| Build de produção | `./mvnw package` |

**Ficheiro `.env` — o que cada um faz:**

| Variável | Obrigatório? | Padrão / O que é | Exemplo |
|---|---|---|---|
| `DB_URL` | **Sim** | Ligação PostgreSQL | `jdbc:postgresql://localhost:5432/storagearchivedb` |
| `DB_USERNAME` | **Sim** | Utilizador BD | `postgres` |
| `DB_PASSWORD` | **Sim** | Password BD | `minha_senha` |
| `JWT_SECRET` | **Sim** | Segredo HMAC256 (≥ 32 chars) | `openssl rand -base64 48` |
| `CLOUD_MASTER_KEY` | **Sim** | Chave AES Base64 (16/24/32 bytes) | `openssl rand -base64 32` ⚠️ **Nunca mudar depois** |
| `MAIL_ENABLED` | Não | `true` para enviar email real | `false` |
| `MAIL_DEV_LOG_OTP` | Não | Mostrar OTP no log dev | `false` |

**Endpoints úteis para testes:**

- `POST /auth/register` — registar novo utilizador
- `POST /auth/login` — login
- `GET /users/me` — perfil do utilizador logado
- `GET /storage/summary` — resumo do armazenamento
- `GET /nodes/system` — nó de sistema (sempre disponível)
- `GET /admin/permissions` — matriz de permissões (ADMIN só)

### 3.2 Frontend (React + Vite)

| Passo | Comando |
|---|---|
| Entrar na pasta | `cd frontend` |
| Copiar ficheiro de ambiente | `cp .env.example .env` |
| Instalar dependências | `npm install` |
| Rodar em desenvolvimento | `npm run dev` |
| Build para produção | `npm run build` |
| Lint | `npm run lint` |

**Ficheiro `.env`:**

| Variável | Padrão | O que faz |
|---|---|---|
| `VITE_API_URL` | `http://localhost:5000` | URL base da API do backend |

**Páginas principais (após login):**

- `/dashboard/arquivos` — Meus ficheiros (upload, listar, pesquisar)
- `/dashboard/armazenamento` — Resumo da quota, política de nós, gestão de dispositivos
- `/dashboard/compartilhados` — Ficheiros compartilhados comigo
- `/dashboard/lixeira` — Ficheiros excluídos (reversível)
- `/dashboard/perfil` — Dados do perfil e password
- `/dashboard/configuracoes` — Política de armazenamento, offline behavior
- `/notificacoes` — Notificações do sistema

### 3.3 Variáveis de ambiente críticas

Estas **não podem** ser definidas no código-fonte — têm de vir do `.env` (backend) ou das Environment Variables do deployment (frontend/Render/Vercel).

| Variável | Onde definir | Porquê crítica |
|---|---|---|
| `CLOUD_MASTER_KEY` | `.env` do backend | Cifrar todos os ficheiros. Mudar isto = ficheiros ficam ilegíveis. |
| `JWT_SECRET` | `.env` do backend | Assinar tokens JWT. Mudar = usuários são deslogados. |
| `DB_URL / DB_USERNAME / DB_PASSWORD` | `.env` do backend | Conectar na base de dados. Sem estas, backend não arranca. |
| `VITE_API_URL` | `.env` do frontend | Apontar para o backend correcto (em dev: `localhost:5000`, em prod: URL da Render). |
| `MAIL_HOST/PORT/USER/PASS` | `.env` do backend (opcional) | Enviar email de OTP. Se não configurar, OTP só aparece no log. |

### 3.4 Testes

- **Backend:** `./mvnw test` — executa testes em `cloudsystem/src/test/`.
- **Frontend:** `npm run lint` — verifica erros de ESLint (5 erros pré-existentes em `input.jsx`, `toast.jsx`, `Home.jsx`).
- **Testes ainda estão limitados** — apenas `CloudsystemApplicationTests` existe (teste de contexto). Não há framework de testes frontend configurado ainda.

---

## 4. Para Utilizadores — Como usar a aplicação

### 4.1 Registrar e login

| Ação | O que fazer |
|---|---|
| **Aceder à aplicação** | Ir a `http://localhost:5173` (ou ao domínio deployado). |
| **Registrar** | Clicar em "Registar" na landing page. Preencher: nome, e-mail, senha. O primeiro utilizador criado torna-se automaticamente **ADMIN**. |
| **Login** | Preencher e-mail/senha no formulário de login. O token JWT fica guardado no frontend (context Auth) e dura 24h. |
| **Logout** | Clicar em "Terminar sessão" no rodapé da sidebar. |

### 4.2 Área de trabalho (Dashboard)

Ao fazer login, é carregada a `DashboardLayout` com sidebar autenticada. A sidebar mostra menus baseados nas permissões do utilizador:

| Menu | Visível para | O que faz |
|---|---|---|
| **Início** | Todos | Página inicial com estatísticas rápidas |
| **Meus Arquivos** | Todos | Upload, listar, pesquisar, gerenciar ficheiros |
| **Compartilhados comigo** | Todos | Ver ficheiros que outros compartilharam comigo |
| **Lixeira** | Todos (com permissão `ARQUIVO_EXCLUIR`) | Ficheiros excluídos, com restore/purge |
| **Armazenamento** | Todos | Ver quota, política de nós, comportamento offline |
| **Administração** | Apenas **ADMIN** | Gestão de utilizadores, papéis, cotas |
| **Perfil** | Todos | Editar nome/sobrenome/email, mudar password |
| **Configurações** | Todos | Política de armazenamento, comportamento offline |
| **Notificações** | Todos | Ver/notificar eventos (partilhas, heartbeat) |
| **Suporte** | Todos | Informações de contacto de suporte |

**Regra importante:** A sidebar filtra itens por permissão. Se o utilizador não tiver permissão para um módulo, o item **não aparece** no DOM (não é só `display: none`). Isso é feito pelo frontend via `filterMenuByPermissions`.

### 4.3 Gerir armazenamento (nuvem + nós pessoais)

#### Página `Armazenamento` (`/dashboard/armazenamento`)

Esta página mostra:

1. **Resumo da nuvem do sistema:**
   - `usedBytes` / `quotaBytes` (cota do utilizador na nuvem).
   - `fileCount` — número de ficheiros.
   - `policy` — política corrente (`SYSTEM_FIRST`, `DEVICE_FIRST`, `MANUAL`).
   - `policyLabel` — nome legível da política.

2. **Lista de nós pessoais:**
   - Cada nó registado aparece com: nome, status (ONLINE/DEGRADED/OFFLINE), tipo (SYSTEM/DEVICE), quota, usage.
   - Botões: **Enable**, **Disable**, **Remove** (com opção de migrar ficheiros).

3. **Ação principal — Upload:**
   - Ao fazer upload de um ficheiro, aparece uma secção de "Destino do upload".
   - **Opções:**
     - **Nuvem do sistema** → ficheiro vai só para o disco do backend.
     - **Dispositivo pessoal** → se houver um nó pessoal online e disponível com espaço, o ficheiro é replicado para esse nó.
     - **Escolher manualmente** → sempre perguntar o destino (respecta a política, mas dá opção de mudar).

#### Política de armazenamento

| Política | Comportamento no upload |
|---|---|
| **SYSTEM_FIRST** (padrão) | O sistema tenta guardar na nuvem do servidor primeiro. Se houver espaço e nó disponível, guarda aí. Só usa dispositivo pessoal se `policy` indicar ou se o usuário escolher manualmente. |
| **DEVICE_FIRST** | Prioriza dispositivos pessoais. Só usa a nuvem do se o dispositivo estiver offline, sem espaço, ou o usuário escolher "nuvem". |
| **MANUAL** | Sempre perguntar ao usuário no momento do upload. O utilizador escolhe `SYSTEM` ou `DEVICE` antes de confirmar. |

#### Comportamento offline (`offlineBehavior`)

Guardado em cada utilizador no BD (`DENY` ou `CACHE`):

- **DENY** (padrão se não configurado): Se o dispositivo pessoal estiver offline, tentar aceder ao ficheiro lança erro (`DeviceOfflineException`). O utilizador não consegue abrir o ficheiro.
- **CACHE**: Se o dispositivo estiver offline, o sistema tenta servir uma réplica guardada no nó de sistema (cloud). Se houver réplica no servidor, o ficheiro abre; se não houver, lança erro.

O utilizador pode mudar isto em **Configurações** → **Comportamento offline**.

### 4.4 Partilha de ficheiros e pastas

#### Partilhar um ficheiro/pasta

1. Ir à pasta **Meus Arquivos**.
2. Selecionar o ficheiro/pasta.
3. Clicar no ícone de "partilhar" (seta/setas ou botão Share).
4. Inserir o e-mail do destinatário.
5. Escolher permissão: **Somente leitura** ou **Edição**.
6. Confirmar.

O destinatário recebe uma notificação (se email estiver configurado) e o item aparece na pasta "Compartilhados comigo".

#### Permissões efetivas

- **Dono do ficheiro/pasta** sempre tem permissão de **edição**.
- **Outros utilizadores** têm a permissão que foi definida no momento da partilha.
- O dono pode **alterar permissões** ou **revogar** o acesso a qualquer momento.
- A revogação remove o acesso e envia notificação ao destinatário.

#### Links públicos (RF15)

1. Em **Meus Arquivos**, selecionar ficheiro/pasta.
2. Clicar em "Gerar link público".
3. Opcionalmente, definir **palavra-passe** (password).
4. O link gerado pode ser enviado a qualquer pessoa — **não precisa de login** no sistema.
5. O destinatário acede via `http://seu-domínio/public/links/{token}`.
6. Se password foi definida, terá de a inserir.
7. **Revogar link:** na lista de links públicos, clicar em "Revogar" — o link deixa de funcionar imediatamente.

### 4.5 Recuperação de senha (OTP)

1. Na página de login, clicar em "Esqueci a minha senha".
2. Inserir o e-mail cadastrado.
3. O sistema gera um **código OTP de 6 dígitos**.
4. **Modo dev:** O código aparece no log do backend (`MAIL_DEV_LOG_OTP=true`).
5. **Modo produção:** Se `MAIL_ENABLED=true` e SMTP estiver configurado, o código é enviado por email. Se não, só aparece no log (não recomendado para produção).
6. Inserir o código de 6 dígitos na página de verificação de OTP.
7. Definir nova password (que tem de cumprir a regra: 8-24 chars, maiúscula, minúscula, número, símbolo).
8. A password antiga deixa de funcionar; a nova funciona no login seguinte.

---

## 5. Deploy em Produção

### 5.1 Backend no Render

#### 5.1.1 Criar serviço Web

1. Ir ao painel do Render → **New Web Service** → **Deploy a new service**.
2. **Connect a repository** → selecionar a pasta `cloudsystem/` (ou o repo inteiro).
3. **Environment**: Deixar em branco (o Render detecta Java a partir do `pom.xml`).
4. **Build Command** (se não usar Docker):
   ```
   ./mvnw spring-boot:build-image
   ```
   **Ou**, mais simples: deixar em branco e deixar o Render usar o Buildpack Java.

5. **Start Command**:
   ```
   java -jar target/cloudsystem-0.0.1-SNAPSHOT.jar
   ```
   **Ou**, com Docker (recomendado), o Dockerfile cuida disto.

#### 5.1.2 Adicionar PostgreSQL interno

1. No dashboard do serviço Web, clicar **Add Internal PostgreSQL**.
2. O Render cria automaticamente um BD PostgreSQL e dá o `Internal Database URL`.
3. **Anotar** esse URL — vai ser a variável `DB_URL`.

#### 5.1.3 Definir variáveis de ambiente

No painel do serviço, secção **Environment Variables**, adicionar:

| Variável | Valor | Observação |
|---|---|---|
| `DB_URL` | URL interno do PostgreSQL fornecido pelo Render | **Obrigatório** |
| `DB_USERNAME` | `postgres` (ou o username do BD Render) | **Obrigatório** |
| `DB_PASSWORD` | Password do BD Render | **Obrigatório** |
| `JWT_SECRET` | `openssl rand -base64 48` | Gerar uma vez e manter para sempre. **Não mudar depois!** |
| `CLOUD_MASTER_KEY` | `openssl rand -base64 32` | **Crítico**: Guardar em local seguro. Mudar = ficheiros ilegíveis. |
| `MAIL_ENABLED` | `true` ou `false` | Se `true`, tem de também configurarMAIL_HOST/PORT/USER/PASS. |
| `MAIL_HOST` | Ex: `smtp.gmail.com` ou `smtp.mailtrap.io` | Só se `MAIL_ENABLED=true`. |
| `MAIL_PORT` | Ex: `587` (com STARTTLS) ou `25` | Só se `MAIL_ENABLED=true`. |
| `MAIL_USERNAME` | Endereço de email do remetente | Só se `MAIL_ENABLED=true`. |
| `MAIL_PASSWORD` | Password do SMTP (ou app-password do Gmail) | Só se `MAIL_ENABLED=true`. |

#### 5.1.4 Deploy

1. Clicar **Create Web Service**.
2. O Render vai fazer o build (Maven) e subir o container.
3. Aguardar ficar "Live".

#### 5.1.5 Testar

- `https://seu-servico.onrender.com/auth/login` → tentar login/registar.
- `https://seu-servico.onrender.com/nodes/system` → should retornar info do nó de sistema.

### 5.2 Frontend no Vercel

#### 5.2.1 Criar projeto

1. Ir ao painel do Vercel → **Add New** → **Project**.
2. **Importar repositório** → selecionar a pasta `frontend/` (ou o repo inteiro).
3. Framework Preset: **Vite** (o Vercel detecta automaticamente).

#### 5.2.2 Variáveis de ambiente

No painel do projeto, secção **Settings** → **Environment Variables**, adicionar:

| Chave | Valor | Observação |
|---|---|---|
| `VITE_API_URL` | `https://seu-backend.onrender.com` | O domínio do backend na Render. **Essencial** — sem isto, o frontend tenta ligar para `localhost:5000`. |

#### 5.2.3 Deploy

1. Clicar **Deploy**.
2. O Vercel vai buildar o React + Vite e subir para a CDN global.
3. Aguardar ficar "Live".

#### 5.2.4 URL final

Depois do deploy, a aplicação estará disponível em algo como:
`https://seu-front.vercel.app`

Os utilizadores podem aceder apenas pelo frontend (Vercel), e todas as chamadas API vão para o backend (Render).

### 5.3 Base de Dados PostgreSQL no Render

- O Render oferece um **PostgreSQL interno** gratuito no plano Starter.
- Limites: 300MB de armazenamento, 1 conexão simultânea (pode ser suficiente para testes/pequeno uso).
- Para produção com mais usuários, recomenda-se:
  - **Plano Pro** do Render (BD com mais recursos).
  - **Ou migrar** o BD para AWS RDS, Supabase, Neon, ou qualquer outro provedor de PostgreSQL.
- O BD interno do Render faz backup automático (pode ser baixado no painel do Render).

### 5.4 Variáveis de ambiente no Render — Resumo rápido

No painel do serviço backend (Render), **Environment Variables**:

```
DB_URL=postgres://postgres:minhasenha@db.teu-app.onrender.com/storagearchivedb
DB_USERNAME=postgres
DB_PASSWORD=minhasenha
JWT_SECRET=aaBBccDDeeFF... (48 bytes base64)
CLOUD_MASTER_KEY=zzYYxxWW... (32 bytes base64)  ← GUARDAR ESTA CHAVE!
MAIL_ENABLED=false
MAIL_HOST=
MAIL_PORT=
MAIL_USERNAME=
MAIL_PASSWORD=
```

### 5.5 Passos finais após deploy

1. **Aceder ao frontend** (`https://seu-front.vercel.app`).
2. **Registrar** a primeira conta (será ADMIN).
3. **Login**.
4. Ir à página **Armazenamento** → verificar que o nó de sistema aparece.
5. **(Opcional) Configurar nó pessoal:** usar o installer `install-node.sh` em um PC próprio, apontando para `https://seu-backend.onrender.com`.
6. **Testar upload:** tentar upload de um ficheiro e verificar onde ele vai (nuvem do sistema ou dispositivo pessoal, dependendo da política).

---

## 6. Segurança e Boas Práticas

### 6.1 Segredos nunca no git

- `.env` está no `.gitignore` — **nunca** comitar credenciais.
- `CLOUD_MASTER_KEY` e `JWT_SECRET` têm de ser gerados uma única vez e guardados permanentemente.
- Se algum segredo foi exposto acidentalmente, **rotacioná-lo imediatamente** e fazer *rebuild* do sistema.

### 6.2 JWT e sessões

- O JWT tem validade de **24h**.
- Após 24h, o utilizador é deslogado e tem de fazer login novamente.
- O frontend renova o token automáticamente enquanto o utilizador tem atividade (o `AuthContext` tem `setAuthToken`).

### 6.3 Nós pessoais e rede

- O nó pessoal (agente num PC) comunica com o backend via **HTTP** (não HTTPS) na porta **8081**.
- Isso é intencional — o agente é um processo local na rede do utilizador.
- **Cuidado:** Se o PC do utilizador ficar exposto à internet sem proteção, terceiros poderão tentar atacar o agente. Em ambiente production, recomenda-se:
  - Usar **Cloudflare Tunnel**, **ngrok**, ou **port-forwarding** seguro no router.
  - Manter o agente atualizado.
  - Não usar a porta 8081 para outros serviços.

### 6.4 Backup

- O Render faz backup automático do PostgreSQL interno.
- Recomenda-se descarregar backups périodicamente (painel do Render → PostgreSQL → Backup → Download).
- Os ficheiros dos utilizadores (cifrados) estão no disco do backend — fazer *snapshot* do disco da Render ou configurar backup externo (S3, Google Cloud) se a crítica dos dados o exigir.

### 6.5 Atualizações de `CLOUD_MASTER_KEY`

**Nunca** mudar `CLOUD_MASTER_KEY` após ficheiros terem sido armazenados.

Se realmente for necessário mudar:
1. Exportar todos os ficheiros existentes (download via links públicos ou API).
2. Mudar a variável no `.env` e *re-deploy*.
3. Fazer upload novamente dos ficheiros (os antigos ficariam cifrados com a antiga chave e inacessíveis).

---

## 7. Solução de Problemas Comuns

### 7.1 Backend não arranca

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot find database` | `DB_URL` errado ou BD não criado | Verificar `DB_URL` no `.env`; criar o BD (`createdb storagearchivedb`). |
| `JWT_SECRET invalid` | `JWT_SECRET` muito curto (< 32 chars) ou inválido | Gerar novo com `openssl rand -base64 48` e colocar no `.env`. |
| `CLOUD_MASTER_KEY invalid` | Base64 inválida ou não são 16/24/32 bytes | Gerar com `openssl rand -base64 32`; **cuidado**: mudar isto torna ficheiros existentes ilegíveis. |
| `Port 5000 already in use` | Outro processo na porta 5000 (local) | Mudar `server.port` no `.env` ou matar o processo que ocupa a porta. |

### 7.2 Frontend não conecta no backend

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Network error` ou `Failed to fetch` | `VITE_API_URL` apontar para localhost em produção | Em produção, definir `VITE_API_URL` para o domínio da Render (ex: `https://backend.onrender.com`). |
| `CORS error` | Origin do frontend não está na lista CORS do Render | No `SecurityConfig.java`, adicionar o domínio do Vercel à lista de `allowedOriginPatterns`. |
| `401 Unauthorized` em todas as chamadas | Token JWT expirado ou não enviado | Fazer login novamente. O frontend deveria renovar automaticamente, mas se o token expirar de propósito (24h), é necessário re-autenticar. |

### 7.3 Nó pessoal não aparece ou está OFFLINE

| Sintoma | Causa provável | Solução |
|---|---|---|
| Nó aparece como **OFFLINE** | O PC onde o agente está ligado foi desligado ou dormiu | Ligar o PC; ou configurar `offlineBehavior=CACHE` para tentar servir cópia do servidor. |
| Nó aparece como **DEGRADED** | O agente está ligado, mas o heartbeat falhou | Verificar se a porta 8081 está aberta no firewall/router; verificar conectividade de rede. |
| Nó **não aparece nada** na lista | O agente não foi instalado corretamente ou o `sessionId` não bate | Reinstalar o agente (`install-node.sh` ou `install-node.bat`); garantir que o IP/ domínio do backend está correcto. |
| Upload para "Dispositivo pessoal" falha | Nenhum nó online disponível com espaço | Verificar quota dos nós; ou mudar política para `SYSTEM_FIRST`; ou aguardar o nó ficar online. |

### 7.4 Email de OTP não chega

| Sintoma | Causa provável | Solução |
|---|---|---|
| `MAIL_ENABLED=false` | O SMTP não está configurado | Se quiser email real, configurar `MAIL_ENABLED=true` + `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`. |
| Código aparece no log mas não chega ao email | `MAIL_DEV_LOG_OTP=true` (modo dev) | Em produção, garantir que `MAIL_DEV_LOG_OTP=false`. |
| SMTP auth falha | Password do Gmail/ Mailtrap errada | Gerar "App Password" no Gmail (se usar Gmail) ou verificar credenciais do Mailtrap. |

### 7.5 Senha não funciona após reset

| Sintoma | Causa provável | Solução |
|---|---|---|
| Nova password não funciona | O novo password não cumpre a regra (8-24, maiúscula, minúscula, número, símbolo) | Verificar a regex: `^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$`. |
| Account bloqueada após 5 tentativas inválidas | Bloqueio temporário de 15 min após 5 tentativas erradas | Aguardar 15 minutos ou contactar admin para desbloquear (não existe admin "super" além do primeiro criado). |

### 7.6 Lentidão ou timeouts

| Sintoma | Causa provável | Solução |
|---|---|---|
| Uploads lentos | Largura de banda limitada ou nó pessoal com conexão lenta | Testar upload para a nuvem do sistema (deveria ser mais rápido, usa o disco do backend). |
| Listagem de ficheiros lenta | Base de dados com muitos ficheiros/réplicas | Considerar indexação ou arquivamento de ficheiros antigos. |
| WebSocket/heartbeat lento | Nó pessoal com CPU/memória alta | Reiniciar o agente ou alocar mais recursos ao PC do nó. |

---

## 8. Histórico de alterações

| Data | Versão | Alterações |
|---|---|---|
| 2026-10-12 | 1.0 | Criação inicial — Quick Start, Developer setup, User guide, Deploy Render/Vercel, Security, Troubleshooting. |
| — | — | — |

---

## Como contribuir / dúvidas

- **Para dúvidas sobre o código:** Abrir issue no repositório ou rever a documentação em `docs/` (identidade visual, arquitetura da sidebar, regras de tecnologias).
- **Para sugestões de funcionalidade:** Discutir na equipa — o sistema já tem roadmap documentado em `docs/requisitos_sistema_armazenamento_nuvem.pdf` e `docs/historias_usuario.pdf`.
- **Para reportar bugs:** Verificar se já existe issue similar; se não, criar issue descrevendo passos para reproduzir, ambiente (OS, navegador, versão do Java/Node) e logs de erro.

---

## Fim da documentação.