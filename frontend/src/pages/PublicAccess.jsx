import { useCallback, useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import {
    FiChevronRight,
    FiDownload,
    FiFile,
    FiFolder,
    FiLock
} from "react-icons/fi";
import Input from "../components/form/input.jsx";
import Button from "../components/button/button.jsx";
import {
    downloadPublicLink,
    getPublicLinkContents,
    getPublicLinkInfo,
    verifyPublicLink
} from "../api/axios.js";
import "./PublicAccess.css";

const formatBytes = (bytes) => {
    if (bytes == null) return "—";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

const triggerDownload = (blob, filename) => {
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = filename || "download";
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(url);
};

function PublicAccess() {
    const { token } = useParams();
    const [info, setInfo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [password, setPassword] = useState("");
    const [unlocked, setUnlocked] = useState(false);
    const [passwordError, setPasswordError] = useState("");
    const [busy, setBusy] = useState(false);
    const [contents, setContents] = useState({ folders: [], files: [] });
    const [stack, setStack] = useState([{ id: null, name: "Início" }]);
    const [status, setStatus] = useState("");

    const requiresPassword = info?.passwordProtected && !unlocked;

    useEffect(() => {
        let active = true;
        (async () => {
            setLoading(true);
            try {
                const res = await getPublicLinkInfo(token);
                if (active) setInfo(res.data);
            } catch (err) {
                if (active) setError(err?.response?.data?.message || "Não foi possível abrir este link.");
            } finally {
                if (active) setLoading(false);
            }
        })();
        return () => { active = false; };
    }, [token]);

    const loadContents = useCallback(async (folderId, pwd) => {
        setBusy(true);
        setStatus("");
        try {
            const res = await getPublicLinkContents(token, folderId, pwd);
            setContents({
                folders: Array.isArray(res.data?.folders) ? res.data.folders : [],
                files: Array.isArray(res.data?.files) ? res.data.files : []
            });
            return true;
        } catch (err) {
            if (err?.response?.status === 401) {
                setPasswordError("Senha incorreta.");
                setUnlocked(false);
            } else {
                setError(err?.response?.data?.message || "Não foi possível carregar o conteúdo.");
            }
            return false;
        } finally {
            setBusy(false);
        }
    }, [token]);

    useEffect(() => {
        if (info?.type === "FOLDER" && !requiresPassword) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            void loadContents(null, password);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [info, requiresPassword]);

    const submitPassword = async (e) => {
        e.preventDefault();
        setPasswordError("");
        try {
            await verifyPublicLink(token, password);
            setUnlocked(true);
            if (info?.type === "FOLDER") {
                await loadContents(null, password);
            }
        } catch {
            setPasswordError("Senha incorreta.");
        }
    };

    const handleDownload = async (file) => {
        setBusy(true);
        setStatus("");
        try {
            const res = await downloadPublicLink(token, password, null);
            triggerDownload(new Blob([res.data]), file?.originalName || info?.name);
        } catch (err) {
            if (err?.response?.status === 401) {
                setPasswordError("Senha incorreta.");
                setUnlocked(false);
            } else {
                setStatus("Não foi possível descarregar o ficheiro.");
            }
        } finally {
            setBusy(false);
        }
    };

    const openFolder = (folder) => {
        setStack((prev) => [...prev, { id: folder.folderID, name: folder.name }]);
        void loadContents(folder.folderID, password);
    };

    const goTo = (index) => {
        const next = stack.slice(0, index + 1);
        setStack(next);
        void loadContents(next[next.length - 1].id, password);
    };

    return (
        <div className="public-access">
            <div className="public-access__card">
                <header className="public-access__header">
                    <span className="public-access__brand">Arquivo Nuvem</span>
                    <h1 className="public-access__title">Partilha pública</h1>
                </header>

                {loading && <p className="dashboard-page__text">A carregar...</p>}

                {!loading && error && (
                    <div className="public-access__error">
                        <p>{error}</p>
                        <Link to="/" className="public-access__link">Ir para a página inicial</Link>
                    </div>
                )}

                {!loading && !error && info && (
                    <>
                        <div className="public-access__item">
                            <span className="public-access__item-icon" aria-hidden="true">
                                {info.type === "FOLDER" ? <FiFolder /> : <FiFile />}
                            </span>
                            <div>
                                <h2 className="public-access__item-name">{info.name}</h2>
                                <p className="public-access__item-meta">
                                    {info.ownerName}
                                    {info.type === "FILE" && info.sizeBytes != null ? ` · ${formatBytes(info.sizeBytes)}` : ""}
                                    {info.expiresAt ? ` · expira em ${String(info.expiresAt).split("T")[0]}` : ""}
                                </p>
                            </div>
                        </div>

                        {requiresPassword && (
                            <form className="public-access__password" onSubmit={submitPassword}>
                                <p className="public-access__password-hint">
                                    <FiLock aria-hidden="true" /> Este link está protegido por senha.
                                </p>
                                <Input
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="Senha"
                                    error={passwordError}
                                    autoComplete="off"
                                />
                                <Button label="Desbloquear" variant="primary" type="submit" disabled={!password} />
                            </form>
                        )}

                        {!requiresPassword && info.type === "FILE" && (
                            <div className="public-access__actions">
                                <Button
                                    label="Descarregar"
                                    variant="primary"
                                    icon={<FiDownload aria-hidden="true" />}
                                    loading={busy}
                                    onClick={() => handleDownload(null)}
                                />
                            </div>
                        )}

                        {!requiresPassword && info.type === "FOLDER" && (
                            <div className="public-access__folder">
                                <nav className="public-access__breadcrumbs" aria-label="Navegação">
                                    {stack.map((crumb, index) => (
                                        <span key={`${crumb.id}-${index}`} className="public-access__crumb">
                                            {index > 0 && <FiChevronRight aria-hidden="true" />}
                                            <button type="button" onClick={() => goTo(index)} disabled={index === stack.length - 1}>
                                                {crumb.name}
                                            </button>
                                        </span>
                                    ))}
                                </nav>

                                {busy ? (
                                    <p className="dashboard-page__text">A carregar...</p>
                                ) : contents.folders.length === 0 && contents.files.length === 0 ? (
                                    <p className="dashboard-page__text">Esta pasta está vazia.</p>
                                ) : (
                                    <ul className="public-access__list">
                                        {contents.folders.map((folder) => (
                                            <li key={`folder-${folder.folderID}`} className="public-access__list-row">
                                                <button type="button" className="public-access__folder-btn" onClick={() => openFolder(folder)}>
                                                    <FiFolder aria-hidden="true" />
                                                    <span>{folder.name}</span>
                                                </button>
                                            </li>
                                        ))}
                                        {contents.files.map((file) => (
                                            <li key={`file-${file.fileID}`} className="public-access__list-row">
                                                <span className="public-access__file">
                                                    <FiFile aria-hidden="true" />
                                                    <span>{file.originalName}</span>
                                                    <span className="public-access__file-size">{formatBytes(file.originalSizeBytes)}</span>
                                                </span>
                                                <button type="button" title="Descarregar" aria-label={`Descarregar ${file.originalName}`} onClick={() => handleDownload(file)}>
                                                    <FiDownload aria-hidden="true" />
                                                </button>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                                {status && <p className="dashboard-page__text">{status}</p>}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

export default PublicAccess;
