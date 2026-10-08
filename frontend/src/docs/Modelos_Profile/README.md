# Modelos Profile + Auth/OTP — imagem base (Regra 1)

> Implementado: `pages/dashboard/Profile.{jsx,css}` (**Opção B — estilo Instagram**,
> referência abaixo), `pages/ForgotPassword.{jsx,css}` (wizard OTP 4 passos),
> auth enriquecida + `/users/me`.

## Profile (/dashboard/perfil) — layout estilo Instagram (ref. verificada)

Referência estrutural obtida da web (não temos screenshot real do IG; layout
"inspirado na estrutura documentada"):
- Gist `Ashar20/ee53825dac8d36de4ef647b9f4ea15d9` (HTML/CSS clone do perfil IG):
  avatar circular 152px → username + botões (Edit Profile / settings) →
  stats (`count` bold + label) → nome real + bio → grelha.
- Artigo `digitalmarketingstudy.com/describe-the-structure-of-an-instagram-profile/`:
  ordem confirmada: avatar → @username → ações → display name → bio → stats → abas.

Mapeamento no nosso domínio (Regra 3 — sem inventar followers/posts):
- Cabeçalho `.profile-header`: avatar 152px (iniciais) + `@username` (email local-part)
  + botões "Editar perfil" / "Alterar password" (tabs) + stats reais do `/users/me`:
  permissões · papel (USER/ADMIN) · membro desde (`dataRegistro`) + bio
  (nome a bold, email, chips de papel).
- Abas tipo IG (ícone + label, sublinhado ativo `--color-primary`): Dados ·
  Segurança · Armazenamento · Administração (só ADMIN) · Perigo — `role=tablist/tab/tabpanel`.
  Em ecrã ≤768px as abas mostram só ícone (como o IG mobile).
- Tokens do projeto sempre (`--color-primary*`, `--font-display`, `--radius-full`),
  sem roxo do IG — Coerência de Marca §5.
- JS inalterado: validações USER_REGEX/PWD_REGEX, checklist 5 requisitos, modal
  EXCLUIR, toasts, foco no título, Admin escondido de USER.

## Auth
- login/register retornam {user{name,surname,email,role,permissions},token,tokenType,expiresIn}.
- JWT com claim role; SecurityFilter reconstrói ROLE_*.
- register força USER; /admin/** exige ADMIN; PATCH liberado no CORS.
- GET/PATCH /users/me, POST /users/me/password, DELETE /users/me.

## OTP
- Passo 1 email -> POST /auth/password/forgot (resposta genérica).
- Passo 2 6 caixas OTP -> POST /auth/password/verify-otp -> resetToken (cooldown 60s).
- Passo 3 nova password -> POST /auth/password/reset (uso único).
- Passo 4 sucesso -> /login.
- OTP guardado como hash + expiração 15min + 5 tentativas; email via starter-mail
  (dev desativado por defeito: `app.mail.enabled=false` — ativar via env
  `MAIL_ENABLED=true` + `MAIL_HOST/PORT/USERNAME/PASSWORD`).
