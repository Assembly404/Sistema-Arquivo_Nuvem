# Sidebar — Arquitetura de Navegação · Sistema Arquivo Nuvem
### Documento de decisão conceitual · Referência para implementação futura

> **Status:** v1.1 — as **Fases 1 e 2 estão implementadas em código** (ver secção 6); a **Fase 3** (estados de navegação por permissão) permanece em **planejamento/decisão**. Toda a documentação de decisão e de implementação vive neste documento — os arquivos de código **não recebem comentários**.
>
> **Vínculo normativo:** este documento **não substitui** `frontend/src/docs/IDENTIDADE_VISUAL.md` nem `frontend/src/docs/RegrasDeTecnologias.md` — ele **detalha e operacionaliza** o que já está normado lá, usando como base visual os 3 mockups desta mesma pasta (`Screenshot From 2026-09-22 19-17-00.png`, `Screenshot From 2026-09-23 14-14-13.png`, `Screenshot From 2026-09-23 14-14-43.png`).

**Índice**
1. [Fase 1 — Estrutura conceitual de navegação](#fase-1--estrutura-conceitual-de-navegação)
2. [Fase 2 — Estrutura de dados e contrato do componente](#fase-2--estrutura-de-dados-e-contrato-do-componente)
3. [Fase 3 — Estados de navegação por permissão](#fase-3--estados-de-navegação-por-permissão)
4. [Gaps conhecidos / pré-requisitos de backend](#4-gaps-conhecidos--pré-requisitos-de-backend)
5. [Checklist de verificação e roteiro de testes](#5-checklist-de-verificação-e-roteiro-de-testes)
6. [Estado de implementação (Fases 1 e 2)](#6-estado-de-implementação-fases-1-e-2)

---

## Fase 1 — Estrutura conceitual de navegação

### 1.1 Domínio da aplicação (fonte: `backend/database/schema`)

A sidebar deve expressar, em navegação, exatamente as entidades de negócio já modeladas nos scripts SQL do projeto (`001_create_usuario.sql`, `002_create_perfil.sql`, `003_create_permissao.sql`, `004_create_pasta.sql`, `005_create_arquivo.sql`, `006_create_armazenamento.sql`):

| Entidade (schema) | Módulo de navegação correspondente |
|---|---|
| `usuario` | Conta / Perfil do usuário (rodapé da sidebar) |
| `perfil` + `permissao` | Administração (visível apenas a quem tem permissão) |
| `pasta` | Arquivos → Meus Arquivos / navegação em árvore de pastas |
| `arquivo` | Arquivos → listagem / upload |
| `armazenamento` | Armazenamento (quota) — componente de negócio normado na secção 4.9 da Identidade Visual |

Nenhum item de menu deve ser criado fora deste domínio (regra 3 de `RegrasDeTecnologias.md`), exceto os utilitários de UX já presentes nos mockups (Notificações, Suporte, Configurações), aceitáveis por analogia de mercado (regra 5).

### 1.2 Inventário de itens de menu (nível 1 e submenus)

```
Sidebar
├─ Início / Dashboard            (rota: /dashboard)
├─ Arquivos                      (agrupador, sem rota própria)      [submenu]
│   ├─ Meus Arquivos             (/dashboard/arquivos)
│   ├─ Compartilhados comigo     (/dashboard/arquivos/compartilhados)
│   └─ Lixeira                   (/dashboard/arquivos/lixeira)
├─ Armazenamento                 (/dashboard/armazenamento)          [+ barra de quota, secção 4.9]
├─ Administração                 (agrupador)                         [submenu · restrito]
│   ├─ Usuários                  (/dashboard/admin/usuarios)
│   └─ Perfis & Permissões       (/dashboard/admin/permissoes)
├─ Notificações                  (/dashboard/notificacoes)           [+ badge opcional]
├─ Suporte                       (/dashboard/suporte)
└─ Configurações                 (/dashboard/configuracoes)
—— rodapé (fora do <nav>) ——
└─ Cartão de conta (avatar, nome, email) → menu com Perfil / Terminar sessão
```

- Nível máximo de aninhamento: **2 níveis** (item → submenu). Nenhum dos 3 mockups usa 3+ níveis; manter essa profundidade evita complexidade de UX e respeita a regra 3.1 da Identidade Visual (hierarquia por tamanho > peso > cor, não por profundidade infinita).
- "Administração" só existe no DOM para perfis com permissão administrativa (ver Fase 3).

### 1.3 Decisões arquiteturais de alto nível

| # | Decisão | Justificativa (regra de origem) |
|---|---|---|
| D1 | A sidebar é **exclusiva da área autenticada** (`/dashboard/*`). A `Navbar` glass (`components/header/Navbar.jsx`) permanece apenas na área pública, e **nunca** coexiste com a sidebar na mesma tela. | Evita dois sistemas de navegação concorrentes. Secção 5.5 da Identidade Visual já separa "Landing Page vs Dashboard". |
| D2 | O layout autenticado passa a ser um **`DashboardLayout`** (a criar) que renderiza `<SideBar />` fixa + `<main>` com o conteúdo da rota via `Outlet` do `react-router`. | Hoje `Dashboard.jsx` é uma página solta; se cada página futura importar a sidebar individualmente, duplica-se markup e divergem os estados de colapso. |
| D3 | Dois modos visuais: **expandido** (ícone + label, 260px — valor já normado na secção 4.6) e **colapsado** (ícone-only, ~72px, submenu em *flyout* lateral). | Replica o comportamento visível nos mockups 2 e 3, sem introduzir largura nova fora da spec. |
| D4 | A largura expandida usa **exatamente 260px**, valor literal já normado em `.sidebar` (secção 4.6); o modo colapsado usa um valor derivado (72px = `var(--space-16)` + `var(--space-1)`), a validar na implementação. | Regra: não inventar medidas fora da escala 4px (`--space-*`) nem tokens não documentados. |
| D5 | A navegação é **data-driven**: um único array de configuração alimenta a renderização. | Permite filtrar por permissão sem duplicar JSX e mantém a sidebar alinhada ao schema. |

### 1.4 Conflitos de navegação identificados e resolução

| # | Conflito | Resolução adotada |
|---|---|---|
| C1 | Navbar pública vs. Sidebar autenticada disputando o mesmo espaço/rota | Sidebar só monta dentro do `DashboardLayout`; o roteamento decide qual layout usar. |
| C2 | Item pai deve parecer "ativo" quando um filho de submenu está selecionado | Ativação em cascata: `aria-current="page"` (estado já normado em 4.6) no item folha; o item pai recebe estado "ativo por filho" quando `location.pathname` casa com algum filho. |
| C3 | Sidebar colapsada não tem espaço para expandir a árvore de submenu | Submenu vira **flyout flutuante** ancorado ao item (`position: fixed`, `z-index: var(--z-dropdown)`), replicando os mockups 2 e 3 — nunca empurra o layout. |
| C4 | Telas pequenas: sidebar fixa consome espaço útil | Abaixo de **992px** (breakpoint já exigido no checklist §6 da Identidade Visual: 600/768/992/1200px), a sidebar vira *overlay* off-canvas, fechada por padrão, acionada por botão hamburger; o conteúdo recebe `--color-overlay` atrás. |
| C5 | Item sem permissão pode "vazar" visualmente antes do carregamento assíncrono de permissões | A sidebar só renderiza a lista **depois** que o contexto resolve perfil/permissões (estado `loadingPermissions`, ver 2.5). Nunca renderizar a lista completa e depois remover itens. |
| C6 | Rodapé (conta/logout) disputando foco com a navegação principal | Rodapé tratado como região semântica separada (`<footer>` dentro do `<aside>`), fora do `<nav><ul>`, evitando ordem de tab confusa. |
| C7 | Rota ativa dessincronizada da URL (estado duplicado) | `activePath` é **sempre derivado** de `useLocation()`; nunca guardado em `useState` manual. |

---

## Fase 2 — Estrutura de dados e contrato do componente

### 2.1 Formato do "menu tree" (fonte única da verdade da navegação)

Arquivo futuro sugerido: `frontend/src/components/header/sidebarConfig.js` (**não criado nesta fase** — apenas especificado aqui).

```js
// Forma conceitual de cada nó do menu (pseudo-código de referência)
{
  id: "arquivos",                    // string única — usada em keys e no estado de submenu aberto
  label: "Arquivos",                 // texto exibido (pt-BR, domínio da aplicação)
  path: null,                        // rota associada; null se for apenas agrupador
  icon: "FiFolder",                  // nome do ícone exportado por react-icons (v5.7.0, já instalado)
  permission: "ARQUIVO_VISUALIZAR",  // chave que casa com a tabela `permissao`
  badge: null,                       // número/label opcional (ex.: contagem de notificações)
  visibility: "public",              // "public" | "sensitive" | "conditional" | "secret" (ver Fase 3)
  children: [ /* nós filhos — máx. 1 nível (ver 1.2) */ ]
}
```

- **Ícones:** usar `react-icons` v5.7.0 (dependência confirmada em `frontend/package.json`). O conjunto Feather (`Fi*`) é o que melhor casa com o traço fino dos mockups; Font Awesome já instalado é alternativa aceitável se o ícone específico não existir em Feather. Tamanho sempre via `.icon` (`1.25rem`, regra 3.2).
- **Labels:** português, curtos (≈20 caracteres) para não quebrar em 260px.
- **`permission`:** string que referencia `permissao` (tabela do schema). O frontend **não inventa** nomes de permissão — eles espelham o que o backend define.
- **`additionalInfo` (à implementar):** campos auxiliares possíveis sem impacto na árvore, ex.: `tooltip` (motivo do `disabled`) e `sensitive: true` (ação destrutiva → modal de confirmação, secção 5.1).

### 2.2 Estrutura completa traduzida para dados

```js
export const SIDEBAR_MENU = [
  { id: "dashboard", label: "Início", path: "/dashboard", icon: "FiHome",
    permission: null, visibility: "public", children: [] },

  { id: "arquivos", label: "Arquivos", path: null, icon: "FiFolder",
    permission: "ARQUIVO_VISUALIZAR", visibility: "public", children: [
      { id: "arquivos-meus", label: "Meus Arquivos", path: "/dashboard/arquivos",
        icon: "FiFile", permission: "ARQUIVO_VISUALIZAR", visibility: "public", children: [] },
      { id: "arquivos-compartilhados", label: "Compartilhados comigo",
        path: "/dashboard/arquivos/compartilhados", icon: "FiShare2",
        permission: "ARQUIVO_VISUALIZAR", visibility: "public", children: [] },
      { id: "arquivos-lixeira", label: "Lixeira", path: "/dashboard/arquivos/lixeira",
        icon: "FiTrash2", permission: "ARQUIVO_EXCLUIR",
        visibility: "conditional",                      // desabilitado se sem permissão
        tooltip: "O seu perfil não permite excluir ficheiros.", children: [] }
    ] },

  { id: "armazenamento", label: "Armazenamento", path: "/dashboard/armazenamento",
    icon: "FiHardDrive", permission: "ARMAZENAMENTO_VISUALIZAR",
    visibility: "public", children: [] },

  { id: "admin", label: "Administração", path: null, icon: "FiShield",
    permission: "ADMIN_ACESSAR", visibility: "secret", children: [
      { id: "admin-usuarios", label: "Usuários", path: "/dashboard/admin/usuarios",
        icon: "FiUsers", permission: "ADMIN_USUARIOS", visibility: "secret", children: [] },
      { id: "admin-permissoes", label: "Perfis & Permissões", path: "/dashboard/admin/permissoes",
        icon: "FiKey", permission: "ADMIN_PERMISSOES", visibility: "secret", children: [] }
    ] },

  { id: "notificacoes", label: "Notificações", path: "/dashboard/notificacoes",
    icon: "FiBell", permission: null, visibility: "public", badge: "count", children: [] },
  { id: "suporte", label: "Suporte", path: "/dashboard/suporte",
    icon: "FiHelpCircle", permission: null, visibility: "public", children: [] },
  { id: "configuracoes", label: "Configurações", path: "/dashboard/configuracoes",
    icon: "FiSettings", permission: null, visibility: "public", children: [] }
]
```

> Os nomes de permissão acima são **provisórios** (definidos por convenção `MÓDULO_ACÇÃO`). Os valores definitivos devem vir de `003_create_permissao.sql` / dos perfis reais — ver secção 4.
>
> Nota de implementação (Fase 2): `badge` ficou `null` em **todos** os nós, porque não existe contagem real de notificações no backend; o componente já renderiza `.sidebar-badge` quando o valor for número ou label (ver 6.2).

### 2.3 Contrato de props/componentes (para a fase de implementação)

```
<SideBar
  menu={SIDEBAR_MENU}            // array já filtrado por permissão (ver 3.2)
  collapsed={boolean}            // estado de colapso controlado pelo DashboardLayout
  onToggleCollapse={fn}          // alterna collapsed
  activePath={string}            // derivado de useLocation().pathname
  user={{ name, email }}         // dados do rodapé (de AuthContext)
  onLogout={fn}
/>

<SidebarItem
  item={menuNode}                // um nó de SIDEBAR_MENU
  collapsed={boolean}
  isActive={boolean}             // aria-current="page" (secção 4.6)
  isParentActive={boolean}       // true se algum filho estiver ativo (conflito C2)
  disabled={boolean}             // true quando visibility === "conditional" sem permissão
  disabledReason={string}        // texto do tooltip (secção 5.1)
/>
```

- O componente **não** decide permissões; ele recebe o `menu` já filtrado e apenas renderiza. Isso separa responsabilidade (dados/permissão × apresentação).
- Nomes de arquivo seguem a convenção já existente do projeto (`components/header/sidebar.jsx`, `sidebar.css`), evitando criar novas pastas.
- **Ajuste de implementação:** `SidebarItem` recebe ainda `activeId`, `isSubmenuOpen` e `onToggleSubmenu` (o estado de submenus abertos pertence ao `SideBar`, conforme 2.5), além de ler `item.disabled` / `item.disabledReason` quando existirem — o que permite à Fase 3 injetar o estado `conditional` sem alterar `sidebar.jsx` (ver 6.2).

### 2.4 Proteção de rotas (integração com `react-router` v8 e `AuthContext`)

- **`RequireAuth`** (a criar): wrapper que envolve as rotas `/dashboard/*` em `App.jsx`, redirecionando para `/login` quando não houver `auth.token`. O `AuthContext` atual já expõe `{ auth, setAuth }` com `token` — suficiente para este nível.
- **`RequirePermission(permission)`** (a criar): decide se a rota renderiza a página ou redireciona para uma página de acesso negado. Usa a **mesma** fonte de verdade (`permission`) que filtra o menu — evita divergência entre "o que a sidebar mostra" e "o que a rota permite".
- **Regra de segurança:** esconder o item de menu **não** é proteção. O bloqueio real tem de estar no wrapper de rota (defesa em profundidade), pois a URL pode ser digitada manualmente — ver checklist da secção 5.
- **Rotas aninhadas:** `/dashboard` passa a `<Route path="/dashboard" element={<RequireAuth><DashboardLayout/></RequireAuth>}>` com filhas (`arquivos`, `armazenamento`, `admin/usuarios`, …) e `<Outlet/>` no layout.

### 2.5 Estados internos do componente `SideBar`

| Estado | Tipo | Persistência | Observação |
|---|---|---|---|
| `collapsed` | boolean | `localStorage` (chave `sidebar:collapsed`) | Preferência do utilizador entre sessões; não é dado de negócio. |
| `openSubmenus` | `Set<id>` / objeto | apenas em memória | Reinicia ao recarregar. Em modo colapsado vira *hover/focus open* (flyout), não clique persistente. |
| `activePath` | string | derivado de `useLocation()` | Nunca `useState` manual (conflito C7). |
| `loadingPermissions` | boolean | apenas em memória | Controla o gate do conflito C5: enquanto `true`, exibir apenas `Spinner` (já existe em `components/utils/spinner.jsx`), nunca a lista completa. |

---

## Fase 3 — Estados de navegação por permissão

### 3.1 Reaproveitamento da matriz normativa (§5.1 da Identidade Visual)

| Estado (`visibility` no nó) | Regra de renderização | Elemento visual |
|---|---|---|
| **`public`** (Permitido) | renderizado normalmente quando autenticado | `.sidebar-item` nos estados completos |
| **`sensitive`** (Permitido mas sensível) | renderizado normalmente; a ação destrutiva exige confirmação | `.sidebar-item` normal + modal (§4.10) ao acionar |
| **`conditional`** (Não permitido condicional) | renderizado no DOM, porém desabilitado, com motivo | `.sidebar-item[aria-disabled="true"]` (§4.6) + tooltip |
| **`secret`** (Não permitido secreto) | **removido** do array por `filterMenuByPermissions` — nunca chega ao DOM | item inexistente na árvore renderizada |

> Esta tabela **não cria regra nova**: é a secção 5.1 do `IDENTIDADE_VISUAL.md` aplicada item a item ao menu de 2.2. A Identidade Visual é explícita: "a sidebar/menu **nunca** mostra items que o perfil não pode aceder" e "itens desativados usam `aria-disabled="true"`".

> O valor `sensitive` está previsto no enum de `visibility` (2.1) para completude da matriz, mas **nenhum item do menu de 2.2 o utiliza atualmente** — a confirmação de ações destrutivas aplica-se sobretudo a botões dentro das páginas (§5.4), não a itens de navegação.

### 3.2 Onde a filtragem acontece (decisão arquitetural)

- **Decisão:** a filtragem ocorre numa **função pura** `filterMenuByPermissions(menu, userPermissions)` executada **antes do render** — nunca via CSS (`display: none`), garantindo que itens `secret` não entrem no DOM.
- Itens `conditional` **permanecem** no array retornado, marcados com `disabled: true` + `disabledReason`.
- A função é recursiva (percorre `children`), preservando a ordem de 2.2 e devolvendo um novo array (sem mutar a configuração original).
- Consome `userPermissions` (lista de strings) — que hoje **não existe** no `AuthContext` (ver Gap 4.3).

### 3.3 Matriz de perfis × itens de menu (modelo conceitual)

| Item de menu | Perfil "Utilizador comum" | Perfil "Administrador" |
|---|---|---|
| Início (`/dashboard`) | público | público |
| Arquivos → Meus Arquivos | público | público |
| Arquivos → Compartilhados | público | público |
| Arquivos → Lixeira | **conditional** (depende de `ARQUIVO_EXCLUIR`) | público |
| Armazenamento | público | público |
| Administração (pai + submenus) | **secret** (não aparece) | público |
| Notificações / Suporte / Configurações | público | público |

> Os perfis reais e os nomes exatos das permissões devem vir de `002_create_perfil.sql` e `003_create_permissao.sql`. Esta matriz é o **modelo conceitual**; os valores concretos (`id_perfil`, `permissao.nome`) serão amarrados quando o backend expuser esses dados (secção 4).

### 3.4 Casos de teste conceituais (roteiro para quando o componente existir)

| # | Cenário | Resultado esperado |
|---|---|---|
| T1 | Utilizador sem token acede a `/dashboard` | Não monta sidebar; redirecionado para `/login` por `RequireAuth`. |
| T2 | Utilizador comum autenticado | Nenhum nó com `id` iniciado em `admin` existe no DOM — verificar com `queryByText`/`queryByRole` (nunca `getByText`, pois o elemento não deve existir). |
| T3 | Utilizador sem `ARQUIVO_EXCLUIR` | Item "Lixeira" **presente** no DOM, com `aria-disabled="true"` e tooltip com o motivo (`pointer-events: none` já normado em §4.6). |
| T4 | Utilizador administrador | Todos os itens presentes e habilitados. |
| T5 | Navegação para um filho de submenu | `activePath` acompanha a URL; item pai reflete estado "ativo por filho" (conflito C2). |
| T6 | Sidebar colapsada | Itens acessíveis via flyout; nenhum item perde `path` nem `permission` associada. |
| T7 | Viewport < 992px | Sidebar assume modo overlay fechado; abrir/fechar não altera a preferência `collapsed` guardada em desktop. |
| T8 | `loadingPermissions === true` | Nenhum "flash" de itens restritos antes do filtro aplicar (testar com throttling de rede). |
| T9 | Acesso direto por URL a rota restrita (`/dashboard/admin/usuarios`) sem permissão | `RequirePermission` bloqueia (página de acesso negado / redirect) — não basta o item estar escondido no menu. |
| T10 | Clicar em "Terminar sessão" no rodapé | Sessão limpa no `AuthContext` e utilizador reencaminhado para a área pública. |

---

## 4. Gaps conhecidos / pré-requisitos de backend

Estes pontos **bloqueiam** a implementação real da Fase 3 (não impedem que as Fases 1 e 2 guiem a estrutura desde já):

1. **O login não devolve perfil nem permissões.** `AuthController.login()` retorna apenas `new LoginResponse(token)`, e `LoginResponse` é um record com um único campo `String token`. Para a sidebar filtrar por permissão real, o backend precisa expor a lista de permissões do utilizador — via claims no JWT gerado por `TokenConfig` e/ou endpoint próprio (ex.: `GET /auth/me`).
2. **`User.getAuthorities()` devolve `List.of()`** (`entity/User.java`), ou seja, nenhuma autoridade/role é carregada. Além disso, o backend **não tem entidades JPA** para `perfil`, `permissao`, `pasta`, `arquivo` e `armazenamento` — só existem os scripts SQL em `backend/database/schema/`. É necessário criar o mapeamento `User` → `Perfil` → `Permissao` antes de qualquer RBAC real.
3. **O `AuthContext` (`context/AuthProvider.jsx`) guarda apenas `{ auth, setAuth }` genérico**, e `Login.jsx` escreve `setAuth({ email, password, token })` — incluindo a **password em memória do cliente**. Antes de implementar `filterMenuByPermissions`, o contexto precisa passar a expor explicitamente algo como `{ user, permissions, token }` (e a password deve deixar de ser armazenada em estado — boa prática de segurança, a tratar fora do escopo deste documento).
4. **Só existe a rota `/dashboard`.** `App.jsx` declara uma única rota de dashboard, sem rotas aninhadas nem qualquer proteção. Toda a árvore de rotas da Fase 1/2 (`/dashboard/arquivos`, `/dashboard/admin/usuarios`, …) precisa de ser criada na fase de implementação com rotas aninhadas do `react-router` + `<Outlet/>`.
5. **Sem infraestrutura de testes no frontend.** `frontend/package.json` não tem framework de testes (nem Vitest, nem Jest, nem Testing Library) e `vite.config.js` não configura nenhum. Os casos da secção 3.4 são, por isso, um **roteiro de verificação manual** até que uma stack de testes seja adicionada (decisão separada, fora deste escopo).
6. **Dependência `react-icon` (singular, `^1.0.0`) presente mas não usada** — não aparece em nenhum `import` do código. A biblioteca de ícones efetivamente disponível é `react-icons@^5.7.0` (e o Font Awesome `@fortawesome/*`). Recomenda-se limpeza da dependência residual numa tarefa própria.

> Nenhuma alteração de backend, `App.jsx` ou `package.json` foi feita nesta fase — os itens acima são apenas pré-requisitos identificados.

---

## 5. Checklist de verificação e roteiro de testes

Antes de considerar a implementação da sidebar concluída, confirmar **todos** os pontos (em complemento ao checklist §6 da Identidade Visual):

- [ ] O menu em `sidebarConfig.js` corresponde 1:1 ao inventário da secção 1.2 (nomes, rotas e ícones).
- [ ] Nenhum item com `visibility: "secret"` chega ao DOM para perfis sem permissão (T2).
- [ ] Itens `conditional` aparecem desabilitados com `aria-disabled="true"` + tooltip, **nunca** escondidos (T3).
- [ ] `activePath` é sempre derivado de `useLocation()`, nunca de estado duplicado (C7).
- [ ] Item pai reflete visualmente o filho ativo (T5 / C2).
- [ ] Sidebar colapsada exibe flyout de submenu sem quebrar o layout, replicando os mockups 2 e 3 (T6 / C3).
- [ ] Abaixo de 992px a sidebar vira overlay off-canvas, coerente com os breakpoints 600/768/992/1200px (T7 / C4).
- [ ] Zero valores hard-coded: apenas `var(--*)` da Identidade Visual, reaproveitando `.sidebar` e `.sidebar-item` da secção 4.6.
- [ ] Estados **normal, hover, active, disabled e focus-visible** presentes, conforme exigência do checklist §6 da Identidade Visual (loading incluído, por ser assíncrono).
- [ ] Alvo de clique ≥ 44px e `cursor: pointer` nos itens clicáveis.
- [ ] `RequireAuth` e `RequirePermission` bloqueiam acesso por URL direta, não apenas escondem o item (T9).
- [ ] Estado `loadingPermissions` impede qualquer "flash" de itens restritos (T8 / C5).
- [ ] Preferência de colapso persiste em `localStorage`; estado de submenus abertos **não** persiste (2.5).
- [ ] Rodapé de conta está fora do `<nav>` e `Esc`/tab order comportam-se corretamente (C6).

## 6. Estado de implementação (Fases 1 e 2)

> Secção de acompanhamento: registra o que foi implementado, os **desvios** face ao planejado, a verificação executada e o que fica bloqueado na Fase 3.

### 6.1 Arquivos

| Arquivo | Estado | Conteúdo |
|---|---|---|
| `frontend/src/components/header/sidebarConfig.js` | **novo** | `SIDEBAR_MENU` (árvore de 2.2), `SIDEBAR_ACCOUNT_MENU`, `SIDEBAR_ICONS` (Feather v5.7), `SIDEBAR_STORAGE_KEY` |
| `frontend/src/components/header/sidebar.jsx` | implementado | `SideBar` (default) + `SidebarItem` (export nomeado), estados de 2.5, conflitos C2/C3/C4/C6/C7 |
| `frontend/src/components/header/sidebar.css` | implementado | `.sidebar`/`.sidebar-item` de 4.6 + flyout, tooltip, overlay, rodapé e `.dropdown-menu`/`.dropdown-item` de 4.7 |
| `frontend/src/pages/dashboard/DashboardLayout.jsx` | **novo** | D2: `<SideBar/>` + `<main>` com `<Outlet/>`; preferência `collapsed` em `localStorage` |
| `frontend/src/pages/dashboard/DashboardLayout.css` | **novo** | offset do conteúdo conforme modo colapsado + shell `.dashboard-page` |
| `frontend/src/pages/dashboard/Placeholder.jsx` | **novo** | página mínima das rotas ainda sem conteúdo |
| `frontend/src/pages/dashboard/Dashboard.jsx` | alterado | passa a renderizar o título de página ("Início"), conforme 3.6 |
| `frontend/src/components/utils/RequireAuth.jsx` | **novo** | 2.4: sem `auth.token` → redireciona para `/login` |
| `frontend/src/App.jsx` | alterado | rotas aninhadas de 2.4 (`/dashboard` + 11 filhas) |

Nenhuma pasta nova foi criada, conforme decidido em 2.3.

### 6.2 Decisões de implementação e desvios face ao planejamento

| # | Ponto | O que foi feito |
|---|---|---|
| 1 | C3 (flyout em modo colapsado) | Implementado com `position: absolute` ancorado ao `<li>` (`.sidebar-item-group`), abertura por `:hover`/`:focus-within` e uma "ponte" invisível (`::before`) que cobre o intervalo até o flyout. `position: fixed` (previsto em C3) só passa a ser necessário se o `<nav>` ganhar scroll interno: a sidebar é `fixed` e não rola, logo o resultado visual é equivalente (pendência 6.4.4). |
| 2 | D4 (largura colapsada) | A aritmética do documento estava errada (`--space-16` + `--space-1` = 68px, não 72px). A implementação usa `calc(var(--space-16) + var(--space-2))` = **72px**, como pretendido em D4. |
| 3 | Alvo de clique ≥ 44px | Não existe token de 44px; usado `calc(var(--space-10) + var(--space-1))` (= 44px) em `.sidebar-item`, botão de colapso, cartão de conta e itens do menu de conta. |
| 4 | Expansão inicial de submenu | O agrupador do item ativo é aberto no primeiro render (estado inicial de `openSubmenus` = ancestrais do item ativo), para que T5/C2 seja visível ao entrar diretamente numa rota filha. Continua **em memória** e não persiste (2.5). |
| 5 | `badge` | Mantido `null` em todos os nós: não existe contagem real de notificações no backend (secção 4). O componente já renderiza `.sidebar-badge` quando o valor for número/label. |
| 6 | Rota `/dashboard/perfil` | Criada porque o rodapé de 1.2 prevê "Perfil" no menu de conta; não constava da coluna de rotas do inventário. |
| 7 | Estado desabilitado | `SidebarItem` já renderiza `aria-disabled="true"`/`disabled` + motivo no `title`, lendo `item.disabled` / `item.disabledReason` do nó — mas **nenhum nó é desabilitado até a Fase 3**, que é quem decide permissão (6.4.1). Itens desabilitados saem como `<span>` (bloqueio real de clique e de teclado, não apenas visual). |
| 8 | `.dropdown-item` | Acrescentado `display: flex; gap` para alinhar ícone + texto (aditivo à spec 4.7, sem alterar cores/raios). |
| 9 | `RequirePermission` | **Não implementado** — pertence à Fase 3 (depende da fonte de permissões). |
| 10 | Campo de busca dos mockups 1 e 2 | **Não implementado**: não consta do inventário de 1.2. Decisão de produto pendente (6.4.3). |

### 6.3 Verificação executada

| Verificação | Resultado |
|---|---|
| `npm run build` | ✅ build de produção sem erros |
| `npm run lint` (arquivos novos/alterados) | ✅ 0 erros nos arquivos desta implementação (os 5 erros restantes do projeto são pré-existentes: `input.jsx`, `toast.jsx`, `Home.jsx`) |
| Render de `SideBar` (SSR com script descartável, removido após uso) | ✅ 31/31 checagens: inventário 1:1, `aria-current` único por rota, item pai ativo por filho (C2), submenu só no DOM quando aberto, flyout em modo colapsado, rodapé fora do `<nav>` (C6), nenhum item ativo em rota desconhecida |
| Inventário da secção 1.2 × `sidebarConfig.js` × rotas de `App.jsx` | ✅ correspondência total (0 rotas em falta; `/dashboard/perfil` é a adição documentada em 6.2) |

Itens do checklist §5 já cobertos pelas Fases 1–2: menu 1:1 com o inventário; `activePath` derivado de `useLocation()` (sem estado duplicado); item pai reflete o filho ativo; flyout na sidebar colapsada; off-canvas abaixo de 992px; uso exclusivo de `var(--*)`; estados normal/hover/active/disabled/focus-visible presentes; alvo ≥ 44px; persistência de `collapsed` em `localStorage`; rodapé de conta fora do `<nav>`. Continuam **abertos** (dependem da Fase 3): T2, T3, T8, T9, `loadingPermissions` e o bloqueio de rotas por permissão.

### 6.4 Pendências

1. **Fase 3 (bloqueada por backend):** `filterMenuByPermissions`, `RequirePermission`, `loadingPermissions` e a desabilitação por `visibility: "conditional"` exigem que o login/JWT exponham perfil e permissões (Gap 4.1) e que o `AuthContext` passe a `{ user, permissions, token }` (Gap 4.3). Enquanto isso, **"Administração" está visível para qualquer sessão autenticada** — é o comportamento esperado e temporário das Fases 1–2, não uma falha.
2. **Nome do utilizador no rodapé:** o login devolve apenas `token`; o rodapé usa o prefixo do email como nome (fallback) até o backend expor o nome real.
3. **Campo de busca dos mockups:** decidir se entra no inventário (e em que secção da Identidade Visual passa a ser normado).
4. **Scroll interno da sidebar:** o menu atual cabe em viewports ≥ ~600px de altura; se o menu crescer, o `<nav>` precisa de scroll próprio e o flyout deve migrar para `position: fixed` com posicionamento por JS, como C3 previa.
5. **`badge` de Notificações:** depende de contagem real vinda do backend.
6. **Limpeza e infraestrutura:** a dependência `react-icon@^1.0.0` continua instalada e sem uso, e o projeto segue sem stack de testes no frontend (Gaps 4.5 e 4.6) — os casos T1–T10 continuam manuais.
7. **Roteiro manual de verificação:** `npm run dev` → `/login` → entrar → `/dashboard`. Conferir: navegação nos itens e submenus; item pai destacado ao entrar em `Arquivos → Compartilhados comigo`; colapso pelo botão do topo da sidebar (a preferência fica em `localStorage`, chave `sidebar:collapsed`); janela abaixo de 992px (sidebar off-canvas com overlay, `Esc`/clique no backdrop fecham); menu de conta no rodapé ("Perfil" e "Terminar sessão", que limpa a sessão e volta à área pública).

---

*Documento v1.1 — consolidado a partir de `IDENTIDADE_VISUAL.md` (secções 4.6, 4.9, 4.10, 5.1, 5.4, 5.5, 6), `RegrasDeTecnologias.md`, dos scripts de schema (`001_create_usuario.sql` … `006_create_armazenamento.sql`), do código atual (`AuthController.java`, `LoginResponse.java`, `User.java`, `AuthProvider.jsx`, `Login.jsx`, `App.jsx`, `sidebar.jsx`, `spinner.jsx`, `package.json`, `vite.config.js`) e dos 3 mockups desta pasta. É a referência normativa da implementação do componente `SideBar` — as **Fases 1 e 2 já estão em código** (secção 6) e a Fase 3 permanece em decisão. Alterações a este documento devem manter a coerência com o `IDENTIDADE_VISUAL.md`.*
