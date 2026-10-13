import { useCallback, useContext, useEffect, useRef, useState } from "react";
import {
    FiCheck,
    FiEdit2,
    FiFolder,
    FiHardDrive,
    FiKey,
    FiPlus,
    FiSave,
    FiShield,
    FiTrash2,
    FiUser,
    FiUsers
} from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    createPapel,
    createPermissao,
    createRecurso,
    deletePapel,
    deletePermissao,
    deleteRecurso,
    listPapeis,
    listRecursos,
    setAuthToken,
    setPapelPermissoes,
    updatePapel,
    updatePermissao,
    updateRecurso
} from "../../api/axios.js";
import "./AdminPermissions.css";

const TOAST_DURATION = 4500;

const ICON_MAP = { FiFolder, FiHardDrive, FiShield, FiKey, FiUser, FiUsers };
const DEFAULT_ICON = FiShield;
const iconFor = (name) => ICON_MAP[name] || DEFAULT_ICON;

const EMPTY_PAPEL = { codigo: "", nome: "", descricao: "", ordem: 99, permissoes: [] };
const EMPTY_RECURSO = { codigo: "", nome: "", descricao: "", icone: "FiFolder", ordem: 99 };
const EMPTY_PERMISSAO = { codigo: "", nome: "", descricao: "", recursoId: null, ordem: 99 };

