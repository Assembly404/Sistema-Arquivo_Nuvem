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

export default RequireAuth;
