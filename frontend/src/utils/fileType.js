const EXT_TONE = {
    doc: "doc", docx: "doc", txt: "doc", md: "doc", rtf: "doc", odt: "doc",
    xls: "sheet", xlsx: "sheet", csv: "sheet", ods: "sheet",
    ppt: "slide", pptx: "slide", odp: "slide",
    pdf: "pdf",
    png: "img", jpg: "img", jpeg: "img", gif: "img", webp: "img", svg: "img", bmp: "img", heic: "img",
    zip: "zip", rar: "zip", "7z": "zip", tar: "zip", gz: "zip",
    js: "code", ts: "code", jsx: "code", tsx: "code", json: "code", java: "code",
    py: "code", c: "code", cpp: "code", html: "code", css: "code", sh: "code",
    sql: "code", xml: "code", yml: "code", yaml: "code", go: "code", rs: "code"
};

const CATEGORY_OF = {
    doc: "documentos",
    sheet: "folhas",
    slide: "apresentacoes",
    pdf: "pdf",
    img: "imagens",
    zip: "arquivos",
    code: "codigo",
    other: "outros"
};

const extensionOf = (name = "") => {
    const base = String(name).split("?")[0].split("#")[0];
    const dot = base.lastIndexOf(".");
    return dot >= 0 ? base.slice(dot + 1).toLowerCase() : "";
};

export const fileTone = (name) => EXT_TONE[extensionOf(name)] || "other";

export const fileCategory = (name) => CATEGORY_OF[fileTone(name)];

export const FILE_FILTERS = ["todos", "documentos", "folhas", "apresentacoes", "pdf", "imagens", "arquivos", "codigo", "outros"];