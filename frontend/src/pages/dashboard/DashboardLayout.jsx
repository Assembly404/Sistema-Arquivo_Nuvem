import { useContext, useEffect, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import SideBar from "../../components/header/sidebar.jsx";
import { SIDEBAR_MENU, SIDEBAR_STORAGE_KEY, filterMenuByPermissions, permissionsForRole } from "../../components/header/sidebarConfig.js";
import AuthContext from "../../context/AuthProvider.jsx";
import "./DashboardLayout.css";

function DashboardLayout() {
    const { auth, setAuth } = useContext(AuthContext);
    const location = useLocation();
    const navigate = useNavigate();
    const [collapsed, setCollapsed] = useState(
        () => window.localStorage.getItem(SIDEBAR_STORAGE_KEY) === "true"
    );

    useEffect(() => {
        window.localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
    }, [collapsed]);

    const handleLogout = () => {
        setAuth({});
        navigate("/");
    };

    const permissions = auth?.user?.permissions || permissionsForRole(auth?.user?.role);
    const visibleMenu = filterMenuByPermissions(SIDEBAR_MENU, permissions);
    const displayName = auth?.user?.name && auth?.user?.surname
        ? `${auth.user.name} ${auth.user.surname}`
        : auth?.user?.name || auth?.user?.email?.split("@")[0] || "";

    return (
        <div className={`dashboard-layout ${collapsed ? "dashboard-layout--collapsed" : ""}`}>
            <SideBar
                menu={visibleMenu}
                collapsed={collapsed}
                onToggleCollapse={() => setCollapsed((current) => !current)}
                activePath={location.pathname}
                user={{ name: displayName, email: auth?.user?.email }}
                onLogout={handleLogout}
            />

            <main className="dashboard-main">
                <Outlet />
            </main>
        </div>
    );
}

export default DashboardLayout;