function AdminPermissions() {
    const { auth } = useContext(AuthContext);

    const [papeis, setPapeis] = useState([]);
    const [recursos, setRecursos] = useState([]);
    const [loading, setLoading] = useState(true);

    /* editor de perfil */
    const [draft, setDraft] = useState(null);       // { id?, codigo, nome, descricao, ordem }
    const [draftPerms, setDraftPerms] = useState(new Set());
    const [savingPapel, setSavingPapel] = useState(false);
    const [pendingDeletePapel, setPendingDeletePapel] = useState(null);

    /* gestão de recursos / permissões */
    const [recursoModal, setRecursoModal] = useState(null);      // { mode, recurso? }
    const [permissaoModal, setPermissaoModal] = useState(null);  // { mode, permissao?, recursoId? }
    const [pendingDeletePermissao, setPendingDeletePermissao] = useState(null);
    const [pendingDeleteRecurso, setPendingDeleteRecurso] = useState(null);
    const [savingRecurso, setSavingRecurso] = useState(false);

    const [toast, setToast] = useState(null);
    const titleRef = useRef(null);

    const closeToast = useCallback(() => setToast(null), []);
    const notify = (message, type = "error") => setToast({ message, type });

    useEffect(() => { titleRef.current?.focus(); }, []);
    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const [p, r] = await Promise.all([listPapeis(), listRecursos()]);
            setPapeis(Array.isArray(p.data) ? p.data : []);
            setRecursos(Array.isArray(r.data) ? r.data : []);
        } catch (err) {
            console.error("[admin-permissions] falha ao carregar", err);
            notify("Não foi possível carregar perfis e permissões.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (auth?.token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void load();
        }
    }, [auth?.token, load]);

    /* ——— seleção e gravação de perfil ——— */
    const selectPapel = (p) => {
        setDraft(p ? { id: p.id, codigo: p.codigo, nome: p.nome, descricao: p.descricao, ordem: p.ordem, sistema: p.sistema } : null);
        setDraftPerms(new Set(p ? p.permissoes || [] : []));
    };

    const startNewPapel = () => {
        setDraft({ ...EMPTY_PAPEL });
        setDraftPerms(new Set());
    };

    const togglePerm = (code) => {
        setDraftPerms((prev) => {
            const next = new Set(prev);
            if (next.has(code)) next.delete(code);
            else next.add(code);
            return next;
        });
    };

    const savePapel = async () => {
        if (!draft || !draft.nome.trim() || !draft.codigo.trim()) {
            notify("Indica o nome e o código do perfil.");
            return;
        }
        if (draftPerms.size === 0) {
            notify("Atribui pelo menos uma permissão ao perfil.");
            return;
        }
        setSavingPapel(true);
        try {
            let saved;
            if (draft.id) {
                const res = await updatePapel(draft.id, {
                    codigo: draft.codigo, nome: draft.nome, descricao: draft.descricao, ordem: draft.ordem
                });
                saved = res.data;
            } else {
                const res = await createPapel({
                    codigo: draft.codigo, nome: draft.nome, descricao: draft.descricao, ordem: draft.ordem
                });
                saved = res.data;
            }
            await setPapelPermissoes(saved.id, [...draftPerms]);
            notify(draft.id ? "Perfil atualizado." : "Perfil criado.", "success");
            await load();
            const updated = (await listPapeis()).data.find((p) => p.id === saved.id) || null;
            selectPapel(updated);
        } catch (err) {
            console.error("[admin-permissions] falha ao guardar perfil", err);
            notify(err?.response?.data?.message || "Não foi possível guardar o perfil.");
        } finally {
            setSavingPapel(false);
        }
    };

    const confirmDeletePapel = async () => {
        if (!pendingDeletePapel) return;
        try {
            await deletePapel(pendingDeletePapel.id);
            notify("Perfil eliminado.", "success");
            setPendingDeletePapel(null);
            setDraft(null);
            setDraftPerms(new Set());
            await load();
        } catch (err) {
            console.error("[admin-permissions] falha ao eliminar perfil", err);
            notify(err?.response?.data?.message || "Não foi possível eliminar o perfil.");
        }
    };

    /* ——— recursos e permissões ——— */
    const saveRecurso = async (e) => {
        e.preventDefault();
        const r = recursoModal.recurso;
        if (!r?.nome.trim() || !r?.codigo.trim()) {
            notify("Indica o nome e o código do recurso.");
            return;
        }
        setSavingRecurso(true);
        try {
            if (recursoModal.mode === "edit" && r.id) {
                await updateRecurso(r.id, { codigo: r.codigo, nome: r.nome, descricao: r.descricao, icone: r.icone, ordem: r.ordem });
                notify("Recurso atualizado.", "success");
            } else {
                await createRecurso({ codigo: r.codigo, nome: r.nome, descricao: r.descricao, icone: r.icone, ordem: r.ordem });
                notify("Recurso criado.", "success");
            }
            setRecursoModal(null);
            await load();
        } catch (err) {
            console.error("[admin-permissions] falha ao guardar recurso", err);
            notify(err?.response?.data?.message || "Não foi possível guardar o recurso.");
        } finally {
            setSavingRecurso(false);
        }
    };

    const savePermissao = async (e) => {
        e.preventDefault();
        const pm = permissaoModal.permissao;
        if (!pm?.nome.trim() || !pm?.codigo.trim()) {
            notify("Indica o nome e o código da permissão.");
            return;
        }
        if (!pm.recursoId) {
            notify("Escolhe o recurso desta permissão.");
            return;
        }
        setSavingRecurso(true);
        try {
            if (permissaoModal.mode === "edit" && pm.id) {
                await updatePermissao(pm.id, { codigo: pm.codigo, nome: pm.nome, descricao: pm.descricao, recursoId: pm.recursoId, ordem: pm.ordem });
                notify("Permissão atualizada.", "success");
            } else {
                await createPermissao({ codigo: pm.codigo, nome: pm.nome, descricao: pm.descricao, recursoId: pm.recursoId, ordem: pm.ordem });
                notify("Permissão criada.", "success");
            }
            setPermissaoModal(null);
            await load();
        } catch (err) {
            console.error("[admin-permissions] falha ao guardar permissão", err);
            notify(err?.response?.data?.message || "Não foi possível guardar a permissão.");
        } finally {
            setSavingRecurso(false);
        }
    };

    const confirmDeletePermissao = async () => {
        if (!pendingDeletePermissao) return;
        try {
            await deletePermissao(pendingDeletePermissao.id);
            notify("Permissão eliminada.", "success");
            setPendingDeletePermissao(null);
            await load();
        } catch (err) {
            console.error("[admin-permissions] falha ao eliminar permissão", err);
            notify(err?.response?.data?.message || "Não foi possível eliminar a permissão.");
        }
    };

    const confirmDeleteRecurso = async () => {
        if (!pendingDeleteRecurso) return;
        try {
            await deleteRecurso(pendingDeleteRecurso.id);
            notify("Recurso eliminado.", "success");
            setPendingDeleteRecurso(null);
            await load();
        } catch (err) {
            console.error("[admin-permissions] falha ao eliminar recurso", err);
            notify(err?.response?.data?.message || "Não foi possível eliminar o recurso.");
        }
    };

    const draftValid = draft && draft.nome.trim() && draft.codigo.trim() && draftPerms.size > 0;

    return (
        <section className="dashboard-page admin-permissions">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Perfis &amp; Permissões</h1>
            <p className="dashboard-page__text">Cria perfis e define, em cada um, os recursos e permissões a que dá acesso.</p>

            {/* ——— listagem + editor de perfis ——— */}
            <div className="card">
                <div className="admin-permissions__toolbar">
                    <h2 className="card__title"><FiShield aria-hidden="true" /> Perfis de acesso</h2>
                    <Button label="Novo perfil" variant="primary" icon={<FiPlus aria-hidden="true" />} onClick={startNewPapel} />
                </div>

                {loading ? (
                    <p className="dashboard-page__text">A carregar perfis...</p>
                ) : (
                    <div className="admin-permissions__grid">
                        <ul className="admin-permissions__roles" aria-label="Lista de perfis">
                            {papeis.map((p) => {
                                const Icon = iconFor(p.codigo === "ADMIN" ? "FiShield" : "FiUser");
                                const active = draft && draft.id === p.id;
                                return (
                                    <li key={p.id}>
                                        <button
                                            type="button"
                                            className={`admin-permissions__role${active ? " is-active" : ""}${p.sistema ? " is-system" : ""}`}
                                            onClick={() => selectPapel(p)}
                                            aria-pressed={!!active}
                                        >
                                            <span className="admin-permissions__role-icon"><Icon aria-hidden="true" /></span>
                                            <span className="admin-permissions__role-body">
                                                <strong>{p.nome}</strong>
                                                <span className="admin-permissions__role-code">{p.codigo}{p.sistema ? " · sistema" : ""}</span>
                                            </span>
                                            <span className="admin-permissions__role-meta">
                                                <span title="Utilizadores"><FiUsers aria-hidden="true" /> {p.utilizadores}</span>
                                                <span title="Permissões"><FiKey aria-hidden="true" /> {p.totalPermissoes}</span>
                                            </span>
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>

                        <div className="admin-permissions__editor">
                            {draft ? (
                                <>
                                    <div className="profile-grid">
                                        <div>
                                            <label htmlFor="papel-nome">Nome do perfil</label>
                                            <Input id="papel-nome" type="text" value={draft.nome}
                                                onChange={(e) => setDraft({ ...draft, nome: e.target.value })} required />
                                        </div>
                                        <div>
                                            <label htmlFor="papel-codigo">Código</label>
                                            {draft.sistema ? (
                                                <input id="papel-codigo" className="inputField" value={draft.codigo} readOnly aria-readonly="true" tabIndex={0} />
                                            ) : (
                                                <Input id="papel-codigo" type="text" value={draft.codigo}
                                                    onChange={(e) => setDraft({ ...draft, codigo: e.target.value.toUpperCase() })} required />
                                            )}
                                            {draft.sistema && <p className="formHint">Código de sistema não é editável.</p>}
                                        </div>
                                    </div>
                                    <div>
                                        <label htmlFor="papel-descricao">Descrição</label>
                                        <Input id="papel-descricao" type="text" value={draft.descricao || ""}
                                            onChange={(e) => setDraft({ ...draft, descricao: e.target.value })} />
                                    </div>

                                    <fieldset className="admin-permissions__perms">
                                        <legend className="card__title"><FiKey aria-hidden="true" /> Permissões</legend>
                                        {recursos.length === 0 ? (
                                            <p className="dashboard-page__text">Sem recursos definidos.</p>
                                        ) : (
                                            recursos.map((r) => {
                                                const perms = r.permissoes || [];
                                                if (perms.length === 0) return null;
                                                const Icon = iconFor(r.icone);
                                                return (
                                                    <div key={r.id} className="admin-permissions__recurso">
                                                        <div className="admin-permissions__recurso-head">
                                                            <Icon aria-hidden="true" />
                                                            <strong>{r.nome}</strong>
                                                            <span className="admin-permissions__recurso-code">{r.codigo}</span>
                                                        </div>
                                                        <div className="admin-permissions__recurso-perms">
                                                            {perms.map((pm) => {
                                                                const checked = draftPerms.has(pm.codigo);
                                                                return (
                                                                    <label key={pm.id} className={checked ? "is-checked" : ""}>
                                                                        <input
                                                                            type="checkbox"
                                                                            checked={checked}
                                                                            onChange={() => togglePerm(pm.codigo)}
                                                                        />
                                                                        <span className="admin-permissions__check"><FiCheck aria-hidden="true" /></span>
                                                                        <span>
                                                                            <strong>{pm.nome}</strong>
                                                                            <span className="admin-permissions__perm-code">{pm.codigo}</span>
                                                                        </span>
                                                                    </label>
                                                                );
                                                            })}
                                                        </div>
                                                    </div>
                                                );
                                            })
                                        )}
                                    </fieldset>

                                    <div className="admin-permissions__editor-actions">
                                        {draft.id && (
                                            <Button label="Eliminar perfil" variant="danger" icon={<FiTrash2 aria-hidden="true" />}
                                                disabled={draft.sistema} onClick={() => setPendingDeletePapel(papeis.find((p) => p.id === draft.id))} />
                                        )}
                                        <Button label="Guardar perfil" variant="primary" icon={<FiSave aria-hidden="true" />}
                                            loading={savingPapel} disabled={!draftValid} onClick={savePapel} />
                                    </div>
                                </>
                            ) : (
                                <p className="dashboard-page__text">Escolhe um perfil para ver e editar o seu acesso, ou cria um novo.</p>
                            )}
                        </div>
                    </div>
                )}
            </div>

            {/* ——— catálogo de recursos e permissões ——— */}
            <div className="card">
                <div className="admin-permissions__toolbar">
                    <h2 className="card__title"><FiFolder aria-hidden="true" /> Recursos &amp; permissões</h2>
                    <Button label="Novo recurso" variant="outline" icon={<FiPlus aria-hidden="true" />} onClick={() => setRecursoModal({ mode: "create", recurso: { ...EMPTY_RECURSO } })} />
                </div>
                {loading ? (
                    <p className="dashboard-page__text">A carregar recursos...</p>
                ) : recursos.length === 0 ? (
                    <p className="dashboard-page__text">Sem recursos definidos.</p>
                ) : (
                    <ul className="admin-permissions__catalog">
                        {recursos.map((r) => {
                            const Icon = iconFor(r.icone);
                            return (
                                <li key={r.id} className="admin-permissions__catalog-item">
                                    <div className="admin-permissions__catalog-head">
                                        <span className="admin-permissions__catalog-icon"><Icon aria-hidden="true" /></span>
                                        <div className="admin-permissions__catalog-info">
                                            <strong>{r.nome}</strong>
                                            <span>{r.codigo}{r.sistema ? " · sistema" : ""}</span>
                                            {r.descricao && <p>{r.descricao}</p>}
                                        </div>
                                        <div className="admin-permissions__catalog-actions">
                                            <button type="button" className="admin-permissions__icon-btn" title="Adicionar permissão"
                                                onClick={() => setPermissaoModal({ mode: "create", permissao: { ...EMPTY_PERMISSAO, recursoId: r.id }, recursoId: r.id })}>
                                                <FiPlus aria-hidden="true" /><span className="sr-only">Adicionar permissão a {r.nome}</span>
                                            </button>
                                            <button type="button" className="admin-permissions__icon-btn" title="Editar recurso"
                                                onClick={() => setRecursoModal({ mode: "edit", recurso: { ...r } })}>
                                                <FiEdit2 aria-hidden="true" /><span className="sr-only">Editar recurso {r.nome}</span>
                                            </button>
                                            <button type="button" className="admin-permissions__icon-btn is-danger" title="Eliminar recurso"
                                                disabled={r.sistema} onClick={() => setPendingDeleteRecurso(r)}>
                                                <FiTrash2 aria-hidden="true" /><span className="sr-only">Eliminar recurso {r.nome}</span>
                                            </button>
                                        </div>
                                    </div>
                                    <ul className="admin-permissions__catalog-perms">
                                        {(r.permissoes || []).map((pm) => (
                                            <li key={pm.id}>
                                                <span className="admin-permissions__perm-name">{pm.nome}</span>
                                                <code>{pm.codigo}</code>
                                                <div className="admin-permissions__catalog-perms-actions">
                                                    <button type="button" className="admin-permissions__icon-btn" title="Editar permissão"
                                                        onClick={() => setPermissaoModal({ mode: "edit", permissao: { ...pm }, recursoId: pm.recursoId })}>
                                                        <FiEdit2 aria-hidden="true" /><span className="sr-only">Editar {pm.nome}</span>
                                                    </button>
                                                    <button type="button" className="admin-permissions__icon-btn is-danger" title="Eliminar permissão"
                                                        disabled={pm.sistema} onClick={() => setPendingDeletePermissao(pm)}>
                                                        <FiTrash2 aria-hidden="true" /><span className="sr-only">Eliminar {pm.nome}</span>
                                                    </button>
                                                </div>
                                            </li>
                                        ))}
                                        {(r.permissoes || []).length === 0 && (
                                            <li className="admin-permissions__catalog-empty">Sem permissões.</li>
                                        )}
                                    </ul>
                                </li>
                            );
                        })}
                    </ul>
                )}
            </div>

            {/* ——— modal recurso ——— */}
            {recursoModal && (
                <div className="modal-backdrop" onClick={() => setRecursoModal(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Recurso" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">{recursoModal.mode === "edit" ? "Editar recurso" : "Novo recurso"}</h2>
                        <form onSubmit={saveRecurso} className="profile-form">
                            <div className="profile-grid">
                                <div>
                                    <label htmlFor="rec-codigo">Código</label>
                                    <Input id="rec-codigo" type="text" value={recursoModal.recurso.codigo}
                                        onChange={(e) => setRecursoModal({ ...recursoModal, recurso: { ...recursoModal.recurso, codigo: e.target.value.toUpperCase() } })} required />
                                </div>
                                <div>
                                    <label htmlFor="rec-ordem">Ordem</label>
                                    <Input id="rec-ordem" type="number" value={recursoModal.recurso.ordem ?? ""}
                                        onChange={(e) => setRecursoModal({ ...recursoModal, recurso: { ...recursoModal.recurso, ordem: Number(e.target.value) } })} />
                                </div>
                            </div>
                            <div>
                                <label htmlFor="rec-nome">Nome</label>
                                <Input id="rec-nome" type="text" value={recursoModal.recurso.nome}
                                    onChange={(e) => setRecursoModal({ ...recursoModal, recurso: { ...recursoModal.recurso, nome: e.target.value } })} required />
                            </div>
                            <div>
                                <label htmlFor="rec-desc">Descrição</label>
                                <Input id="rec-desc" type="text" value={recursoModal.recurso.descricao || ""}
                                    onChange={(e) => setRecursoModal({ ...recursoModal, recurso: { ...recursoModal.recurso, descricao: e.target.value } })} />
                            </div>
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setRecursoModal(null)} disabled={savingRecurso} />
                                <Button label="Guardar" variant="primary" type="submit" loading={savingRecurso} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ——— modal permissão ——— */}
            {permissaoModal && (
                <div className="modal-backdrop" onClick={() => setPermissaoModal(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Permissão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">{permissaoModal.mode === "edit" ? "Editar permissão" : "Nova permissão"}</h2>
                        <form onSubmit={savePermissao} className="profile-form">
                            <div className="profile-grid">
                                <div>
                                    <label htmlFor="pm-codigo">Código</label>
                                    <Input id="pm-codigo" type="text" value={permissaoModal.permissao.codigo}
                                        onChange={(e) => setPermissaoModal({ ...permissaoModal, permissao: { ...permissaoModal.permissao, codigo: e.target.value.toUpperCase() } })} required />
                                </div>
                                <div>
                                    <label htmlFor="pm-ordem">Ordem</label>
                                    <Input id="pm-ordem" type="number" value={permissaoModal.permissao.ordem ?? ""}
                                        onChange={(e) => setPermissaoModal({ ...permissaoModal, permissao: { ...permissaoModal.permissao, ordem: Number(e.target.value) } })} />
                                </div>
                            </div>
                            <div>
                                <label htmlFor="pm-nome">Nome</label>
                                <Input id="pm-nome" type="text" value={permissaoModal.permissao.nome}
                                    onChange={(e) => setPermissaoModal({ ...permissaoModal, permissao: { ...permissaoModal.permissao, nome: e.target.value } })} required />
                            </div>
                            <div>
                                <label htmlFor="pm-desc">Descrição</label>
                                <Input id="pm-desc" type="text" value={permissaoModal.permissao.descricao || ""}
                                    onChange={(e) => setPermissaoModal({ ...permissaoModal, permissao: { ...permissaoModal.permissao, descricao: e.target.value } })} />
                            </div>
                            <div>
                                <label htmlFor="pm-recurso">Recurso</label>
                                <select id="pm-recurso" className="admin-users__role" value={permissaoModal.permissao.recursoId ?? ""}
                                    onChange={(e) => setPermissaoModal({ ...permissaoModal, permissao: { ...permissaoModal.permissao, recursoId: Number(e.target.value) } })}>
                                    <option value="" disabled>Escolher recurso</option>
                                    {recursos.map((r) => (
                                        <option key={r.id} value={r.id}>{r.nome} ({r.codigo})</option>
                                    ))}
                                </select>
                            </div>
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setPermissaoModal(null)} disabled={savingRecurso} />
                                <Button label="Guardar" variant="primary" type="submit" loading={savingRecurso} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ——— modais de confirmação ——— */}
            {pendingDeletePapel && (
                <div className="modal-backdrop" onClick={() => setPendingDeletePapel(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Confirmar exclusão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Eliminar perfil?</h2>
                        <p className="dashboard-page__text">Vais eliminar o perfil <strong>{pendingDeletePapel.nome}</strong> ({pendingDeletePapel.codigo}).</p>
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setPendingDeletePapel(null)} />
                            <Button label="Eliminar" variant="danger" onClick={confirmDeletePapel} />
                        </div>
                    </div>
                </div>
            )}
            {pendingDeletePermissao && (
                <div className="modal-backdrop" onClick={() => setPendingDeletePermissao(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Confirmar exclusão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Eliminar permissão?</h2>
                        <p className="dashboard-page__text">Vais eliminar <strong>{pendingDeletePermissao.nome}</strong> ({pendingDeletePermissao.codigo}). Será removida de todos os perfis.</p>
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setPendingDeletePermissao(null)} />
                            <Button label="Eliminar" variant="danger" onClick={confirmDeletePermissao} />
                        </div>
                    </div>
                </div>
            )}
            {pendingDeleteRecurso && (
                <div className="modal-backdrop" onClick={() => setPendingDeleteRecurso(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Confirmar exclusão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Eliminar recurso?</h2>
                        <p className="dashboard-page__text">Vais eliminar o recurso <strong>{pendingDeleteRecurso.nome}</strong> ({pendingDeleteRecurso.codigo}).</p>
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setPendingDeleteRecurso(null)} />
                            <Button label="Eliminar" variant="danger" onClick={confirmDeleteRecurso} />
                        </div>
                    </div>
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default AdminPermissions;