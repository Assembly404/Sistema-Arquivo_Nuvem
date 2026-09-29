# IDENTIDADE VISUAL — Sistema Arquivo Nuvem
### Documento normativo de UI/UX · OBRIGATÓRIO para todos os desenvolvedores

> **Status:** REGRA ESTABLECIDA. Todo componente visual novo ou modificado **deve** seguir este documento. Qualquer PR que viole uma regra aqui escrita será rejeitado na revisão.
>
> **Versão:** **v1.2** (2026-09-29). Os snippets §4.1–§4.4 foram sincronizados com o código real; as telas de autenticação (§4.11) e o fluxo de reposição de password (§5.6) passaram a ter especificação própria. Ver histórico no fim do documento.
>
> **Como usar:** este ficheiro é auto-suficiente — basta copiar/colar. Não é permitido "pensar no estilo" ao construir: os tokens, estados e specs já estão definidos.

**Índice**
1. [Tokens da entidade visual (CSS pronto)](#1-tokens-da-entidade-visual)
2. [Tipografia](#2-tipografia)
3. [As 7 regras visuais obrigatórias](#3-as-7-regras-visuais-obrigatórias)
4. [Specs de componentes (CSS pronto, todos os estados)](#4-specs-de-componentes)
5. [Regras de negócio aplicadas à UI](#5-regras-de-negócio-aplicadas-à-ui)
6. [Checklist de revisão obrigatória](#6-checklist-de-revisão-obrigatória)

---

## 1. Tokens da entidade visual

**Copie o bloco abaixo para o ficheiro global de estilos.** Localização real (v1.2): `frontend/src/App.css` (importado por `App.jsx`), **não** em `index.css` — o `index.css` só tem o reset mínimo (`* { margin: 0; padding: 0; box-sizing: border-box }`) e é carregado antes dos tokens. Todos os valores vêm consolidados das paletas existentes (`navbar.css`, `button.css`, `Login.css`, `toast.jsx`): **marrom/dourado = identidade da marca**, **ciano = acento de sistema**, cinzas neutros = superfícies.

```css
/* ============================================================
   DESIGN TOKENS — Sistema Arquivo Nuvem (fonte única da verdade)
   PROIBIDO usar cores/fontes/raios/sombras hard-coded fora daqui.
   ============================================================ */
  
@import url('https://fonts.googleapis.com/css2?family=Montserrat:ital,wght@0,100..900;1,100..900&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Rubik:ital,wght@0,300..900;1,300..900&display=swap');

:root {
  /* ---- 1.1 Cores de marca (primárias) ---- */
  --color-primary:        #8b6f47; /* botões, links ativos, foco da marca */
  --color-primary-hover:  #6f5738; /* hover */
  --color-primary-active: #5a462c; /* active/pressed (mais escuro que o hover) */
  --color-primary-subtle: #e2ba8f; /* secundária: destaques suaves, ícones */
  --color-primary-tint:   #f5ede2; /* fundo suave de área de marca */

  /* ---- 1.2 Acento de sistema (upload/nuvem/atividade) ---- */
  --color-accent:         rgb(58, 215, 226);
  --color-accent-hover:   rgb(40, 180, 190);
  --color-accent-active:  rgb(30, 150, 160);
  --color-accent-ink:     rgb(22, 81, 85);   /* texto sobre acento claro */
  /* Nota (v1.2): o acento é EXCLUSIVO de ações de sistema (upload/nuvem/atividade).
     PROIBIDO usá-lo nas telas de autenticação (Login/Register/Redefinição) — ali
     manda só a marca (--color-primary*). Ver §4.11 e §5.6. */

  /* ---- 1.3 Neutros e superfícies ---- */
  --color-ink:        #2b2420;  /* texto principal e títulos */
  --color-ink-strong: #1a1512;  /* texto de maior contraste */
  --color-ink-muted:  #78716c;  /* texto secundário, subtítulos */
  --color-bg:         #faf7f2;  /* fundo de app/cards (claro) */
  --color-surface:    #ffffff;  /* cards, modais, superfícies elevadas */
  --color-surface-2:  hsl(0, 0%, 91%); /* fundo de painel/área neutra */
  --color-border:     #e8e8e8;  /* bordas padrão */
  --color-border-strong: #d6d3d1; /* bordas de input em repouso */
  --color-glass:      rgba(177, 177, 177, 0.55); /* superfície glass (navbar) */
  --color-overlay:    rgba(26, 21, 18, 0.55);     /* backdrop de modal */

  /* ---- 1.4 Estado semântico (feedback) — alinhado ao Toast ---- */
  --color-success:        #047857; --color-success-bg: #7dffbc;
  --color-info:           #1e3a8a; --color-info-bg:    #7eb8ff;
  --color-warning:        #78350f; --color-warning-bg: #ffe57e;
  --color-danger:         #7f1d1d; --color-danger-bg:  #ff7e7e;
  --color-danger-action:  rgb(255, 0, 34); /* borda/ícone de erro em input */
  --color-focus-ring:     #8b6f47;         /* anel de foco por teclado */

  /* ---- 1.5 Estados de interação (obliteradores de cor) ---- */
  --color-disabled-bg:    #e5e5e5;
  --color-disabled-ink:   #a8a29e;
  --color-disabled-border:#d6d3d1;

  /* ---- 1.6 Espaçamento (escala 4px — usar SEMPRE destes valores) ---- */
  --space-1: 4px;  --space-2: 8px;   --space-3: 12px; --space-4: 16px;
  --space-5: 20px; --space-6: 24px;  --space-8: 32px; --space-10: 40px;
  --space-12: 48px; --space-16: 64px;

  /* ---- 1.7 Raios (não inventar outros) ---- */
  --radius-sm: 6px;   /* toasts, chips, ícones */
  --radius-md: 10px;  /* cards, inputs, painéis */
  --radius-lg: 15px;  /* botões */
  --radius-xl: 20px;  /* botões pill (navbar) */
  --radius-glass: 30px; /* superfícies glass (navbar) */
  --radius-full: 999px;

  /* ---- 1.8 Sombras ---- */
  --shadow-sm: 0 6px 6px rgba(0, 0, 0, 0.18);
  --shadow-md: 4px 8px 19px -3px rgba(0, 0, 0, 0.27);
  --shadow-lg: 0 7px 50px rgba(214, 223, 213, 0.9);
  --shadow-toast: rgba(111, 111, 111, 0.2) 0 8px 24px;

  /* ---- 1.9 Elevação/z-index ---- */
  --z-base: 0; --z-dropdown: 100; --z-sticky: 500;
  --z-navbar: 1000; --z-toast: 1100; --z-modal: 1200;
  /* Nota (v1.1): --z-dropdown (100) é a camada de um dropdown que vive DENTRO do stacking
     context do próprio componente. Uma sobreposição portada para fora da árvore (ex.: flyout
     da sidebar colapsada, em document.body) usa --z-navbar, porque tem de pintar acima de
     superfícies sticky (--z-sticky: 500). Ver SIDEBAR_ARQUITETURA 6.2 (#12). */

  /* ---- 1.10 Movimento (regra 7) ---- */
  --dur-fast: 150ms;   /* hover, foco, toggles pequenos */
  --dur-base: 250ms;   /* transições de componente (padrão) */
  --dur-slow: 400ms;   /* entrada/saída de painéis, toasts */
  --ease-out: cubic-bezier(0.22, 1, 0.36, 1);
  --ease-in-out: ease-in-out;

  /* ---- 1.11 Tipografia (ver secção 2) ---- */
  --font-display: "Montserrat", sans-serif; /* títulos, botões, UI */
  --font-body: "Rubik", sans-serif;         /* corpo de texto */
  --fs-caption: 0.75rem;  /* 12px */ --lh-caption: 1.4;
  --fs-small:   0.8125rem;/* 13px */ --lh-small: 1.5;
  --fs-base:    0.875rem; /* 14px — corpo padrão */
  --fs-md:      1rem;     /* 16px */
  --fs-lg:      1.25rem;  /* 20px — h4/títulos de card */
  --fs-xl:      1.5rem;   /* 24px — h3 */
  --fs-2xl:     2rem;     /* 32px — h2 */
  --fs-3xl:     2.5rem;   /* 40px — h1 landing */

  /* ---- 1.12 Layout persistente (off-set de conteúdo) ---- */
  --sidebar-width: 260px;                                       /* largura expandida (§4.6) */
  --sidebar-width-collapsed: calc(var(--space-16) + var(--space-2)); /* 72px (D4) */
}

/* Reset cooperado com os tokens */
* { margin: 0; padding: 0; box-sizing: border-box; font-family: var(--font-display); }
html { font-size: 90%; } /* 1rem = 14.4px — NÃO alterar */
body { font-family: var(--font-body); color: var(--color-ink); background: var(--color-bg); }
ol, ul { list-style: none; }
a { text-decoration: none; color: var(--color-primary); }

/* Foco visível universal (regra: convenções de plataforma) */
:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 2px;
  border-radius: var(--radius-sm);
}

/* Movimento com propósito (regra 7) */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

## 2. Tipografia

| Regra | Valor |
|---|---|
| Fonte de títulos/UI/botões | **Montserrat** (`--font-display`) |
| Fonte de corpo de texto | **Rubik** (`--font-body`) |
| ~~Segoe UI~~ | **PROIBIDO** — removido do sistema |
| Base html | `90%` (1rem = 14.4px) — não alterar por componente |

**Escala tipográfica obrigatória:**

| Elemento | Token | Tamanho | Peso | Uso |
|---|---|---|---|---|
| H1 | `--fs-3xl` | 40px | 800 | Título de landing/hero |
| H2 | `--fs-2xl` | 32px | 700 | Título de secção |
| H3 | `--fs-xl` | 24–25px | 600 | Título de página/formulário |
| H4 / título de card | `--fs-lg` | 20px | 600 | Cards, painéis |
| Corpo | `--fs-base` | 14px | 400 | Texto corrido, labels |
| Botão | `--fs-md` | 16–17px | 600–700 | `button` |
| Legenda/erro | `--fs-caption` | 12px | 400 | `.errorText`, dicas |
| Subtítulo | `--fs-small` | 13px | 400–500 | Texto secundário (`--color-ink-muted`) |

- Peso **1000 não existe em Montserrat** → usar **700** no máximo em botões (já aplicado em `button.css`, v1.2).
- **Aplicação das fontes (v1.2):** o reset global (`*`) aplica `--font-display`; todo o texto de corpo — parágrafos, `label`, `input`, `.errorText`, `.formHint`, `.toast__text` — **declara** `--font-body` explicitamente. A regra legada `body { font-family: 'Segoe UI', sans-serif; }` foi **removida** de `App.css`; não recriar esta ambiguidade (era o que fazia o sistema inteiro renderizar fora da escala).
- Line-height: títulos `1.2`, corpo `1.5`, legendas `1.4`.
- Texto nunca deve ser centralizado em parágrafos > 2 linhas (alinhamento à esquerda, exceto títulos de hero/auth).

---

## 3. As 7 regras visuais obrigatórias

### 3.1 Hierarquia visual
- Toda tela tem **um** elemento H1 (ou equivalente) e no máximo **uma** ação primária visível por região.
- Hierarquia por: **tamanho > peso > cor** — nessa ordem. Nunca usar mais de 2 pesos diferentes na mesma região.
- Profundidade = sombra: `--shadow-sm` (repouso) → `--shadow-md` (elevado/hover) → `--shadow-lg` (modal). Superfície mais elevada = fundo mais claro (`--color-surface-2` → `--color-surface`).
- Região de conteúdo: `--space-6` entre blocos, `--space-4` dentro de blocos, `--space-2` entre itens irmãos relacionados.

### 3.2 Consistência
- **Proibido** escrever hex/rgb, fontes, `border-radius`, `box-shadow` ou tempos fora dos tokens. Uso obrigatório: `var(--...).
- Mesma função visual = mesma cor o sistema inteiro:
  - Marrom/dourado (`--color-primary*`) → ações de marca, navegação **e autenticação** (Login/Register/Redefinição — §4.11, §5.6).
  - Ciano (`--color-accent*`) → ações de sistema (upload, nuvem, atividade). **Nunca** em telas de autenticação (§1.2).
  - `--color-danger*` → exclusão/erro, **nunca** para destaque.
  - Verde/azul/amarelo/vermelho semânticos → apenas feedback (toast/badges).
- Raios: botão `--radius-lg`, pill `--radius-xl`, card/input `--radius-md`, chip `--radius-sm`. Não misturar.
- Ícones: stroke SVG 2px, tamanho `1.25rem` (`.icon`), sempre com `margin-right: var(--space-2)` quando acompanham texto.

### 3.3 Contraste e legibilidade
- Texto normal **≥ 4.5:1**; texto grande (≥18.66px bold / 24px) **≥ 3:1** (WCAG AA). Pares aprovados:
  - `--color-ink` sobre `--color-bg` / `--color-surface` ✅
  - `--color-ink-muted` sobre `--color-surface` ✅ (uso apenas para texto secundário ≥ 13px)
  - `#fff` sobre `--color-primary` ✅ · `#fff` sobre `--color-accent` ⚠️ (usar `--color-accent-ink` ou texto escuro)
  - `--color-danger-action` sobre branco ✅ apenas para `--fs-caption`+ bold nunca; manter texto de erro ≥12px e **não** usar como fundo de texto claro.
- Texto sobre vídeo/imagem **exige** overlay/scrim ou `backdrop-filter: blur()`. Padrão aprovado (v1.2, §4.11): a superfície de autenticação usa **imagem de nuvens brancas sobre fundo cinzento** (`src/images/hero.jpg`, sRGB ≈ 83%), com título em `--color-ink-strong` e parágrafo em `--color-ink` → ≥ 10:1; a barra de rodapé tem veio branco de 70% (`color-mix(in srgb, var(--color-surface) 70%, transparent)`) + `backdrop-filter: blur(10px)` e texto em `--color-ink`. **Trocar a imagem por uma escura obriga a rever os tokens de texto** — texto claro sobre imagem clara é proibido.

Pares de contraste acrescentados na v1.2 (todos ≥ 4.5:1):

| Texto | Fundo | Rácio |
|---|---|---|
| `--color-ink-strong` / `--color-ink` | imagem de nuvens (cinzento claro) | 10–12:1 ✅ |
| `--color-ink` | barra glass de rodapé (veio branco 70%) | ≥ 8:1 ✅ |
| `--color-surface` (conteúdo do botão dourado) | `--color-primary` | 4.7:1 ✅ |
| `--color-primary` (conteúdo no hover) | `--color-surface` (preenchimento do hover) | 4.7:1 ✅ |
| `--color-ink` (texto do toast) | `--color-*-bg` semânticos | 6.2–12.3:1 ✅ |
| `--color-ink-strong` | `--color-accent` / `--color-accent-active` (`.btn-secondary`) | 10.3:1 / 5.1:1 ✅ |

> Os pares `cor + bg` do snippet §4.3 davam **4.07:1** (erro) e **4.41:1** (sucesso) — abaixo do mínimo. Por isso o texto do toast é `--color-ink` e só o **fundo e o ícone** ficam semânticos (fundo/ícone = forma, texto = leitura).
- Nunca `opacity < 0.7` em texto informativo.
- Máximo **~70 caracteres por linha** em textos corridos.

### 3.4 Affordance
- Elemento clicável deve **parecer** clicável: cursor `pointer`, elevação ou borda que se destaca do fundo.
- Botão vs link vs card: botão = fundo preenchido + raio `--radius-lg` + padding `15px 25px`; link = cor `--color-primary` + sublinhado no hover/inline; card = superfície + `--shadow-sm` e **não** deve ser acionável inteiro sem indicador (hover eleva para `--shadow-md`).
- Área de toque mínima **44×44px** (mobile) — respeitar `--space-4` de padding mínimo em cliques.
- Ações destrutivas (excluir) **nunca** usam a cor primária; usam `--color-danger` e exigem confirmação.

### 3.5 Feedback de estado (OBRIGATÓRIO)
> **Todo elemento interativo DEVE ter, no mínimo: `:normal`, `:hover`, `:active`, `[disabled]` + `:focus-visible` (teclado) + estado `loading` quando executa assincronia.**

Matriz obrigatória (aplicada na secção 4 a cada componente):

| Estado | Como implementar (padrão do sistema) |
|---|---|
| **normal** | fundo/ borda do token da variante |
| **hover** | trocar para `--color-*-hover` **OU** preencher com `--dur-base var(--ease-out)`; elevar sombra `--shadow-sm → --shadow-md` |
| **active/pressed** | `--color-*-active` (mais escuro) **e/ou** `transform: scale(0.98)`; `transition: var(--dur-fast)` |
| **disabled** | `--color-disabled-bg` + `--color-disabled-ink` + `cursor: not-allowed` + `pointer-events: none` **e** `disabled` no DOM (não só visual) |
| **focus-visible** | `outline: 2px solid var(--color-focus-ring); outline-offset: 2px` (herdado do reset global) |
| **loading** | desactivar interação (`disabled` + `aria-busy="true"`), mostrar `<Spinner/>` no sítio do ícone/texto, manter a largura do botão (não pode saltitar) |

- Erros de formulário: borda `var(--color-danger-action)` + mensagem `.errorText` com ícone — **nunca** apenas cor vermelha sem texto. O destaque de 2px faz-se com **anel** (`box-shadow: 0 0 0 1px`), não aumentando a largura da borda (ver §4.2).
- **Invariante (v1.2) — mudar de estado não altera a geometria:** corrigiu-se o caso em que o `input` crescia ao mostrar erro. Duas regras obrigatórias: (a) a **largura da borda é constante** em todos os estados (1px + anel), porque `border-box` não protege elementos com `height: auto`; (b) a **coluna do formulário tem largura definida**, para a mensagem de erro quebrar linha em vez de alargar o campo. O mesmo vale para `padding`, `margin` e tamanho de fonte entre estados.
- Sucesso/aviso/erro global → `<Toast type="success|info|warning|error">`.

### 3.6 Convenções de plataforma
- Navegação: navbar fixa glass no topo (landing), **sidebar à esquerda** no dashboard autenticado; breadcrumb ou título de página no topo do conteúdo.
- Foco de teclado sempre visível (não usar `outline: none` sem substituto).
- Scrollbar do sistema é ocultada (`::-webkit-scrollbar { display: none }`) — manter scroll por gesto/wheel, nunca esconder indicadores de scroll em áreas de lista sem substituto visual.
- Na sidebar (§4.6) a área de navegação tem scroll próprio com a scrollbar oculta: o **substituto visual do indicador** é o *fade* de `--color-surface` no topo/base da área (`.sidebar-nav-wrap::before/::after`, visível apenas quando existe conteúdo fora da vista). Cabeçalho e rodapé da sidebar **nunca** rolam.
- Inputs com `label` associada (`htmlFor`), placeholder nunca substitui label.
- Z-index só via tokens (`--z-*`); proibido valores arbitrários (ex.: `9999`).
- Responsivo — breakpoints oficiais (consolidados dos existentes): `600px` (mobile), `768px` (tablet), `992px` (desktop), `1200px` (wide). Regras antigas (500/602/678/918/927) devem ser migradas para estes 4 valores.

### 3.7 Movimento com propósito
- Durações **somente**: `--dur-fast` (150ms hover/foco), `--dur-base` (250ms componente, padrão), `--dur-slow` (400ms entrada/saída de painel, toast).
- Easing: `--ease-out` para entradas/hover, `--ease-in-out` para deslocamentos.
- Animações com função comunicativa apenas:
  - Preenchimento do botão (`.btn::before` width 0→100%) = **estado hover**, 250ms.
  - Barra de progresso do toast = **tempo restante**.
  - Spinner = **operação em curso** (nunca decorativo).
  - Toast `scale(1.01)` hover / `scale(1.05)` active = affordance.
- Proibido: animações que atrapalhem leitura, parallax, loop infinito fora de spinners, animações sem `prefers-reduced-motion` respeitado.
- Troca de estado de um elemento **não** pode esconder informação durante a animação.

---

## 4. Specs de componentes

> CSS pronto. Todos usam os tokens. Substitui/refatora o CSS atual para estes padrões.

### 4.1 Botão (`.btn` + variantes) — 6 estados

```css
.btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 15px 25px;
  border: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-display);
  font-weight: 700;              /* Montserrat não tem 1000 */
  font-size: var(--fs-md);
  width: 100%;
  text-align: center;
  text-decoration: none;         /* o botão do rodapé de auth é um <a> (Link) */
  cursor: pointer;
  position: relative;
  z-index: 1;
  overflow: hidden;
  transition: all var(--dur-base) var(--ease-out);
}
.btn:disabled,
.btn[aria-disabled="true"] {
  background: var(--color-disabled-bg) !important;
  color: var(--color-disabled-ink) !important;
  box-shadow: none !important;
  cursor: not-allowed;
  pointer-events: none;
}
.btn.is-loading { cursor: progress; pointer-events: none; }
.btn.is-loading .btn-label { visibility: hidden; }
.btn .spinner-slot {
  position: absolute; inset: 0; margin: auto;
  display: none; place-items: center;
  width: 1.1em; height: 1.1em; color: inherit;
}
.btn.is-loading .spinner-slot { display: grid; }
.btn.is-loading .spinner { font-size: 1.25em; }   /* reescala as blades ao slot */

/* Variante PRIMARY (marca/dourado) — no hover as cores INVERTEM, mantendo a animação */
.btn-primary { background: var(--color-primary); color: var(--color-surface); box-shadow: var(--shadow-md); }
.btn-primary::before {
  content: ""; position: absolute; inset: 0 auto 0 0;
  width: 0; background: var(--color-surface);        /* preenchimento branco */
  border-radius: inherit; z-index: -1;
  transition: width var(--dur-base) var(--ease-out);
}
.btn-primary:hover { color: var(--color-primary); box-shadow: var(--shadow-md); }
.btn-primary:hover::before { width: 100%; }
.btn-primary:active { color: var(--color-primary); transform: scale(0.98); transition-duration: var(--dur-fast); }

/* Variante SECONDARY (acento de sistema — PROIBIDA em autenticação, ver §4.11) */
.btn-secondary { background: var(--color-accent); color: var(--color-ink-strong); box-shadow: var(--shadow-md); }
.btn-secondary::before {
  content: ""; position: absolute; inset: 0 auto 0 0;
  width: 0; background: var(--color-accent-active);
  border-radius: inherit; z-index: -1;
  transition: width var(--dur-base) var(--ease-out);
}
.btn-secondary:hover { color: var(--color-ink-strong); }  /* §3.3: o branco sobre o acento falha 4.5:1 */
.btn-secondary:hover::before { width: 100%; }
.btn-secondary:active { transform: scale(0.98); transition-duration: var(--dur-fast); }

/* Variante OUTLINE (secundário) — o hover inverte para preenchido */
.btn-outline {
  background: transparent; color: var(--color-primary);
  border: 1px solid var(--color-primary);
  padding: var(--space-2) var(--space-6); border-radius: var(--radius-xl);
  font-weight: 600; font-size: var(--fs-small); width: auto;
}
.btn-outline:hover { background: var(--color-primary); color: var(--color-surface); border-color: var(--color-primary); }
.btn-outline:active { background: var(--color-primary); color: var(--color-surface); transform: scale(0.98); }

/* Variante GHOST */
.btn-ghost { background: transparent; color: var(--color-primary); width: auto; padding: var(--space-2) var(--space-3); }
.btn-ghost:hover { background: var(--color-primary-tint); }
.btn-ghost:active { background: var(--color-primary-subtle); }

/* Variante DANGER (exclusão) — hover/active escurecem com color-mix (sem cores soltas) */
.btn-danger { background: var(--color-danger); color: var(--color-surface); }
.btn-danger:hover { background: color-mix(in srgb, var(--color-danger) 88%, var(--color-ink-strong)); }
.btn-danger:active { background: color-mix(in srgb, var(--color-danger) 76%, var(--color-ink-strong)); transform: scale(0.98); }

/* Foco herdado do reset global (:focus-visible) — NÃO remover */
```

> Notas v1.2:
> - `.btn-secundary` (typo legado) foi renomeado para `.btn-secondary`; `button.jsx` ainda aceita o alias para não quebrar chamadas antigas.
> - `#fff` passou a `var(--color-surface)` (mesmo valor `#ffffff`) para cumprir o "só `var(--...)`" do §6.
> - O `::before` usa `border-radius: inherit`, para acompanhar variantes com raio diferente (ex.: pill de 20px do rodapé de auth).
> - **Inversão do primary (aprovada pelo requisitante, 2026-09-29):** no hover o preenchimento é branco e o conteúdo passa a dourado — substitui o preenchimento `--color-primary-active` da v1.1. A animação (`::before` 0 → 100%, `--dur-base`, `--ease-out`) mantém-se igual.
> - `!important` no `:disabled` é o **único** uso legítimo de `!important` em cor: tem de vencer qualquer variante.
> - Componentes assíncronos passam `loading` ao `Button`: aplica `.is-loading`, `disabled`, `aria-busy="true"` e `aria-label` (porque `.btn-label` fica `visibility: hidden`) — ver §5.3.
> - **`.btn-secondary` não pode ser usado em telas de autenticação** (§1.2 e §4.11).

### 4.2 Input (`.inputField`) — estados

```css
.input_container { display: flex; flex-direction: column; margin-bottom: var(--space-3); text-align: left; }
.input_container label {
  color: var(--color-ink); font-family: var(--font-body); font-weight: 500;
  font-size: var(--fs-small); line-height: var(--lh-small);
  padding: var(--space-1) 0; display: block;
}
/* Ícone de validação no label (Register/Reset) — cor só por classe, nunca inline no JSX */
.input_container label .fieldIcon { margin-left: var(--space-2); font-size: var(--fs-caption); }
.fieldIcon--valid { color: var(--color-success); }
.fieldIcon--invalid { color: var(--color-danger-action); }
.inputField {
  width: 100%; padding: 10px var(--space-3);
  min-height: 44px;                                            /* §3.4 + altura estável */
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: var(--fs-base); color: var(--color-ink);
  transition: border-color var(--dur-fast) var(--ease-out),
              box-shadow var(--dur-fast) var(--ease-out);
}
.inputField::placeholder { color: var(--color-ink-muted); opacity: 1; }
.inputField:hover:not(:disabled) { border-color: var(--color-primary-subtle); }
.inputField:focus { outline: none; border-color: var(--color-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-focus-ring) 25%, transparent); }   /* focus */
.inputField:disabled { background: var(--color-disabled-bg); color: var(--color-disabled-ink); cursor: not-allowed; }
.inputField:read-only { background: var(--color-surface-2); }
/* Erro: largura de borda CONSTANTE (1px de borda + 1px de anel = 2px visuais) */
.inputField.inputError, .inputField[aria-invalid="true"] {
  border-color: var(--color-danger-action);
  box-shadow: 0 0 0 1px var(--color-danger-action);
}
.inputField.inputError:focus, .inputField[aria-invalid="true"]:focus {
  border-color: var(--color-danger-action);
  box-shadow: 0 0 0 1px var(--color-danger-action),
              0 0 0 4px color-mix(in srgb, var(--color-danger-action) 25%, transparent);
}
.errorText {
  text-align: left; color: var(--color-danger-action);
  font-size: var(--fs-caption); line-height: var(--lh-caption);
  margin-top: var(--space-1); display: block; white-space: pre-line;
}
.errorText .icon-info { font-size: inherit; margin-right: var(--space-1); color: var(--color-ink); }

/* Formulários de autenticação: largura definida (§4.11) — sem isto a mensagem de
   erro alarga a coluna e o input "cresce". Em Login.css/Register.css:
   .loginPage .formDiv .form, .registerPage .formDiv .form { width: 100%; max-width: 320px; } */
```

> Notas v1.2 (input): o estado de erro **não** usa `border: 2px` — como o input tem `height: auto`, o `border-box` não evita o salto e o campo **crescia 2px** ao aparecer o erro (ver §3.5); os anéis de foco usam `color-mix` sobre `--color-focus-ring` / `--color-danger-action` em vez de `rgba()` solto (§3.2); `min-height: 44px` fixa a altura em todos os estados e garante o alvo do §3.4; `label` e `input` declaram `--font-body` (§2). O toggle de mostrar/ocultar password (mockups §5.6) é um `<button type="button">` dentro do `.input_container`, com `aria-label`, `aria-pressed` e alvo ≥44px.

### 4.3 Toast (feedback global) — cores semânticas

```css
.toast-container { position: fixed; top: var(--space-5); right: var(--space-5);
  display: flex; flex-direction: column; gap: var(--space-2); z-index: var(--z-toast); }
.toast {
  position: relative; display: flex; align-items: center; gap: var(--space-3);
  min-width: 260px; max-width: 360px; padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-sm); box-shadow: var(--shadow-toast);
  color: var(--color-ink);                 /* texto — ver pares de contraste em §3.3 */
  font-family: var(--font-body); font-size: var(--fs-base); line-height: var(--lh-small);
  overflow: hidden;
  animation: toast-in var(--dur-base) var(--ease-out);
  transition: transform var(--dur-base) var(--ease-out);
}
.toast:hover  { transform: scale(1.01); }
.toast:active { transform: scale(1.05); transition-duration: var(--dur-fast); }
.toast__icon { display: inline-flex; flex-shrink: 0; width: 1.25rem; height: 1.25rem; }
.toast__close {                              /* alvo de 44px sem inflacionar a altura */
  width: 44px; height: 44px; padding: 0; border: none;
  margin: calc(var(--space-3) * -1) calc(var(--space-3) * -1) calc(var(--space-3) * -1) 0;
  border-radius: var(--radius-full); background: transparent; color: inherit; cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out);
}
.toast__close:hover { background: color-mix(in srgb, currentColor 15%, transparent); }
.toast__progress { position: absolute; bottom: 0; left: 0; width: 100%; height: 2px;
  transform-origin: left center; background: currentColor;
  animation-name: toast-progress; animation-timing-function: linear; animation-fill-mode: forwards; }
.toast--success { background: var(--color-success-bg); }   /* operações concluídas */
.toast--info    { background: var(--color-info-bg); }      /* informação de sistema */
.toast--warning { background: var(--color-warning-bg); }   /* quota perto do limite, avisos */
.toast--error   { background: var(--color-danger-bg); }    /* falhas de operação */
.toast--success .toast__icon { color: var(--color-success); }
.toast--info    .toast__icon { color: var(--color-info); }
.toast--warning .toast__icon { color: var(--color-warning); }
.toast--error   .toast__icon { color: var(--color-danger); }
@keyframes toast-in { from { opacity: 0; transform: translateY(calc(var(--space-2) * -1)); } to { opacity: 1; transform: none; } }
@keyframes toast-progress { from { transform: scaleX(1); } to { transform: scaleX(0); } }  /* = tempo restante */
```

Posição fixa `top: var(--space-5); right: var(--space-5); z-index: var(--z-toast)`; duração padrão **4500ms** (faixa 3000–5000ms), aplicada inline em `style={{ animationDuration }}` na `.toast__progress`.

> Notas v1.2 (toast): implementado em **CSS puro** (`components/toast/toast.css` + `toast.jsx`) — o `styled-components` foi removido (Regra 4); as classes legadas `.notification-item` e `.success|.info|.warning|.error` passaram a `.toast` e `.toast--*` (não havia outros consumidores); `role="status"` em sucesso/info/aviso e `role="alert"` em erro; botão de fechar com `aria-label="Fechar notificação"`; o temporizador **reinicia** quando muda a `message`; o bug legado que invocava `onClose()` no `mount` foi corrigido. Quem mostra o toast (Login/Register) renderiza-o localmente e limpa o timer de navegação no unmount (§5.3).

### 4.4 Spinner / Loading

```css
.spinner { font-size: 28px; width: .8em; height: .8em; }
/* as blades animam de currentColor → transparent: herdam a cor do contexto (v1.2)
   — --color-primary sobre fundo claro, cor do texto dentro de .btn-*.
   #69717d foi REMOVIDO: não há cor fixa no spinner. */
```
- Regra: **um** spinner por região de espera; em botões usar o estado `.is-loading` (secção 4.1), nunca desativar a página inteira sem overlay.
- **Notas v1.2:** o componente tem `role="status"` + `aria-label="A processar"`; dentro do botão o `.spinner-slot` é `aria-hidden="true"`, porque quem comunica o estado é o `aria-busy` do próprio botão (§4.1).

### 4.5 Navbar glass

```css
.nav-container {
  background: var(--color-glass);
  backdrop-filter: blur(10px);
  padding: var(--space-4) var(--space-6);
  position: fixed; width: 75%; left: 50%; transform: translateX(-50%); top: var(--space-4);
  border-radius: var(--radius-glass);
  border: 1px solid rgba(255, 255, 255, 0.7);
  box-shadow: var(--shadow-sm);
  display: grid; grid-template-columns: 1fr auto 1fr; align-items: center;
  z-index: var(--z-navbar);
}
.items ul a { color: var(--color-ink); font-weight: 500; padding: 5px 0;
  transition: color var(--dur-base) var(--ease-in-out); position: relative; }
.items ul a:hover { color: var(--color-primary-hover); }
.items ul a:active { color: var(--color-primary-active); }
.items ul a[aria-current="page"] { color: var(--color-primary); font-weight: 600; } /* estado ativo */
.btn-cadastro { background: var(--color-primary); color: var(--color-surface); border: 1px solid var(--color-primary); }
.btn-cadastro:hover { background: var(--color-primary-hover); border-color: var(--color-primary-hover); }
.btn-cadastro:active { background: var(--color-primary-active); transform: scale(0.98); }
.btn-login { background: transparent; color: var(--color-primary); border: 1px solid var(--color-primary); }
.btn-login:hover { background: var(--color-primary-tint); }
.btn-login:active { background: var(--color-primary); color: var(--color-surface); }
```

> Nota v1.2: isto é o **alvo** desta secção. Os ficheiros reais (`components/header/navbar.css` e `css/landingPage.css`) ainda usam a paleta legada (`--primaria`, `--vidro`, `rgba(37, 99, 235, …)` no hover do `.btn-login` — **azul fora da identidade**) e estão **pendentes de migração** para estes tokens. Ao tocar no componente: trocar `#fff` por `var(--color-surface)`, remover o azul e passar as queries aos breakpoints oficiais (§3.6).

### 4.6 Sidebar (dashboard autenticado)

```css
.sidebar {
  position: fixed; top: 0; left: 0; bottom: 0;
  width: 260px; padding: var(--space-6) var(--space-4);
  background: var(--color-surface); border-right: 1px solid var(--color-border);
  z-index: var(--z-sticky); display: flex; flex-direction: column; gap: var(--space-2);
}
.sidebar-item {
  display: flex; align-items: center; gap: var(--space-3);
  padding: var(--space-3); border-radius: var(--radius-md);
  color: var(--color-ink); font-weight: 500; font-size: var(--fs-base);
  transition: background var(--dur-fast) var(--ease-out), color var(--dur-fast) var(--ease-out);
}
.sidebar-item:hover { background: var(--color-primary-tint); color: var(--color-primary-hover); }
.sidebar-item:active { background: var(--color-primary-subtle); }
.sidebar-item[aria-current="page"] {
  background: var(--color-primary); color: var(--color-surface); font-weight: 600;
}
.sidebar-item[aria-disabled="true"] { color: var(--color-disabled-ink); cursor: not-allowed; pointer-events: none; }
```

**Contrato adicional (adendo v1.1 — scroll, popovers e modo colapsado):**

- **Larguras por token:** a sidebar usa `--sidebar-width` (260px) e `--sidebar-width-collapsed` (72px, §1.12); nenhum ficheiro repete esses literais — `DashboardLayout.css` usa os mesmos tokens no `margin-left` do conteúdo.
- **Scroll interno:** `.sidebar-header` e `.sidebar-footer` são `flex-shrink: 0`; a navegação vive em `.sidebar-nav-wrap` (`display: flex; flex: 1 1 auto; min-height: 0`) + `.sidebar-nav` (`min-height: 0; overflow-y: auto; overscroll-behavior: contain`, scrollbar oculta). O `padding-left/right: var(--space-1)` com `margin-left/right: calc(var(--space-1) * -1)` mantém a largura dos itens idêntica à do conteúdo e cria gutter para o `outline` de foco (§3.5). O indicador de scroll é o *fade* `.sidebar-nav-wrap[data-scroll-top="true"]::before` / `[data-scroll-bottom="true"]::after` (§3.6).
- **Popovers do modo colapsado** (flyout do submenu, etiqueta do item, motivo de item desabilitado): renderizados com `createPortal(..., document.body)`, `position: fixed`, `z-index: var(--z-navbar)` e posicionamento por JS a partir de `getBoundingClientRect()` do item — **nunca** `position: absolute` dentro do `<nav>`, onde seriam recortados pelo scroll. O cálculo faz *clamp* vertical (nunca sai da viewport) e aplica `max-height: calc(100vh - (var(--space-4) * 2))` com scroll interno para listas longas. Distância de ancoragem `--space-2`; abertura por `hover`/`focus` e fecho com atraso de 150ms (`--dur-fast`); qualquer scroll ou `Esc` fecha.
- **Menu de conta (`.sidebar-account__menu`):** largura fixa `calc(var(--sidebar-width) - (var(--space-4) * 2))` = 228px — exatamente a largura interna do modo expandido — **nos dois modos**; no modo colapsado ancora à direita do rail (`left: calc(100% + var(--space-2)); bottom: 0`), para não tapar os ícones. Entrada por `dd-in-up` (painel abre para cima) e itens com `white-space: nowrap`.
- **Escopo de classes:** o menu de conta usa `.sidebar-menu`/`.sidebar-menu__item` (mesmos valores de 4.7, escopo próprio) em vez de `.dropdown-*`, para não competir com `components/dropdown/dropdown.jsx`.
- **Item destrutivo:** texto em `--color-danger`, ícone em `--color-danger-action` (uso não-textual, §1.4) e hover `color-mix(in srgb, var(--color-danger-bg) 25%, var(--color-surface))` — mantém AA nos três estados.
- **Itens desabilitados:** o motivo deixa de usar `title` nativo (o `pointer-events: none` de 4.6 impede o `title` de abrir) e passa a aparecer no popover (variante *reason*) e no `aria-label` como `"<label> — <motivo>"`.
- **Mobile (< 992px):** a sidebar fechada recebe `inert` (links off-canvas não são tabuláveis) e, com o drawer aberto, o scroll do `body` é bloqueado; o acionador flutuante usa a superfície glass de 4.5.
- **Rota ativa:** ao mudar de rota, os ancestrais do item ativo voltam a abrir; alternar um agrupador em modo colapsado não altera estado (não há submenu inline nesse modo).
- Restrição de implementação: `.sidebar` **não** pode receber `overflow: hidden` (recortaria o menu de conta ancorado à direita do rail no modo colapsado).

### 4.7 Dropdown

```css
.dropdown-menu {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-md);
  padding: var(--space-1); z-index: var(--z-dropdown);
  animation: dd-in var(--dur-fast) var(--ease-out);
}
.dropdown-item { padding: var(--space-2) var(--space-3); border-radius: var(--radius-sm);
  font-size: var(--fs-base); color: var(--color-ink); width: 100%; text-align: left;
  background: transparent; border: none; cursor: pointer;
  transition: background var(--dur-fast) var(--ease-out); }
.dropdown-item:hover { background: var(--color-primary-tint); }
.dropdown-item:active { background: var(--color-primary-subtle); }
.dropdown-item[disabled] { color: var(--color-disabled-ink); cursor: not-allowed; }
.dropdown-item.is-danger { color: var(--color-danger); }             /* AA: 10:1 (adendo v1.1) */
.dropdown-item.is-danger .dropdown-item__icon { color: var(--color-danger-action); } /* ícone: 4:1 ≥ 3:1 */
.dropdown-item.is-danger:hover { background: color-mix(in srgb, var(--color-danger-bg) 25%, var(--color-surface)); }
@keyframes dd-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }
@keyframes dd-in-up { from { opacity: 0; transform: translateY(var(--space-1)); } to { opacity: 1; transform: none; } }
```

- `dd-in` é a entrada de painéis que abrem **para baixo**; `dd-in-up` é a variante de painéis que abrem **para cima** (ex.: menu de conta da sidebar, §4.6).
- **Contraste (correção v1.1):** `--color-danger-action` (`rgb(255,0,34)`) atinge ~4:1 sobre `--color-surface` e ~2.6:1 sobre `--color-danger-bg` — suficiente para **gráficos/ícones** (mínimo 3:1), insuficiente para **texto** (mínimo 4.5:1). Daí texto em `--color-danger` e ícone em `--color-danger-action`.
- **Consumidores:** a sidebar **não** usa estas classes (usa `.sidebar-menu`/`.sidebar-menu__item` com os mesmos valores, por escopo); `.dropdown-*` fica reservado a dropdowns de conteúdo/formulário, como `components/dropdown/dropdown.jsx`.

### 4.8 Card

```css
.card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); padding: var(--space-6);
  box-shadow: var(--shadow-sm);
  transition: box-shadow var(--dur-base) var(--ease-out), transform var(--dur-base) var(--ease-out);
}
.card:hover { box-shadow: var(--shadow-md); }              /* só se for clicável */
.card:active { transform: scale(0.99); }
```

### 4.9 Barra de quota de armazenamento (componente de negócio)

```css
.quota-bar { height: 8px; border-radius: var(--radius-full); background: var(--color-surface-2); overflow: hidden; }
.quota-fill { height: 100%; border-radius: var(--radius-full);
  transition: width var(--dur-slow) var(--ease-out);
  background: var(--color-success); }                        /* <70% */
.quota-fill.is-warning { background: var(--color-warning-bg); } /* 70–89% */
.quota-fill.is-critical { background: var(--color-danger-action); } /* ≥90% */
.quota-fill.is-blocked { animation: pulse 1.5s var(--ease-in-out) infinite; } /* 100% */
```

### 4.10 Modal / confirmação

```css
.modal-backdrop { position: fixed; inset: 0; background: var(--color-overlay);
  z-index: var(--z-modal); display: grid; place-items: center;
  animation: fade-in var(--dur-base) var(--ease-out); }
.modal {
  background: var(--color-surface); border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg); padding: var(--space-8); max-width: 480px; width: 90%;
  animation: modal-in var(--dur-base) var(--ease-out);
}
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes modal-in { from { opacity: 0; transform: translateY(8px) scale(0.98); } to { opacity: 1; transform: none; } }
```
- Modal **sempre** fecha com `Esc`, tem botão de fechar com `aria-label` e foco preso dentro dele.

### 4.11 Superfície de autenticação (Login / Register) — v1.2

> Implementação de referência: `pages/Login.{jsx,css}` e `pages/Register.{jsx,css}`. Estas telas **são** a referência visual dos fluxos de auth (não há mockup externo para Login/Register — a Regra 1 fica satisfeita por elas). O fluxo de reposição de password tem mockups próprios (§5.6) e reutiliza o cartão descrito lá.
>
> Os seletores abaixo usam os nomes **reais** do código; `videoDiv`/`footerDiv` são nomes legados (eram o painel do vídeo) mantidos para não partir a estrutura existente.

**Estrutura (desktop):** dois painéis num contentor cinzento; no painel esquerdo vive o rodapé glass.

```css
.loginPage { height: 100vh; }                    /* Login: altura fixa */
.registerPage { min-height: 100vh; }             /* Register: min-height (tem 5 campos) */
.loginPage, .registerPage { width: 100%; margin: auto;
  border-radius: var(--radius-md); overflow: hidden; box-shadow: var(--shadow-lg); }
.loginPage .container, .registerPage .container { width: 60%; margin: auto;
  justify-content: space-between; border-radius: var(--radius-md);
  background: var(--color-surface-2); }        /* fundos cinzentos — decisão do owner */
```

**Painel esquerdo — imagem de nuvens brancas + texto escuro + rodapé glass:**

```css
.loginPage .videoDiv img { position: absolute; inset: 0; height: 100%; width: 100%;
  object-fit: cover; object-position: center 25%; }   /* sem blur, sem <video> */
.loginPage .textDiv .title { color: var(--color-ink-strong); font-family: var(--font-display);
  font-size: var(--fs-2xl); font-weight: 800; line-height: 1.2; }
.loginPage .textDiv p { color: var(--color-ink); font-family: var(--font-body);
  padding: var(--space-4); font-weight: 400; line-height: 1.5; }
.loginPage .footerDiv { position: absolute; bottom: var(--space-3); left: var(--space-3); right: var(--space-3);
  height: 60px; padding: 0 var(--space-4); justify-content: space-between;
  backdrop-filter: blur(10px);
  background: color-mix(in srgb, var(--color-surface) 70%, transparent);
  border-radius: var(--radius-md); }
.loginPage .footerDiv .text { color: var(--color-ink); font-family: var(--font-body);
  font-size: var(--fs-base); }
.loginPage .btn-account { width: auto; padding: var(--space-3) var(--space-6);
  border-radius: var(--radius-xl); font-family: var(--font-display);
  font-size: var(--fs-small); font-weight: 600; }
```

- **Proibido** vídeo ou qualquer tom azul neste painel: o acento ciano não entra em autenticação (§1.2). A imagem é decorativa (`alt=""`).
- Texto sobre a imagem **tem** de usar `--color-ink*` (§3.3) — a imagem é clara. O texto branco da v1.0 existia porque havia um vídeo escuro.
- O botão de troca de ecrã é um `<Link>` com as classes `.btn .btn-primary .btn-account`: o `.btn` tem `display: inline-flex` e `text-decoration: none` precisamente para funcionar em `<a>`.

**Painel direito — ícone dourado, H1 único, formulário de largura fixa:**

```css
.loginPage .formDiv { flex-basis: 50%; margin: auto; flex-direction: column;
  gap: var(--space-5); transform: translateY(calc(var(--space-4) * -1)); }
/* Register difere: .formDiv { flex: 1; gap: var(--space-4); } */
.loginPage .headerDiv .headIcon { color: var(--color-primary); background: transparent;
  font-size: calc(var(--fs-3xl) * 1.5); padding-bottom: var(--space-2); }
.loginPage .headerDiv .authTitle { font-family: var(--font-display); font-size: var(--fs-xl);
  color: var(--color-ink-strong); font-weight: 600; line-height: 1.2; }   /* H1 único (§3.1) */
.loginPage .formDiv .form { width: 100%; max-width: 320px; gap: var(--space-4); }  /* §4.2: não cresce */
.loginPage .formDiv .formHint { color: var(--color-ink-muted); font-family: var(--font-body); text-align: center; }
.loginPage .forgotPassword { color: var(--color-ink-muted); font-family: var(--font-body); font-size: var(--fs-small); text-align: center; }
.loginPage .forgotPassword a { color: var(--color-primary); font-weight: 500;
  transition: color var(--dur-fast) var(--ease-out); }
.loginPage .forgotPassword a:hover, .loginPage .forgotPassword a:focus-visible {
  color: var(--color-primary-hover); text-decoration: underline; }
```

- **Ícone do cabeçalho:** fundo transparente + `--color-primary`, um por página — **Login** `faRightToBracket`, **Register** `faUserPlus` (`aria-hidden="true"`). Proibido raster colorido neste lugar (a legada `nuvem.png`, azul/ciano, foi removida).
- **Botão de submit:** `.btn-primary` com `loading` (§4.1). Ação de troca de página: `.btn-primary .btn-account` (pill dourado) ou `.btn-outline`; nunca texto cru.
- **Breakpoints** (ordem descendente, para a regra mais restrita vencer): **992px** contentor 85%; **768px** contentor 100% + títulos menores; **600px** coluna única com `padding: var(--space-5)`.
- O CSS é **escopado** (`.loginPage`, `.registerPage`) e não interfere com o resto da app; única exceção: `::-webkit-scrollbar { display: none }` (§3.6).

---

## 5. Regras de negócio aplicadas à UI

A identidade visual **serve** as regras de negócio — nunca as esconde:

### 5.1 Perfil/Permissão (tabela `perfil.permissao`)
Estado visual obrigatório por permissão:

| Permissão no UI | Implementação |
|---|---|
| **Permitido** | componente normal (estados completos) |
| **Permitido mas sensível** | visível + ação destrutiva exige confirmação em modal |
| **Não permitido (condicional)** | `[disabled]` + tooltip explicando o motivo (nunca remover sem explicar) |
| **Não permitido (secreto)** | não renderizar no DOM (ex.: item de admin na sidebar) |

- A sidebar/menu **nunca** mostra items que o perfil não pode aceder; itens desativados usam `aria-disabled="true"` com o estado visual da secção 3.5.
- O estado `--aria-current="page"` identifica sempre a secção ativa.

### 5.2 Armazenamento (quota) — semântica de cor fixa
| Faixa | Cor da barra | Ação acompanhante |
|---|---|---|
| < 70% | `--color-success` | nenhum |
| 70–89% | `--color-warning-bg` | `Toast type="warning"` a avisar |
| ≥ 90% | `--color-danger-action` | `Toast type="error"` + botão de upgrade/limpeza |
| 100% | pulsante `.is-blocked` | upload **disabled** com mensagem `--color-danger-action` |

- Upload em progresso: botão `.is-loading` + `Spinner` + barra de progresso; nunca upload silencioso.
- Falha de upload: `Toast type="error"` e o botão **volta ao estado normal** (não fica preso em loading).

### 5.3 Fluxos de autenticação (Login / Register / Redefinição de password)
- Erro de credenciais → `Toast type="error"` **ou** `.errorText` no campo (erro de campo) — erro de servidor nunca apaga o que o utilizador digitou.
- Sucesso → `Toast type="success"` antes de redirecionar.
- Botão de submit durante requisição: `.is-loading`, `disabled`, largura estável.
- Links auxiliares ("Esqueceu a senha?", "Criar conta") em `--color-primary` com sublinhado no hover.
- **Sem acento/azul:** estes fluxos usam apenas a marca (§1.2, §4.11).

**Contrato implementado (v1.2):**

| Regra | Como |
|---|---|
| Estado de espera | `loading` em `useState`: `true` antes do pedido, `false` no `finally` — o botão **volta ao normal** quando falha (nunca fica preso em loading) |
| Sucesso | `Toast type="success"` com `duration` de **1500ms** e só depois `navigate(...)` — o toast tem de ser visível antes do redirecionamento |
| Erro | `Toast type="error"` com 4500ms; os campos mantêm os valores digitados |
| Mensagens por causa | sem resposta do servidor · **403** sem autorização · **409** email já registado (Register) · **429** demasiadas tentativas · resto → credenciais incorrectas / dados inválidos |
| Trilha de erro | `console.error("[login] …", err)` / `console.error("[register] …", err)` **obrigatório** em todos os ramos de erro. **Proibido** registar token ou credenciais (mesmo em caso de sucesso) |
| Navegação | o timer do `navigate` fica em `useRef` e é limpo no unmount |
| Foco | ao montar, o foco vai para o primeiro campo (`userRef.current?.focus()`) |

### 5.4 Operações de ficheiros/pastas (criar, renomear, mover, excluir)
- Exclusão: modal de confirmação + `btn-danger`; feedback via `Toast`.
- Sucesso: `Toast type="success"`; Falha: `Toast type="error"` com mensagem acionável.
- Operação em lote: spinner na região afetada, itens permanecem visíveis.

### 5.5 Landing Page vs Dashboard
- Landing: superfícies glass + `--color-primary` (marca).
- Dashboard autenticado: fundo `--color-bg`, superfícies `--color-surface`, acento `--color-accent` para ações de nuvem/upload, identidade mantida no item ativo da sidebar.

### 5.6 Redefinição de password ("esqueceu-se da password") — especificado, ainda por implementar

> **Referência visual normativa:** `frontend/src/docs/Modelos_forgotpassword/` (4 telas modelo). Ao contrário do Login/Register, este fluxo é um **cartão centrado de coluna única** com um *badge* de ícone no topo — **não** usa o painel de imagem do §4.11.
> Estado no código: **por implementar** (o backend também ainda não tem endpoints de reposição).

```css
.resetPage { min-height: 100vh; display: grid; place-items: center;
  background: var(--color-bg); padding: var(--space-6); }
.reset-card {
  background: var(--color-surface); border: 1px solid var(--color-border);
  border-radius: var(--radius-md); box-shadow: var(--shadow-sm);
  padding: var(--space-8); max-width: 480px; width: 90%; margin: auto; text-align: center;
}
.reset-badge {                        /* badge do ícone: dourado + fundo transparente */
  width: 64px; height: 64px; margin: 0 auto var(--space-4);
  display: grid; place-items: center;
  background: transparent; border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-primary); font-size: var(--fs-xl);
}
.reset-title { font-family: var(--font-display); font-size: var(--fs-xl); font-weight: 600; color: var(--color-ink-strong); }
.reset-subtitle { font-family: var(--font-body); font-size: var(--fs-base); color: var(--color-ink-muted); }
.reset-subtitle strong { color: var(--color-ink-strong); }   /* email confirmado, em destaque */
.reset-back { color: var(--color-primary); font-size: var(--fs-small); }
.reset-back:hover, .reset-back:focus-visible { color: var(--color-primary-hover); text-decoration: underline; }
```

> O roxo dos mockups mapeia para `--color-primary`; o cartão e o badge usam os tokens de superfície/borda do sistema.

#### As 4 telas (fidelidade ao mockup)

| # | Título | Badge | Conteúdo | Ação primária | Auxiliares |
|---|---|---|---|---|---|
| 1 | *Forgot password?* | chave (`faKey`) | "No worries, we'll send you reset instructions." + campo **Email** (§4.2) | **Reset password** (`.btn-primary`) | "← Back to log in" |
| 2 | *Check your email* | envelope (`faEnvelope`) | "We sent a password reset link to **{email}**" (email em `--color-ink-strong`) | **Open email app** | "Didn't receive the email? **Click to resend**" (com cooldown de 60s + `[disabled]`) e "← Back to log in" |
| 3 | *Set new password* | cadeado (`faLock`) | "Your new password must be different to previously used passwords." + **Password** e **Confirm password** (com toggle de visibilidade) + checklist de requisitos | **Reset password** | "← Back to log in" |
| 4 | *Password reset* | visto (`faCircleCheck`) | "Your password has been successfully reset. Click below to log in magically." | **Continue** → `/login` | "← Back to log in" |

- **Idioma:** os mockups estão em inglês e o resto do sistema em português → decisão pendente do requisitante; até lá mantém-se o texto dos mockups (não misturar idiomas na mesma tela).
- **Checklist de password:** o mockup mostra 2 itens ("at least 8 characters", "one special character"), mas a regra real é a `PWD_REGEX` do Register (8–24 + maiúscula + minúscula + número + símbolo). Como a identidade **serve** as regras de negócio (§5), a checklist lista os **5 requisitos reais**, cada um com ícone ✓/• **e** texto — `--color-success` quando cumprido, `--color-disabled-ink` quando não.
- **Toggle de visibilidade:** `<button type="button">` dentro do `.input_container`, `aria-label="Mostrar password"` / `"Ocultar password"`, `aria-pressed`, ícone `--color-ink-muted`, alvo ≥44px.
- **Feedback:** erros com `Toast type="error"` + `.errorText` (§5.3); sucesso da reposição → `Toast type="success"` **e** a tela 4; submit com `loading` (§4.1); foco movido para o título a cada passo e `aria-live` na região de mensagem.

#### Contrato de API (backend a implementar)

| Tela | Endpoint | Notas |
|---|---|---|
| 1 | `POST /auth/password/forgot { email }` | resposta **sempre genérica** — nunca revela se o email existe (anti-enumeração) |
| 2 | (ecrã de espera) | reenviar = repetir o endpoint 1, com cooldown de 60s no botão |
| 3–4 | `POST /auth/password/reset { token, password }` | o `token` vem do **link** enviado por email (nos mockups não há código digitado); uso único e curto |

- Requisitos de backend: novas rotas em `AuthController` + `permitAll` no `SecurityConfig`, persistência do token (hash + expiração) e envio de email. A regra do mockup *"must be different to previously used passwords"* tem de ser validada no **servidor**.
- Rotas de frontend: `/forgotpassword` (telas 1–2 como passos do mesmo ecrã) e `/resetpassword?token=…` (telas 3–4). O token vive em estado/`useRef` e é limpo no unmount — **nunca** em `localStorage`.

#### Checklist específico deste fluxo
- [ ] Badge de ícone dourado com fundo transparente (`--color-primary`) — sem azul/acento.
- [ ] Nenhum passo revela se o email existe (mensagem genérica e resposta indistinguível).
- [ ] Checklist cobre os **5** requisitos reais da `PWD_REGEX`.
- [ ] Token nunca persistido no cliente; expiração e uso único no servidor.
- [ ] `aria-live` na região de mensagem e foco movido para o título a cada passo.
- [ ] Estados completos no submit, no reenviar (cooldown) e no toggle da password.

---

## 6. Checklist de revisão obrigatória

Antes de entregar qualquer componente, confirmar **todos** os itens:

- [ ] Usa exclusivamente `var(--...)` para cor, fonte, raio, sombra, espaço e duração (branco = `var(--color-surface)`, **nunca** `#fff`; anéis de foco/erro com `color-mix`, **nunca** `rgba()` solto).
- [ ] **Mudar de estado não altera a geometria**: largura de borda constante, o erro não «cresce» o campo, coluna de formulário com largura definida (§3.5, §4.2).
- [ ] Se é tela de autenticação: **sem acento/azuis**, segue o §4.11; se é a reposição de password, segue o §5.6 e os mockups em `docs/Modelos_forgotpassword/`.
- [ ] Texto sobre imagem/vídeo usa tokens de texto adequados ao fundo (fundo claro → `--color-ink*`), com contraste verificado (§3.3).
- [ ] Tipografia na escala da secção 2 (Montserrat/Rubik, sem Segoe UI, peso ≤ 700).
- [ ] Tem **normal, hover, active, disabled, focus-visible** documentados; e **loading** se for assíncrono.
- [ ] `disabled` existe no DOM (`disabled`/`aria-disabled`), não só visualmente.
- [ ] Contraste AA verificado para todos os pares de cor usados (≥ 4.5:1).
- [ ] Alvo de clique ≥ 44px; `cursor: pointer` em clicáveis.
- [ ] Erros sempre com ícone **e** texto (nunca só cor).
- [ ] Apenas as 4 durações/2 easings da secção 3.7; `prefers-reduced-motion` respeitado.
- [ ] Z-index vem dos tokens (`--z-*`).
- [ ] Áreas com scroll interno têm indicador visível (a scrollbar do sistema é oculta — usar o *fade* normado em 3.6).
- [ ] Responsivo nos breakpoints 600/768/992/1200px.
- [ ] Estados por permissão conforme secção 5.1 (nenhum item proibido chega ao DOM).
- [ ] Feedback de operação via `Toast` (success/error/warning) conforme secção 5.4.

---

*Documento v1.1 — gerado a partir da consolidação de `App.css`, `index.css`, `navbar.css`, `button.css`, `input.css`, `Login.css`, `spinner.css`, `toast.jsx` e do schema de negócio (`perfil`, `permissao`, `pasta`, `arquivo`, `armazenamento`). O adendo v1.1 (tokens `--sidebar-width*` em §1.12, contrato de scroll/popovers em §4.6, correção de contraste em §4.7, nota de z-index em §1.9 e indicador de scroll em §3.6) resulta da revisão de UI da sidebar aprovada pelo requisitante da tarefa; demais alterações a este documento exigem aprovação do líder técnico.*

*Documento **v1.2** (2026-09-29) — atualiza a v1.1: **(a)** sincronização dos snippets §4.1–§4.4 com o código em produção (`button.css`/`button.jsx`, `input.css`, `toast.css`/`toast.jsx`, `spinner.css`/`spinner.jsx`), incluindo a **inversão de cores no hover do `.btn-primary`**, o branco por `var(--color-surface)`, o `border-radius: inherit` do `::before`, o favo de erro com **largura de borda constante** (+`min-height: 44px`), os anéis por `color-mix`, o texto do toast em `--color-ink` por contraste e as classes `.toast/.toast--*`; **(b)** nova secção **§4.11 — Superfície de autenticação** (imagem de nuvens brancas sobre fundo cinzento em vez do vídeo, texto `--color-ink*`, rodapé glass, ícone dourado transparente por página, formulário com largura definida, breakpoints 992/768/600, fundos cinzentos mantidos); **(c)** novo **§5.6 — Redefinição de password**, especificado a partir das referências `docs/Modelos_forgotpassword/` (4 telas: chave → envelope → cadeado → visto), com contrato de API, estados e checklist; **(d)** o acento ciano passa a ser explicitamente **proibido em autenticação** (§1.2, §3.2), o local real dos tokens passa a `App.css` (§1) e a tipografia de corpo/Rubik fica sem ambiguidade (§2); **(e)** checklist §6 alargada. Alterações pedidas e aprovadas pelo requisitante da tarefa (2026-09-29); alterações futuras a este documento continuam a exigir aprovação do líder técnico.*