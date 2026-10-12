import { useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useSearchParams } from "react-router";
import {
    FiArchive,
    FiChevronRight,
    FiClock,
    FiDownload,
    FiFile,
    FiFolder,
    FiFolderPlus,
    FiGrid,
    FiHardDrive,
    FiList,
    FiMaximize,
    FiMoreVertical,
    FiMove,
    FiPlus,
    FiShare2,
    FiTrash2,
    FiUpload,
    FiX
} from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Button from "../../components/button/button.jsx";
import Input from "../../components/form/input.jsx";
import Toast from "../../components/toast/toast.jsx";
import {
    compressFile,
    copyFileToSpace,
    createFolder,
    createPublicLink,
    decompressFile,
    deleteFile,
    deleteFolder,
    downloadFile,
    listFiles,
    listFolders,
    listNodes,
    listPublicLinks,
    listSharedByMe,
    listVersions,
    moveFile,
    moveFileToSpace,
    moveFolder,
    renameFile,
    restoreVersion,
    revokePublicLink,
    revokeShare,
    setAuthToken,
    shareItem,
    updateSharePermission,
    uploadFile
} from "../../api/axios.js";
import { fileCategory, fileTone, FILE_FILTERS } from "../../utils/fileType.js";
import "./MyFiles.css";

const TOAST_DURATION = 4500;

