import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { FiPlus, FiTrash2, FiUsers } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import { changeUserRole, createUser, deleteUser, listUsers, setAuthToken } from "../../api/axios.js";
import "./AdminUsers.css";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TOAST_DURATION = 4500;

function AdminUsers() {
    const { auth } = useContext(AuthContext);

    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [savingRole, setSavingRole] = useState(null);
    const [showCreate, setShowCreate] = useState(false);
    const [creating, setCreating] = useState(false);
    const [pendingDelete, setPendingDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [toast, setToast] = useState(null);
    const titleRef = useRef(null);

    const [newUser, setNewUser] = useState({ name: "", surname: "", email: "", password: "", role: "USER" });

    const closeToast = useCallback(() => setToast(null), []);
    useEffect(() => { titleRef.current?.focus(); }, []);
    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const res = await listUsers();
            setUsers(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("[admin-users] falha ao carregar", err);
            setToast({ message: "Não foi possível carregar os utilizadores.", type: "error" });
        } finally {
            setLoading(false);
        }
    }, []);

     
    useEffect(() => {
        if (auth?.token) {
            void load();
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps, react-hooks/set-state-in-effect
    }, [auth?.token]);

    const myEmail = auth?.user?.email;
    const roleError = (msg) => setToast({ message: msg, type: "error" });

    const handleRoleChange = async (user, nextRole) => {
        if (nextRole === user.role) return;
        setSavingRole(user.id);
        try {
            const res = await changeUserRole(user.id, { role: nextRole });
            setUsers((prev) => prev.map((u) => (u.id === user.id ? res.data : u)));
            setToast({ message: `Papel de ${user.name} atualizado para ${nextRole}.`, type: "success" });
        } catch (err) {
            console.error("[admin-users] falha ao mudar papel", err);
            roleError(err?.response?.data?.message || "Não foi possível alterar o papel.");
            load(); /* reverte o select para o estado real */
        } finally { setSavingRole(null); }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        if (!USER_REGEX.test(newUser.name) || !USER_REGEX.test(newUser.surname)) {
            roleError("Nome e apelido: mínimo 3 caracteres, a começar por letra.");
            return;
        }
        if (!EMAIL_REGEX.test(newUser.email)) { roleError("Email inválido."); return; }
        if (!PWD_REGEX.test(newUser.password)) {
            roleError("A password não cumpre os requisitos (8-24, maiúscula, minúscula, número, símbolo).");
            return;
        }
        setCreating(true);
        try {
            await createUser(newUser);
            setToast({ message: "Utilizador criado com sucesso.", type: "success" });
            setShowCreate(false);
            setNewUser({ name: "", surname: "", email: "", password: "", role: "USER" });
            load();
        } catch (err) {
            console.error("[admin-users] falha ao criar", err);
            roleError(err?.response?.data?.message || "Não foi possível criar o utilizador.");
        } finally { setCreating(false); }
    };

    const handleDelete = async () => {
        if (!pendingDelete) return;
        setDeleting(true);
        try {
            await deleteUser(pendingDelete.id);
            setToast({ message: "Utilizador excluído.", type: "success" });
            setPendingDelete(null);
            load();
        } catch (err) {
            console.error("[admin-users] falha ao excluir", err);
            roleError(err?.response?.data?.message || "Não foi possível excluir.");
        } finally { setDeleting(false); }
    };

    const nameOk = USER_REGEX.test(newUser.name);
    const surnameOk = USER_REGEX.test(newUser.surname);
    const emailOk = EMAIL_REGEX.test(newUser.email);
    const pwdOk = PWD_REGEX.test(newUser.password);

    return (
        <section className="dashboard-page admin-users">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Utilizadores</h1>
            <p className="dashboard-page__text">Gere contas e define papéis (USER / ADMIN) no sistema.</p>

            <div className="card">
                <div className="admin-users__toolbar">
                    <h2 className="card__title"><FiUsers aria-hidden="true" /> {users.length} utilizador{users.length === 1 ? "" : "es"}</h2>
                    <Button label="Criar utilizador" variant="primary" icon={<FiPlus aria-hidden="true" />} onClick={() => setShowCreate(true)} />
                </div>

                {loading ? (
                    <p className="dashboard-page__text">A carregar utilizadores...</p>
                ) : users.length === 0 ? (
                    <p className="dashboard-page__text">Sem utilizadores para mostrar.</p>
                ) : (
                    <div className="admin-users__table-wrap">
                        <table className="admin-users__table">
                            <thead>
                                <tr>
                                    <th scope="col">Nome</th>
                                    <th scope="col">Email</th>
                                    <th scope="col">Papel</th>
                                    <th scope="col">Registo</th>
                                    <th scope="col">Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((u) => {
                                    const isSelf = u.email === myEmail;
                                    return (
                                        <tr key={u.id}>
                                            <td data-label="Nome">
                                                <span className="admin-users__name">{u.name} {u.surname}</span>
                                                {isSelf && <span className="admin-users__you">tu</span>}
                                            </td>
                                            <td data-label="Email">{u.email}</td>
                                            <td data-label="Papel">
                                                <select
                                                    className="admin-users__role"
                                                    value={u.role}
                                                    disabled={savingRole === u.id}
                                                    aria-label={`Papel de ${u.name} ${u.surname}`}
                                                    onChange={(e) => handleRoleChange(u, e.target.value)}
                                                >
                                                    <option value="USER">USER</option>
                                                    <option value="ADMIN">ADMIN</option>
                                                </select>
                                            </td>
                                            <td data-label="Registo">{u.dataRegistro ? String(u.dataRegistro).split("T")[0].split("-").reverse().join("/") : "—"}</td>
                                            <td data-label="Ações">
                                                <button
                                                    type="button"
                                                    className="admin-users__delete"
                                                    title={isSelf ? "Não podes excluir a tua própria conta" : `Excluir ${u.name}`}
                                                    disabled={isSelf || deleting}
                                                    onClick={() => setPendingDelete(u)}
                                                >
                                                    <FiTrash2 aria-hidden="true" />
                                                    <span className="sr-only">Excluir {u.name} {u.surname}</span>
                                                </button>
                                            </td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

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
                                <label htmlFor="new-role">Papel</label>
                                <select id="new-role" className="admin-users__role" value={newUser.role} onChange={(e) => setNewUser({ ...newUser, role: e.target.value })}>
                                    <option value="USER">USER</option>
                                    <option value="ADMIN">ADMIN</option>
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
