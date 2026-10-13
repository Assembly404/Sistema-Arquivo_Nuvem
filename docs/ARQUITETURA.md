# Arquitectura do Sistema de Armazenamento em Nuvem

> **Documento de arquitectura** — problema, solução, diferencial e decisões técnicas.
>
> **Stack:** Spring Boot 4.1 / Java 17 · PostgreSQL 14+ · JWT · React 19 + Vite
> **Requisitos de referência:** RF01–RF30, RNF01–RNF07 (`requisitos_sistema_armazenamento_nuvem.pdf`)

---

## 1. Sumário executivo

O **Sistema de Armazenamento em Nuvem** é uma plataforma de armazenamento híbrida
que permite a um utilizador comum **ceder espaço do seu próprio computador** como
nó de armazenamento da nuvem, convivendo no mesmo interface com a nuvem oficial
do sistema.

A diferença face a soluções como Google Drive, Dropbox ou OneDrive não está na
interface — está no **modelo de propriedade do armazenamento**: em vez de o
utilizador ser apenas consumidor de espaço alheio, ele pode tornar-se
**provedor** do seu próprio espaço, sem que isso implique expor os seus dados
nem os dos outros.

**Resultado:** espaço de armazenamento com custo marginal próximo de zero,
cifrado de ponta a ponta por ficheiro, com réplicas em múltiplos nós.

---

## 2. O problema

### 2.1 As três dores

| # | Dor | Evidência |
|---|---|---|
| P1 | **Custo recorrente** — 100–200 GB custam uma subscrição mensal permanente | Modelo de negócio de todos os fornecedores mainstream |
| P2 | **Desperdício assimétrico** — o PC doméstico tem 500 GB–1 TB, maioritariamente vazio, enquanto se paga por espaço remoto | Qualquer portátil actual |
| P3 | **Dependência de fornecedor** — vendor lock-in, mudança de preços, e questões de privacidade/soberania de dados | Concentração do mercado em 3–4 operadores |

### 2.2 A complicação técnica

A resposta ingénua — "usa o disco do utilizador" — falha em três frentes, e é
aqui que o sistema se distingue:

1. **Confiança.** Quem cede 100 GB do seu PC não pode ler ficheiros de outras
   pessoas. Sem cifra, o modelo é inaceitável.
2. **Disponibilidade.** Um PC pessoal desliga-se, dorme, muda de rede. Não tem
   SLA. Ficheiros lá guardados deixam de estar acessíveis.
3. **Alcance de rede.** Um PC doméstico está quase sempre atrás de NAT. O
   servidor **não consegue** alcançar `192.168.x.x` — detalhado no §5.1.

Os requisitos RF17–RF18 são explícitos sobre isto: *"representam uma decisão
arquitectural com forte impacto em segurança, consistência e disponibilidade"*.

---

## 3. A solução

### 3.1 Conceito central: o utilizador duplo

O modelo de identidade separa **papéis**, não contas (ver histórias de
utilizador, secção 0.2):

```
                ┌─────────────────────────────┐
                │      Conta de utilizador     │
                └──────────────┬──────────────┘
                               │  acumula papéis
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌───────────────┐    ┌───────────────────┐    ┌──────────────┐
│ Cliente Comum │    │  Cliente Provedor │    │ Administrador│
│  (consumidor) │    │  (cede espaço)    │    │  (governança)│
└───────────────┘    └───────────────────┘    └──────────────┘
```

Separar os papéis justifica-se porque o Cliente Provedor tem fluxos que **não
existem** para o consumidor: configurar dispositivo, instalar agente local,
gerir dois espaços em simultâneo e lidar com a indisponibilidade do seu próprio
equipamento.

### 3.2 Os dois espaços unificados

O sistema trata o armazenamento como um conjunto de **nós** homogéneos
(`StorageNode`), de dois tipos (`NodeType`):

| Tipo | Dono | Significado |
|---|---|---|
| `SYSTEM` | `null` (partilhado) | Nuvem oficial — sempre disponível |
| `DEVICE` | utilizador | PC pessoal do Cliente Provedor |

