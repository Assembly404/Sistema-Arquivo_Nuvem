import { useCallback, useEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { Link } from "react-router";
import { FiChevronDown, FiMenu, FiX } from "react-icons/fi";
import logo from "../../assets/logo.png";
import { SIDEBAR_ACCOUNT_MENU, SIDEBAR_ICONS, SIDEBAR_MENU } from "./sidebarConfig.js";
import "./sidebar.css";

const MOBILE_QUERY = "(max-width: 992px)";
const ROOT_PATH = "/dashboard";

function normalizePath(path) {
    if (!path) return "";
    return path.length > 1 && path.endsWith("/") ? path.slice(0, -1) : path;
}

function isPathMatch(path, activePath) {
    if (!path) return false;
    if (path === ROOT_PATH) return activePath === path;
    return activePath === path || activePath.startsWith(`${path}/`);
}

function findActiveItem(items, activePath) {
    let deepest = null;

    items.forEach((item) => {
        if (isPathMatch(item.path, activePath)) {
            if (!deepest || item.path.length > deepest.path.length) deepest = item;
        }
        if (item.children && item.children.length > 0) {
            const childMatch = findActiveItem(item.children, activePath);
            if (childMatch) deepest = childMatch;
        }
    });

    return deepest;
}

function findAncestorIds(items, activeId) {
    const ancestorIds = [];

    const visit = (nodes, trail) => {
        nodes.forEach((node) => {
            if (node.id === activeId) {
                ancestorIds.push(...trail);
                return;
            }
            if (node.children && node.children.length > 0) {
                visit(node.children, [...trail, node.id]);
            }
        });
    };

    visit(items, []);

    return new Set(ancestorIds);
}

function useIsMobile() {
    const mediaQuery = useMemo(() => window.matchMedia(MOBILE_QUERY), []);
    const subscribe = useCallback(
        (onStoreChange) => {
            mediaQuery.addEventListener("change", onStoreChange);
            return () => mediaQuery.removeEventListener("change", onStoreChange);
        },
        [mediaQuery]
    );
    const getSnapshot = useCallback(() => mediaQuery.matches, [mediaQuery]);

    return useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
}

function NodeIcon({ name, className = "icon" }) {
    const Icon = SIDEBAR_ICONS[name];

    if (!Icon) return null;

    return <Icon className={className} aria-hidden="true" />;
}

export function SidebarItem({
    item,
    collapsed = false,
    activeId = "",
    isActive = false,
    isParentActive = false,
    disabled = false,
    disabledReason = "",
    isSubmenuOpen = false,
    onToggleSubmenu
}) {
    const hasChildren = Boolean(item.children && item.children.length > 0);
    const submenuId = `sidebar-submenu-${item.id}`;
    const reason = disabled ? disabledReason : "";
    const badge = item.badge === null || item.badge === undefined ? null : (
        <span className="sidebar-badge">{item.badge}</span>
    );

    if (hasChildren) {
        return (
            <li className="sidebar-item-group">
                <button
                    type="button"
                    className={`sidebar-item sidebar-item--parent ${isParentActive ? "sidebar-item--parent-active" : ""}`}
                    aria-expanded={collapsed ? false : isSubmenuOpen}
                    aria-controls={submenuId}
                    aria-disabled={disabled ? "true" : undefined}
                    disabled={disabled}
                    aria-label={item.label}
                    title={reason}
                    data-label={item.label}
                    onClick={() => onToggleSubmenu?.(item.id)}
                >
                    <NodeIcon name={item.icon} className="icon sidebar-item__icon" />
                    <span className="sidebar-item__label">{item.label}</span>
                    {badge}
                    <FiChevronDown className="icon sidebar-item__chevron" aria-hidden="true" />
                </button>

                {!collapsed && isSubmenuOpen && (
                    <ul className="sidebar-submenu" id={submenuId}>
                        {item.children.map((child) => (
                            <SidebarItem
                                key={child.id}
                                item={child}
                                collapsed={collapsed}
                                activeId={activeId}
                                isActive={child.id === activeId}
                                disabled={Boolean(child.disabled)}
                                disabledReason={child.disabledReason ?? ""}
                            />
                        ))}
                    </ul>
                )}

                {collapsed && (
                    <div className="sidebar-flyout">
                        <p className="sidebar-flyout__title">{item.label}</p>
                        <ul className="sidebar-flyout__list">
                            {item.children.map((child) => (
                                <li key={child.id}>
                                    {child.disabled ? (
                                        <span
                                            className="sidebar-flyout__item"
                                            aria-disabled="true"
                                            title={child.disabledReason ?? ""}
                                        >
                                            {child.label}
                                        </span>
                                    ) : (
                                        <Link
                                            to={child.path}
                                            className="sidebar-flyout__item"
                                            aria-current={child.id === activeId ? "page" : undefined}
                                            title={child.label}
                                        >
                                            {child.label}
                                        </Link>
                                    )}
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </li>
        );
    }

    if (disabled) {
        return (
            <li>
                <span
                    className="sidebar-item"
                    aria-disabled="true"
                    aria-label={item.label}
                    title={reason}
                    data-label={item.label}
                >
                    <NodeIcon name={item.icon} className="icon sidebar-item__icon" />
                    <span className="sidebar-item__label">{item.label}</span>
                    {badge}
                </span>
            </li>
        );
    }

    return (
        <li>
            <Link
                to={item.path}
                className="sidebar-item"
                aria-current={isActive ? "page" : undefined}
                aria-label={item.label}
                data-label={item.label}
            >
                <NodeIcon name={item.icon} className="icon sidebar-item__icon" />
                <span className="sidebar-item__label">{item.label}</span>
                {badge}
            </Link>
        </li>
    );
}

function SideBar({
    menu = SIDEBAR_MENU,
    collapsed = false,
    onToggleCollapse,
    activePath = ROOT_PATH,
    user,
    onLogout
}) {
    const isMobile = useIsMobile();
    const [isMobileOpen, setIsMobileOpen] = useState(false);
    const [openSubmenus, setOpenSubmenus] = useState(() => {
        const activeNode = findActiveItem(menu, normalizePath(activePath));
        return activeNode ? findAncestorIds(menu, activeNode.id) : new Set();
    });
    const [accountMenuOpen, setAccountMenuOpen] = useState(false);
    const accountRef = useRef(null);

    const isCollapsed = collapsed && !isMobile;
    const path = normalizePath(activePath);
    const activeItem = useMemo(() => findActiveItem(menu, path), [menu, path]);
    const activeId = activeItem ? activeItem.id : "";
    const ancestorIds = useMemo(() => findAncestorIds(menu, activeId), [menu, activeId]);

    const email = user?.email ?? "";
    const name = user?.name?.trim() || email.split("@")[0] || "Utilizador";
    const initials = name
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0))
        .join("")
        .toUpperCase() || "U";

    const [renderedContext, setRenderedContext] = useState({ path, isMobile });

    if (renderedContext.path !== path || renderedContext.isMobile !== isMobile) {
        setRenderedContext({ path, isMobile });
        setIsMobileOpen(false);
        setAccountMenuOpen(false);
    }

    useEffect(() => {
        if (!accountMenuOpen && !isMobileOpen) return undefined;

        const handlePointerDown = (event) => {
            if (accountRef.current && !accountRef.current.contains(event.target)) {
                setAccountMenuOpen(false);
            }
        };

        const handleKeyDown = (event) => {
            if (event.key === "Escape") {
                setAccountMenuOpen(false);
                setIsMobileOpen(false);
            }
        };

        document.addEventListener("mousedown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.removeEventListener("mousedown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [accountMenuOpen, isMobileOpen]);

    const handleToggle = () => {
        if (isMobile) {
            setIsMobileOpen((open) => !open);
            return;
        }
        onToggleCollapse?.();
    };

    const handleToggleSubmenu = (id) => {
        setOpenSubmenus((current) => {
            const next = new Set(current);
            if (next.has(id)) {
                next.delete(id);
            } else {
                next.add(id);
            }
            return next;
        });
    };

    const handleLogout = () => {
        setAccountMenuOpen(false);
        onLogout?.();
    };

    const toggleLabel = isMobile
        ? (isMobileOpen ? "Fechar menu" : "Abrir menu")
        : (collapsed ? "Expandir menu" : "Recolher menu");

    return (
        <>
            {isMobile && !isMobileOpen && (
                <button
                    type="button"
                    className="sidebar-trigger"
                    onClick={() => setIsMobileOpen(true)}
                    aria-label="Abrir menu"
                    aria-controls="sidebar"
                    aria-expanded="false"
                >
                    <FiMenu className="icon" aria-hidden="true" />
                </button>
            )}

            {isMobile && isMobileOpen && (
                <div className="sidebar-overlay" onClick={() => setIsMobileOpen(false)} aria-hidden="true" />
            )}

            <aside
                id="sidebar"
                className={`sidebar ${isCollapsed ? "sidebar--collapsed" : ""} ${isMobileOpen ? "sidebar--open" : ""}`}
                aria-label="Navegação principal"
            >
                <header className="sidebar-header">
                    <Link to={ROOT_PATH} className="sidebar-brand" aria-label="Ir para o início">
                        <img src={logo} alt="Arquivo Nuvem" />
                    </Link>

                    <button
                        type="button"
                        className="sidebar-toggle"
                        onClick={handleToggle}
                        aria-label={toggleLabel}
                        aria-controls="sidebar"
                        aria-expanded={isMobile ? isMobileOpen : !collapsed}
                    >
                        {isMobile && isMobileOpen
                            ? <FiX className="icon" aria-hidden="true" />
                            : <FiMenu className="icon" aria-hidden="true" />}
                    </button>
                </header>

                <nav className="sidebar-nav">
                    <ul className="sidebar-nav__list">
                        {menu.map((item) => (
                            <SidebarItem
                                key={item.id}
                                item={item}
                                collapsed={isCollapsed}
                                activeId={activeId}
                                isActive={item.id === activeId}
                                isParentActive={ancestorIds.has(item.id)}
                                disabled={Boolean(item.disabled)}
                                disabledReason={item.disabledReason ?? ""}
                                isSubmenuOpen={openSubmenus.has(item.id)}
                                onToggleSubmenu={handleToggleSubmenu}
                            />
                        ))}
                    </ul>
                </nav>

                <footer className="sidebar-footer">
                    <div className="sidebar-account" ref={accountRef}>
                        <button
                            type="button"
                            className="sidebar-account__button"
                            onClick={() => setAccountMenuOpen((open) => !open)}
                            aria-expanded={accountMenuOpen}
                            aria-haspopup="menu"
                            aria-label={name}
                        >
                            <span className="sidebar-avatar" aria-hidden="true">{initials}</span>
                            <span className="sidebar-account__info">
                                <span className="sidebar-account__name">{name}</span>
                                <span className="sidebar-account__email">{email}</span>
                            </span>
                        </button>

                        {accountMenuOpen && (
                            <ul className="dropdown-menu sidebar-account__menu">
                                {SIDEBAR_ACCOUNT_MENU.map((entry) => (
                                    <li key={entry.id}>
                                        {entry.action === "logout" ? (
                                            <button
                                                type="button"
                                                className={`dropdown-item ${entry.danger ? "is-danger" : ""}`}
                                                onClick={handleLogout}
                                            >
                                                <NodeIcon name={entry.icon} />
                                                {entry.label}
                                            </button>
                                        ) : (
                                            <Link
                                                to={entry.path}
                                                className="dropdown-item"
                                                onClick={() => setAccountMenuOpen(false)}
                                            >
                                                <NodeIcon name={entry.icon} />
                                                {entry.label}
                                            </Link>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </footer>
            </aside>
        </>
    );
}

export default SideBar;




