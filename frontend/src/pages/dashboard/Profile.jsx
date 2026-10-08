import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router";
import { FiHardDrive, FiLock, FiMail, FiShield, FiTrash2, FiUser } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import { changePassword, deleteMe, getMe, patchMe, setAuthToken } from "../../api/axios.js";
import "./Profile.css";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const TOAST_DURATION = 4500;

const passwordChecks = (pwd) => ([
    { label: "8 a 24 caracteres", ok: pwd.length >= 8 && pwd.length <= 24 },
    { label: "Uma letra maiúscula", ok: /[A-Z]/.test(pwd) },
    { label: "Uma letra minúscula", ok: /[a-z]/.test(pwd) },
    { label: "Um número", ok: /[0-9]/.test(pwd) },
    { label: "Um símbolo (!@#$%)", ok: /[!@#$%]/.test(pwd) }
]);

function Profile() {
    const { auth, setAuth, hasPermission } = useContext(AuthContext);

    const navigate = useNavigate();
    const [me, setMe] = useState(auth?.user || null);
    const [loadingMe, setLoadingMe] = useState(() => !auth?.user);
    const [name, setName] = useState(auth?.user?.name || "");
    const [surname, setSurname] = useState(auth?.user?.surname || "");
    const [saving, setSaving] = useState(false);
    const [currentPwd, setCurrentPwd] = useState("");
    const [newPwd, setNewPwd] = useState("");
    const [confirmPwd, setConfirmPwd] = useState("");
    const [savingPwd, setSavingPwd] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [confirmDelete, setConfirmDelete] = useState("");
    const [deleting, setDeleting] = useState(false);
    const [tab, setTab] = useState("dados");
    const [toast, setToast] = useState(null);
    const titleRef = useRef(null);
    const closeToast = useCallback(() => setToast(null), []);
    useEffect(() => { titleRef.current?.focus(); }, []);
    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);
    useEffect(() => {
        if (!auth?.token) return undefined;
        let mounted = true;
        getMe().then((res) => {
            if (!mounted) return;
            setMe(res.data);
            setName(res.data?.name || "");
            setSurname(res.data?.surname || "");
            setAuth((prev) => ({ ...prev, user: { ...(prev.user || {}), ...res.data } }));
        }).catch((err) => {
            console.error("[profile] falha ao carregar perfil", err);
            if (mounted) setToast({ message: "Não foi possível carregar o perfil.", type: "error" });
        }).finally(() => { if (mounted) setLoadingMe(false); });
        return () => { mounted = false; };
    }, [auth?.token, setAuth]);
    const role = me?.role || auth?.user?.role || "USER";
    const isAdmin = role === "ADMIN";
    const permissions = me?.permissions || auth?.user?.permissions || [];
    const canDelete = isAdmin || hasPermission("ARQUIVO_EXCLUIR") || permissions.includes("ARQUIVO_EXCLUIR");
    const initials = `${name?.[0] || ""}${surname?.[0] || ""}`.toUpperCase() || "U";
    const nameError = name && !USER_REGEX.test(name) ? "Mínimo 3 caracteres, a começar por letra." : "";
    const surnameError = surname && !USER_REGEX.test(surname) ? "Mínimo 3 caracteres." : "";
    const newPwdError = newPwd && !PWD_REGEX.test(newPwd) ? "8 a 24 caracteres, com maiúscula, minúscula, número e símbolo." : "";
    const confirmError = confirmPwd && newPwd !== confirmPwd ? "As passwords não coincidem." : "";
    /* — Cabeçalho estilo Instagram: só dados reais do /users/me (Regra 3) — */
    const username = "@" + (me?.email || auth?.user?.email || "utilizador").split("@")[0];
    const memberSince = (() => {
        const raw = me?.dataRegistro;
        if (!raw) return "—";
        const [datePart] = String(raw).split("T");
        const [y, m, d] = String(datePart).split("-");
        return m && d ? `${d}/${m}/${y}` : "—";
    })();
    const tabsList = [
        { id: "dados", label: "Dados", icon: <FiUser aria-hidden="true" /> },
        { id: "seguranca", label: "Segurança", icon: <FiLock aria-hidden="true" /> },
        { id: "armazenamento", label: "Armazenamento", icon: <FiHardDrive aria-hidden="true" /> },
        ...(isAdmin ? [{ id: "admin", label: "Administração", icon: <FiShield aria-hidden="true" /> }] : []),
        { id: "perigo", label: "Perigo", icon: <FiTrash2 aria-hidden="true" /> }
    ];
    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const res = await patchMe({ name, surname });
            setMe(res.data);
            setAuth((prev) => ({ ...prev, user: { ...(prev.user || {}), ...res.data } }));
            setToast({ message: "Dados atualizados com sucesso.", type: "success" });
        } catch (err) {
            console.error("[profile] falha ao atualizar dados", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível guardar.", type: "error" });
        } finally { setSaving(false); }
    };
    const handlePassword = async (e) => {
        e.preventDefault();
        setSavingPwd(true);
        try {
            await changePassword({ currentPassword: currentPwd, newPassword: newPwd });
            setCurrentPwd(""); setNewPwd(""); setConfirmPwd("");
            setToast({ message: "Password atualizada com sucesso.", type: "success" });
        } catch (err) {
            console.error("[profile] falha ao trocar password", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível atualizar.", type: "error" });
        } finally { setSavingPwd(false); }
    };
    const handleDelete = async () => {
        if (confirmDelete !== "EXCLUIR") {
            setToast({ message: "Escreva EXCLUIR para confirmar.", type: "warning" });
            return;
        }
        setDeleting(true);
        try {
            await deleteMe();
            setAuthToken(null);
            setAuth({});
            navigate("/login");
        } catch (err) {
            console.error("[profile] falha ao excluir conta", err);
            setToast({ message: "Não foi possível excluir a conta.", type: "error" });
        } finally { setDeleting(false); setShowDelete(false); }
    };

    return (
        <section className="dashboard-page profile-page">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Perfil</h1>
            <p className="dashboard-page__text">Gira os teus dados, segurança e papel na plataforma.</p>
            <header className="card profile-header">
                <div className="profile-avatar" aria-hidden="true">{initials}</div>
                <div className="profile-header__info">
                    <div className="profile-header__top">
                        <span className="profile-username">{loadingMe ? "…" : username}</span>
                        <div className="profile-header__actions">
                            <Button label="Editar perfil" variant="primary" onClick={() => setTab("dados")} />
                            <Button label="Alterar password" variant="outline" onClick={() => setTab("seguranca")} />
                        </div>
                    </div>
                    <ul className="profile-stats">
                        <li><span className="profile-stat__count">{permissions.length}</span><span className="profile-stat__label">permissões</span></li>
                        <li><span className="profile-stat__count">{isAdmin ? "ADMIN" : "USER"}</span><span className="profile-stat__label">papel</span></li>
                        <li><span className="profile-stat__count">{memberSince}</span><span className="profile-stat__label">membro desde</span></li>
                    </ul>
                    <div className="profile-bio">
                        <p className="profile-real-name">{loadingMe ? "A carregar..." : `${me?.name || ""} ${me?.surname || ""}`.trim()}</p>
                        <p className="profile-email"><FiMail aria-hidden="true" /> {me?.email || auth?.user?.email}</p>
                        <p className="profile-meta">
                            <span className="profile-chip"><FiUser aria-hidden="true" /> {isAdmin ? "Administrador" : "Utilizador"}</span>
                            {isAdmin && <span className="profile-chip profile-chip--admin"><FiShield aria-hidden="true" /> Acesso total</span>}
                        </p>
                    </div>
                </div>
            </header>
            <nav className="profile-tabs" role="tablist" aria-label="Secções do perfil">
                {tabsList.map((t) => (
                    <button
                        key={t.id}
                        type="button"
                        role="tab"
                        id={`tab-${t.id}`}
                        aria-selected={tab === t.id}
                        aria-controls={`tabpanel-${t.id}`}
                        className={`profile-tab${tab === t.id ? " profile-tab--active" : ""}`}
                        onClick={() => setTab(t.id)}
                    >
                        {t.icon}
                        <span>{t.label}</span>
                    </button>
                ))}
            </nav>
            {tab === "dados" && (
            <div className="card" role="tabpanel" id="tabpanel-dados" aria-labelledby="tab-dados">
                <h2 className="card__title">Dados pessoais</h2>
                <form onSubmit={handleSave} className="profile-form">
                    <div className="profile-grid">
                        <div>
                            <label htmlFor="profile-name">Nome</label>
                            <Input id="profile-name" type="text" value={name} onChange={(e) => setName(e.target.value)} required errorMessage={nameError} />
                        </div>
                        <div>
                            <label htmlFor="profile-surname">Apelido</label>
                            <Input id="profile-surname" type="text" value={surname} onChange={(e) => setSurname(e.target.value)} required errorMessage={surnameError} />
                        </div>
                    </div>
                    <div>
                        <label htmlFor="profile-email">Email</label>
                        <input id="profile-email" className="inputField" value={me?.email || ""} readOnly aria-readonly="true" />
                        <p className="formHint">Para trocar o email contacte o suporte.</p>
                    </div>
                    <Button label="Guardar alterações" variant="primary" type="submit" loading={saving} disabled={!USER_REGEX.test(name) || !USER_REGEX.test(surname)} />
                </form>
            </div>
            )}

            {tab === "seguranca" && (
            <div className="card" role="tabpanel" id="tabpanel-seguranca" aria-labelledby="tab-seguranca">
                <h2 className="card__title">Segurança</h2>
                <form onSubmit={handlePassword} className="profile-form">
                    <div>
                        <label htmlFor="profile-current">Password atual</label>
                        <Input id="profile-current" type="password" value={currentPwd} onChange={(e) => setCurrentPwd(e.target.value)} required autoComplete="current-password" />
                    </div>
                    <div className="profile-grid">
                        <div>
                            <label htmlFor="profile-new">Nova password</label>
                            <Input id="profile-new" type="password" value={newPwd} onChange={(e) => setNewPwd(e.target.value)} required autoComplete="new-password" errorMessage={newPwdError} />
                        </div>
                        <div>
                            <label htmlFor="profile-confirm">Confirmar password</label>
                            <Input id="profile-confirm" type="password" value={confirmPwd} onChange={(e) => setConfirmPwd(e.target.value)} required autoComplete="new-password" errorMessage={confirmError} />
                        </div>
                    </div>
                    <ul className="pwd-checks" aria-live="polite">
                        {passwordChecks(newPwd).map((c) => (
                            <li key={c.label} className={c.ok ? "pwd-check--ok" : ""}>{c.ok ? "✓" : "•"} {c.label}</li>
                        ))}
                    </ul>
                    <Button label="Atualizar password" variant="primary" type="submit" loading={savingPwd} disabled={!currentPwd || !PWD_REGEX.test(newPwd) || newPwd !== confirmPwd} />
                </form>
            </div>
            )}
            {isAdmin && tab === "admin" && (
                <div className="card" role="tabpanel" id="tabpanel-admin" aria-labelledby="tab-admin">
                    <h2 className="card__title">Administração</h2>
                    <p className="dashboard-page__text">Papel com acesso total à plataforma.</p>
                    <p className="profile-perms">{(permissions.length ? permissions : ["ADMIN_ACESSAR"]).map((p) => (<span key={p} className="profile-chip">{p}</span>))}</p>
                    <Link className="btn btn-outline" to="/dashboard/admin/usuarios">Gerir utilizadores</Link>
                </div>
            )}
            {tab === "armazenamento" && (
            <div className="card" role="tabpanel" id="tabpanel-armazenamento" aria-labelledby="tab-armazenamento">
                <h2 className="card__title"><FiHardDrive aria-hidden="true" /> Armazenamento</h2>
                <div className="quota-bar" role="progressbar" aria-label="Quota de armazenamento" aria-valuenow={0} aria-valuemin={0} aria-valuemax={100}>
                    <div className="quota-fill" style={{ width: "0%" }} />
                </div>
                <p className="dashboard-page__text">A ligar ao módulo de quota em <Link to="/dashboard/armazenamento">Armazenamento</Link>.</p>
            </div>
            )}
            {tab === "perigo" && (
            <div className="card card--danger" role="tabpanel" id="tabpanel-perigo" aria-labelledby="tab-perigo">
                <h2 className="card__title"><FiTrash2 aria-hidden="true" /> Zona de perigo</h2>
                <p className="dashboard-page__text">Excluir a conta remove o acesso permanentemente.</p>
                <Button label={canDelete ? "Excluir a minha conta" : "Exclusão indisponível para o teu perfil"} variant="danger" disabled={!canDelete} onClick={() => setShowDelete(true)} />
                {!canDelete && <p className="errorText">O seu perfil não permite excluir a conta.</p>}
            </div>
            )}
            {showDelete && (
                <div className="modal-backdrop" onClick={() => setShowDelete(false)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Confirmar exclusão" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Excluir conta?</h2>
                        <p className="dashboard-page__text">Escreva <strong>EXCLUIR</strong> para confirmar.</p>
                        <Input id="delete-confirm" type="text" value={confirmDelete} onChange={(e) => setConfirmDelete(e.target.value)} placeholder="EXCLUIR" />
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setShowDelete(false)} />
                            <Button label="Excluir" variant="danger" loading={deleting} disabled={confirmDelete !== "EXCLUIR"} onClick={handleDelete} />
                        </div>
                    </div>
                </div>
            )}
            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Profile;