O utilizador vê e gere ambos no mesmo ecrã (RF18/RF19) e escolhe o destino
**no momento do upload** (`UploadDestination`, RF20):

```
Upload ──► destino escolhido
            │
            ├─ AUTO   ──► política padrão do utilizador (RF22)
            │              ex.: "preferir dispositivo pessoal"
            │
            ├─ SYSTEM ──► nuvem oficial (sempre online)
            │
            └─ DEVICE ──► nó pessoal específico (tem de estar online)
```

### 3.3 Fluxo de vinculação de um dispositivo (RF17)

```
┌──────────────┐                        ┌──────────────┐        ┌──────────────┐
│  Frontend    │                        │   Backend    │        │  Agente (PC) │
└──────┬───────┘                        └──────┬───────┘        └──────┬───────┘
       │  POST /nodes/pairing-code             │                       │
       │───────────────────────────────────────►│  código de 15 min    │
       │◄───────────────────────────────────────│  (uso único)         │
       │                                       │                       │
       │  utilizador corre: bash install-node.sh A7K9M2P4               │
       │──────────────────────────────────────────────────────────────►│
       │                                       │                       │
       │            POST /nodes/register       │                       │
       │            { name, localPath,         │                       │
       │              totalSpaceBytes,         │                       │
       │              pairingCode }            │                       │
       │◄──────────────────────────────────────┼───────────────────────┤
       │                                       │                       │
       │                            consome código, cria StorageNode    │
       │                            (type=DEVICE, status=ONLINE)       │
       │                            cifra deviceToken                  │
       │                                       │                       │
       │◄── { nodeID, deviceToken } ───────────┼───────────────────────┤
       │                                       │                       │
       │                                       │   agente guarda token │
       │                                       │   e sobe servidor HTTP│
       │                                       │   local na porta 8081 │
```

**Segurança do pareamento:** o código expira em 15 minutos
(`cloud.node.pairing-ttl-minutes`), é de uso único e é consumido pelo backend.
Nunca é exposto um token longo ao utilizador.

### 3.4 Fluxo de escrita de um ficheiro (replicação)

```
ReplicationService.replicateFile(fileID, owner, containerData, dest, targetNodeId)
    │
    ├─ resolveTargets() ──► política + disponibilidade + espaço livre
    │
    ├─ para cada nó alvo:
    │     ├─ ContainerClient.uploadReplica()
    │     │    ├─ nó SYSTEM ──► escrita local directa (evita chamar-se a si)
    │     │    └─ nó DEVICE ──► HTTP POST /nodes/replicas/{id}
    │     │                      header X-Node-Token (autenticação do nó)
    │     └─ FileReplica.status ──► SYNCED | PENDING | FAILED
    │
    └─ se nenhum nó aceitou ──► NoStorageAvailableException + notificação
```

**Notificação de falha:** se a réplica falha num dispositivo, o utilizador é
notificado (`REPLICA_FAILED`) — não há falha silenciosa.

### 3.5 Gestão honesta da disponibilidade (RF23–RF24)

Um PC pessoal desliga-se. O sistema assume isso em vez de fingir uptime:

| Mecanismo | Implementação |
|---|---|
| Detecção de estado | `HeartbeatMonitorService` → `ONLINE` / `OFFLINE` / `DEGRADED` |
| Comportamento offline | `OfflineBehavior` = `DENY` (negar) ou `CACHE` (cópia em cache) |
| Remoção segura | migrar réplicas **antes** de apagar (`migrateReplicas`, RF17.7) |
| Alertas de quota | notificação ao aproximar-se do limite (RF26) |
| Estado observável | ecrã de sincronização por ficheiro (RF28) |

---

## 4. O diferencial

### 4.1 Comparativo

