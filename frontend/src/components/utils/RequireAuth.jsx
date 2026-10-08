import { useContext } from "react";
import { Navigate, useLocation } from "react-router";
import AuthContext from "../../context/AuthProvider.jsx";

function RequireAuth({ children }) {
    const { auth } = useContext(AuthContext);
    const location = useLocation();

    if (!auth?.token) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }

    return children;
}

export function RequireRole({ children, role = null, permission = null }) {
    const { auth, hasRole, hasPermission } = useContext(AuthContext);
    const location = useLocation();

    if (!auth?.token) {
        return <Navigate to="/login" replace state={{ from: location.pathname }} />;
    }
    if (role && !hasRole(role)) {
        return <Navigate to="/dashboard" replace />;
    }
    if (permission && !hasPermission(permission)) {
        return <Navigate to="/dashboard" replace />;
    }
    return children;
}

export default RequireAuth;

