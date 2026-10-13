import { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router";
import {
    FiCheck,
    FiFolder,
    FiHardDrive,
    FiKey,
    FiLock,
    FiMail,
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
    changePassword,
    deleteMe,
    getMe,
    getMeuAcesso,
    getStorageSummary,
    patchMe,
    setAuthToken
} from "../../api/axios.js";
import "./Profile.css";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const TOAST_DURATION = 4500;

const RESOURCE_ICONS = { FiFolder, FiHardDrive, FiShield, FiKey, FiUser, FiUsers };
const resourceIcon = (name) => RESOURCE_ICONS[name] || FiKey;

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

    /* "o que o meu perfil pode fazer" + armazenamento real */
    const [acesso, setAcesso] = useState(null);
    const [storage, setStorage] = useState(null);
    const [loadingStorage, setLoadingStorage] = useState(false);

    const closeToast = useCallback(() => setToast(null), []);
    const notify = ({ message, type }) => setToast({ message, type });

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
            if (mounted) notify({ message: "Não foi possível carregar o perfil.", type: "error" });
        }).finally(() => { if (mounted) setLoadingMe(false); });

        getMeuAcesso().then((res) => {
            if (mounted) setAcesso(res.data || null);
        }).catch((err) => {
            console.error("[profile] falha ao carregar acessos", err);
        });

        return () => { mounted = false; };
    }, [auth?.token, setAuth]);

    const loadStorage = useCallback(() => {
        if (!auth?.token || loadingStorage) return;
        setLoadingStorage(true);
        getStorageSummary()
            .then((res) => setStorage(res.data || null))
            .catch((err) => {
                console.error("[profile] falha ao carregar armazenamento", err);
                notify({ message: "Não foi possível carregar o armazenamento.", type: "error" });
            })
            .finally(() => setLoadingStorage(false));
    }, [auth?.token, loadingStorage]);

    useEffect(() => {
        if (tab === "armazenamento" && !storage) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            loadStorage();
        }
    }, [tab, storage, loadStorage]);

    const role = me?.role || auth?.user?.role || "USER";
    const roleNome = acesso?.roleNome || me?.roleNome || auth?.user?.roleNome || role;
    const isAdmin = role === "ADMIN";
    const permissions = me?.permissions || auth?.user?.permissions || [];
    const canDelete = hasPermission("ARQUIVO_EXCLUIR") || permissions.includes("ARQUIVO_EXCLUIR");
    const initials = `${name?.[0] || ""}${surname?.[0] || ""}`.toUpperCase() || "U";
    const nameError = name && !USER_REGEX.test(name) ? "Mínimo 3 caracteres, a começar por letra." : "";
    const surnameError = surname && !USER_REGEX.test(surname) ? "Mínimo 3 caracteres." : "";
    const newPwdError = newPwd && !PWD_REGEX.test(newPwd) ? "8 a 24 caracteres, com maiúscula, minúscula, número e símbolo." : "";
    const confirmError = confirmPwd && newPwd !== confirmPwd ? "As passwords não coincidem." : "";

    const username = "@" + (me?.email || auth?.user?.email || "utilizador").split("@")[0];
    const memberSince = useMemo(() => {
        const raw = me?.dataRegistro;
        if (!raw) return "—";
        const [datePart] = String(raw).split("T");
        const [y, m, d] = String(datePart).split("-");
        return m && d ? `${d}/${m}/${y}` : "—";
    }, [me]);

    const quota = useMemo(() => {
        const cloud = storage?.cloud;
        if (!cloud) return null;
        const used = cloud.usedBytes || 0;
        const quotaBytes = cloud.quotaBytes;
        const hasQuota = Boolean(cloud.hasQuota && quotaBytes);
        const percent = hasQuota ? Math.min(100, Math.round((used / quotaBytes) * 100)) : 0;
        const tone = !hasQuota || percent < 70 ? "ok" : percent < 90 ? "warn" : "danger";
        const usedLabel = formatBytes(used);
        const quotaLabel = hasQuota ? formatBytes(quotaBytes) : "Ilimitada";
        return { used, hasQuota, percent, tone, usedLabel, quotaLabel };
    }, [storage]);

    function formatBytes(bytes) {
        if (!bytes || bytes <= 0) return "0 B";
        const units = ["B", "KB", "MB", "GB", "TB"];
        const i = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
        return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
    }

    const tabsList = [
        { id: "dados", label: "Dados", icon: <FiUser aria-hidden="true" /> },
        { id: "seguranca", label: "Segurança", icon: <FiLock aria-hidden="true" /> },
        { id: "acessos", label: "Acessos", icon: <FiKey aria-hidden="true" /> },
        { id: "armazenamento", label: "Armazenamento", icon: <FiHardDrive aria-hidden="true" /> },
        { id: "perigo", label: "Perigo", icon: <FiTrash2 aria-hidden="true" /> }
    ];

    const handleSave = async (e) => {
        e.preventDefault();
        setSaving(true);
        try {
            const res = await patchMe({ name, surname });
            setMe(res.data);
            setAuth((prev) => ({ ...prev, user: { ...(prev.user || {}), ...res.data } }));
            notify({ message: "Dados atualizados com sucesso.", type: "success" });
        } catch (err) {
            console.error("[profile] falha ao atualizar dados", err);
            notify({ message: err?.response?.data?.message || "Não foi possível guardar.", type: "error" });
        } finally { setSaving(false); }
    };

    const handlePassword = async (e) => {
        e.preventDefault();
        setSavingPwd(true);
        try {
            await changePassword({ currentPassword: currentPwd, newPassword: newPwd });
            setCurrentPwd(""); setNewPwd(""); setConfirmPwd("");
            notify({ message: "Password atualizada com sucesso.", type: "success" });
        } catch (err) {
            console.error("[profile] falha ao trocar password", err);
            notify({ message: err?.response?.data?.message || "Não foi possível atualizar.", type: "error" });
        } finally { setSavingPwd(false); }
    };

    const handleDelete = async () => {
        if (confirmDelete !== "EXCLUIR") {
            notify({ message: "Escreva EXCLUIR para confirmar.", type: "warning" });
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
            notify({ message: "Não foi possível excluir a conta.", type: "error" });
        } finally { setDeleting(false); setShowDelete(false); }
    };

    return (
        <section className="dashboard-page profile-page">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Perfil</h1>
            <p className="dashboard-page__text">Gere os teus dados, segurança e o acesso do teu perfil na plataforma.</p>

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
                        <li><span className="profile-stat__count">{acesso ? acesso.recursos.reduce((n, r) => n + r.permissoes.length, 0) : permissions.length}</span><span className="profile-stat__label">permissões</span></li>
                        <li><span className="profile-stat__count">{roleNome}</span><span className="profile-stat__label">perfil</span></li>
                        <li><span className="profile-stat__count">{memberSince}</span><span className="profile-stat__label">membro desde</span></li>
                    </ul>
                    <div className="profile-bio">
                        <p className="profile-real-name">{loadingMe ? "A carregar..." : `${me?.name || ""} ${me?.surname || ""}`.trim()}</p>
                        <p className="profile-email"><FiMail aria-hidden="true" /> {me?.email || auth?.user?.email}</p>
                        <p className="profile-meta">
                            <span className="profile-chip"><FiUser aria-hidden="true" /> {roleNome}</span>
                            {isAdmin && <span className="profile-chip profile-chip--admin"><FiShield aria-hidden="true" /> Administração</span>}
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

            {tab === "acessos" && (
            <div className="card" role="tabpanel" id="tabpanel-acessos" aria-labelledby="tab-acessos">
                <h2 className="card__title"><FiKey aria-hidden="true" /> O que o meu perfil pode fazer</h2>
                <p className="dashboard-page__text">
                    O perfil <strong>{roleNome}</strong> ({role}) tem acesso aos seguintes recursos:
                </p>
                {!acesso ? (
                    <p className="dashboard-page__text">A carregar acessos...</p>
                ) : acesso.recursos.length === 0 ? (
                    <p className="dashboard-page__text">Este perfil não tem permissões atribuídas.</p>
                ) : (
                    <ul className="profile-access">
                        {acesso.recursos.map((r) => {
                            const Icon = resourceIcon(r.icone);
                            return (
                                <li key={r.codigo} className="profile-access__recurso">
                                    <div className="profile-access__head">
                                        <span className="profile-access__icon"><Icon aria-hidden="true" /></span>
                                        <div>
                                            <strong>{r.nome}</strong>
                                            <span className="profile-access__code">{r.codigo}</span>
                                        </div>
                                    </div>
                                    <ul className="profile-access__perms">
                                        {r.permissoes.map((pm) => (
                                            <li key={pm.codigo}>
                                                <FiCheck className="profile-access__check" aria-hidden="true" />
                                                <span>
                                                    <strong>{pm.nome}</strong>
                                                    <span className="profile-access__perm-code">{pm.codigo}</span>
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                </li>
                            );
                        })}
                    </ul>
                )}
                {hasPermission("ADMIN_PERMISSOES") && (
                    <Link className="btn btn-outline" to="/dashboard/admin/permissoes">Gerir perfis &amp; permissões</Link>
                )}
            </div>
            )}

            {tab === "armazenamento" && (
            <div className="card" role="tabpanel" id="tabpanel-armazenamento" aria-labelledby="tab-armazenamento">
                <h2 className="card__title"><FiHardDrive aria-hidden="true" /> Armazenamento</h2>
                {loadingStorage && !storage ? (
                    <p className="dashboard-page__text">A carregar armazenamento...</p>
                ) : !storage || !quota ? (
                    <p className="dashboard-page__text">Sem dados de armazenamento.</p>
                ) : (
                    <>
                        <div
                            className={`quota-bar ${quota.percent >= 90 ? "is-critical" : ""}`}
                            role="progressbar"
                            aria-label="Quota de armazenamento"
                            aria-valuenow={quota.percent}
                            aria-valuemin={0}
                            aria-valuemax={100}
                        >
                            <div className={`quota-fill quota-fill--${quota.tone}`} style={{ width: `${quota.percent}%` }} />
                        </div>
                        <p className="dashboard-page__text">
                            <strong>{quota.usedLabel}</strong> utilizados{quota.hasQuota ? ` de ${quota.quotaLabel} (${quota.percent}%)` : " — quota ilimitada"}.
                        </p>
                    </>
                )}
                <Link className="btn btn-outline" to="/dashboard/armazenamento">Ver armazenamento em detalhe</Link>
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