import { createContext, useMemo, useState } from "react";

const AuthContext = createContext({});

const ROLE_PERMISSIONS = {
    ADMIN: ["ADMIN_ACESSAR", "ADMIN_USUARIOS", "ADMIN_PERMISSOES", "ARQUIVO_VISUALIZAR", "ARQUIVO_EXCLUIR", "ARMAZENAMENTO_VISUALIZAR"],
    USER: ["ARQUIVO_VISUALIZAR", "ARMAZENAMENTO_VISUALIZAR"]
};

export const AuthProvider = ({ children }) => {
    const [auth, setAuth] = useState({});

    const value = useMemo(() => {
        const role = auth?.user?.role || null;
        const permissions = auth?.user?.permissions || (role ? ROLE_PERMISSIONS[role] || [] : []);
        const hasRole = (wanted) => role === wanted;
        const hasPermission = (wanted) => permissions.includes(wanted);
        const logout = () => setAuth({});
        return { auth, setAuth, role, permissions, hasRole, hasPermission, logout };
    }, [auth]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
