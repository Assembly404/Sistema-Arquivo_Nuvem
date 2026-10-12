import { useCallback, useContext, useEffect, useState } from "react";
import { FiFile, FiFolder, FiRefreshCw, FiTrash2 } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    listTrashFiles,
    listTrashFolders,
    purgeFile,
    purgeFolder,
    restoreFile,
    restoreFolder,
    setAuthToken
} from "../../api/axios.js";
import "./MyFiles.css";

const TOAST_DURATION = 4500;

const formatDate = (value) => (value ? String(value).split("T")[0].split("-").reverse().join("/") : "—");

function Trash() {
    const { auth } = useContext(AuthContext);
    const [files, setFiles] = useState([]);
    const [folders, setFolders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState(null);
    const closeToast = useCallback(() => setToast(null), []);

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const [filesRes, foldersRes] = await Promise.all([listTrashFiles(), listTrashFolders()]);
            setFiles(Array.isArray(filesRes.data) ? filesRes.data : []);
            setFolders(Array.isArray(foldersRes.data) ? foldersRes.data : []);
        } catch (err) {
            console.error("[trash] falha ao carregar", err);
            setToast({ message: "Não foi possível carregar a lixeira.", type: "error" });
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (auth?.token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void load();
        }
    }, [auth?.token, load]);

    const restore = async (fn, id, label) => {
        try {
            await fn(id);
            setToast({ message: `${label} restaurado.`, type: "success" });
            void load();
        } catch (err) {
            console.error("[trash] falha ao restaurar", err);
            setToast({ message: "Não foi possível restaurar.", type: "error" });
        }
    };

    const purge = async (fn, id, label) => {
        try {
            await fn(id);
            setToast({ message: `${label} eliminado definitivamente.`, type: "success" });
            void load();
        } catch (err) {
            console.error("[trash] falha ao eliminar definitivamente", err);
            setToast({ message: "Não foi possível eliminar definitivamente.", type: "error" });
        }
    };

    const isEmpty = !loading && files.length === 0 && folders.length === 0;

    return (
        <section className="dashboard-page files-page">
            <h1 className="dashboard-page__title">Lixeira</h1>
            <p className="dashboard-page__text">Os itens aqui podem ser restaurados ou eliminados definitivamente.</p>

            {loading ? (
                <p className="dashboard-page__text">A carregar...</p>
            ) : isEmpty ? (
                <p className="dashboard-page__text">A lixeira está vazia.</p>
            ) : (
                <div className="files-list">
                    {folders.map((folder) => (
                        <div key={`folder-${folder.folderID}`} className="files-row">
                            <div className="files-row__main">
                                <FiFolder className="files-row__icon" aria-hidden="true" />
                                <span className="files-row__name">{folder.name}</span>
                                <span className="files-row__meta">Pasta · {formatDate(folder.deletedAt)}</span>
                            </div>
                            <div className="files-row__actions">
                                <button type="button" title="Restaurar" aria-label={`Restaurar pasta ${folder.name}`} onClick={() => restore(restoreFolder, folder.folderID, `Pasta "${folder.name}"`)}>
                                    <FiRefreshCw aria-hidden="true" />
                                </button>
                                <button type="button" title="Eliminar definitivamente" aria-label={`Eliminar pasta ${folder.name}`} onClick={() => purge(purgeFolder, folder.folderID, `Pasta "${folder.name}"`)}>
                                    <FiTrash2 aria-hidden="true" />
                                </button>
                            </div>
                        </div>
                    ))}

                    {files.map((file) => (
                        <div key={`file-${file.fileID}`} className="files-row">
                            <div className="files-row__main">
                                <FiFile className="files-row__icon" aria-hidden="true" />
                                <span className="files-row__name">{file.originalName}</span>
                                <span className="files-row__meta">{formatDate(file.deletedAt)}</span>
                            </div>
                            <div className="files-row__actions">
                                <button type="button" title="Restaurar" aria-label={`Restaurar ${file.originalName}`} onClick={() => restore(restoreFile, file.fileID, `"${file.originalName}"`)}>
                                    <FiRefreshCw aria-hidden="true" />
                                </button>
                                <button type="button" title="Eliminar definitivamente" aria-label={`Eliminar ${file.originalName}`} onClick={() => purge(purgeFile, file.fileID, `"${file.originalName}"`)}>
                                    <FiTrash2 aria-hidden="true" />
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {!loading && !isEmpty && (
                <div className="files-toolbar">
                    <Button label="Atualizar" variant="ghost" icon={<FiRefreshCw aria-hidden="true" />} onClick={load} />
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Trash;
