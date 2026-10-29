# Seed inicial do sistema

O backend cria automaticamente os dados mínimos para o sistema arrancar e ser
utilizável. O processo é **idempotente**: pode correr em todos os arranques sem
duplicar registos.

## O que é criado

| Ordem | Componente | Tabelas | Onde |
|---|---|---|---|
| 1 | Recursos (`ARQUIVOS`, `ARMAZENAMENTO`, `ADMINISTRACAO`) | `recursos` | `PermissionService#seed` |
| 2 | Permissões (`ARQUIVO_*`, `ARMAZENAMENTO_*`, `ADMIN_*`) | `permissoes` | `PermissionService#seed` |
| 3 | Papéis `ADMIN` e `USER` + associações | `papeis`, `papel_permissao` | `PermissionService#seed` |
| 4 | Nó de armazenamento da nuvem oficial | `storage_nodes` | `SystemNodeService` |
| 5 | Conta **ADMIN** inicial | `users` | `DataSeeder` |
| 6 | Pastas demo (opcional) | `folders` | `DataSeeder` |

> O registo público (`POST /auth/register`) cria sempre utilizadores `USER`.
> Sem a conta ADMIN do seed ninguém consegue aceder a `/admin/**`.

## Configuração

As variáveis podem ser definidas no `cloudsystem/.env` ou no ambiente do
servidor. Ver `cloudsystem/.env.example`.

| Variável | Default | Descrição |
|---|---|---|
| `SEED_ENABLED` | `true` | Liga/desliga o seed do ADMIN e do nó de sistema. |
| `SEED_ADMIN_EMAIL` | `admin@arquivonuvem.local` | Email da conta ADMIN inicial. |
| `SEED_ADMIN_PASSWORD` | `Admin@1234` | Password da conta ADMIN. Regra: 8–24 caracteres, maiúscula, minúscula, número e símbolo. |
| `SEED_ADMIN_NAME` | `Administrador` | Nome próprio. |
| `SEED_ADMIN_SURNAME` | `Sistema` | Apelido. |
| `SEED_DEMO_DATA` | `false` | Cria as pastas `Documentos`, `Imagens` e `Projetos`. |

Exemplo de `cloudsystem/.env`:

```dotenv
SEED_ENABLED=true
SEED_ADMIN_EMAIL=admin@exemplo.pt
SEED_ADMIN_PASSWORD=UmaPassword@Forte1
SEED_DEMO_DATA=true
```

## Como usar

1. Configurar a base de dados, `JWT_SECRET` e `CLOUD_MASTER_KEY` (ver `README.md`).
2. Arrancar o backend:

   ```bash
   cd cloudsystem
   ./mvnw spring-boot:run
   ```

3. No log de arranque aparece algo como:

   ```
   Seed: conta ADMIN criada (admin@arquivonuvem.local).
   ```

4. Abrir o frontend (`http://localhost:5173`) e fazer login com as credenciais
   configuradas.
5. Confirmar em **Armazenamento** que o nó “Nuvem do sistema” aparece `ONLINE`.

## Comportamento idempotente

- Se a conta com `SEED_ADMIN_EMAIL` já existir, **não é alterada** (nome,
  password e papel ficam como estão).
- Se já existir **qualquer** ADMIN mas o email do seed não existir, a conta
  padrão não é criada (evita um admin extra em bases já configuradas).
- O nó `SYSTEM` é reutilizado se já existir.
- As pastas demo só são criadas se ainda não existirem.
- Recursos, permissões e papéis são verificados individualmente pelo código.

## Desativar o seed

```dotenv
SEED_ENABLED=false
```

Nesse caso é necessário criar o primeiro ADMIN manualmente (por SQL ou
temporariamente com o seed ligado).

## Repor a conta ADMIN

1. Apagar a conta com o email configurado:

   ```sql
   DELETE FROM users WHERE email = 'admin@arquivonuvem.local';
   ```

2. Reiniciar o backend com `SEED_ENABLED=true`.

> Apagar um utilizador remove em cascata ficheiros, pastas, nós, partilhas e
> notificações associados. Para uma base de dados de teste, prefere recriar a BD.

## Segurança em produção

- **Define sempre** `SEED_ADMIN_EMAIL` e `SEED_ADMIN_PASSWORD` próprios.
- O valor padrão `Admin@1234` é apenas para desenvolvimento.
- Depois do primeiro arranque podes desativar o seed (`SEED_ENABLED=false`).
- Nunca committar o `.env` com credenciais reais.

## O que NÃO é seeded

- Ficheiros e respetivos containers cifrados (`.dbs`) — exigem a master key e
  devem ser criados via `FileService`/`ContainerService`.
- Réplicas, versões, partilhas, links públicos, acessos a nós e notificações —
  são dados operacionais/de negócio.
- Tokens de reposição de password — são efémeros.
- Os SQL em `backend/database/schema/` são um modelo legado e **não** são
  executados pelo Spring Boot.

## Problemas comuns

| Sintoma | Causa / solução |
|---|---|
| `Seed: SEED_ADMIN_PASSWORD não cumpre a regra` | A password configurada não tem maiúscula, minúscula, número ou símbolo, ou tem menos de 8 / mais de 24 caracteres. |
| Não consigo entrar em `/admin` | A conta usada não tem papel `ADMIN`; confirma `SELECT email, role FROM users;`. |
| `Seed: já existe uma conta ... mas o papel é USER` | Já existia uma conta com esse email. Usa outro email ou altera o papel em `/admin/users`. |
| O nó de sistema não aparece | Verifica o log de `SystemNodeService` e se `cloud.node.storage-dir` é gravável. |
| Upload falha com “sem armazenamento disponível” | O nó `SYSTEM` não foi criado ou está desativado; reinicia o backend. |
