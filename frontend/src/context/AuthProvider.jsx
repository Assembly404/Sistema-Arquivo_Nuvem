import { createContext, useEffect, useMemo, useState } from "react";
import { setAuthToken } from "../api/axios.js";

const AuthContext = createContext({});

const STORAGE_KEY = "arquivo-nuvem.auth";

const ROLE_PERMISSIONS = {
    ADMIN: ["ADMIN_ACESSAR", "ADMIN_USUARIOS", "ADMIN_PERMISSOES", "ARQUIVO_VISUALIZAR", "ARQUIVO_ENVIAR", "ARQUIVO_EDITAR", "ARQUIVO_EXCLUIR", "ARQUIVO_PARTILHAR", "ARMAZENAMENTO_VISUALIZAR", "ARMAZENAMENTO_GERIR"],
    USER: ["ARQUIVO_VISUALIZAR", "ARQUIVO_ENVIAR", "ARQUIVO_EDITAR", "ARQUIVO_EXCLUIR", "ARQUIVO_PARTILHAR", "ARMAZENAMENTO_VISUALIZAR"]
};

const readStoredAuth = () => {
    if (typeof window === "undefined") return {};
    try {
        const raw = window.localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : {};
    } catch {
        return {};
    }
};

export const AuthProvider = ({ children }) => {
    const [auth, setAuth] = useState(readStoredAuth);

    useEffect(() => {
        setAuthToken(auth?.token || null);
        try {
            if (auth?.token) window.localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
            else window.localStorage.removeItem(STORAGE_KEY);
        } catch {
            /* armazenamento indisponível — a sessão dura apenas enquanto a aba estiver aberta */
        }
    }, [auth]);

    const value = useMemo(() => {
        const role = auth?.user?.role || null;
        const roleNome = auth?.user?.roleNome || (role ? ROLE_PERMISSIONS[role] ? (role === "ADMIN" ? "Administrador" : "Utilizador") : role : "");
        const permissions = auth?.user?.permissions || (role ? ROLE_PERMISSIONS[role] || [] : []);
        const hasRole = (wanted) => role === wanted;
        const hasPermission = (wanted) => permissions.includes(wanted);
        const logout = () => setAuth({});
        return { auth, setAuth, role, roleNome, permissions, hasRole, hasPermission, logout };
    }, [auth]);

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

export default AuthContext;
