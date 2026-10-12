import { useCallback, useContext, useEffect, useRef, useState } from "react";
import { FiCheck, FiX } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Toast from "../../components/toast/toast.jsx";
import { getPermissions, setAuthToken } from "../../api/axios.js";
import "./AdminPermissions.css";

const TOAST_DURATION = 4500;

const PERMISSION_LABELS = {
    ADMIN_ACESSAR: "Aceder à administração",
    ADMIN_USUARIOS: "Gerir utilizadores",
    ADMIN_PERMISSOES: "Gerir perfis & permissões",
    ARQUIVO_VISUALIZAR: "Visualizar arquivos",
    ARQUIVO_EXCLUIR: "Excluir arquivos",
    ARMAZENAMENTO_VISUALIZAR: "Visualizar armazenamento"
};

function AdminPermissions() {
    const { auth } = useContext(AuthContext);
    const [matrix, setMatrix] = useState({});
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState(null);
    const titleRef = useRef(null);
    const closeToast = useCallback(() => setToast(null), []);
    useEffect(() => { titleRef.current?.focus(); }, []);
    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    useEffect(() => {
        if (!auth?.token) return undefined;
        let mounted = true;
        getPermissions()
            .then((res) => { if (mounted) setMatrix(res.data || {}); })
            .catch((err) => {
                console.error("[admin-permissions] falha ao carregar", err);
                if (mounted) setToast({ message: "Não foi possível carregar as permissões.", type: "error" });
            })
            .finally(() => { if (mounted) setLoading(false); });
        return () => { mounted = false; };
    }, [auth?.token]);

    const roles = Object.keys(matrix);
    const allPermissions = [...new Set(Object.values(matrix).flat())].sort();

    return (
        <section className="dashboard-page admin-permissions">
            <h1 className="dashboard-page__title" ref={titleRef} tabIndex={-1}>Perfis &amp; Permissões</h1>
            <p className="dashboard-page__text">Papéis disponíveis e as permissões atribuídas a cada um.</p>

            <div className="card">
                {loading ? (
                    <p className="dashboard-page__text">A carregar permissões...</p>
                ) : roles.length === 0 ? (
                    <p className="dashboard-page__text">Sem papéis para mostrar.</p>
                ) : (
                    <div className="admin-users__table-wrap">
                        <table className="admin-users__table perm-table">
                            <thead>
                                <tr>
                                    <th scope="col">Permissão</th>
                                    {roles.map((role) => (
                                        <th key={role} scope="col">{role}</th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody>
                                {allPermissions.map((permission) => (
                                    <tr key={permission}>
                                        <td data-label="Permissão">
                                            <span className="admin-users__name">{PERMISSION_LABELS[permission] || permission}</span>
                                            <span className="perm-code">{permission}</span>
                                        </td>
                                        {roles.map((role) => {
                                            const has = (matrix[role] || []).includes(permission);
                                            return (
                                                <td key={role} data-label={role} className={has ? "perm-yes" : "perm-no"}>
                                                    {has
                                                        ? <FiCheck className="perm-check" aria-label="Permitido" />
                                                        : <FiX className="perm-cross" aria-label="Não permitido" />}
                                                    <span className="sr-only">{has ? "Sim" : "Não"}</span>
                                                </td>
                                            );
                                        })}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default AdminPermissions;