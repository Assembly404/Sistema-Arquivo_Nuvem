# IDENTIDADE VISUAL — Sistema Arquivo Nuvem
### Documento normativo de UI/UX · OBRIGATÓRIO para todos os desenvolvedores

> **Status:** REGRA ESTABLECIDA. Todo componente visual novo ou modificado **deve** seguir este documento. Qualquer PR que viole uma regra aqui escrita será rejeitado na revisão.
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

**Copie o bloco abaixo e cole no topo do `frontend/src/index.css` (após o reset).** Todos os valores vêm consolidados das paletas existentes (`navbar.css`, `button.css`, `Login.css`, `toast.jsx`): **marrom = identidade da marca**, **ciano = acento de sistema**, cinzas neutros = superfícies.

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

- Peso **1000 não existe em Montserrat** → usar **700** no máximo em botões (o `font-weight: 1000` atual em `.btn` deve ser corrigido para `700`).
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
  - Marrom (`--color-primary*`) → ações de marca/navegação.
  - Ciano (`--color-accent*`) → ações de sistema (upload, nuvem, conta).
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
- Texto sobre vídeo/imagem **exige** overlay/scrim ou `backdrop-filter: blur()` (padrão do Login).
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

- Erros de formulário: borda `2px solid var(--color-danger-action)` + mensagem `.errorText` com ícone — **nunca** apenas cor vermelha sem texto.
- Sucesso/aviso/erro global → `<Toast type="success|info|warning|error">`.

### 3.6 Convenções de plataforma
- Navegação: navbar fixa glass no topo (landing), **sidebar à esquerda** no dashboard autenticado; breadcrumb ou título de página no topo do conteúdo.
- Foco de teclado sempre visível (não usar `outline: none` sem substituto).
- Scrollbar do sistema é ocultada (`::-webkit-scrollbar { display: none }`) — manter scroll por gesto/wheel, nunca esconder indicadores de scroll em áreas de lista sem substituto visual.
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
  padding: 15px 25px;
  border: none;
  border-radius: var(--radius-lg);
  font-family: var(--font-display);
  font-weight: 700;              /* corrige font-weight:1000 (inexistente) */
  font-size: var(--fs-md);
  width: 100%;
  text-align: center;
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
  display: none; width: 1.1em; height: 1.1em;
}
.btn.is-loading .spinner-slot { display: block; }

