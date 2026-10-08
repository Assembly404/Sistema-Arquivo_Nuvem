# OTP por Email — Guia Completo de Implementação

> Estado atual: **código pronto, envio desativado**. Este documento descreve,
> passo a passo e com divisão de tarefas (TU = utilizador / EU = agente),
> tudo o que é preciso para o código de reposição de password (OTP) chegar
> ao email real.

---

## 1. Como funciona hoje (já implementado)

```
TU pedes "esqueci a password"
  → POST /auth/password/forgot  { email }
      - gera OTP de 6 dígitos (SecureRandom)
      - guarda HASH bcrypt em password_reset_token (expira em 15 min, 5 tentativas)
      - EmailService.sendOtp(...)   ← AQUI falha em silêncio (ver §2)
      - resposta SEMPRE 200 genérica (anti-enumeration)
  → POST /auth/password/verify-otp  { email, otp }
      - valida hash + expiração + tentativas → devolve resetToken (uso único, 10 min)
  → POST /auth/password/reset  { resetToken, password }
      - troca a password (bcrypt) e marca o token como usado
```

Ficheiros envolvidos:
- `cloudsystem/src/main/java/.../service/EmailService.java` — envio (flag `app.mail.enabled`)
- `cloudsystem/src/main/java/.../service/PasswordResetService.java` — geração/guarda do OTP
- `cloudsystem/src/main/java/.../controller/AuthController.java` — endpoints `/auth/password/*`
- `cloudsystem/src/main/resources/application.properties` — config SMTP (linhas 15-23)
- `frontend/src/pages/ForgotPassword.jsx` — wizard de 4 passos (UI pronta)

## 2. Porque é que o email NÃO chega (causa raiz)

Duas linhas, ambas em `application.properties`:

```properties
app.mail.enabled=false                 # ← EmailService faz early-return, nunca envia
spring.mail.host=${MAIL_HOST:localhost} # ← localhost:1025 (MailHog) não existe na tua máquina
spring.mail.properties.mail.smtp.auth=false        # ← correto para MailHog, ERRADO para Gmail/Mailtrap
spring.mail.properties.mail.smtp.starttls.enable=false  # ← idem
```

Em `EmailService.java`:
- `if (!enabled || mailSender == null) { log.info("modo dev, envio desativado"); return; }`
- Ou seja: o OTP é gerado e guardado, **mas nunca sai do servidor**.
- Isto é **deliberado em dev** (não spamma caixas de email), mas impede o teste real.

## 3. Opção A — Gmail com App-Password (recomendada para teste real)

### TU fazes (≈5 min, só na primeira vez)
1. Entra em **myaccount.google.com** com a conta que vai enviar os emails.
2. **Segurança → Verificação em duas etapas** → ativa se ainda não estiver (obrigatório).
3. **Segurança → App passwords** → cria uma com o nome `cloudsystem`.
   - *(Se não vires a opção: pesquisa "App passwords" na barra do Google Account.)*
4. Copia a **password de 16 caracteres** gerada (formato `abcd efgh ijkl mnop`).
5. Passa-me (por chat ou, melhor, escreves direto no terminal):
   - `MAIL_USERNAME` = a conta Gmail completa (ex. `tuaconta@gmail.com`)
   - `MAIL_PASSWORD` = a app-password de 16 chars **sem espaços**

> ⚠️ NUNCA uses a tua password normal do Google — o Gmail rejeita-la (bloqueio
> de apps "menos seguros"). Só aceita App-Password com 2FA ativo.

### EU faço (depois de ter as credenciais)
1. `cloudsystem/src/main/resources/application.properties`:
   ```properties
   app.mail.enabled=true
   spring.mail.host=${MAIL_HOST:smtp.gmail.com}
   spring.mail.port=${MAIL_PORT:587}
   spring.mail.username=${MAIL_USERNAME:}
   spring.mail.password=${MAIL_PASSWORD:}
   spring.mail.properties.mail.smtp.auth=true
   spring.mail.properties.mail.smtp.starttls.enable=true
   app.mail.from=${MAIL_FROM:no-reply@arquivonuvem.local}
   ```
   *(host/porta/auth/starttls já vêm por env — o teu valor só precisa de estar
   nas variáveis de ambiente, **nunca hardcoded no ficheiro**.)*
2. Garantir que `EmailService.sendOtp` tem try/catch e loga a causa do erro
   (parcialmente feito — verificar no log do backend).
3. Reiniciar o backend: `cd cloudsystem && mvn spring-boot:run`
   *(a app que está a correr NÃO tem as alterações novas).*

### Teste conjunto (EU corro, TU confirmas o email)
```bash
curl -X POST http://127.0.0.1:5000/auth/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"email":"mlktecno@gmail.com"}'
# Esperado: 200 {"message":"Se existir uma conta..."}  (sempre, mesmo sem conta)

# Confirmar no log do backend que NÃO aparece "envio desativado"
# e que NÃO há AuthenticationFailedException → TU abre a caixa de email
```

## 4. Opção B — Mailtrap (sem usar conta real)

### TU fazes (≈3 min)
1. Conta gratuita em **mailtrap.io** → cria inbox `dev`.
2. Na inbox, aba **SMTP Settings → Node.js / Universal** → copia:
   host, porta, username, password (token).
3. Passas-me esses 4 valores.

### EU faço
Igual à Opção A, mas com as credenciais do Mailtrap
(`smtp.mailtrap.io`, porta 25 ou 587 com STARTTLS). O email aparece na
inbox web do Mailtrap — nunca chega a ninguém real.

## 5. Opção C — Modo dev sem SMTP (para testar o fluxo sem email)

### EU faço
1. Mantenho `app.mail.enabled=false`.
2. No log do backend, depois do `POST /forgot`, mostro-te o OTP gerado
   (ou acrescento um `log.info` temporário **apenas em dev** com o código).
3. TU lês o código no log e segues o wizard normalmente.

- Prós: testa o fluxo completo em 2 minutos, zero credenciais.
- Contras: o OTP aparece no log (inaceitável em produção — manter
  `enabled=true` + SMTP real lá).

## 6. Checklist final (comum)

- [ ] `app.mail.enabled=true` (ou env `MAIL_ENABLED=true`)
- [ ] SMTP host/porta/auth/starttls corretos para a opção escolhida
- [ ] Credenciais só via **variáveis de ambiente**, nunca no git
      (`application.properties` tem hoje a password da BD em plaintext —
      mover também: `spring.datasource.password=${DB_PASSWORD:...}`)
- [ ] Backend reiniciado (`mvn spring-boot:run`) — alterações só têm efeito aí
- [ ] `POST /auth/password/forgot` → 200 sem exceção no log
- [ ] Email recebido com código de 6 dígitos
- [ ] Wizard ForgotPassword: email → código → nova password → login OK
- [ ] OTP expira (15 min) e esgota tentativas (5) → mensagens corretas na UI
- [ ] Password antiga deixa de funcionar; nova funciona no login

## 7. Decisão pendente

Escolhe **A**, **B** ou **C** (§3/§4/§5) e fornece as credenciais da opção
escolhida → implementação e teste seguem imediatamente.

