import { useCallback, useContext, useEffect, useMemo, useState } from "react";
import {
    FiActivity,
    FiCheckCircle,
    FiCopy,
    FiCpu,
    FiHardDrive,
    FiKey,
    FiRefreshCw,
    FiSettings,
    FiShield,
    FiTrash2,
    FiUploadCloud,
    FiXCircle
} from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    createPairingCode,
    disableNode,
    enableNode,
    getNodeSyncStatus,
    getStorageSummary,
    migrateNode,
    registerNode,
    removeNode,
    setAuthToken,
    setNodeQuota,
    setNodeVisibility,
    setOfflineBehavior,
    setStoragePolicy
} from "../../api/axios.js";
import "./Storage.css";

const TOAST_DURATION = 4500;

const formatBytes = (bytes) => {
    if (bytes == null) return "—";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

const formatDate = (value) => (value ? String(value).split(".")[0].split("T").join(" ") : "—");

const POLICY_OPTIONS = [
    { value: "SYSTEM_FIRST", label: "Nuvem do sistema primeiro" },
    { value: "DEVICE_FIRST", label: "Dispositivo pessoal primeiro" },
    { value: "MANUAL", label: "Escolher manualmente no upload" }
];

const OFFLINE_OPTIONS = [
    { value: "DENY", label: "Negar acesso ao ficheiro" },
    { value: "CACHE", label: "Servir cópia em cache quando exista" }
];

const OS_OPTIONS = ["Windows", "macOS", "Linux"];

const STATUS_META = {
    ONLINE: { label: "Online", className: "status-online" },
    DEGRADED: { label: "Degradado", className: "status-degraded" },
    OFFLINE: { label: "Offline", className: "status-offline" }
};

function Storage() {
    const { auth } = useContext(AuthContext);
    const [summary, setSummary] = useState(null);
    const [loading, setLoading] = useState(true);
    const [savingQuotaNode, setSavingQuotaNode] = useState(null);
    const [editingQuota, setEditingQuota] = useState(null);
    const [quotaValue, setQuotaValue] = useState("");
    const [savingPolicy, setSavingPolicy] = useState(false);
    const [savingOffline, setSavingOffline] = useState(false);
    const [changingNode, setChangingNode] = useState(null);
    const [toast, setToast] = useState(null);

    // Configuração/registo de dispositivos
    const [showConfig, setShowConfig] = useState(false);
    const [savingConfig, setSavingConfig] = useState(false);
    const [cfgName, setCfgName] = useState("");
    const [cfgAddress, setCfgAddress] = useState("");
    const [cfgPath, setCfgPath] = useState("");
    const [cfgSpaceGb, setCfgSpaceGb] = useState("100");
    const [cfgOs, setCfgOs] = useState("Linux");
    const [cfgVisibility, setCfgVisibility] = useState("PERSONAL");
    const [registered, setRegistered] = useState(null);
    const [pairingCode, setPairingCode] = useState(null);

    // Remoção / migração / sincronização
    const [removing, setRemoving] = useState(null);
    const [migrateOnRemove, setMigrateOnRemove] = useState(false);
    const [syncNode, setSyncNode] = useState(null);
    const [syncData, setSyncData] = useState(null);
    const [syncLoading, setSyncLoading] = useState(false);

    const closeToast = useCallback(() => setToast(null), []);
    const apiUrl = useMemo(() => import.meta.env.VITE_API_URL || "http://localhost:5000", []);
    const isWindows = useMemo(
        () => typeof navigator !== "undefined" && /win/i.test(navigator.platform || navigator.userAgent || ""),
        []
    );

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const res = await getStorageSummary();
            setSummary(res.data);
        } catch (err) {
            console.error("[storage] falha ao carregar resumo", err);
            setToast({ message: "Não foi possível carregar o armazenamento.", type: "error" });
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

    const cloud = summary?.cloud || {};
    const system = summary?.system || {};
    const policy = summary?.policy || "SYSTEM_FIRST";
    const offlineBehavior = summary?.offlineBehavior || "DENY";
    const hasQuota = cloud.hasQuota;
    const quota = cloud.quotaBytes;
    const used = cloud.usedBytes || 0;
    const usagePercent = hasQuota && quota > 0 ? Math.min(Math.round((used * 100) / quota), 100) : 0;
    const nodes = Array.isArray(summary?.nodes) ? summary.nodes : [];

    const copy = async (text) => {
        try {
            await navigator.clipboard.writeText(text);
            setToast({ message: "Copiado para a área de transferência.", type: "success" });
        } catch {
            setToast({ message: "Não foi possível copiar.", type: "error" });
        }
    };

    const handlePolicy = async (e) => {
        const value = e.target.value;
        setSavingPolicy(true);
        try {
            const res = await setStoragePolicy({ policy: value });
            setSummary((prev) => ({ ...prev, policy: res.data.policy, policyLabel: res.data.label }));
            setToast({ message: "Política de armazenamento atualizada.", type: "success" });
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível atualizar a política.", type: "error" });
        } finally {
            setSavingPolicy(false);
        }
    };

    const handleOfflineBehavior = async (e) => {
        const value = e.target.value;
        setSavingOffline(true);
        try {
            const res = await setOfflineBehavior(value);
            setSummary((prev) => ({ ...prev, offlineBehavior: res.data.offlineBehavior, offlineBehaviorLabel: res.data.label }));
            setToast({ message: "Comportamento offline atualizado.", type: "success" });
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível atualizar o comportamento.", type: "error" });
        } finally {
            setSavingOffline(false);
        }
    };

    const startQuotaEdit = (node) => {
        setEditingQuota(node.nodeID);
        setQuotaValue(String(node.totalBytes || ""));
    };

    const submitNodeQuota = async (node) => {
        const raw = Number(quotaValue);
        if (!Number.isFinite(raw) || raw < 0) {
            setToast({ message: "Indica um valor válido em bytes.", type: "error" });
            return;
        }
        if (raw < node.usedBytes) {
            setToast({ message: "A quota não pode ser inferior ao espaço usado.", type: "error" });
            return;
        }
        setSavingQuotaNode(node.nodeID);
        try {
            await setNodeQuota(node.nodeID, raw);
            setEditingQuota(null);
            setToast({ message: `Quota de "${node.name}" atualizada.`, type: "success" });
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível atualizar a quota.", type: "error" });
        } finally {
            setSavingQuotaNode(null);
        }
    };

    const handleVisibility = async (node, value) => {
        setChangingNode(node.nodeID);
        try {
            await setNodeVisibility(node.nodeID, value);
            setToast({ message: `Visibilidade de "${node.name}" atualizada.`, type: "success" });
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível alterar a visibilidade.", type: "error" });
        } finally {
            setChangingNode(null);
        }
    };

    const toggleEnabled = async (node) => {
        setChangingNode(node.nodeID);
        try {
            if (node.enabled) await disableNode(node.nodeID);
            else await enableNode(node.nodeID);
            setToast({ message: `Dispositivo "${node.name}" ${node.enabled ? "desativado" : "ativado"}.`, type: "success" });
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível alterar o estado.", type: "error" });
        } finally {
            setChangingNode(null);
        }
    };

    const openConfig = () => {
        setCfgName("");
        setCfgAddress("");
        setCfgPath("");
        setCfgSpaceGb("100");
        setCfgOs("Linux");
        setCfgVisibility("PERSONAL");
        setRegistered(null);
        setPairingCode(null);
        setShowConfig(true);
    };

    const submitConfig = async (e) => {
        e.preventDefault();
        if (!cfgName.trim()) {
            setToast({ message: "Indica o nome do dispositivo.", type: "error" });
            return;
        }
        if (!cfgPath.trim()) {
            setToast({ message: "Indica a pasta do dispositivo a partilhar.", type: "error" });
            return;
        }
        const gb = Number(cfgSpaceGb);
        const totalBytes = Number.isFinite(gb) && gb > 0 ? Math.round(gb * 1024 * 1024 * 1024) : null;
        setSavingConfig(true);
        try {
            const res = await registerNode({
                name: cfgName.trim(),
                address: cfgAddress.trim() || null,
                localPath: cfgPath.trim(),
                totalSpaceBytes: totalBytes,
                operatingSystem: cfgOs,
                visibility: cfgVisibility
            });
            setRegistered({
                ...res.data,
                localPath: cfgPath.trim(),
                operatingSystem: cfgOs,
                address: cfgAddress.trim() || null
            });
            setToast({ message: "Dispositivo registado e vinculado à tua conta.", type: "success" });
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível configurar o dispositivo.", type: "error" });
        } finally {
            setSavingConfig(false);
        }
    };

    const generatePairing = async () => {
        try {
            const res = await createPairingCode();
            setPairingCode(res.data);
            setToast({ message: "Código de pareamento gerado.", type: "success" });
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível gerar o código.", type: "error" });
        }
    };

    const confirmRemove = async () => {
        if (!removing) return;
        setChangingNode(removing.nodeID);
        try {
            await removeNode(removing.nodeID, migrateOnRemove);
            setToast({ message: `Dispositivo "${removing.name}" removido.`, type: "success" });
            setRemoving(null);
            setMigrateOnRemove(false);
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível remover o dispositivo.", type: "error" });
        } finally {
            setChangingNode(null);
        }
    };

    const runMigrate = async (node) => {
        setChangingNode(node.nodeID);
        try {
            const res = await migrateNode(node.nodeID);
            setToast({ message: `${res.data.migrated} ficheiro(s) migrado(s) para a nuvem do sistema.`, type: "success" });
            void load();
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível migrar os ficheiros.", type: "error" });
        } finally {
            setChangingNode(null);
        }
    };

    const openSync = async (node) => {
        setSyncNode(node);
        setSyncData(null);
        setSyncLoading(true);
        try {
            const res = await getNodeSyncStatus(node.nodeID);
            setSyncData(res.data);
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível obter o estado de sincronização.", type: "error" });
        } finally {
            setSyncLoading(false);
        }
    };

    return (
        <section className="dashboard-page storage-page">
            <h1 className="dashboard-page__title">Armazenamento</h1>
            <p className="dashboard-page__text">Gere a nuvem do sistema e os teus dispositivos pessoais como espaços de armazenamento.</p>

            {loading ? (
                <p className="dashboard-page__text">A carregar...</p>
            ) : (
                <>
                    <div className="storage-grid">
                        <div className="card storage-card">
                            <h2 className="card__title"><FiUploadCloud aria-hidden="true" /> Nuvem do sistema (tua cota)</h2>
                            <div className="storage-usage" role="progressbar"
                                aria-label="Uso da nuvem do sistema"
                                aria-valuenow={usagePercent} aria-valuemin={0} aria-valuemax={100}>
                                <div className="storage-usage__fill" style={{ width: `${usagePercent}%` }} />
                            </div>
                            <p className="storage-card__numbers">
                                <strong>{formatBytes(used)}</strong>
                                {hasQuota ? ` de ${formatBytes(quota)}` : " (ilimitado)"} utilizados
                            </p>
                            <p className="dashboard-page__text">{cloud.fileCount ?? 0} ficheiro{cloud.fileCount === 1 ? "" : "s"} guardados</p>
                            <p className="storage-device__meta">
                                Servidor: {system.unlimited ? "espaço ilimitado" : `${formatBytes(system.usedBytes)} / ${formatBytes(system.totalBytes)}`}
                            </p>
                        </div>

                        <div className="card storage-card">
                            <h2 className="card__title"><FiCpu aria-hidden="true" /> Política de armazenamento</h2>
                            <p className="dashboard-page__text">Define o destino preferido por omissão nos carregamentos.</p>
                            <label className="sr-only" htmlFor="storage-policy">Política de armazenamento</label>
                            <select id="storage-policy" className="admin-users__role storage-policy" value={policy} onChange={handlePolicy} disabled={savingPolicy}>
                                {POLICY_OPTIONS.map((opt) => (
                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                ))}
                            </select>

                            <h2 className="card__title"><FiShield aria-hidden="true" /> Dispositivo offline</h2>
                            <p className="dashboard-page__text">O que fazer quando um dispositivo que guarda o ficheiro está offline.</p>
                            <label className="sr-only" htmlFor="storage-offline">Comportamento offline</label>
                            <select id="storage-offline" className="admin-users__role storage-policy" value={offlineBehavior} onChange={handleOfflineBehavior} disabled={savingOffline}>
                                {OFFLINE_OPTIONS.map((opt) => (
                                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    <div className="card storage-card">
                        <div className="storage-toolbar">
                            <h2 className="card__title"><FiHardDrive aria-hidden="true" /> Dispositivos pessoais</h2>
                            <div className="storage-toolbar__actions">
                                <Button label="Configurar agente local" variant="primary" icon={<FiSettings aria-hidden="true" />} onClick={openConfig} />
                                <Button label="Atualizar" variant="ghost" icon={<FiRefreshCw aria-hidden="true" />} onClick={load} />
                            </div>
                        </div>
                        <p className="dashboard-page__text">
                            Liga um computador (agente local) à tua conta para o usar como espaço de armazenamento extra.
                            Podes registar vários dispositivos com nomes diferentes.
                        </p>

                        {nodes.length === 0 ? (
                            <p className="dashboard-page__text">Ainda não tens dispositivos configurados.</p>
                        ) : (
                            <div className="files-list">
                                {nodes.map((node) => {
                                    const meta = STATUS_META[node.status] || STATUS_META.OFFLINE;
                                    const pct = node.totalBytes > 0 ? Math.min(Math.round((node.usedBytes * 100) / node.totalBytes), 100) : 0;
                                    const isEditing = editingQuota === node.nodeID;
                                    return (
                                        <div key={node.nodeID} className="files-row storage-device">
                                            <div className="files-row__main">
                                                <FiHardDrive className="files-row__icon" aria-hidden="true" />
                                                <span className="files-row__name">{node.name || "Dispositivo"}</span>
                                                <span className={`storage-status ${meta.className}`}>{meta.label}</span>
                                                {!node.enabled && <span className="storage-status status-offline">Desativado</span>}
                                                <select
                                                    className="admin-users__role storage-visibility"
                                                    value={node.visibility || "PERSONAL"}
                                                    onChange={(e) => handleVisibility(node, e.target.value)}
                                                    disabled={changingNode === node.nodeID}
                                                    aria-label={`Visibilidade de ${node.name}`}
                                                >
                                                    <option value="PERSONAL">Pessoal</option>
                                                    <option value="PUBLIC">Público</option>
                                                </select>
                                            </div>
                                            <div className="storage-device__info">
                                                <div className="storage-usage storage-usage--sm" role="progressbar"
                                                    aria-label={`Uso de ${node.name}`}
                                                    aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
                                                    <div className="storage-usage__fill" style={{ width: `${pct}%` }} />
                                                </div>
                                                <span className="storage-device__numbers">
                                                    {formatBytes(node.usedBytes)} / {formatBytes(node.totalBytes)}
                                                </span>
                                                <span className="storage-device__meta">
                                                    {node.operatingSystem ? `${node.operatingSystem} · ` : ""}
                                                    {node.localPath || "pasta não indicada"} · Última ligação: {formatDate(node.lastHeartbeat)}
                                                </span>
                                            </div>
                                            <div className="files-row__actions">
                                                {isEditing ? (
                                                    <>
                                                        <input
                                                            type="number"
                                                            className="inputField storage-quota-input"
                                                            value={quotaValue}
                                                            onChange={(e) => setQuotaValue(e.target.value)}
                                                            placeholder="bytes"
                                                            aria-label={`Quota de ${node.name} em bytes`}
                                                        />
                                                        <Button label="Guardar" variant="primary" loading={savingQuotaNode === node.nodeID}
                                                            onClick={() => submitNodeQuota(node)} />
                                                        <Button label="Cancelar" variant="ghost" onClick={() => setEditingQuota(null)} />
                                                    </>
                                                ) : (
                                                    <>
                                                        <Button label="Quota" variant="ghost" onClick={() => startQuotaEdit(node)} />
                                                        <Button label="Sincronização" variant="ghost" onClick={() => openSync(node)} />
                                                        <Button label="Migrar" variant="ghost" onClick={() => runMigrate(node)} loading={changingNode === node.nodeID} />
                                                        <Button
                                                            label={node.enabled ? "Desativar" : "Ativar"}
                                                            variant="ghost"
                                                            onClick={() => toggleEnabled(node)}
                                                            loading={changingNode === node.nodeID}
                                                        />
                                                        <Button label="Remover" variant="ghost" icon={<FiTrash2 aria-hidden="true" />} onClick={() => { setRemoving(node); setMigrateOnRemove(false); }} />
                                                    </>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                </>
            )}

            {showConfig && (
                <div className="modal-backdrop" onClick={() => setShowConfig(false)}>
                    <div className="modal storage-config" role="dialog" aria-modal="true" aria-label="Configurar agente local" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Configurar agente local</h2>
                        {!registered ? (
                            <>
                                <p className="dashboard-page__text">
                                    Regista um computador como espaço de armazenamento adicional. O agente vincula-se à tua conta com um token seguro.
                                </p>
                                <form onSubmit={submitConfig}>
                                    <label htmlFor="storage-node-name">Nome do dispositivo</label>
                                    <Input id="storage-node-name" type="text" value={cfgName} onChange={(e) => setCfgName(e.target.value)} required placeholder="Ex.: PC de Casa" autoComplete="off" autoFocus />

                                    <label htmlFor="storage-node-path">Pasta do dispositivo</label>
                                    <Input id="storage-node-path" type="text" value={cfgPath} onChange={(e) => setCfgPath(e.target.value)} required placeholder="Ex.: C:\\CloudSystem ou /home/user/cloud" autoComplete="off" />

                                    <label htmlFor="storage-node-address">Endereço do agente (opcional)</label>
                                    <Input id="storage-node-address" type="text" value={cfgAddress} onChange={(e) => setCfgAddress(e.target.value)} placeholder="http://192.168.1.10:8081" autoComplete="off" />

                                    <div className="storage-config__row">
                                        <div>
                                            <label htmlFor="storage-node-space">Espaço a disponibilizar (GB)</label>
                                            <Input id="storage-node-space" type="number" min="1" step="any" value={cfgSpaceGb} onChange={(e) => setCfgSpaceGb(e.target.value)} placeholder="100" />
                                        </div>
                                        <div>
                                            <label htmlFor="storage-node-os">Sistema operativo</label>
                                            <select id="storage-node-os" className="admin-users__role" value={cfgOs} onChange={(e) => setCfgOs(e.target.value)}>
                                                {OS_OPTIONS.map((os) => (
                                                    <option key={os} value={os}>{os}</option>
                                                ))}
                                            </select>
                                        </div>
                                        <div>
                                            <label htmlFor="storage-node-visibility">Visibilidade</label>
                                            <select id="storage-node-visibility" className="admin-users__role" value={cfgVisibility} onChange={(e) => setCfgVisibility(e.target.value)}>
                                                <option value="PERSONAL">Pessoal (só para ti)</option>
                                                <option value="PUBLIC">Público (utilizadores com acesso)</option>
                                            </select>
                                        </div>
                                    </div>

                                    <div className="modal__actions">
                                        <Button label="Cancelar" variant="ghost" onClick={() => setShowConfig(false)} />
                                        <Button label="Registar dispositivo" variant="primary" type="submit" loading={savingConfig} disabled={!cfgName.trim() || !cfgPath.trim()} />
                                    </div>
                                </form>
                            </>
                        ) : (
                            <div className="storage-result">
                                <p className="dashboard-page__text"><FiCheckCircle aria-hidden="true" /> Dispositivo <strong>{registered.name}</strong> vinculado (nó #{registered.nodeID}).</p>

                                <h3 className="card__title"><FiKey aria-hidden="true" /> Token do dispositivo</h3>
                                <p className="dashboard-page__text">Guarda este token no agente local. Não voltará a ser mostrado.</p>
                                <pre className="storage-config__props">{registered.deviceToken}</pre>
                                <div className="storage-toolbar__actions">
                                    <Button label="Copiar token" variant="ghost" icon={<FiCopy aria-hidden="true" />} onClick={() => copy(registered.deviceToken)} />
                                    <Button label="Gerar código de pareamento" variant="ghost" icon={<FiKey aria-hidden="true" />} onClick={generatePairing} />
                                </div>
                                {pairingCode && (
                                    <p className="dashboard-page__text">
                                        Código de pareamento: <strong>{pairingCode.code}</strong> (válido {pairingCode.expiresInMinutes} min).
                                    </p>
                                )}

                                <h3 className="card__title"><FiSettings aria-hidden="true" /> Como ligar o teu computador</h3>
                                <p className="dashboard-page__text">
                                    Segue o código de pareamento e corre o instalador no computador que queres usar como armazenamento.
                                </p>

                                {pairingCode ? (
                                    <div className="storage-config__steps">
                                        <p><strong>1.</strong> No site, copia este código:</p>
                                        <pre className="storage-config__props">{pairingCode.code}</pre>

                                        <p><strong>2.</strong> No computador, corre um destes comandos:</p>
                                        {isWindows ? (
                                            <pre className="storage-config__props">{`install-node.bat ${pairingCode.code}`}</pre>
                                        ) : (
                                            <pre className="storage-config__props">{`bash install-node.sh ${pairingCode.code}`}</pre>
                                        )}
                                        <p className="dashboard-page__text">
                                            Não tens o instalador? Transfere-o primeiro:
                                        </p>
                                        <div className="storage-toolbar__actions">
                                            <a className="storage-config__link" href={`${apiUrl}/api/install/install-node.sh`} download>Linux / macOS (install-node.sh)</a>
                                            <a className="storage-config__link" href={`${apiUrl}/api/install/install-node.bat`} download>Windows (install-node.bat)</a>
                                        </div>
                                    </div>
                                ) : (
                                    <div className="storage-config__steps">
                                        <p>Gera um código de pareamento para começar:</p>
                                        <Button label="Gerar código de pareamento" variant="primary" icon={<FiKey aria-hidden="true" />} onClick={generatePairing} />
                                    </div>
                                )}

                                <div className="modal__actions">
                                    <Button label="Concluir" variant="primary" onClick={() => setShowConfig(false)} />
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {removing && (
                <div className="modal-backdrop" onClick={() => setRemoving(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Remover dispositivo" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Remover dispositivo</h2>
                        <p className="dashboard-page__text">
                            Vais remover <strong>{removing.name}</strong> como espaço de armazenamento.
                        </p>
                        <label className="storage-check">
                            <input type="checkbox" checked={migrateOnRemove} onChange={(e) => setMigrateOnRemove(e.target.checked)} />
                            Migrar os ficheiros para a nuvem do sistema antes de remover (requer o dispositivo online)
                        </label>
                        <div className="modal__actions">
                            <Button label="Cancelar" variant="ghost" onClick={() => setRemoving(null)} />
                            <Button label="Remover" variant="primary" loading={changingNode === removing.nodeID} onClick={confirmRemove} />
                        </div>
                    </div>
                </div>
            )}

            {syncNode && (
                <div className="modal-backdrop" onClick={() => setSyncNode(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Estado de sincronização" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Sincronização · {syncNode.name}</h2>
                        {syncLoading || !syncData ? (
                            <p className="dashboard-page__text">A carregar...</p>
                        ) : (
                            <>
                                <p className="dashboard-page__text">
                                    <FiActivity aria-hidden="true" /> Sincronizados: {syncData.synced} · Pendentes: {syncData.pending} · Erros: {syncData.failed}
                                </p>
                                {syncData.items?.length === 0 ? (
                                    <p className="dashboard-page__text">Sem ficheiros neste dispositivo.</p>
                                ) : (
                                    <ul className="files-versions">
                                        {syncData.items.map((item) => (
                                            <li key={`${item.fileID}-${item.updatedAt}`}>
                                                <span>{item.fileName || `Ficheiro #${item.fileID}`}</span>
                                                <span className="files-versions__actions">
                                                    <span className={`storage-status ${item.status === "SYNCED" ? "status-online" : item.status === "FAILED" ? "status-offline" : "status-degraded"}`}>
                                                        {item.status === "SYNCED" ? <FiCheckCircle aria-hidden="true" /> : <FiXCircle aria-hidden="true" />} {item.status}
                                                    </span>
                                                </span>
                                            </li>
                                        ))}
                                    </ul>
                                )}
                            </>
                        )}
                        <div className="modal__actions">
                            <Button label="Fechar" variant="ghost" onClick={() => setSyncNode(null)} />
                        </div>
                    </div>
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Storage;