| Capacidade | Drive / Dropbox / OneDrive | Este sistema |
|---|---|---|
| Ceder espaço do próprio PC como nó | ✗ | ✓ (RF17) |
| Dois espaços geridos em conjunto | ✗ | ✓ (RF18) |
| Escolha de destino no upload | ✗ | ✓ (RF20) |
| Política de armazenamento configurável | ✗ | ✓ (RF22) |
| Comportamento offline explícito | ✗ | ✓ (RF24) |
| Migração de réplicas ao remover nó | ✗ | ✓ (RF17.7) |
| Réplicas em múltiplos nós | parcial (opaco ao utilizador) | ✓ observável (RF28) |
| Versionamento | parcial | ✓ (RF13) |
| Links públicos com password | ✓ | ✓ (RF16) |

### 4.2 Os três argumentos técnicos que sustentam o diferencial

#### A. A cifra é o que torna o modelo possível — não um extra

Sem encriptação, "oferecer 100 GB do meu PC" significaria guardar ficheiros de
estranhos em texto legível no meu disco. Ninguém aceitaria. A arquitectura
resolve isto com **cifra por ficheiro + embrulho de chave**
(`ContainerService`):

```
ficheiro ──► AES-256-GCM ──► container .dbs (opaco)
   │
   └─ chave AES por ficheiro ──► wrapKey(masterKey) ──► FileMetadata.encryptedDataKey
```

- cada ficheiro tem a **sua própria chave AES** (IV único)
- essa chave é **embrulhada** com a master key do servidor
- a master key vive só em `CLOUD_MASTER_KEY`, nunca no repositório

**Consequência prática:** quem cede o disco vê blocos `.dbs` ilegíveis. Quem
tem o servidor, não tem os ficheiros sem a master key. A confiança necessária
ao modelo é mínima.

#### B. Indisponibilidade tratada como requisito, não como falha

A maioria dos projectos de "nuvem distribuída" assume nós sempre online. Aqui, o
PC desligar é **cenário esperado**, com política explícita do lado do
utilizador (`DENY` vs `CACHE`) e migração obrigatória antes de desvincular.

#### C. O nó é uma entidade de primeira classe, não um cliente burro

`StorageNode` tem quota, espaço usado, visibilidade (`PERSONAL`/`PUBLIC`),
partilha com outros utilizadores (`NodeAccess`), versão de agente, SO reportado
e espaço livre — o que permite **múltiplos dispositivos por conta** (RF17.5) e
governança real.

---

## 5. Decisões arquitecturais

### 5.1 Limitação conhecida: alcance de rede e NAT

> **Estado actual — limitação documentada.**

O backend alcança um nó pessoal pelo endereço que o agente **anuncia no
registo** (`StorageNode.sessionId` → `ContainerClient.nodeUrl()`):

```java
private String nodeUrl(StorageNode node) {
    String sessionId = node.getSessionId();
    if (sessionId == null) return "http://localhost";
    return sessionId.startsWith("http") ? sessionId : "http://" + sessionId;
}
```

Isto produz o seguinte comportamento:

| Cenário | Resultado |
|---|---|
| Backend e PC na **mesma rede local** | ✅ funciona |
| PC com **IP público** ou **túnel** (Tailscale, ngrok, Cloudflare Tunnel) | ✅ funciona |
| PC doméstico **atrás de NAT**, sem configuração | ❌ **falha** — o servidor não alcança `192.168.x.x` |

**Impacto:** num deploy real (backend no Render, PCs dos utilizadores em casa),
o caso 3 é o comum. Actualmente o agente pode registar-se e servir réplicas
localmente, mas o backend não as alcança.

### 5.2 Solução proposta: ligação invertida (opção 2)

Em vez de o servidor **puxar** do agente, o **agente puxa** do servidor.

```
HOJE (push do servidor)                PROPOSTO (pull do agente)

   Backend ──────HTTP──────► Agente        Agente ──────HTTP──────► Backend
        ✗ bloqueado por NAT                     ✓ sempre permitido
                                                 (saída 443)
```

**Como funcionaria:**

