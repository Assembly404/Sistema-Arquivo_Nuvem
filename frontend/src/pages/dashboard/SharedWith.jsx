import { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router";
import { FiChevronRight, FiDownload, FiFile, FiFolder, FiGrid, FiList, FiUser } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Toast from "../../components/toast/toast.jsx";
import { downloadFile, listSharedContents, listSharedWithMe, setAuthToken } from "../../api/axios.js";
import { fileTone } from "../../utils/fileType.js";
import "./MyFiles.css";
import "./SharedWith.css";

const TOAST_DURATION = 4500;

const formatBytes = (bytes) => {
    if (bytes == null) return "—";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

const formatDate = (value) => (value ? String(value).split("T")[0].split("-").reverse().join("/") : "—");

const permissionLabel = (permission) => (permission === "EDIT" ? "Pode editar" : "Só leitura");

function SharedWith() {
    const { auth } = useContext(AuthContext);
    const location = useLocation();

    const [path, setPath] = useState([]);
    const [navKey, setNavKey] = useState(location.key);
    const [folders, setFolders] = useState([]);
    const [files, setFiles] = useState([]);
    const [loading, setLoading] = useState(true);
    const [viewMode, setViewMode] = useState("grid");
    const [toast, setToast] = useState(null);
    const closeToast = useCallback(() => setToast(null), []);

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    if (navKey !== location.key) {
        setNavKey(location.key);
        setPath([]);
    }

    const currentFolderId = path.length ? path[path.length - 1].id : null;

    const load = useCallback(async (folderId) => {
        setLoading(true);
        try {
            if (folderId == null) {
                const res = await listSharedWithMe();
                const items = Array.isArray(res.data) ? res.data : [];
                setFolders(items.filter((i) => i.type === "FOLDER").map((i) => ({
                    folderID: i.itemID,
                    name: i.name,
                    ownerName: i.ownerName,
                    permission: i.permission,
                    createdAt: i.createdAt
                })));
                setFiles(items.filter((i) => i.type === "FILE").map((i) => ({
                    fileID: i.itemID,
                    originalName: i.name,
                    originalSizeBytes: i.sizeBytes,
                    mimeType: i.mimeType,
                    ownerName: i.ownerName,
                    permission: i.permission,
                    uploadedAt: i.createdAt
                })));
            } else {
                const res = await listSharedContents(folderId);
                const data = res.data || {};
                setFolders(Array.isArray(data.folders) ? data.folders : []);
                setFiles(Array.isArray(data.files) ? data.files : []);
            }
        } catch (err) {
            console.error("[shared] falha ao carregar", err);
            setToast({ message: "Não foi possível carregar os itens partilhados.", type: "error" });
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        if (auth?.token) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void load(currentFolderId);
        }
    }, [auth?.token, currentFolderId, load]);

    const openFolder = (folder) => setPath((prev) => [...prev, { id: folder.folderID, name: folder.name }]);
    const goToBreadcrumb = (index) => setPath((prev) => (index < 0 ? [] : prev.slice(0, index + 1)));

    const handleDownload = async (file) => {
        try {
            const res = await downloadFile(file.fileID);
            const url = URL.createObjectURL(res.data);
            const a = document.createElement("a");
            a.href = url;
            a.download = file.originalName;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
        } catch (err) {
            console.error("[shared] falha no download", err);
            setToast({ message: "Não foi possível descarregar o ficheiro.", type: "error" });
        }
    };

    const isEmpty = !loading && folders.length === 0 && files.length === 0;

    const breadcrumbs = useMemo(() => ([
        { id: null, name: "Compartilhados" },
        ...path
    ]), [path]);

    return (
        <section className="dashboard-page files-page shared-page">
            <div className="drive-toolbar">
                <h1 className="drive-title">Compartilhados comigo</h1>

                <div className="drive-toolbar__right">
                    <div className="drive-view" role="group" aria-label="Modo de visualização">
                        <button
                            type="button"
                            className={viewMode === "list" ? "on" : ""}
                            aria-pressed={viewMode === "list"}
                            aria-label="Ver como lista"
                            onClick={() => setViewMode("list")}
                        >
                            <FiList aria-hidden="true" />
                        </button>
                        <button
                            type="button"
                            className={viewMode === "grid" ? "on" : ""}
                            aria-pressed={viewMode === "grid"}
                            aria-label="Ver como grelha"
                            onClick={() => setViewMode("grid")}
                        >
                            <FiGrid aria-hidden="true" />
                        </button>
                    </div>
                </div>
            </div>

            <p className="dashboard-page__text">Ficheiros e pastas que outros utilizadores partilharam contigo.</p>

            <nav className="files-breadcrumb" aria-label="Caminho">
                {breadcrumbs.map((crumb, index) => (
                    <span key={crumb.id ?? "root"} className="files-breadcrumb__item">
                        {index > 0 && <FiChevronRight aria-hidden="true" />}
                        <button type="button" onClick={() => goToBreadcrumb(index - 1)} disabled={index === breadcrumbs.length - 1}>
                            {crumb.name}
                        </button>
                    </span>
                ))}
            </nav>

            {loading ? (
                <p className="dashboard-page__text">A carregar...</p>
            ) : isEmpty ? (
                <p className="dashboard-page__text">Ainda não tens itens partilhados contigo.</p>
            ) : viewMode === "grid" ? (
                <>
                    {folders.length > 0 && (
                        <>
                            <h2 className="drive-section__title">Pastas</h2>
                            <div className="drive-folders">
                                {folders.map((folder) => (
                                    <div key={`folder-${folder.folderID}`} className="drive-folder">
                                        <button type="button" className="drive-card__main" onClick={() => openFolder(folder)}>
                                            <span className="drive-icon drive-icon--folder"><FiFolder aria-hidden="true" /></span>
                                            <span className="drive-folder__name" title={folder.name}>{folder.name}</span>
                                        </button>
                                        <span className="drive-card__meta shared-row__meta">
                                            {folder.ownerName ?? ""}
                                            <span className="shared-row__permission">{permissionLabel(folder.permission)}</span>
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}

                    {files.length > 0 && (
                        <>
                            <h2 className="drive-section__title">Arquivos</h2>
                            <div className="drive-files">
                                {files.map((file) => (
                                    <div key={`file-${file.fileID}`} className="drive-file">
                                        <span className={`drive-file__thumb drive-icon--${fileTone(file.originalName)}`}>
                                            <FiFile aria-hidden="true" />
                                        </span>
                                        <div className="drive-file__body">
                                            <span className="drive-file__name" title={file.originalName}>{file.originalName}</span>
                                            <span className="drive-card__meta">
                                                {formatBytes(file.originalSizeBytes)} · {formatDate(file.uploadedAt)}
                                            </span>
                                            <span className="shared-row__meta">
                                                <FiUser aria-hidden="true" /> {file.ownerName ?? ""}
                                                <span className="shared-row__permission">{permissionLabel(file.permission)}</span>
                                            </span>
                                        </div>
                                        <div className="drive-card__actions">
                                            <button type="button" title="Descarregar" aria-label={`Descarregar ${file.originalName}`} onClick={() => handleDownload(file)}>
                                                <FiDownload aria-hidden="true" />
                                            </button>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </>
            ) : (
                (folders.length > 0 || files.length > 0) && (
                    <table className="drive-table">
                        <thead>
                            <tr>
                                <th>Nome</th>
                                <th>Tamanho</th>
                                <th>Partilhado por</th>
                                <th>Permissão</th>
                                <th className="drive-table__actions-col">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {folders.map((folder) => (
                                <tr key={`folder-${folder.folderID}`}>
                                    <td>
                                        <button type="button" className="drive-table__name" onClick={() => openFolder(folder)}>
                                            <span className="drive-icon drive-icon--folder"><FiFolder aria-hidden="true" /></span>
                                            {folder.name}
                                        </button>
                                    </td>
                                    <td>—</td>
                                    <td>{folder.ownerName ?? ""}</td>
                                    <td>{permissionLabel(folder.permission)}</td>
                                    <td className="drive-table__actions-col" />
                                </tr>
                            ))}
                            {files.map((file) => (
                                <tr key={`file-${file.fileID}`}>
                                    <td>
                                        <span className="drive-table__name">
                                            <span className={`drive-icon drive-icon--${fileTone(file.originalName)}`}><FiFile aria-hidden="true" /></span>
                                            {file.originalName}
                                        </span>
                                    </td>
                                    <td>{formatBytes(file.originalSizeBytes)}</td>
                                    <td>{file.ownerName ?? ""}</td>
                                    <td>{permissionLabel(file.permission)}</td>
                                    <td className="drive-table__actions-col">
                                        <div className="drive-card__actions">
                                            <button type="button" title="Descarregar" aria-label={`Descarregar ${file.originalName}`} onClick={() => handleDownload(file)}>
                                                <FiDownload aria-hidden="true" />
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default SharedWith;