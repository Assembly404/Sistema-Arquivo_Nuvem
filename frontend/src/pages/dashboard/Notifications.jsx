import { useCallback, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router";
import {
    FiAlertTriangle,
    FiBell,
    FiCheck,
    FiCloudOff,
    FiRefreshCw,
    FiShare2,
    FiTrash2,
    FiX
} from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    clearNotifications,
    deleteNotification,
    listNotifications,
    markAllNotificationsRead,
    markNotificationRead,
    setAuthToken
} from "../../api/axios.js";
import "./Notifications.css";

const TOAST_DURATION = 4500;

const TYPE_STYLE = {
    SHARE_RECEIVED: { icon: FiShare2, tone: "info" },
    SHARE_REVOKED: { icon: FiShare2, tone: "muted" },
    NODE_OFFLINE: { icon: FiCloudOff, tone: "warning" },
    NODE_ONLINE: { icon: FiRefreshCw, tone: "muted" },
    REPLICA_FAILED: { icon: FiAlertTriangle, tone: "danger" },
    QUOTA_WARNING: { icon: FiAlertTriangle, tone: "warning" },
    SYSTEM: { icon: FiBell, tone: "muted" }
};

const formatDateTime = (value) => {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return String(value).replace("T", " ").slice(0, 16);
    return date.toLocaleString("pt-PT", { dateStyle: "short", timeStyle: "short" });
};

function Notifications() {
    const { auth } = useContext(AuthContext);
    const navigate = useNavigate();
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState(null);
    const closeToast = useCallback(() => setToast(null), []);

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const res = await listNotifications();
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("[notifications] falha ao carregar", err);
            setToast({ message: "Não foi possível carregar as notificações.", type: "error" });
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

    const unread = items.filter((n) => !n.read).length;

    const open = async (notification) => {
        if (!notification.read) {
            try {
                await markNotificationRead(notification.notificationID);
                setItems((prev) => prev.map((n) => (
                    n.notificationID === notification.notificationID ? { ...n, read: true } : n
                )));
            } catch (err) {
                console.error("[notifications] falha ao marcar como lida", err);
            }
        }
        if (notification.link) navigate(notification.link);
    };

    const markAll = async () => {
        try {
            await markAllNotificationsRead();
            setItems((prev) => prev.map((n) => ({ ...n, read: true })));
            setToast({ message: "Todas as notificações foram marcadas como lidas.", type: "success" });
        } catch (err) {
            console.error("[notifications] falha ao marcar todas", err);
            setToast({ message: "Não foi possível marcar as notificações.", type: "error" });
        }
    };

    const remove = async (notification) => {
        try {
            await deleteNotification(notification.notificationID);
            setItems((prev) => prev.filter((n) => n.notificationID !== notification.notificationID));
        } catch (err) {
            console.error("[notifications] falha ao remover", err);
            setToast({ message: "Não foi possível remover a notificação.", type: "error" });
        }
    };

    const clearAll = async () => {
        try {
            await clearNotifications();
            setItems([]);
            setToast({ message: "Notificações limpas.", type: "success" });
        } catch (err) {
            console.error("[notifications] falha ao limpar", err);
            setToast({ message: "Não foi possível limpar as notificações.", type: "error" });
        }
    };

    return (
        <section className="dashboard-page notifications-page">
            <div className="notifications-page__head">
                <div>
                    <h1 className="dashboard-page__title">Notificações</h1>
                    <p className="dashboard-page__text">
                        Eventos relevantes da tua conta{unread > 0 ? ` · ${unread} por ler` : ""}.
                    </p>
                </div>
                <div className="notifications-page__actions">
                    {unread > 0 && (
                        <Button label="Marcar todas como lidas" variant="ghost" icon={<FiCheck aria-hidden="true" />} onClick={markAll} />
                    )}
                    {items.length > 0 && (
                        <Button label="Limpar tudo" variant="ghost" icon={<FiTrash2 aria-hidden="true" />} onClick={clearAll} />
                    )}
                </div>
            </div>

            {loading ? (
                <p className="dashboard-page__text">A carregar...</p>
            ) : items.length === 0 ? (
                <p className="dashboard-page__text">Não tens notificações.</p>
            ) : (
                <ul className="notifications-list">
                    {items.map((notification) => {
                        const style = TYPE_STYLE[notification.type] || TYPE_STYLE.SYSTEM;
                        const Icon = style.icon;
                        return (
                            <li
                                key={notification.notificationID}
                                className={`notification notification--${style.tone} ${notification.read ? "" : "is-unread"}`}
                            >
                                <span className="notification__icon" aria-hidden="true"><Icon /></span>
                                <button type="button" className="notification__body" onClick={() => open(notification)}>
                                    <span className="notification__title">{notification.title}</span>
                                    <span className="notification__message">{notification.message}</span>
                                    <span className="notification__time">{formatDateTime(notification.createdAt)}</span>
                                </button>
                                <button
                                    type="button"
                                    className="notification__remove"
                                    title="Remover"
                                    aria-label="Remover notificação"
                                    onClick={() => remove(notification)}
                                >
                                    <FiX aria-hidden="true" />
                                </button>
                            </li>
                        );
                    })}
                </ul>
            )}

            {!loading && items.length > 0 && (
                <div className="files-toolbar">
                    <Button label="Atualizar" variant="ghost" icon={<FiRefreshCw aria-hidden="true" />} onClick={load} />
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Notifications;