const formatBytes = (bytes) => {
    if (bytes == null) return "—";
    if (bytes === 0) return "0 B";
    const units = ["B", "KB", "MB", "GB", "TB"];
    const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
    return `${(bytes / Math.pow(1024, i)).toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
};

const formatDate = (value) => (value ? String(value).split("T")[0].split("-").reverse().join("/") : "—");

const FILTER_LABELS = {
    todos: "Todos",
    documentos: "Documentos",
    folhas: "Folhas de cálculo",
    apresentacoes: "Apresentações",
    pdf: "PDF",
    imagens: "Imagens",
    arquivos: "Arquivos",
    codigo: "Código",
    outros: "Outros"
};

function MyFiles() {
    const { auth } = useContext(AuthContext);
    const location = useLocation();
    const [searchParams, setSearchParams] = useSearchParams();

    const [path, setPath] = useState([]);
    const [navKey, setNavKey] = useState(location.key);
    const [folders, setFolders] = useState([]);
    const [files, setFiles] = useState([]);
    const [loading, setLoading] = useState(true);
    const search = searchParams.get("q") ?? "";
    const [filter, setFilter] = useState("todos");
    const [viewMode, setViewMode] = useState("grid");
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState(null);
    const [fabOpen, setFabOpen] = useState(false);
    const [uploading, setUploading] = useState(null);
    const [dropTarget, setDropTarget] = useState(null);
    const [showNewFolder, setShowNewFolder] = useState(false);
    const [newFolderName, setNewFolderName] = useState("");
    const [renaming, setRenaming] = useState(null);
    const [renameValue, setRenameValue] = useState("");
    const [versionsFor, setVersionsFor] = useState(null);
    const [versions, setVersions] = useState([]);
    const [sharing, setSharing] = useState(null);
    const [shareEmail, setShareEmail] = useState("");
    const [sharePermission, setSharePermission] = useState("VIEW");
    const [myShares, setMyShares] = useState([]);
    const [sharingBusy, setSharingBusy] = useState(false);
    const [publicLinks, setPublicLinks] = useState([]);
    const [linkExpiry, setLinkExpiry] = useState("0");
    const [linkPassword, setLinkPassword] = useState("");
    const [linkBusy, setLinkBusy] = useState(false);
    const [nodes, setNodes] = useState([]);
    const [uploadDestination, setUploadDestination] = useState("AUTO");
    const [uploadNodeId, setUploadNodeId] = useState(null);
    const [uploadCompression, setUploadCompression] = useState("AUTO");

    const [spaceFile, setSpaceFile] = useState(null);
    const [spaceAction, setSpaceAction] = useState("move");
    const [spaceDest, setSpaceDest] = useState("SYSTEM");
    const [spaceNodeId, setSpaceNodeId] = useState(null);
    const [spaceBusy, setSpaceBusy] = useState(false);

    // Context menu (botão direito)
    const [contextMenu, setContextMenu] = useState({ visible: false, x: 0, y: 0, file: null });

    useEffect(() => {
        const handleClick = () => setContextMenu((prev) => ({ ...prev, visible: false }));
        document.addEventListener("click", handleClick);
        return () => document.removeEventListener("click", handleClick);
    }, []);
    const fileInputRef = useRef(null);
    const folderInputRef = useRef(null);
    const dragItem = useRef(null);

    const currentFolderId = path.length ? path[path.length - 1].id : null;
    const closeToast = useCallback(() => setToast(null), []);

    if (navKey !== location.key) {
        setNavKey(location.key);
        setPath([]);
    }

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    const load = useCallback(async (folderId, searchTerm) => {
        setLoading(true);
        try {
            if (searchTerm) {
                const res = await listFiles({ search: searchTerm });
                setFolders([]);
                setFiles(Array.isArray(res.data) ? res.data : []);
            } else {
                const [foldersRes, filesRes] = await Promise.all([
                    listFolders(folderId),
                    listFiles({ folderId })
                ]);
                setFolders(Array.isArray(foldersRes.data) ? foldersRes.data : []);
                setFiles(Array.isArray(filesRes.data) ? filesRes.data : []);
            }
        } catch (err) {
            console.error("[files] falha ao carregar", err);
            setToast({ message: "Não foi possível carregar os ficheiros.", type: "error" });
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        if (auth?.token) void load(currentFolderId, search.trim() || null);
    }, [auth?.token, currentFolderId, search, load]);

    useEffect(() => {
        if (!auth?.token) return;
        let active = true;
        void (async () => {
            try {
                const res = await listNodes();
                if (active) setNodes(Array.isArray(res.data) ? res.data : []);
            } catch (err) {
                console.error("[files] falha ao carregar dispositivos", err);
            }
        })();
        return () => { active = false; };
    }, [auth?.token]);

    const openFolder = (folder) => setPath((prev) => [...prev, { id: folder.folderID, name: folder.name }]);
    const goToBreadcrumb = (index) => setPath((prev) => (index < 0 ? [] : prev.slice(0, index + 1)));

    const ensureFolder = useCallback(async (name, parentId) => {
        const res = await listFolders(parentId);
        const existing = res.data.find((f) => f.name.toLowerCase() === name.toLowerCase());
        if (existing) return existing.folderID;
        try {
            const created = await createFolder({ name, parentId });
            return created.data.folderID;
        } catch {
            const retry = await listFolders(parentId);
            const dup = retry.data.find((f) => f.name.toLowerCase() === name.toLowerCase());
            return dup ? dup.folderID : null;
        }
    }, []);

    const uploadOne = useCallback(async (file, folderId) => {
        const options = {
            destination: uploadDestination === "SYSTEM" || uploadDestination === "DEVICE" ? uploadDestination : undefined,
            nodeId: uploadDestination === "DEVICE" && uploadNodeId != null ? uploadNodeId : undefined,
            compression: uploadCompression && uploadCompression !== "AUTO" ? uploadCompression : undefined
        };
        await uploadFile(file, folderId, undefined, options);
    }, [uploadDestination, uploadNodeId, uploadCompression]);

    const runUploads = useCallback(async (fileList, folderId) => {
        const items = Array.from(fileList).filter((f) => f.size > 0 || f.name.trim());
        if (items.length === 0) return;
        setUploading({ done: 0, total: items.length, name: items[0].name });
        const failed = [];
        for (let i = 0; i < items.length; i++) {
            try {
                await uploadOne(items[i], folderId);
            } catch {
                failed.push(items[i].name);
            }
            setUploading({ done: i + 1, total: items.length, name: failed.length ? failed[0] : items[i].name });
        }
        setUploading(null);
        if (failed.length === items.length) {
            setToast({ message: "Não foi possível carregar os ficheiros.", type: "error" });
        } else if (failed.length > 0) {
            setToast({ message: `${items.length - failed.length} carregados; ${failed.length} falharam.`, type: "error" });
        } else {
            setToast({ message: `${items.length} ficheiro${items.length === 1 ? "" : "s"} carregados.`, type: "success" });
        }
        void load(currentFolderId, null);
    }, [currentFolderId, load, uploadOne]);

    const uploadFiles = useCallback(async (fileList, folderId) => {
        if (!fileList || fileList.length === 0) return;
        await runUploads(fileList, folderId);
    }, [runUploads]);

    const uploadFolderFiles = useCallback(async (fileList, folderId) => {
        const files = Array.from(fileList).filter((f) => f.name).sort((a, b) => a.webkitRelativePath.length - b.webkitRelativePath.length);
        if (files.length === 0) return;
        setUploading({ done: 0, total: files.length, name: files[0].name });
        const dirCache = new Map();
        dirCache.set("", folderId);
        const failed = [];
        for (let i = 0; i < files.length; i++) {
            const f = files[i];
            try {
                const parts = (f.webkitRelativePath || f.name).split("/");
                const dirs = parts.slice(0, -1);
                let parentPath = "";
                let parentId = folderId;
                for (const dirName of dirs) {
                    const key = parentPath ? `${parentPath}/${dirName}` : dirName;
                    if (!dirCache.has(key)) {
                        const id = await ensureFolder(dirName, parentId);
                        dirCache.set(key, id);
                    }
                    parentPath = key;
                    parentId = dirCache.get(key);
                }
                if (parentId != null) await uploadOne(f, parentId);
                else failed.push(f.name);
            } catch {
                failed.push(f.name);
            }
            setUploading({ done: i + 1, total: files.length, name: failed.length ? failed[0] : f.name });
        }
        setUploading(null);
        if (failed.length === files.length) {
            setToast({ message: "Não foi possível carregar a pasta.", type: "error" });
        } else if (failed.length > 0) {
            setToast({ message: `Pasta carregada, ${files.length - failed.length} de ${files.length}.`, type: "error" });
        } else {
            setToast({ message: `Pasta carregada (${files.length} ficheiro${files.length === 1 ? "" : "s"}).`, type: "success" });
        }
        void load(currentFolderId, null);
    }, [currentFolderId, ensureFolder, load, uploadOne]);

    const handleFileInput = (e) => {
        const list = e.target.files;
        e.target.value = "";
        if (list && list.length) {
            setFabOpen(false);
            void uploadFiles(list, currentFolderId);
        }
    };

    const handleFolderInput = (e) => {
        const list = e.target.files;
        e.target.value = "";
        if (list && list.length) {
            setFabOpen(false);
            void uploadFolderFiles(list, currentFolderId);
        }
    };

    const handleCreateFolder = async (e) => {
        e.preventDefault();
        if (!newFolderName.trim()) return;
        setBusy(true);
        try {
            await createFolder({ name: newFolderName.trim(), parentId: currentFolderId });
            setShowNewFolder(false);
            setNewFolderName("");
            setFabOpen(false);
            setToast({ message: "Pasta criada.", type: "success" });
            void load(currentFolderId, null);
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível criar a pasta.", type: "error" });
        } finally {
            setBusy(false);
        }
    };

    const handleDownload = async (file, version) => {
        try {
            const res = await downloadFile(file.fileID, version);
            const url = URL.createObjectURL(res.data);
            const a = document.createElement("a");
            a.href = url;
            a.download = file.originalName;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
        } catch (err) {
            console.error("[files] falha no download", err);
            setToast({ message: "Não foi possível descarregar o ficheiro.", type: "error" });
        }
    };

    const openVersions = async (file) => {
        setVersionsFor(file);
        setVersions([]);
        try {
            const res = await listVersions(file.fileID);
            setVersions(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("[files] falha ao listar versões", err);
            setToast({ message: "Não foi possível listar as versões.", type: "error" });
        }
    };

    const handleRestoreVersion = async (versionNumber) => {
        try {
            await restoreVersion(versionsFor.fileID, versionNumber);
            setVersionsFor(null);
            setToast({ message: "Versão restaurada.", type: "success" });
            void load(currentFolderId, search.trim() || null);
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível restaurar a versão.", type: "error" });
        }
    };

    const submitRename = async (e) => {
        e.preventDefault();
        if (!renaming || !renameValue.trim()) return;
        try {
            await renameFile(renaming.fileID, renameValue.trim());
            setRenaming(null);
            setToast({ message: "Ficheiro renomeado.", type: "success" });
            void load(currentFolderId, search.trim() || null);
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível renomear.", type: "error" });
        }
    };

    const handleDeleteFile = async (file) => {
        try {
            await deleteFile(file.fileID);
            setFiles((prev) => prev.filter((f) => f.fileID !== file.fileID));
            setToast({ message: `"${file.originalName}" movido para a lixeira.`, type: "success" });
        } catch (err) {
            console.error("[files] falha ao excluir", err);
            setToast({ message: "Não foi possível excluir.", type: "error" });
        }
    };

    const handleCompressFile = async (file) => {
        try {
            const res = await compressFile(file.fileID);
            setFiles((prev) => prev.map((f) => f.fileID === file.fileID ? res.data : f));
            setToast({ message: `"${file.originalName}" comprimido.`, type: "success" });
        } catch (err) {
            console.error("[files] falha ao comprimir", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível comprimir.", type: "error" });
        }
    };

    const handleDecompressFile = async (file) => {
        try {
            const res = await decompressFile(file.fileID);
            setFiles((prev) => prev.map((f) => f.fileID === file.fileID ? res.data : f));
            setToast({ message: `"${file.originalName}" descomprimido.`, type: "success" });
        } catch (err) {
            console.error("[files] falha ao descomprimir", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível descomprimir.", type: "error" });
        }
    };

    const handleDeleteFolder = async (folder) => {
        try {
            await deleteFolder(folder.folderID);
            setFolders((prev) => prev.filter((f) => f.folderID !== folder.folderID));
            setToast({ message: `Pasta "${folder.name}" movida para a lixeira.`, type: "success" });
        } catch (err) {
            console.error("[files] falha ao excluir a pasta", err);
            setToast({ message: "Não foi possível excluir a pasta.", type: "error" });
        }
    };

    const loadShares = useCallback(async () => {
        try {
            const res = await listSharedByMe();
            setMyShares(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("[files] falha ao carregar partilhas", err);
        }
    }, []);

    const loadLinks = useCallback(async () => {
        try {
            const res = await listPublicLinks();
            setPublicLinks(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            console.error("[files] falha ao carregar links públicos", err);
        }
    }, []);

    const openShare = async (kind, id, name) => {
        setSharing({ kind, id, name });
        setShareEmail("");
        setSharePermission("VIEW");
        setLinkExpiry("0");
        setLinkPassword("");
        await Promise.all([loadShares(), loadLinks()]);
    };

    const itemShares = (sharing) => myShares.filter(
        (s) => s.type === (sharing?.kind === "file" ? "FILE" : "FOLDER") && s.itemID === sharing?.id
    );

    const itemLinks = (sharing) => publicLinks.filter(
        (l) => l.type === (sharing?.kind === "file" ? "FILE" : "FOLDER") && l.itemID === sharing?.id
    );

    const linkState = (link) => {
        if (link.revoked) return "Revogado";
        if (link.expired) return "Expirado";
        return "Ativo";
    };

    const submitPublicLink = async () => {
        if (!sharing) return;
        setLinkBusy(true);
        try {
            const payload = {
                expiresInHours: linkExpiry === "0" ? null : Number(linkExpiry),
                password: linkPassword.trim() || null
            };
            if (sharing.kind === "file") payload.fileId = sharing.id;
            else payload.folderId = sharing.id;
            await createPublicLink(payload);
            setLinkPassword("");
            setToast({ message: `Link público de "${sharing.name}" criado.`, type: "success" });
            await loadLinks();
        } catch (err) {
            console.error("[files] falha ao criar link público", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível criar o link.", type: "error" });
        } finally {
            setLinkBusy(false);
        }
    };

    const copyPublicLink = async (link) => {
        const url = `${window.location.origin}/s/${link.token}`;
        try {
            await navigator.clipboard.writeText(url);
            setToast({ message: "Link copiado para a área de transferência.", type: "success" });
        } catch {
            window.prompt("Copia o link:", url);
        }
    };

    const handleRevokePublicLink = async (link) => {
        try {
            await revokePublicLink(link.linkID);
            setPublicLinks((prev) => prev.map((l) => (l.linkID === link.linkID ? { ...l, revoked: true } : l)));
            setToast({ message: "Link público revogado.", type: "success" });
        } catch (err) {
            console.error("[files] falha ao revogar link", err);
            setToast({ message: "Não foi possível revogar o link.", type: "error" });
        }
    };

    const submitShare = async (e) => {
        e.preventDefault();
        if (!shareEmail.trim() || !sharing) return;
        setSharingBusy(true);
        try {
            const payload = { email: shareEmail.trim(), permission: sharePermission };
            if (sharing.kind === "file") payload.fileId = sharing.id;
            else payload.folderId = sharing.id;
            await shareItem(payload);
            setShareEmail("");
            setToast({ message: `"${sharing.name}" partilhado com ${shareEmail.trim()}.`, type: "success" });
            await loadShares();
        } catch (err) {
            console.error("[files] falha ao partilhar", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível partilhar.", type: "error" });
        } finally {
            setSharingBusy(false);
        }
    };

    const handleSharePermission = async (share, permission) => {
        try {
            const res = await updateSharePermission(share.shareID, { permission });
            setMyShares((prev) => prev.map((s) => (s.shareID === share.shareID ? res.data : s)));
            setToast({ message: "Permissão atualizada.", type: "success" });
        } catch (err) {
            console.error("[files] falha ao atualizar permissão", err);
            setToast({ message: "Não foi possível atualizar a permissão.", type: "error" });
        }
    };

    const handleRevokeShare = async (share) => {
        try {
            await revokeShare(share.shareID);
            setMyShares((prev) => prev.filter((s) => s.shareID !== share.shareID));
            setToast({ message: "Partilha revogada.", type: "success" });
        } catch (err) {
            console.error("[files] falha ao revogar", err);
            setToast({ message: "Não foi possível revogar a partilha.", type: "error" });
        }
    };

    const searching = search.trim().length > 0;
    const visibleFiles = useMemo(() => {
        if (filter === "todos") return files;
        return files.filter((f) => fileCategory(f.originalName) === filter);
    }, [files, filter]);
    const isEmpty = !loading && folders.length === 0 && visibleFiles.length === 0;

    const breadcrumbs = useMemo(() => ([
        { id: null, name: "Início" },
        ...path
    ]), [path]);

    const clearSearch = () => {
        const next = new URLSearchParams(searchParams);
        next.delete("q");
        setSearchParams(next, { replace: true });
    };

    const dragProps = (item) => ({
        draggable: !searching,
        onDragStart: (e) => {
            dragItem.current = item;
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", item.name);
        },
        onDragEnd: () => {
            dragItem.current = null;
            setDropTarget(null);
        }
    });

    const handleMove = async (targetFolder) => {
        const item = dragItem.current;
        if (!item || item.kind === "folder" && item.id === targetFolder.folderID) return;
        const label = item.kind === "folder" ? item.name : (files.find((f) => f.fileID === item.id)?.originalName || item.name);
        try {
            if (item.kind === "folder") await moveFolder(item.id, targetFolder.folderID);
            else await moveFile(item.id, targetFolder.folderID);
            setToast({ message: `"${label}" movido para "${targetFolder.name}".`, type: "success" });
            void load(currentFolderId, null);
        } catch (err) {
            console.error("[files] falha ao mover", err);
            setToast({ message: err?.response?.data?.message || "Não foi possível mover.", type: "error" });
        } finally {
            dragItem.current = null;
            setDropTarget(null);
        }
    };

    const handleContainerDrop = (e) => {
        e.preventDefault();
        setDropTarget(null);
        if (e.dataTransfer.types?.includes("Files")) {
            const files = Array.from(e.dataTransfer.files);
            setFabOpen(false);
            if (files.some((f) => f.webkitRelativePath)) void uploadFolderFiles(files, currentFolderId);
            else void uploadFiles(files, currentFolderId);
        }
        dragItem.current = null;
    };

    const openSpace = (file) => {
        setSpaceFile(file);
        setSpaceAction("move");
        setSpaceDest(file.space === "DEVICE" ? "SYSTEM" : "DEVICE");
        setSpaceNodeId(nodes[0]?.nodeID ?? null);
    };

    const submitSpace = async (e) => {
        e.preventDefault();
        if (!spaceFile) return;
        const nodeId = spaceDest === "DEVICE" ? (spaceNodeId ?? null) : null;
        setSpaceBusy(true);
        try {
            if (spaceAction === "copy") await copyFileToSpace(spaceFile.fileID, spaceDest, nodeId);
            else await moveFileToSpace(spaceFile.fileID, spaceDest, nodeId);
            setToast({ message: `"${spaceFile.originalName}" ${spaceAction === "copy" ? "copiado" : "movido"} para ${spaceDest === "SYSTEM" ? "a nuvem do sistema" : "o dispositivo"}.`, type: "success" });
            setSpaceFile(null);
            void load(currentFolderId, null);
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível concluir a operação.", type: "error" });
        } finally {
            setSpaceBusy(false);
        }
    };

    const renderFolderActions = (folder) => (
        <div className="drive-card__actions">
            <button type="button" title="Partilhar pasta" aria-label={`Partilhar pasta ${folder.name}`} onClick={() => openShare("folder", folder.folderID, folder.name)}>
                <FiShare2 aria-hidden="true" />
            </button>
            <button type="button" title="Excluir pasta" aria-label={`Excluir pasta ${folder.name}`} onClick={() => handleDeleteFolder(folder)}>
                <FiTrash2 aria-hidden="true" />
            </button>
        </div>
    );

    const handleContextMenu = (e, file) => {
        e.preventDefault();
        setContextMenu({ visible: true, x: e.clientX, y: e.clientY, file });
    };

    const handleContextMenuAction = async (action, file) => {
        setContextMenu({ ...contextMenu, visible: false });
        if (action === "compress") await handleCompressFile(file);
        if (action === "decompress") await handleDecompressFile(file);
    };

    const renderFileActions = (file) => {
        const isCompressed = file.compressionAlgorithm && file.compressionAlgorithm !== "NONE";
        return (
            <div className="drive-card__actions">
                <button type="button" title="Descarregar" aria-label={`Descarregar ${file.originalName}`} onClick={() => handleDownload(file)}>
                    <FiDownload aria-hidden="true" />
                </button>
                <button type="button" title="Versões" aria-label={`Versões de ${file.originalName}`} onClick={() => openVersions(file)}>
                    <FiClock aria-hidden="true" />
                </button>
                <button type="button" title="Mover/copiar entre espaços" aria-label={`Mover ou copiar ${file.originalName} entre espaços`} onClick={() => openSpace(file)}>
                    <FiMove aria-hidden="true" />
                </button>
                <button type="button" title="Renomear" aria-label={`Renomear ${file.originalName}`} onClick={() => { setRenaming(file); setRenameValue(file.originalName); }}>
                    <FiMoreVertical aria-hidden="true" />
                </button>
                {!isCompressed && (
                    <button
                        type="button"
                        title="Comprimir"
                        aria-label={`Comprimir ${file.originalName}`}
                        onClick={() => handleCompressFile(file)}
                    >
                        <FiArchive aria-hidden="true" />
                    </button>
                )}
                {isCompressed && (
                    <button
                        type="button"
                        title="Descomprimir"
                        aria-label={`Descomprimir ${file.originalName}`}
                        onClick={() => handleDecompressFile(file)}
                    >
                        <FiMaximize aria-hidden="true" />
                    </button>
                )}
                <button type="button" title="Partilhar" aria-label={`Partilhar ${file.originalName}`} onClick={() => openShare("file", file.fileID, file.originalName)}>
                    <FiShare2 aria-hidden="true" />
                </button>
                <button type="button" title="Excluir" aria-label={`Excluir ${file.originalName}`} onClick={() => handleDeleteFile(file)}>
                    <FiTrash2 aria-hidden="true" />
                </button>
            </div>
        );
    };

    return (
        <section className="dashboard-page files-page">
            <div className="drive-toolbar">
                <h1 className="drive-title">{searching ? "Pesquisa" : "Meus Arquivos"}</h1>

                <div className="drive-toolbar__right">
                    <div className="drive-destination" role="group" aria-label="Destino do upload">
                        <FiHardDrive aria-hidden="true" />
                        <label className="sr-only" htmlFor="upload-destination">Destino do upload</label>
                        <select
                            id="upload-destination"
                            className="admin-users__role drive-destination__select"
                            value={uploadDestination}
                            onChange={(e) => {
                                const value = e.target.value;
                                setUploadDestination(value);
                                if (value === "DEVICE" && uploadNodeId == null) {
                                    setUploadNodeId(nodes[0]?.nodeID ?? null);
                                }
                            }}
                        >
                            <option value="AUTO">Automático (política)</option>
                            <option value="SYSTEM">Nuvem do sistema</option>
                            <option value="DEVICE">Dispositivo pessoal</option>
                        </select>
                        {uploadDestination === "DEVICE" && (
                            <>
                                <label className="sr-only" htmlFor="upload-node">Dispositivo</label>
                                <select
                                    id="upload-node"
                                    className="admin-users__role drive-destination__select"
                                    value={uploadNodeId ?? ""}
                                    onChange={(e) => setUploadNodeId(e.target.value ? Number(e.target.value) : null)}
                                    disabled={nodes.length === 0}
                                >
                                    {nodes.length === 0 ? (
                                        <option value="">Sem dispositivos</option>
                                    ) : (
                                        nodes.map((node) => (
                                            <option key={node.nodeID} value={node.nodeID}>
                                                {node.name}{node.online ? "" : " (offline)"}
                                            </option>
                                        ))
                                    )}
                                </select>
                            </>
                        )}
                    </div>
                    <div className="drive-compression" role="group" aria-label="Compressão do upload">
                        <FiUpload aria-hidden="true" />
                        <label className="sr-only" htmlFor="upload-compression">Compressão</label>
                        <select
                            id="upload-compression"
                            className="admin-users__role drive-destination__select"
                            value={uploadCompression}
                            onChange={(e) => setUploadCompression(e.target.value)}
                        >
                            <option value="AUTO">Automático (se ganho ≥ 5%)</option>
                            <option value="NONE">Sem compressão</option>
                            <option value="DEFLATE">DEFLATE</option>
                            <option value="GZIP">GZIP</option>
                        </select>
                    </div>

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

            <nav className="files-breadcrumb" aria-label="Caminho">
                {!searching && breadcrumbs.map((crumb, index) => (
                    <span key={crumb.id ?? "root"} className="files-breadcrumb__item">
                        {index > 0 && <FiChevronRight aria-hidden="true" />}
                        <button type="button" onClick={() => goToBreadcrumb(index - 1)} disabled={index === breadcrumbs.length - 1}>
                            {crumb.name}
                        </button>
                    </span>
                ))}
                {searching && <span className="files-breadcrumb__item">Resultados da pesquisa</span>}
            </nav>

            <div className="drive-filters" role="group" aria-label="Filtros">
                <span className="drive-filters__label">Tipo</span>
                {FILE_FILTERS.map((key) => (
                    <button
                        key={key}
                        type="button"
                        className={`drive-chip ${filter === key ? "on" : ""}`}
                        aria-pressed={filter === key}
                        onClick={() => setFilter(key === "todos" ? "todos" : (filter === key ? "todos" : key))}
                    >
                        {FILTER_LABELS[key]}
                    </button>
                ))}
            </div>

            <div
                className="drive-content"
                onDragOver={(e) => { if (e.dataTransfer.types?.includes("Files")) e.preventDefault(); }}
                onDrop={handleContainerDrop}
            >
                {loading ? (
                    <p className="dashboard-page__text">A carregar...</p>
                ) : isEmpty ? (
                    <p className="dashboard-page__text">
                        {searching ? "Nenhum ficheiro corresponde à pesquisa." : (filter !== "todos" ? "Esta pasta não tem ficheiros deste tipo." : "Esta pasta está vazia.")}
                        {searching && (
                            <button type="button" className="drive-clear-search" onClick={clearSearch}>
                                Limpar pesquisa <FiX aria-hidden="true" />
                            </button>
                        )}
                    </p>
                ) : viewMode === "grid" ? (
                    <>
                        {folders.length > 0 && (
                            <>
                                <h2 className="drive-section__title">Pastas</h2>
                                <div className="drive-folders">
                                    {folders.map((folder) => (
                                        <div
                                            key={`folder-${folder.folderID}`}
                                            className={`drive-folder ${dropTarget === folder.folderID ? "drive-drop" : ""}`}
                                            {...dragProps({ kind: "folder", id: folder.folderID, name: folder.name })}
                                            onDragOver={(e) => { e.preventDefault(); if (dragItem.current) setDropTarget(folder.folderID); }}
                                            onDragLeave={() => setDropTarget((prev) => (prev === folder.folderID ? null : prev))}
                                            onDrop={(e) => { e.preventDefault(); e.stopPropagation(); if (dragItem.current) { void handleMove(folder); } }}
                                        >
                                            <button type="button" className="drive-card__main" onClick={() => openFolder(folder)}>
                                                <span className="drive-icon drive-icon--folder"><FiFolder aria-hidden="true" /></span>
                                                <span className="drive-folder__name" title={folder.name}>{folder.name}</span>
                                            </button>
                                            <span className="drive-card__meta">Pasta · {formatDate(folder.updatedAt)}</span>
                                            {renderFolderActions(folder)}
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}

                        {visibleFiles.length > 0 && (
                            <>
                                <h2 className="drive-section__title">Arquivos</h2>
                                <div className="drive-files">
                                    {visibleFiles.map((file) => (
                                        <div
                                            key={`file-${file.fileID}`}
                                            className="drive-file"
                                            {...dragProps({ kind: "file", id: file.fileID, name: file.originalName })}
                                            onContextMenu={(e) => handleContextMenu(e, file)}
                                        >
                                            <span className={`drive-file__thumb drive-icon--${fileTone(file.originalName)}`}>
                                                <FiFile aria-hidden="true" />
                                            </span>
                                            <div className="drive-file__body">
                                                <span className="drive-file__name" title={file.originalName} data-compressed={file.compressionAlgorithm && file.compressionAlgorithm !== "NONE" ? "true" : "false"}>{file.originalName}</span>
                                                <span className="drive-card__meta">
                                                    {formatBytes(file.originalSizeBytes)} · {formatDate(file.uploadedAt)}
                                                </span>
                                            </div>
                                            {renderFileActions(file)}
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}
                    </>
                ) : (
                    <>
                        {(folders.length > 0 || visibleFiles.length > 0) && (
                            <table className="drive-table">
                                <thead>
                                    <tr>
                                        <th>Nome</th>
                                        <th>Tamanho</th>
                                        <th>Modificado</th>
                                        <th className="drive-table__actions-col">Ações</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {folders.map((folder) => (
                                        <tr
                                            key={`folder-${folder.folderID}`}
                                            className={dropTarget === folder.folderID ? "drive-drop-row" : ""}
                                            {...dragProps({ kind: "folder", id: folder.folderID, name: folder.name })}
                                            onDragOver={(e) => { e.preventDefault(); if (dragItem.current) setDropTarget(folder.folderID); }}
                                            onDragLeave={() => setDropTarget((prev) => (prev === folder.folderID ? null : prev))}
                                            onDrop={(e) => { e.preventDefault(); e.stopPropagation(); if (dragItem.current) { void handleMove(folder); } }}
                                        >
                                            <td>
                                                <button type="button" className="drive-table__name" onClick={() => openFolder(folder)}>
                                                    <span className="drive-icon drive-icon--folder"><FiFolder aria-hidden="true" /></span>
                                                    {folder.name}
                                                </button>
                                            </td>
                                            <td>—</td>
                                            <td>{formatDate(folder.updatedAt)}</td>
                                            <td className="drive-table__actions-col">{renderFolderActions(folder)}</td>
                                        </tr>
                                    ))}
                                    {visibleFiles.map((file) => (
                                        <tr
                                            key={`file-${file.fileID}`}
                                            {...dragProps({ kind: "file", id: file.fileID, name: file.originalName })}
                                            onContextMenu={(e) => handleContextMenu(e, file)}
                                        >
                                            <td>
                                                <span className="drive-table__name" data-compressed={file.compressionAlgorithm && file.compressionAlgorithm !== "NONE" ? "true" : "false"}>
                                                    <span className={`drive-icon drive-icon--${fileTone(file.originalName)}`}><FiFile aria-hidden="true" /></span>
                                                    {file.originalName}
                                                </span>
                                            </td>
                                            <td>{formatBytes(file.originalSizeBytes)}</td>
                                            <td>{formatDate(file.uploadedAt)}</td>
                                            <td className="drive-table__actions-col">{renderFileActions(file)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </>
                )}
            </div>

            <input ref={fileInputRef} type="file" multiple hidden onChange={handleFileInput} />
            <input ref={folderInputRef} type="file" hidden webkitdirectory mozdirectory directory onChange={handleFolderInput} />

            {uploading && (
                <div className="fab-upload" role="status" aria-live="polite">
                    <FiUpload aria-hidden="true" /> {uploading.name}
                    <span className="fab-upload__bar">
                        <span style={{ width: `${Math.round((uploading.done * 100) / uploading.total)}%` }} />
                    </span>
                    <span className="fab-upload__count">{uploading.done}/{uploading.total}</span>
                </div>
            )}

            <div className="fab">
                <button
                    type="button"
                    className="fab__main"
                    aria-label="Novo"
                    aria-expanded={fabOpen}
                    onClick={() => setFabOpen((open) => !open)}
                >
                    <FiPlus aria-hidden="true" />
                </button>
                {fabOpen && (
                    <>
                        <div className="fab-menu__backdrop" onClick={() => setFabOpen(false)} />
                        <div className="fab__menu" role="menu" aria-label="Criar ou carregar">
                            <button type="button" role="menuitem" onClick={() => fileInputRef.current?.click()}>
                                <FiUpload aria-hidden="true" /> Carregar ficheiros
                            </button>
                            <button type="button" role="menuitem" onClick={() => folderInputRef.current?.click()}>
                                <FiFolderPlus aria-hidden="true" /> Carregar pasta
                            </button>
                            <button type="button" role="menuitem" onClick={() => setShowNewFolder(true)}>
                                <FiFolder aria-hidden="true" /> Nova pasta
                            </button>
                        </div>
                    </>
                )}
            </div>

            {showNewFolder && (
                <div className="modal-backdrop" onClick={() => setShowNewFolder(false)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Nova pasta" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Nova pasta</h2>
                        <form onSubmit={handleCreateFolder}>
                            <label htmlFor="new-folder">Nome</label>
                            <Input id="new-folder" type="text" value={newFolderName} onChange={(e) => setNewFolderName(e.target.value)} required autoFocus />
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setShowNewFolder(false)} />
                                <Button label="Criar" variant="primary" type="submit" loading={busy} disabled={!newFolderName.trim()} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {renaming && (
                <div className="modal-backdrop" onClick={() => setRenaming(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Renomear ficheiro" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Renomear</h2>
                        <form onSubmit={submitRename}>
                            <label htmlFor="rename-file">Nome</label>
                            <Input id="rename-file" type="text" value={renameValue} onChange={(e) => setRenameValue(e.target.value)} required autoFocus />
                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setRenaming(null)} />
                                <Button label="Guardar" variant="primary" type="submit" disabled={!renameValue.trim()} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {versionsFor && (
                <div className="modal-backdrop" onClick={() => setVersionsFor(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label="Versões do ficheiro" onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Versões de {versionsFor.originalName}</h2>
                        <p className="dashboard-page__text">Versão atual: v{versionsFor.containerVersion ?? 1}</p>
                        {versions.length === 0 ? (
                            <p className="dashboard-page__text">Ainda não existem versões anteriores.</p>
                        ) : (
                            <ul className="files-versions">
                                {versions.map((v) => (
                                    <li key={v.versionID}>
                                        <span>v{v.versionNumber} · {formatBytes(v.originalSizeBytes)} · {formatDate(v.createdAt)}</span>
                                        <span className="files-versions__actions">
                                            <button type="button" onClick={() => handleDownload(versionsFor, v.versionNumber)}>Descarregar</button>
                                            <button type="button" onClick={() => handleRestoreVersion(v.versionNumber)}>Restaurar</button>
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        )}
                        <div className="modal__actions">
                            <Button label="Fechar" variant="ghost" onClick={() => setVersionsFor(null)} />
                        </div>
                    </div>
                </div>
            )}

            {sharing && (
                <div className="modal-backdrop" onClick={() => setSharing(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label={`Partilhar ${sharing.name}`} onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Partilhar · {sharing.name}</h2>
                        {(() => {
                            const shares = itemShares(sharing);
                            const links = itemLinks(sharing);
                            return (
                                <>
                                    <form onSubmit={submitShare}>
                                        <label htmlFor="share-email">Email do utilizador</label>
                                        <Input id="share-email" type="email" value={shareEmail} onChange={(e) => setShareEmail(e.target.value)} required placeholder="nome@exemplo.com" autoComplete="off" />
                                        <label htmlFor="share-permission">Permissão</label>
                                        <select id="share-permission" className="admin-users__role" value={sharePermission} onChange={(e) => setSharePermission(e.target.value)}>
                                            <option value="VIEW">Só leitura</option>
                                            <option value="EDIT">Edição</option>
                                        </select>
                                        <div className="modal__actions">
                                            <Button label="Cancelar" variant="ghost" onClick={() => setSharing(null)} />
                                            <Button label="Partilhar" variant="primary" type="submit" loading={sharingBusy} disabled={!shareEmail.trim()} />
                                        </div>
                                    </form>
                                    {shares.length > 0 && (
                                        <div>
                                            <h3 className="dashboard-page__text">Partilhado com</h3>
                                            <ul className="files-versions">
                                                {shares.map((s) => (
                                                    <li key={s.shareID}>
                                                        <span>{s.recipientEmail} · {s.permission === "EDIT" ? "Edição" : "Só leitura"}</span>
                                                        <span className="files-versions__actions">
                                                            <select
                                                                className="admin-users__role"
                                                                value={s.permission}
                                                                onChange={(e) => handleSharePermission(s, e.target.value)}
                                                                aria-label={`Permissão de ${s.recipientEmail}`}
                                                            >
                                                                <option value="VIEW">Só leitura</option>
                                                                <option value="EDIT">Edição</option>
                                                            </select>
                                                            <button type="button" onClick={() => handleRevokeShare(s)}>Revogar</button>
                                                        </span>
                                                    </li>
                                                ))}
                                            </ul>
                                        </div>
                                    )}
                                    <div className="share-links">
                                        <h3 className="dashboard-page__text">Link público</h3>
                                        <p className="dashboard-page__text">Qualquer pessoa com o link pode descarregar este item (só leitura).</p>
                                        <div className="share-links__form">
                                            <label htmlFor="link-expiry">Expiração</label>
                                            <select id="link-expiry" className="admin-users__role" value={linkExpiry} onChange={(e) => setLinkExpiry(e.target.value)}>
                                                <option value="0">Sem expiração</option>
                                                <option value="24">24 horas</option>
                                                <option value="168">7 dias</option>
                                                <option value="720">30 dias</option>
                                            </select>
                                            <label htmlFor="link-password">Senha (opcional)</label>
                                            <Input id="link-password" type="text" value={linkPassword} onChange={(e) => setLinkPassword(e.target.value)} placeholder="Sem senha" autoComplete="off" />
                                            <div className="modal__actions">
                                                <Button label="Gerar link" variant="primary" type="button" loading={linkBusy} onClick={submitPublicLink} />
                                            </div>
                                        </div>
                                        {links.length > 0 && (
                                            <ul className="files-versions">
                                                {links.map((l) => (
                                                    <li key={l.linkID}>
                                                        <span>
                                                            {linkState(l)}
                                                            {l.passwordProtected ? " · com senha" : ""}
                                                            {l.expiresAt ? ` · expira ${String(l.expiresAt).split("T")[0]}` : " · sem expiração"}
                                                            {` · ${l.accessCount ?? 0} acesso(s)`}
                                                        </span>
                                                        <span className="files-versions__actions">
                                                            {linkState(l) === "Ativo" && (
                                                                <button type="button" onClick={() => copyPublicLink(l)}>Copiar</button>
                                                            )}
                                                            {!l.revoked && (
                                                                <button type="button" onClick={() => handleRevokePublicLink(l)}>Revogar</button>
                                                            )}
                                                        </span>
                                                    </li>
                                                ))}
                                            </ul>
                                        )}
                                    </div>
                                </>
                            );
                        })()}
                    </div>
                </div>
            )}

            {spaceFile && (
                <div className="modal-backdrop" onClick={() => setSpaceFile(null)}>
                    <div className="modal" role="dialog" aria-modal="true" aria-label={`Mover ou copiar ${spaceFile.originalName}`} onClick={(e) => e.stopPropagation()}>
                        <h2 className="card__title">Espaços · {spaceFile.originalName}</h2>
                        <p className="dashboard-page__text">
                            Espaço atual: {spaceFile.space === "DEVICE" ? "dispositivo pessoal" : spaceFile.space === "SYSTEM" ? "nuvem do sistema" : "desconhecido"} (RF21).
                        </p>
                        <form onSubmit={submitSpace}>
                            <label htmlFor="space-action">Operação</label>
                            <select id="space-action" className="admin-users__role" value={spaceAction} onChange={(e) => setSpaceAction(e.target.value)}>
                                <option value="move">Mover</option>
                                <option value="copy">Copiar</option>
                            </select>

                            <label htmlFor="space-dest">Destino</label>
                            <select id="space-dest" className="admin-users__role" value={spaceDest} onChange={(e) => setSpaceDest(e.target.value)}>
                                <option value="SYSTEM">Nuvem do sistema</option>
                                <option value="DEVICE">Dispositivo pessoal</option>
                            </select>

                            {spaceDest === "DEVICE" && (
                                <>
                                    <label htmlFor="space-node">Dispositivo</label>
                                    <select
                                        id="space-node"
                                        className="admin-users__role"
                                        value={spaceNodeId ?? ""}
                                        onChange={(e) => setSpaceNodeId(e.target.value ? Number(e.target.value) : null)}
                                        disabled={nodes.length === 0}
                                    >
                                        {nodes.length === 0 ? (
                                            <option value="">Sem dispositivos</option>
                                        ) : (
                                            nodes.map((node) => (
                                                <option key={node.nodeID} value={node.nodeID}>
                                                    {node.name}{node.online ? "" : " (offline)"}
                                                </option>
                                            ))
                                        )}
                                    </select>
                                </>
                            )}

                            <div className="modal__actions">
                                <Button label="Cancelar" variant="ghost" onClick={() => setSpaceFile(null)} />
                                <Button label={spaceAction === "copy" ? "Copiar" : "Mover"} variant="primary" type="submit" loading={spaceBusy} />
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {contextMenu.visible && contextMenu.file && (
                <div
                    className="context-menu"
                    style={{ left: contextMenu.x, top: contextMenu.y }}
                    role="menu"
                    aria-label={`Ações para ${contextMenu.file.originalName}`}
                >
                    <button
                        role="menuitem"
                        className={contextMenu.file.compressionAlgorithm && contextMenu.file.compressionAlgorithm !== "NONE" ? "disabled" : ""}
                        disabled={contextMenu.file.compressionAlgorithm && contextMenu.file.compressionAlgorithm !== "NONE"}
                        onClick={() => handleContextMenuAction("compress", contextMenu.file)}
                    >
                        <FiArchive aria-hidden="true" /> Comprimir
                    </button>
                    <button
                        role="menuitem"
                        className={!contextMenu.file.compressionAlgorithm || contextMenu.file.compressionAlgorithm === "NONE" ? "disabled" : ""}
                        disabled={!contextMenu.file.compressionAlgorithm || contextMenu.file.compressionAlgorithm === "NONE"}
                        onClick={() => handleContextMenuAction("decompress", contextMenu.file)}
                    >
                        <FiMaximize aria-hidden="true" /> Descomprimir
                    </button>
                </div>
            )}

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default MyFiles;