/* Variante PRIMARY (marca) — normal→hover→active */
.btn-primary { background: var(--color-primary); color: #fff; box-shadow: var(--shadow-md); }
.btn-primary::before {
  content: ""; position: absolute; inset: 0 auto 0 0;
  width: 0; background: var(--color-primary-active);
  border-radius: var(--radius-lg); z-index: -1;
  transition: width var(--dur-base) var(--ease-out);
}
.btn-primary:hover { color: #fff; box-shadow: var(--shadow-md); }
.btn-primary:hover::before { width: 100%; }
.btn-primary:active { transform: scale(0.98); transition-duration: var(--dur-fast); }

/* Variante SECONDARY (acento de sistema) */
.btn-secondary { background: var(--color-accent); color: var(--color-ink-strong); box-shadow: var(--shadow-md); }
.btn-secondary::before {
  content: ""; position: absolute; inset: 0 auto 0 0;
  width: 0; background: var(--color-accent-active);
  border-radius: var(--radius-lg); z-index: -1;
  transition: width var(--dur-base) var(--ease-out);
}
.btn-secondary:hover { color: #fff; }
.btn-secondary:hover::before { width: 100%; }
.btn-secondary:active { transform: scale(0.98); transition-duration: var(--dur-fast); }

/* Variante OUTLINE (secundário de navbar/login) */
.btn-outline {
  background: transparent; color: var(--color-primary);
  border: 1px solid var(--color-primary);
  padding: 0.5rem 1.5rem; border-radius: var(--radius-xl);
  font-weight: 600; font-size: 0.9rem; width: auto;
}
.btn-outline:hover { background: var(--color-primary-tint); }
.btn-outline:active { background: var(--color-primary); color: #fff; transform: scale(0.98); }

/* Variante GHOST */
.btn-ghost { background: transparent; color: var(--color-primary); width: auto; padding: var(--space-2) var(--space-3); }
.btn-ghost:hover { background: var(--color-primary-tint); }
.btn-ghost:active { background: var(--color-primary-subtle); }

/* Variante DANGER (exclusão) */
.btn-danger { background: var(--color-danger); color: #fff; }
.btn-danger:hover { background: #991b1b; }
.btn-danger:active { background: #7f1d1d; transform: scale(0.98); }

/* Foco herdado do reset global (:focus-visible) — NÃO remover */
```

> Nota: `.btn-secundary` (typo atual) deve ser renomeado para `.btn-secondary` e atualizado nos JSX.

### 4.2 Input (`.inputField`) — estados

```css
.input_container { display: flex; flex-direction: column; margin-bottom: var(--space-3); text-align: left; }
.input_container label {
  color: var(--color-ink); font-weight: 500;
  font-size: var(--fs-small); padding: var(--space-1) 0; display: block;
}
.inputField {
  width: 100%; padding: 10px var(--space-3);
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  font-size: var(--fs-base); color: var(--color-ink);
  transition: border-color var(--dur-fast) var(--ease-out),
              box-shadow var(--dur-fast) var(--ease-out);
}
.inputField::placeholder { color: var(--color-ink-muted); opacity: 1; }
.inputField:hover:not(:disabled) { border-color: var(--color-primary-subtle); }
.inputField:focus { outline: none; border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(139, 111, 71, 0.25); }           /* focus */
.inputField:disabled { background: var(--color-disabled-bg); color: var(--color-disabled-ink); cursor: not-allowed; }
.inputField:read-only { background: var(--color-surface-2); }
.inputField.inputError, .inputField[aria-invalid="true"] {
  border: 2px solid var(--color-danger-action);                 /* erro */
}
.inputField.inputError:focus { box-shadow: 0 0 0 3px rgba(255, 0, 34, 0.25); }
.errorText {
  text-align: left; color: var(--color-danger-action);
  font-size: var(--fs-caption); line-height: var(--lh-caption);
  margin-top: var(--space-1); display: block; white-space: pre-line;
}
.errorText .icon-info { font-size: inherit; margin-right: 5px; color: var(--color-ink); }
```

### 4.3 Toast (feedback global) — cores semânticas

```css
.notification-item { border-radius: var(--radius-sm); box-shadow: var(--shadow-toast);
  transition: all var(--dur-base) var(--ease-out); }
.notification-item:hover   { transform: scale(1.01); }
.notification-item:active  { transform: scale(1.05); transition-duration: var(--dur-fast); }
.success { color: #047857; background-color: #7dffbc; }  /* operações concluídas */
.info    { color: #1e3a8a; background-color: #7eb8ff; }  /* informação de sistema */
.warning { color: #78350f; background-color: #ffe57e; }  /* quota perto do limite, avisos */
.error   { color: #7f1d1d; background-color: #ff7e7e; }  /* falhas de operação */
.notification-progress-bar { animation: progressBar var(--dur-slow) linear forwards; }
```

Posição fixa `top: 20px; right: 20px; z-index: var(--z-toast)`; duração padrão 3000–5000ms.

### 4.4 Spinner / Loading

```css
.spinner { font-size: 28px; width: .8em; height: .8em; }
/* blades mantêm #69717d → substituir por var(--color-primary) quando sobre fundo claro */
```
- Regra: **um** spinner por região de espera; em botões usar o estado `.is-loading` (secção 4.1), nunca desativar a página inteira sem overlay.

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
.btn-cadastro { background: var(--color-primary); color: #fff; border: 1px solid var(--color-primary); }
.btn-cadastro:hover { background: var(--color-primary-hover); border-color: var(--color-primary-hover); }
.btn-cadastro:active { background: var(--color-primary-active); transform: scale(0.98); }
.btn-login { background: transparent; color: var(--color-primary); border: 1px solid var(--color-primary); }
.btn-login:hover { background: var(--color-primary-tint); }
.btn-login:active { background: var(--color-primary); color: #fff; }
```

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
  background: var(--color-primary); color: #fff; font-weight: 600;
}
.sidebar-item[aria-disabled="true"] { color: var(--color-disabled-ink); cursor: not-allowed; pointer-events: none; }
```

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
.dropdown-item.is-danger { color: var(--color-danger-action); }
.dropdown-item.is-danger:hover { background: var(--color-danger-bg); }
@keyframes dd-in { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }
```

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

### 5.3 Fluxos de autenticação (Login / Register / ForgotPassword)
- Erro de credenciais → `Toast type="error"` **ou** `.errorText` no campo (erro de campo) — erro de servidor nunca apaga o que o utilizador digitou.
- Sucesso → `Toast type="success"` antes de redirecionar.
- Botão de submit durante requisição: `.is-loading`, `disabled`, largura estável.
- Links auxiliares ("Esqueceu a senha?", "Criar conta") em `--color-primary` com sublinhado no hover.

### 5.4 Operações de ficheiros/pastas (criar, renomear, mover, excluir)
- Exclusão: modal de confirmação + `btn-danger`; feedback via `Toast`.
- Sucesso: `Toast type="success"`; Falha: `Toast type="error"` com mensagem acionável.
- Operação em lote: spinner na região afetada, itens permanecem visíveis.

### 5.5 Landing Page vs Dashboard
- Landing: superfícies glass + `--color-primary` (marca).
- Dashboard autenticado: fundo `--color-bg`, superfícies `--color-surface`, acento `--color-accent` para ações de nuvem/upload, identidade mantida no item ativo da sidebar.

---

## 6. Checklist de revisão obrigatória

Antes de entregar qualquer componente, confirmar **todos** os itens:

- [ ] Usa exclusivamente `var(--...)` para cor, fonte, raio, sombra, espaço e duração.
- [ ] Tipografia na escala da secção 2 (Montserrat/Rubik, sem Segoe UI, peso ≤ 700).
- [ ] Tem **normal, hover, active, disabled, focus-visible** documentados; e **loading** se for assíncrono.
- [ ] `disabled` existe no DOM (`disabled`/`aria-disabled`), não só visualmente.
- [ ] Contraste AA verificado para todos os pares de cor usados (≥ 4.5:1).
- [ ] Alvo de clique ≥ 44px; `cursor: pointer` em clicáveis.
- [ ] Erros sempre com ícone **e** texto (nunca só cor).
- [ ] Apenas as 4 durações/2 easings da secção 3.7; `prefers-reduced-motion` respeitado.
- [ ] Z-index vem dos tokens (`--z-*`).
- [ ] Responsivo nos breakpoints 600/768/992/1200px.
- [ ] Estados por permissão conforme secção 5.1 (nenhum item proibido chega ao DOM).
- [ ] Feedback de operação via `Toast` (success/error/warning) conforme secção 5.4.

---

*Documento v1.0 — gerado a partir da consolidação de `App.css`, `index.css`, `navbar.css`, `button.css`, `input.css`, `Login.css`, `spinner.css`, `toast.jsx` e do schema de negócio (`perfil`, `permissao`, `pasta`, `arquivo`, `armazenamento`). Alterações a este documento exigem aprovação do líder técnico.*