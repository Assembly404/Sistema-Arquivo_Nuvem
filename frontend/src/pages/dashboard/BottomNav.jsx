import { useMemo, useState, useRef, useEffect } from "react";
import { useLocation, useNavigate } from "react-router";
import { createPortal } from "react-dom";
import {
    FiHome,
    FiFolder,
    FiBell,
    FiHardDrive,
    FiSettings,
    FiHelpCircle,
    FiMoreHorizontal,
    FiUser,
    FiLogOut,
    FiShield,
} from "react-icons/fi";
import "./BottomNav.css";

const NAV_ITEMS = [
    { id: "home", label: "Início", icon: FiHome, path: "/dashboard" },
    { id: "files", label: "Arquivos", icon: FiFolder, path: "/dashboard/arquivos" },
    { id: "notifications", label: "Notificações", icon: FiBell, path: "/dashboard/notificacoes" },
    { id: "storage", label: "Armazenamento", icon: FiHardDrive, path: "/dashboard/armazenamento" },
];

const MORE_MENU_ITEMS = [
    { id: "perfil", label: "Perfil", icon: FiUser, path: "/dashboard/perfil" },
    { id: "configuracoes", label: "Configurações", icon: FiSettings, path: "/dashboard/configuracoes" },
    { id: "suporte", label: "Suporte", icon: FiHelpCircle, path: "/dashboard/suporte" },
    { id: "admin", label: "Administração", icon: FiShield, path: "/dashboard/admin/usuarios", adminOnly: true },
    { divider: true },
    { id: "logout", label: "Terminar sessão", icon: FiLogOut, action: "logout", danger: true },
];

function BottomNav({ activePath, onAction, unreadCount = 0 }) {
    const [moreOpen, setMoreOpen] = useState(false);
    const moreRef = useRef(null);
    const location = useLocation();
    const navigate = useNavigate();

    // Close more menu on route change
    /* eslint-disable react-hooks/set-state-in-effect */
    useEffect(() => {
        if (moreOpen) setMoreOpen(false);
    }, [location.pathname, moreOpen]);
    /* eslint-enable react-hooks/set-state-in-effect */

    // Close more menu on outside click
    useEffect(() => {
        if (!moreOpen) return;
        const handleClick = (event) => {
            if (moreRef.current && !moreRef.current.contains(event.target)) {
                setMoreOpen(false);
            }
        };
        document.addEventListener("mousedown", handleClick);
        return () => document.removeEventListener("mousedown", handleClick);
    }, [moreOpen]);

    const getActiveNavId = (path) => {
        const normalized = normalizePath(path);
        if (normalized === "/dashboard") return "home";
        if (normalized.startsWith("/dashboard/arquivos")) return "files";
        if (normalized.startsWith("/dashboard/notificacoes")) return "notifications";
        if (normalized.startsWith("/dashboard/armazenamento")) return "storage";
        return null;
    };

    const activeNavId = useMemo(() => getActiveNavId(activePath), [activePath]);

    const handleItemClick = (item) => {
        if (item.action === "logout") {
            onAction("logout");
            return;
        }
        if (item.path) {
            navigate(item.path);
        }
        onAction(item.id);
    };

    const renderBottomNav = () => (
        <nav className="bottom-nav" role="navigation" aria-label="Navegação principal">
            {NAV_ITEMS.map((item) => {
                const isActive = activeNavId === item.id;
                const badge = item.id === "notifications" && unreadCount > 0
                    ? unreadCount > 9 ? "9+" : unreadCount
                    : null;
                return (
                    <button
                        key={item.id}
                        type="button"
                        className={`bottom-nav__item ${isActive ? "bottom-nav__item--active" : ""}`}
                        onClick={() => handleItemClick(item)}
                        aria-current={isActive ? "page" : undefined}
                        aria-label={item.label}
                    >
                        <span className="bottom-nav__icon" aria-hidden="true">
                            <item.icon className={`icon ${isActive ? "icon--active" : ""}`} />
                        </span>
                        <span className="bottom-nav__label">{item.label}</span>
                        {badge && (
                            <span className="bottom-nav__badge" aria-label={`${unreadCount} notificações não lidas`}>
                                {badge}
                            </span>
                        )}
                    </button>
                );
            })}

            <div className="bottom-nav__more" ref={moreRef}>
                <button
                    type="button"
                    className={`bottom-nav__item bottom-nav__item--more ${moreOpen ? "bottom-nav__item--more-open" : ""}`}
                    onClick={() => setMoreOpen(!moreOpen)}
                    aria-expanded={moreOpen}
                    aria-haspopup="menu"
                    aria-label="Mais opções"
                >
                    <span className="bottom-nav__icon" aria-hidden="true">
                        <FiMoreHorizontal className="icon" />
                    </span>
                    <span className="bottom-nav__label">Mais</span>
                </button>

                {moreOpen && (
                    <div className="bottom-nav__dropdown" role="menu" aria-label="Mais opções">
                        {MORE_MENU_ITEMS.map((item, index) => {
                            if (item.divider) {
                                return <hr key={`divider-${index}`} className="bottom-nav__divider" />;
                            }
                            return (
                                <button
                                    key={item.id}
                                    type="button"
                                    role="menuitem"
                                    className={`bottom-nav__dropdown-item ${item.danger ? "bottom-nav__dropdown-item--danger" : ""}`}
                                    onClick={() => {
                                        setMoreOpen(false);
                                        handleItemClick(item);
                                    }}
                                >
                                    <span className="bottom-nav__dropdown-icon" aria-hidden="true">
                                        <item.icon className="icon" />
                                    </span>
                                    <span className="bottom-nav__dropdown-label">{item.label}</span>
                                </button>
                            );
                        })}
                    </div>
                )}
            </div>
        </nav>
    );

    // On mobile, render as portal to body for proper positioning above content
    const isMobile = useMemo(() => window.innerWidth < 992, []);
    
    if (isMobile) {
        return createPortal(renderBottomNav(), document.body);
    }
    
    return null;
}

function normalizePath(path) {
    if (!path) return "";
    return path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
}

export default BottomNav;