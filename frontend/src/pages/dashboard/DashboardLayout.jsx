import { useContext, useEffect, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router";
import SideBar from "../../components/header/sidebar.jsx";
import { SIDEBAR_MENU, SIDEBAR_STORAGE_KEY } from "../../components/header/sidebarConfig.js";
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

    return (
        <div className={`dashboard-layout ${collapsed ? "dashboard-layout--collapsed" : ""}`}>
            <SideBar
                menu={SIDEBAR_MENU}
                collapsed={collapsed}
                onToggleCollapse={() => setCollapsed((current) => !current)}
                activePath={location.pathname}
                user={{ name: auth?.name, email: auth?.email }}
                onLogout={handleLogout}
            />

            <main className="dashboard-main">
                <Outlet />
            </main>
        </div>
    );
}

export default DashboardLayout;