1. O agente, ao arrancar, abre uma **ligação de saída persistente** ao backend
   (WebSocket/STOMP — a dependência `spring-boot-starter-websocket` já existe no
   projecto) ou faz **long-polling** a `/nodes/{id}/commands`.
2. O backend deixa de chamar `nodeUrl(node)` directamente. Passa a **enfileirar
   comandos** (`UPLOAD_REPLICA`, `DELETE_REPLICA`, `HEALTH`).
3. O agente recolhe o comando, executa localmente e devolve o resultado pelo
   mesmo canal.
4. O heartbeat passa a ser implícito: a ligação aberta **é** o sinal de online.

**Vantagens:**
- funciona atrás de NAT, firewall e IP dinâmico — sem configuração do utilizador
- elimina a necessidade de túneis ou port forwarding
- o `sessionId` deixa de ser um endereço e passa a ser um identificador de canal
- o estado online/offline passa a ser fiável (ligação real, não ping)

**Custos:**
- o agente precisa de manter ligação aberta (reconnect com backoff)
- o backend precisa de um registo de canais/sessões activos
- comandos precisam de ser idempotentes e ter timeout

### 5.3 Alternativas consideradas

| Opção | Mecanismo | Prós | Contras |
|---|---|---|---|
| 1 | Túnel automático no agente (Tailscale/Cloudflare) | mantém modelo actual | dependência externa; configuração no PC do utilizador |
| **2** | **Ligação invertida (agente → servidor)** | **zero configuração; funciona em qualquer rede** | **mais trabalho no backend (sessões, filas)** |
| 3 | Sincronização em lote quando online | simples; tolera offline | réplicas não são imediatas; consistência eventual |

**Recomendação: opção 2.** É a única que resolve o problema do utilizador final
sem lhe pedir conhecimentos de redes, e alinha-se com o que os requisitos
prometem (RF17.3 exige um agente local; RF23/RF24 já assumem dispositivos
intermitentes).

---

## 6. Modelo de segurança

| Camada | Mecanismo |
|---|---|
| Autenticação de utilizador | JWT (HMAC256, `JWT_SECRET` ≥ 32 chars) |
| Autenticação do nó | `deviceToken` cifrado reversivelmente; header `X-Node-Token` |
| Pareamento de dispositivo | código de 15 min, uso único (`PairingCodeService`) |
| Cifra de ficheiros | AES-256-GCM por ficheiro, IV único |
| Protecção de chaves | chave por ficheiro embrulhada com `CLOUD_MASTER_KEY` |
| Autorização | roles (`ADMIN`), quotas, visibilidade do nó, `NodeAccess` |
| Transporte | HTTPS em produção |

**Regra de ouro (documentada no README):** mudar `CLOUD_MASTER_KEY` torna
**ilegíveis** todos os ficheiros já armazenados. A rotação de segredos é uma
operação de migração, não uma configuração.

---

## 7. Riscos e trabalhos futuros

| # | Item | Tipo |
|---|---|---|
| 1 | **Ligação invertida** (§5.2) | bloqueante para uso real em NAT |
| 2 | Documento de arquitectura de dados da replicação | os requisitos pedem-no explicitamente |
| 3 | Testes automáticos de frontend | ausentes (assumido no README) |
| 4 | Rotação segura da master key | hoje é destrutivo por desenho |
| 5 | Auditoria de acessos | RF29/RF30 a consolidar |
| 6 | Política de retenção de réplicas órfãs | ao remover nós offline |

---

## 8. Referências

- `docs/requisitos_sistema_armazenamento_nuvem.pdf` — RF01–RF30, RNF01–RNF07
- `docs/historias_usuario.pdf` — actores, épicos, histórias
- `README.md` — arranque, variáveis de ambiente, segurança
- `docs/OTP_EMAIL_SETUP.md` — configuração de OTP por email
- Código: `service/ReplicationService`, `service/ContainerClient`,
  `service/ContainerService`, `entity/StorageNode`, `entity/FileMetadata`
