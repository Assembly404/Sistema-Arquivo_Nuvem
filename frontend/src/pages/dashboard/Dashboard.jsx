import { useCallback, useContext, useEffect, useState } from "react";
import { Link } from "react-router";
import { FiFile, FiFolder, FiHardDrive, FiShare2, FiUploadCloud } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Toast from "../../components/toast/toast.jsx";
import { getStorageSummary, listFiles, listSharedWithMe, setAuthToken } from "../../api/axios.js";
import "./Dashboard.css";

const TOAST_DURATION = 4500;

const formatBytes = (bytes) => {
    if (bytes == null) return "0 B";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

function Dashboard() {
    const { auth } = useContext(AuthContext);
    const [summary, setSummary] = useState(null);
    const [sharedCount, setSharedCount] = useState(0);
    const [recent, setRecent] = useState([]);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState(null);
    const closeToast = useCallback(() => setToast(null), []);

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    useEffect(() => {
        if (!auth?.token) return undefined;
        let mounted = true;
        Promise.all([getStorageSummary(), listSharedWithMe(), listFiles({ search: "" })])
            .then(([summaryRes, sharedRes, filesRes]) => {
                if (!mounted) return;
                setSummary(summaryRes.data);
                setSharedCount(Array.isArray(sharedRes.data) ? sharedRes.data.length : 0);
                const files = Array.isArray(filesRes.data) ? filesRes.data : [];
                setRecent(files.slice(0, 5));
            })
            .catch((err) => {
                console.error("[dashboard] falha ao carregar", err);
                if (mounted) setToast({ message: "Não foi possível carregar o painel.", type: "error" });
            })
            .finally(() => { if (mounted) setLoading(false); });
        return () => { mounted = false; };
    }, [auth?.token]);

    const cloud = summary?.cloud || {};
    const quota = cloud.quotaBytes;
    const used = cloud.usedBytes || 0;
    const hasQuota = cloud.hasQuota;
    const pct = hasQuota && quota > 0 ? Math.min(Math.round((used * 100) / quota), 100) : 0;
    const nodes = Array.isArray(summary?.nodes) ? summary.nodes : [];
    const onlineNodes = nodes.filter((n) => n.status === "ONLINE").length;

    return (
        <section className="dashboard-page dashboard-home">
            <h1 className="dashboard-page__title">Início</h1>
            <p className="dashboard-page__text">Visão geral do teu espaço na nuvem.</p>

            {loading ? (
                <p className="dashboard-page__text">A carregar...</p>
            ) : (
                <>
                    <div className="home-cards">
                        <div className="card home-card home-card--grow">
                            <h2 className="card__title"><FiUploadCloud aria-hidden="true" /> Nuvem do sistema</h2>
                            <div className="storage-usage" role="progressbar" aria-label="Uso da nuvem do sistema"
                                aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
                                <div className="storage-usage__fill" style={{ width: `${pct}%` }} />
                            </div>
                            <p className="home-card__numbers">
                                {formatBytes(used)} {hasQuota ? `de ${formatBytes(quota)}` : "(ilimitado)"}
                            </p>
                            <Link className="home-card__link" to="/dashboard/armazenamento">Ver detalhes</Link>
                        </div>

                        <div className="card home-card">
                            <h2 className="card__title"><FiFile aria-hidden="true" /> Ficheiros</h2>
                            <p className="home-card__numbers">{cloud.fileCount ?? 0}</p>
                            <Link className="home-card__link" to="/dashboard/arquivos">Abrir arquivos</Link>
                        </div>

                        <div className="card home-card">
                            <h2 className="card__title"><FiShare2 aria-hidden="true" /> Compartilhados</h2>
                            <p className="home-card__numbers">{sharedCount}</p>
                            <Link className="home-card__link" to="/dashboard/arquivos/compartilhados">Ver partilhas</Link>
                        </div>

                        <div className="card home-card">
                            <h2 className="card__title"><FiHardDrive aria-hidden="true" /> Dispositivos</h2>
                            <p className="home-card__numbers">
                                {onlineNodes}/{nodes.length} <span className="home-card__suffix">online</span>
                            </p>
                            <Link className="home-card__link" to="/dashboard/armazenamento">Gerir dispositivos</Link>
                        </div>
                    </div>

                    <div className="card home-card home-card--recent">
                        <h2 className="card__title"><FiFolder aria-hidden="true" /> Últimos arquivados</h2>
                        {recent.length === 0 ? (
                            <p className="dashboard-page__text">Ainda não tens ficheiros. <Link to="/dashboard/arquivos">Carrega o primeiro.</Link></p>
                        ) : (
                            <ul className="home-recent">
                                {recent.map((file) => (
                                    <li key={file.fileID}>
                                        <FiFile aria-hidden="true" />
                                        <span className="home-recent__name">{file.originalName}</span>
                                        <span className="home-recent__size">{formatBytes(file.originalSizeBytes)}</span>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Dashboard;