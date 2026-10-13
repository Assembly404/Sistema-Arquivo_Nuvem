import { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import {
    FiEdit2,
    FiMail,
    FiPauseCircle,
    FiPlayCircle,
    FiPlus,
    FiSearch,
    FiTrash2,
    FiUsers
} from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    createUser,
    deleteUser,
    listPapeis,
    listUsers,
    setAuthToken,
    suspendUser,
    unsuspendUser,
    updateUser
} from "../../api/axios.js";
import "./AdminUsers.css";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TOAST_DURATION = 4500;

const fallbackPapeis = [
    { codigo: "USER", nome: "Utilizador" },
    { codigo: "ADMIN", nome: "Administrador" }
];

function AdminUsers() {
    const { auth } = useContext(AuthContext);

    const [users, setUsers] = useState([]);
    const [papeis, setPapeis] = useState(fallbackPapeis);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState("");
    const [debouncedSearch, setDebouncedSearch] = useState("");
    const [showCreate, setShowCreate] = useState(false);
    const [creating, setCreating] = useState(false);
    const [editUser, setEditUser] = useState(null);        // utilizador em edição
    const [editForm, setEditForm] = useState(null);        // { name, surname, role, quotaGiB }
    const [savingEdit, setSavingEdit] = useState(false);
    const [togglingSuspend, setTogglingSuspend] = useState(null);
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [toast, setToast] = useState(null);
    const titleRef = useRef(null);

    const [newUser, setNewUser] = useState({ name: "", surname: "", email: "", password: "", role: "USER" });

    const closeToast = useCallback(() => setToast(null), []);
    const notify = (message, type = "error") => setToast({ message, type });

    useEffect(() => { titleRef.current?.focus(); }, []);
    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    /* debounce da pesquisa */
    useEffect(() => {
        const t = setTimeout(() => setDebouncedSearch(search), 300);
        return () => clearTimeout(t);
    }, [search]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const params = debouncedSearch.trim() ? { search: debouncedSearch.trim() } : {};
            const [usersRes, papeisRes] = await Promise.all([
                listUsers(params),
                listPapeis().catch((err) => {
                    console.error("[admin-users] falha ao carregar perfis", err);
                    return null;
                })
            ]);
            setUsers(Array.isArray(usersRes.data) ? usersRes.data : []);
            if (papeisRes && Array.isArray(papeisRes.data) && papeisRes.data.length > 0) {
                setPapeis(papeisRes.data.map((p) => ({ codigo: p.codigo, nome: p.nome })));
            }
        } catch (err) {
            console.error("[admin-users] falha ao carregar", err);
            notify("Não foi possível carregar os utilizadores.");
        } finally {
            setLoading(false);
        }
    }, [debouncedSearch]);

    useEffect(() => {
        if (auth?.token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void load();
        }
    }, [auth?.token, load]);

    const myEmail = auth?.user?.email;
    const myId = auth?.user?.id ?? auth?.user?.userID ?? null;

    const counts = useMemo(() => {
        const active = users.filter((u) => !u.suspended).length;
        return { total: users.length, active, suspended: users.length - active };
    }, [users]);

    const formatQuota = (bytes) => {
        if (bytes == null || bytes <= 0) return { label: "Ilimitada", unlimited: true };
        const gib = bytes / Math.pow(1024, 3);
        return { label: `${gib % 1 === 0 ? gib.toFixed(0) : gib.toFixed(2)} GiB`, unlimited: false };
    };

    /* ——— criar ——— */
    const handleCreate = async (e) => {
        e.preventDefault();
        if (!USER_REGEX.test(newUser.name) || !USER_REGEX.test(newUser.surname)) {
            notify("Nome e apelido: mínimo 3 caracteres, a começar por letra.");
            return;
        }
        if (!EMAIL_REGEX.test(newUser.email)) { notify("Email inválido."); return; }
        if (!PWD_REGEX.test(newUser.password)) {
            notify("A password não cumpre os requisitos (8-24, maiúscula, minúscula, número, símbolo).");
            return;
        }
        setCreating(true);
        try {
            await createUser(newUser);
            notify("Utilizador criado com sucesso.", "success");
            setShowCreate(false);
            setNewUser({ name: "", surname: "", email: "", password: "", role: "USER" });
            load();
        } catch (err) {
            console.error("[admin-users] falha ao criar", err);
            notify(err?.response?.data?.message || "Não foi possível criar o utilizador.");
        } finally { setCreating(false); }
    };

    /* ——— editar ——— */
    const startEdit = (u) => {
        const quota = u.quotaBytes ? String(Math.round((u.quotaBytes / Math.pow(1024, 3)) * 100) / 100) : "";
        setEditUser(u);
        setEditForm({ name: u.name, surname: u.surname, role: u.role, quotaGiB: quota });
    };

    const handleEditSubmit = async (e) => {
        e.preventDefault();
        if (!USER_REGEX.test(editForm.name) || !USER_REGEX.test(editForm.surname)) {
            notify("O nome e o apelido devem ter mínimo 3 caracteres, a começar por letra.");
            return;
        }
        let quotaBytes = null;
        const trimmed = editForm.quotaGiB.trim();
        if (trimmed) {
            const gib = Number(trimmed.replace(",", "."));
            if (!Number.isFinite(gib) || gib <= 0) {
                notify("A quota tem de ser um número positivo em GiB (em branco = ilimitada).");
                return;
            }
            quotaBytes = Math.round(gib * Math.pow(1024, 3));
        }
        setSavingEdit(true);
        try {
            const res = await updateUser(editUser.id, {
                name: editForm.name,
                surname: editForm.surname,
                role: editForm.role,
                quotaBytes
            });
            setUsers((prev) => prev.map((x) => (x.id === editUser.id ? res.data : x)));
            notify("Utilizador atualizado.", "success");
            setEditUser(null);
            setEditForm(null);
        } catch (err) {
            console.error("[admin-users] falha ao atualizar", err);
            notify(err?.response?.data?.message || "Não foi possível atualizar o utilizador.");
        } finally { setSavingEdit(false); }
    };

    /* ——— suspender / reativar ——— */
    const toggleSuspend = async (u) => {
        setTogglingSuspend(u.id);
        try {
            const res = u.suspended ? await unsuspendUser(u.id) : await suspendUser(u.id);
            setUsers((prev) => prev.map((x) => (x.id === u.id ? res.data : x)));
            notify(u.suspended ? "Conta reativada." : "Conta suspensa.", "success");
        } catch (err) {
            console.error("[admin-users] falha ao alternar suspensão", err);
            notify(err?.response?.data?.message || "Não foi possível alternar o estado da conta.");
        } finally { setTogglingSuspend(null); }
    };

    /* ——— eliminar ——— */
    const handleDelete = async () => {
        if (!pendingDelete) return;
        setDeleting(true);
        try {
            await deleteUser(pendingDelete.id);
            notify("Utilizador excluído.", "success");
            setPendingDelete(null);
            load();
        } catch (err) {
            console.error("[admin-users] falha ao excluir", err);
            notify(err?.response?.data?.message || "Não foi possível excluir.");
        } finally { setDeleting(false); }
    };

    const nameOk = USER_REGEX.test(newUser.name);
    const surnameOk = USER_REGEX.test(newUser.surname);
    const emailOk = EMAIL_REGEX.test(newUser.email);
    const pwdOk = PWD_REGEX.test(newUser.password);

    /* ——— render ——— */
    return (
        <section className="dashboard-page admin-users">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Utilizadores</h1>
            <p className="dashboard-page__text">Pesquisa contas, escolhe o perfil de cada utilizador e gere o seu acesso.</p>

            <div className="card">
                <div className="admin-users__toolbar">
                    <div className="admin-users__toolbar-left">
                        <h2 className="card__title"><FiUsers aria-hidden="true" /> {users.length} utilizador{users.length === 1 ? "" : "es"}</h2>
                        <div className="admin-users__stats" aria-label="Resumo">
                            <span className="admin-users__stat">{counts.active} ativos</span>
                            <span className="admin-users__stat is-warn">{counts.suspended} suspensos</span>
                        </div>
                    </div>
                    <div className="admin-users__toolbar-right">
                        <div className="admin-users__search">
                            <FiSearch aria-hidden="true" />
                            <Input type="search" value={search} placeholder="Pesquisar por nome ou email"
                                onChange={(e) => setSearch(e.target.value)} aria-label="Pesquisar utilizadores" />
                        </div>
                        <Button label="Criar utilizador" variant="primary" icon={<FiPlus aria-hidden="true" />} onClick={() => setShowCreate(true)} />
                    </div>
                </div>

                {loading ? (
                    <p className="dashboard-page__text">A carregar utilizadores...</p>
                ) : users.length === 0 ? (
                    <p className="dashboard-page__text">
                        {debouncedSearch.trim()
                            ? `Sem resultados para "${debouncedSearch.trim()}".`
                            : "Sem utilizadores para mostrar."}
                    </p>
                ) : (
                    <div className="admin-users__table-wrap">
                        <table className="admin-users__table">
                            <thead>
                                <tr>
                                    <th scope="col">Utilizador</th>
                                    <th scope="col">Perfil</th>
                                    <th scope="col">Quota</th>
                                    <th scope="col">Estado</th>
                                    <th scope="col">Registo</th>
                                    <th scope="col">Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((u) => {
                                    const isSelf = (myId !== null && myId !== undefined && u.id === myId) || (!myId && u.email === myEmail);
                                    const quota = formatQuota(u.quotaBytes);
                                    const papel = papeis.find((p) => p.codigo === u.role);
                                    const initials = `${u.name?.[0] || ""}${u.surname?.[0] || ""}`.toUpperCase() || "U";
                                    return (
                                        <tr key={u.id} className={u.suspended ? "is-suspended" : ""}>
                                            <td data-label="Utilizador">
                                                <div className="admin-users__user">
                                                    <span className="admin-users__avatar" aria-hidden="true">{initials}</span>
                                                    <span className="admin-users__user-info">
                                                        <span className="admin-users__name">
                                                            {u.name} {u.surname}
                                                            {isSelf && <span className="admin-users__you">tu</span>}
                                                        </span>
                                                        <span className="admin-users__email"><FiMail aria-hidden="true" /> {u.email}</span>
                                                    </span>
                                                </div>
                                            </td>
                                            <td data-label="Perfil">
                                                <span className="admin-users__role-badge">
                                                    {papel?.nome || u.role}
                                                    <code>{u.role}</code>
                                                </span>
                                            </td>
                                            <td data-label="Quota">
                                                <span className={quota.unlimited ? "" : ""}>{quota.label}</span>
                                            </td>
                                            <td data-label="Estado">
                                                <span className={`admin-users__state${u.suspended ? " is-suspended" : " is-active"}`}>
                                                    {u.suspended ? "Suspenso" : "Ativo"}
                                                </span>
                                            </td>
                                            <td data-label="Registo">
                                                {u.dataRegistro ? String(u.dataRegistro).split("T")[0].split("-").reverse().join("/") : "—"}
                                            </td>
                                            <td data-label="Ações">
                                                <div className="admin-users__actions">
                                                    <button type="button" className="admin-users__action" title={`Editar ${u.name}`} onClick={() => startEdit(u)}>
                                                        <FiEdit2 aria-hidden="true" /><span className="sr-only">Editar {u.name} {u.surname}</span>
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className={`admin-users__action ${u.suspended ? "" : "is-danger"}`}
                                                        title={u.suspended ? `Reativar ${u.name}` : `Suspender ${u.name}`}
                                                        disabled={isSelf || togglingSuspend === u.id}
                                                        onClick={() => toggleSuspend(u)}
                                                    >
                                                        {u.suspended
                                                            ? <FiPlayCircle aria-hidden="true" />
                                                            : <FiPauseCircle aria-hidden="true" />}
                                                        <span className="sr-only">{u.suspended ? `Reativar ${u.name}` : `Suspender ${u.name}`}</span>
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="admin-users__action is-danger"
                                                        title={isSelf ? "Não podes excluir a tua própria conta" : `Excluir ${u.name}`}
                                                        disabled={isSelf || deleting}
                                                        onClick={() => setPendingDelete(u)}
                                                    >
                                                        <FiTrash2 aria-hidden="true" />
                                                        <span className="sr-only">Excluir {u.name} {u.surname}</span>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* ——— criar utilizador ——— */}
            {showCreate && (
                <div className="modal-backdrop" onClick={() => setShowCreate(false)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Criar utilizador" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Criar utilizador</h2>
                        <form onSubmit={handleCreate} className="profile-form">
                            <div className="profile-grid">
                                <div>
                                    <label htmlFor="new-name">Nome</label>
                                    <Input id="new-name" type="text" value={newUser.name} onChange={(e) => setNewUser({ ...newUser, name: e.target.value })} required />
                                </div>
                                <div>
                                    <label htmlFor="new-surname">Apelido</label>
                                    <Input id="new-surname" type="text" value={newUser.surname} onChange={(e) => setNewUser({ ...newUser, surname: e.target.value })} required />
                                </div>
                            </div>
                            <div>
                                <label htmlFor="new-email">Email</label>
                                <Input id="new-email" type="email" value={newUser.email} onChange={(e) => setNewUser({ ...newUser, email: e.target.value })} required />
                            </div>
                            <div>
                                <label htmlFor="new-password">Password inicial</label>
                                <Input id="new-password" type="password" value={newUser.password} onChange={(e) => setNewUser({ ...newUser, password: e.target.value })} required autoComplete="new-password" />
                                <p className="formHint">8-24 caracteres, com maiúscula, minúscula, número e símbolo (!@#$%).</p>
                            </div>
                            <div>
                                <label htmlFor="new-role">Perfil</label>
                                <select id="new-role" className="admin-users__role" value={newUser.role} onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}>
                                    {papeis.map((p) => (
                                        <option key={p.codigo} value={p.codigo}>{p.nome} ({p.codigo})</option>
                                    ))}
                                </select>
                            </div>
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setShowCreate(false)} />
                                <Button label="Criar" variant="primary" type="submit" loading={creating} disabled={!nameOk || !surnameOk || !emailOk || !pwdOk} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ——— editar utilizador ——— */}
            {editUser && editForm && (
                <div className="modal-backdrop" onClick={() => setEditUser(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Editar utilizador" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Editar {editUser.name} {editUser.surname}</h2>
                        <form onSubmit={handleEditSubmit} className="profile-form">
                            <div className="profile-grid">
                                <div>
                                    <label htmlFor="edit-name">Nome</label>
                                    <Input id="edit-name" type="text" value={editForm.name} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} required />
                                </div>
                                <div>
                                    <label htmlFor="edit-surname">Apelido</label>
                                    <Input id="edit-surname" type="text" value={editForm.surname} onChange={(e) => setEditForm({ ...editForm, surname: e.target.value })} required />
                                </div>
                            </div>
                            <div>
                                <label htmlFor="edit-role">Perfil</label>
                                <select id="edit-role" className="admin-users__role" value={editForm.role} onChange={(e) => setEditForm({ ...editForm, role: e.target.value })}>
                                    {papeis.map((p) => (
                                        <option key={p.codigo} value={p.codigo}>{p.nome} ({p.codigo})</option>
                                    ))}
                                </select>
                                <p className="formHint">O perfil define os recursos que este utilizador pode aceder.</p>
                            </div>
                            <div>
                                <label htmlFor="edit-quota">Quota (GiB)</label>
                                <Input id="edit-quota" type="number" min="0" step="0.5" value={editForm.quotaGiB ?? ""}
                                    onChange={(e) => setEditForm({ ...editForm, quotaGiB: e.target.value })}
                                    placeholder="Em branco = ilimitada" />
                            </div>
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => { setEditUser(null); setEditForm(null); }} disabled={savingEdit} />
                                <Button label="Guardar" variant="primary" type="submit" loading={savingEdit} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* ——— confirmar exclusão ——— */}
            {pendingDelete && (
                <div className="modal-backdrop" onClick={() => setPendingDelete(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Confirmar exclusão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Excluir utilizador?</h2>
                        <p className="dashboard-page__text">Vais excluir <strong>{pendingDelete.name} {pendingDelete.surname}</strong> ({pendingDelete.email}). Esta ação não pode ser desfeita.</p>
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setPendingDelete(null)} />
                            <Button label="Excluir" variant="danger" loading={deleting} onClick={handleDelete} />
                        </div>
                    </div>
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default AdminUsers;