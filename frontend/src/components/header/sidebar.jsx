import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";
import { Link } from "react-router";
import { FiChevronDown, FiMenu, FiX } from "react-icons/fi";
import logo from "../../assets/logo.png";
import { SIDEBAR_ACCOUNT_MENU, SIDEBAR_ICONS, SIDEBAR_MENU } from "./sidebarConfig.js";
import "./sidebar.css";

const MOBILE_QUERY = "(max-width: 992px)";
const ROOT_PATH = "/dashboard";
const POPOVER_GAP = 8;
const POPOVER_CLOSE_DELAY = 150;

function getPopoverPanelId(id) {
    return `sidebar-popover-${id}`;
}

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
    isPopoverOpen = false,
    onToggleSubmenu,
    onPopoverOpen,
    onPopoverClose
}) {
    const hasChildren = Boolean(item.children && item.children.length > 0);
    const submenuId = `sidebar-submenu-${item.id}`;
    const popoverPanelId = getPopoverPanelId(item.id);
    const reason = disabled ? disabledReason : "";
    const accessibleLabel = reason ? `${item.label} — ${reason}` : item.label;
    const popoverHandlers = onPopoverOpen && (collapsed || disabled)
        ? {
            onMouseEnter: (event) => onPopoverOpen({ item, node: event.currentTarget, reason }),
            onMouseLeave: onPopoverClose,
            onFocus: (event) => onPopoverOpen({ item, node: event.currentTarget, reason }),
            onBlur: onPopoverClose
        }
        : {};
    const badge = item.badge === null || item.badge === undefined ? null : (
        <span className="sidebar-badge">{item.badge}</span>
    );

    if (hasChildren) {
        return (
            <li className="sidebar-item-group" {...popoverHandlers}>
                <button
                    type="button"
                    className={`sidebar-item sidebar-item--parent ${isParentActive ? "sidebar-item--parent-active" : ""}`}
                    aria-expanded={collapsed ? isPopoverOpen : isSubmenuOpen}
                    aria-controls={collapsed ? (isPopoverOpen ? popoverPanelId : undefined) : submenuId}
                    aria-haspopup={collapsed ? "true" : undefined}
                    aria-disabled={disabled ? "true" : undefined}
                    disabled={disabled}
                    aria-label={accessibleLabel}
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
                                onPopoverOpen={onPopoverOpen}
                                onPopoverClose={onPopoverClose}
                            />
                        ))}
                    </ul>
                )}
            </li>
        );
    }

    if (disabled) {
        return (
            <li {...popoverHandlers}>
                <span
                    className="sidebar-item"
                    aria-disabled="true"
                    aria-label={accessibleLabel}
                >
                    <NodeIcon name={item.icon} className="icon sidebar-item__icon" />
                    <span className="sidebar-item__label">{item.label}</span>
                    {badge}
                </span>
            </li>
        );
    }

    return (
        <li {...popoverHandlers}>
            <Link
                to={item.path}
                className="sidebar-item"
                aria-current={isActive ? "page" : undefined}
                aria-label={item.label}
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
    const [popover, setPopover] = useState(null);
    const [navScroll, setNavScroll] = useState({ top: false, bottom: false });
    const accountRef = useRef(null);
    const popoverRef = useRef(null);
    const popoverTimer = useRef(null);
    const navRef = useRef(null);

    const isCollapsed = collapsed && !isMobile;
    const path = normalizePath(activePath);
    const activeItem = useMemo(() => findActiveItem(menu, path), [menu, path]);
    const activeId = activeItem ? activeItem.id : "";
    const ancestorIds = useMemo(() => findAncestorIds(menu, activeId), [menu, activeId]);
    const [renderedNav, setRenderedNav] = useState({ path, isMobile });
    const [renderedCollapse, setRenderedCollapse] = useState(isCollapsed);

    const email = user?.email ?? "";
    const name = user?.name?.trim() || email.split("@")[0] || "Utilizador";
    const initials = name
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0))
        .join("")
        .toUpperCase() || "U";

    const updateNavScroll = useCallback(() => {
        const node = navRef.current;
        if (!node) return;
        const top = node.scrollTop > 1;
        const bottom = node.scrollTop + node.clientHeight < node.scrollHeight - 1;
        setNavScroll((current) => (current.top === top && current.bottom === bottom ? current : { top, bottom }));
    }, []);

    const handlePopoverOpen = useCallback(({ item, node, reason = "" }) => {
        window.clearTimeout(popoverTimer.current);
        const variant = reason
            ? "reason"
            : (item.children && item.children.length > 0 ? "menu" : "label");
        setPopover({
            id: item.id,
            label: item.label,
            reason,
            variant,
            children: item.children ?? [],
            rect: node.getBoundingClientRect()
        });
    }, []);

    const handlePopoverHold = useCallback(() => {
        window.clearTimeout(popoverTimer.current);
    }, []);

    const handlePopoverClose = useCallback((event) => {
        window.clearTimeout(popoverTimer.current);
        const next = event?.relatedTarget;
        if (next instanceof Node && popoverRef.current && popoverRef.current.contains(next)) return;
        popoverTimer.current = window.setTimeout(() => setPopover(null), POPOVER_CLOSE_DELAY);
    }, []);

    useEffect(() => () => window.clearTimeout(popoverTimer.current), []);

    useEffect(() => {
        const node = navRef.current;
        if (!node) return undefined;
        const observer = new ResizeObserver(updateNavScroll);
        observer.observe(node);
        if (node.firstElementChild) observer.observe(node.firstElementChild);
        window.addEventListener("resize", updateNavScroll);
        return () => {
            observer.disconnect();
            window.removeEventListener("resize", updateNavScroll);
        };
    }, [updateNavScroll]);

    useEffect(() => {
        if (!isMobile || !isMobileOpen) return undefined;
        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";
        return () => {
            document.body.style.overflow = previousOverflow;
        };
    }, [isMobile, isMobileOpen]);

    useLayoutEffect(() => {
        if (!popover) return undefined;
        const panel = popoverRef.current;
        if (!panel) return undefined;

        const place = () => {
            const { rect } = popover;
            const height = panel.offsetHeight;
            const width = panel.offsetWidth;
            const anchorTop = popover.variant === "menu" ? rect.top : rect.top + ((rect.height - height) / 2);
            const maxTop = Math.max(POPOVER_GAP, window.innerHeight - height - POPOVER_GAP);
            const top = Math.min(Math.max(anchorTop, POPOVER_GAP), maxTop);
            const left = Math.min(rect.right + POPOVER_GAP, Math.max(POPOVER_GAP, window.innerWidth - width - POPOVER_GAP));
            panel.style.top = `${top}px`;
            panel.style.left = `${left}px`;
        };

        const dismiss = (event) => {
            if (popoverRef.current && event.target instanceof Node && popoverRef.current.contains(event.target)) return;
            setPopover(null);
        };

        place();
        window.addEventListener("resize", place);
        window.addEventListener("scroll", dismiss, true);

        return () => {
            window.removeEventListener("resize", place);
            window.removeEventListener("scroll", dismiss, true);
        };
    }, [popover]);

    useEffect(() => {
        if (!accountMenuOpen && !isMobileOpen && !popover) return undefined;

        const handlePointerDown = (event) => {
            if (accountRef.current && !accountRef.current.contains(event.target)) {
                setAccountMenuOpen(false);
            }
            if (popoverRef.current && !popoverRef.current.contains(event.target)) {
                setPopover(null);
            }
        };

        const handleKeyDown = (event) => {
            if (event.key === "Escape") {
                setAccountMenuOpen(false);
                setIsMobileOpen(false);
                setPopover(null);
            }
        };

        document.addEventListener("mousedown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.removeEventListener("mousedown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [accountMenuOpen, isMobileOpen, popover]);

    const handleToggle = () => {
        if (isMobile) {
            setIsMobileOpen((open) => !open);
            return;
        }
        onToggleCollapse?.();
    };

    const handleToggleSubmenu = (id) => {
        if (isCollapsed) return;
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

    if (renderedNav.path !== path || renderedNav.isMobile !== isMobile) {
        const pathChanged = renderedNav.path !== path;
        setRenderedNav({ path, isMobile });
        setIsMobileOpen(false);
        setAccountMenuOpen(false);
        setPopover(null);

        if (pathChanged) {
            const activeNode = findActiveItem(menu, path);
            const activeAncestors = activeNode ? findAncestorIds(menu, activeNode.id) : new Set();
            setOpenSubmenus((current) => {
                let changed = false;
                const next = new Set(current);
                activeAncestors.forEach((id) => {
                    if (!next.has(id)) {
                        next.add(id);
                        changed = true;
                    }
                });
                return changed ? next : current;
            });
        }
    }

    if (renderedCollapse !== isCollapsed) {
        setRenderedCollapse(isCollapsed);
        setPopover(null);
    }

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
                inert={isMobile && !isMobileOpen ? true : undefined}
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

                <div
                    className="sidebar-nav-wrap"
                    data-scroll-top={navScroll.top}
                    data-scroll-bottom={navScroll.bottom}
                >
                    <nav className="sidebar-nav" ref={navRef} onScroll={updateNavScroll}>
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
                                    isPopoverOpen={popover?.id === item.id}
                                    onToggleSubmenu={handleToggleSubmenu}
                                    onPopoverOpen={handlePopoverOpen}
                                    onPopoverClose={handlePopoverClose}
                                />
                            ))}
                        </ul>
                    </nav>
                </div>

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
                            <FiChevronDown className="icon sidebar-account__chevron" aria-hidden="true" />
                        </button>

                        {accountMenuOpen && (
                            <ul className="sidebar-menu sidebar-account__menu">
                                {SIDEBAR_ACCOUNT_MENU.map((entry) => (
                                    <li key={entry.id}>
                                        {entry.action === "logout" ? (
                                            <button
                                                type="button"
                                                className={`sidebar-menu__item ${entry.danger ? "is-danger" : ""}`}
                                                onClick={handleLogout}
                                            >
                                                <span className="sidebar-menu__icon">
                                                    <NodeIcon name={entry.icon} />
                                                </span>
                                                <span className="sidebar-menu__label">{entry.label}</span>
                                            </button>
                                        ) : (
                                            <Link
                                                to={entry.path}
                                                className="sidebar-menu__item"
                                                onClick={() => setAccountMenuOpen(false)}
                                            >
                                                <span className="sidebar-menu__icon">
                                                    <NodeIcon name={entry.icon} />
                                                </span>
                                                <span className="sidebar-menu__label">{entry.label}</span>
                                            </Link>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </footer>
            </aside>

            {popover && createPortal(
                <div
                    id={getPopoverPanelId(popover.id)}
                    className={popover.variant === "menu" ? "sidebar-flyout" : "sidebar-flyout sidebar-flyout--label"}
                    role={popover.variant === "menu" ? "group" : "tooltip"}
                    aria-label={popover.label}
                    ref={popoverRef}
                    style={{ top: popover.rect.top, left: popover.rect.right + POPOVER_GAP }}
                    onMouseEnter={handlePopoverHold}
                    onMouseLeave={handlePopoverClose}
                    onBlur={handlePopoverClose}
                >
                    {popover.variant === "menu" ? (
                        <>
                            <p className="sidebar-flyout__title">{popover.label}</p>
                            <ul className="sidebar-flyout__list">
                                {popover.children.map((child) => (
                                    <li key={child.id}>
                                        {child.disabled ? (
                                            <span className="sidebar-flyout__item" aria-disabled="true">
                                                {child.label}
                                            </span>
                                        ) : (
                                            <Link
                                                to={child.path}
                                                className="sidebar-flyout__item"
                                                aria-current={child.id === activeId ? "page" : undefined}
                                                onClick={() => setPopover(null)}
                                            >
                                                {child.label}
                                            </Link>
                                        )}
                                    </li>
                                ))}
                            </ul>
                        </>
                    ) : (
                        <span className="sidebar-flyout__label">
                            {popover.variant === "reason" ? popover.reason : popover.label}
                        </span>
                    )}
                </div>,
                document.body
            )}
        </>
    );
}

export default SideBar;




