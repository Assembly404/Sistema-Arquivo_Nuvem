import { useContext, useEffect, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import SideBar from "../../components/header/sidebar.jsx";
import { SIDEBAR_MENU, SIDEBAR_STORAGE_KEY, filterMenuByPermissions, permissionsForRole } from "../../components/header/sidebarConfig.js";
import AuthContext from "../../context/AuthProvider.jsx";
import { getStorageSummary, getUnreadNotificationCount, setAuthToken } from "../../api/axios.js";
import DashboardHeader from "./DashboardHeader.jsx";
import BottomNav from "./BottomNav.jsx";
import "./DashboardLayout.css";

function DashboardLayout() {
    const { auth, setAuth } = useContext(AuthContext);
    const location = useLocation();
    const navigate = useNavigate();
    const [collapsed, setCollapsed] = useState(
        () => window.localStorage.getItem(SIDEBAR_STORAGE_KEY) === "true"
    );
    const [storage, setStorage] = useState(null);
    const [unread, setUnread] = useState(0);
    const [bottomNavOpen, setBottomNavOpen] = useState(false);

    useEffect(() => {
        window.localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
    }, [collapsed]);

    useEffect(() => {
        if (!auth?.token) return undefined;
        setAuthToken(auth.token);
        let cancelled = false;
        getStorageSummary()
            .then((res) => {
                if (!cancelled) setStorage(res.data || null);
            })
            .catch((err) => {
                console.error("[layout] falha ao carregar armazenamento", err);
            });
        return () => {
            cancelled = true;
        };
    }, [auth?.token]);

    useEffect(() => {
        if (!auth?.token) return undefined;
        let cancelled = false;
        const loadUnread = () => {
            getUnreadNotificationCount()
                .then((res) => {
                    if (!cancelled) setUnread(Number(res.data?.unread) || 0);
                })
                .catch(() => {});
        };
        loadUnread();
        const timer = window.setInterval(loadUnread, 60000);
        return () => {
            cancelled = true;
            window.clearInterval(timer);
        };
    }, [auth?.token, location.pathname]);

    const handleLogout = () => {
        setAuth({});
        navigate("/");
    };

    const permissions = auth?.user?.permissions || permissionsForRole(auth?.user?.role);
    const visibleMenu = filterMenuByPermissions(SIDEBAR_MENU, permissions).map((item) => (
        item.id === "notificacoes"
            ? { ...item, badge: unread > 0 ? (unread > 9 ? "9+" : unread) : null }
            : item
    ));
    const displayName = auth?.user?.name && auth?.user?.surname
        ? `${auth.user.name} ${auth.user.surname}`
        : auth?.user?.name || auth?.user?.email?.split("@")[0] || "";

    const handleBottomNavAction = (action) => {
        switch (action) {
            case "home":
                navigate("/dashboard");
                break;
            case "files":
                navigate("/dashboard/arquivos");
                break;
            case "notifications":
                navigate("/dashboard/notificacoes");
                break;
            case "storage":
                navigate("/dashboard/armazenamento");
                break;
            case "more":
                setBottomNavOpen(true);
                break;
        }
    };

    return (
        <div className={`dashboard-layout ${collapsed ? "dashboard-layout--collapsed" : ""}`}>
            <SideBar
                menu={visibleMenu}
                collapsed={collapsed}
                onToggleCollapse={() => setCollapsed((current) => !current)}
                activePath={location.pathname}
                user={{ name: displayName, email: auth?.user?.email }}
                onLogout={handleLogout}
                storage={storage}
            />

            <main className="dashboard-main">
                <DashboardHeader onLogout={handleLogout} />
                <div className="dashboard-content">
                    <Outlet />
                </div>
            </main>

            <BottomNav
                activePath={location.pathname}
                onAction={handleBottomNavAction}
                isOpen={bottomNavOpen}
                onClose={() => setBottomNavOpen(false)}
                unreadCount={unread}
                role={auth?.user?.role}
            />
        </div>
    );
}

export default DashboardLayout;