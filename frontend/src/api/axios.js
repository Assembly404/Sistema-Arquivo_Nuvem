import axios from "axios";

const http = axios.create({
    baseURL: import.meta.env.VITE_API_URL || "http://localhost:5000",
    headers: { "Content-Type": "application/json" },
    withCredentials: true
});

let authToken = null;

export const setAuthToken = (token) => {
    authToken = token || null;
};

http.interceptors.request.use((config) => {
    if (authToken) {
        config.headers.Authorization = `Bearer ${authToken}`;
    }
    return config;
});

export const register = (data) => {
    return http.post('/auth/register', data)
}
export const login = (data) => {
    return http.post("/auth/login", data)
}

export const getMe = () => http.get("/users/me");
export const getMeuAcesso = () => http.get("/users/me/acesso");
export const patchMe = (data) => http.patch("/users/me", data);
export const changePassword = (data) => http.post("/users/me/password", data);
export const deleteMe = () => http.delete("/users/me");

export const listUsers = (params = {}) => http.get("/admin/users", { params });
export const createUser = (data) => http.post("/admin/users", data);
export const updateUser = (id, data) => http.patch(`/admin/users/${id}`, data);
export const changeUserRole = (id, data) => http.patch(`/admin/users/${id}/role`, data);
export const deleteUser = (id) => http.delete(`/admin/users/${id}`);
export const suspendUser = (id) => http.post(`/admin/users/${id}/suspend`);
export const unsuspendUser = (id) => http.post(`/admin/users/${id}/unsuspend`);

export const forgotPassword = (data) => http.post("/auth/password/forgot", data);
export const verifyOtp = (data) => http.post("/auth/password/verify-otp", data);
export const resetPassword = (data) => http.post("/auth/password/reset", data);

export const listFiles = (params = {}) => http.get("/files", { params });
export const listTrashFiles = () => http.get("/files/trash");
export const uploadFile = (file, folderId, onUploadProgress, options = {}) => {
    const form = new FormData();
    form.append("file", file);
    if (folderId != null) form.append("folderId", folderId);
    if (options.destination) form.append("destination", options.destination);
    if (options.nodeId != null) form.append("nodeId", options.nodeId);

    return http.post("/files/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
        onUploadProgress
    });
};
export const updateFileContent = (id, file, options = {}) => {
    const form = new FormData();
    form.append("file", file);
    if (options.compression) form.append("compression", options.compression);
    return http.put(`/files/${id}/content`, form, {
        headers: { "Content-Type": "multipart/form-data" }
    });
};
export const renameFile = (id, name) => http.patch(`/files/${id}`, { name });
export const moveFile = (id, folderId) => http.patch(`/files/${id}/move`, { folderId });
export const moveFileToSpace = (id, destination, nodeId) => http.post(`/files/${id}/move-to-space`, { destination, nodeId: nodeId ?? null });
export const copyFileToSpace = (id, destination, nodeId) => http.post(`/files/${id}/copy-to-space`, { destination, nodeId: nodeId ?? null });
export const compressFile = (id) => http.post(`/files/${id}/compress`);
export const decompressFile = (id) => http.post(`/files/${id}/decompress`);
export const deleteFile = (id) => http.delete(`/files/${id}`);
export const restoreFile = (id) => http.post(`/files/${id}/restore`);
export const purgeFile = (id) => http.delete(`/files/${id}/purge`);
export const listVersions = (id) => http.get(`/files/${id}/versions`);
export const restoreVersion = (id, versionNumber) => http.post(`/files/${id}/versions/${versionNumber}/restore`);
export const downloadFile = (id, version) => http.get(`/files/${id}/download`, {
    params: version != null ? { version } : {},
    responseType: "blob"
});

export const listFolders = (parentId) => http.get("/folders", {
    params: parentId != null ? { parentId } : {}
});
export const listTrashFolders = () => http.get("/folders/trash");
export const createFolder = (data) => http.post("/folders", data);
export const updateFolder = (id, data) => http.patch(`/folders/${id}`, data);
export const deleteFolder = (id) => http.delete(`/folders/${id}`);
export const restoreFolder = (id) => http.post(`/folders/${id}/restore`);
export const purgeFolder = (id) => http.delete(`/folders/${id}/purge`);

export const shareItem = (data) => http.post("/shares", data);
export const listSharedWithMe = () => http.get("/shares/shared");
export const listSharedContents = (folderId) => http.get("/shares/shared/contents", { params: { folderId } });
export const listSharedByMe = () => http.get("/shares/owned");
export const updateSharePermission = (id, data) => http.patch(`/shares/${id}`, data);
export const revokeShare = (id) => http.delete(`/shares/${id}`);

export const createPublicLink = (data) => http.post("/shares/links", data);
export const listPublicLinks = () => http.get("/shares/links");
export const revokePublicLink = (id) => http.delete(`/shares/links/${id}`);

export const getPublicLinkInfo = (token) => http.get(`/public/links/${token}`);
export const verifyPublicLink = (token, password) =>
    http.post(`/public/links/${token}/verify`, null, { params: password ? { password } : {} });
export const downloadPublicLink = (token, password, version) =>
    http.get(`/public/links/${token}/download`, {
        params: {
            ...(password ? { password } : {}),
            ...(version != null ? { version } : {})
        },
        responseType: "blob"
    });
export const getPublicLinkContents = (token, folderId, password) =>
    http.get(`/public/links/${token}/contents`, {
        params: {
            ...(folderId != null ? { folderId } : {}),
            ...(password ? { password } : {})
        }
    });

export const listNotifications = () => http.get("/notifications");
export const getUnreadNotificationCount = () => http.get("/notifications/unread-count");
export const markNotificationRead = (id) => http.post(`/notifications/${id}/read`);
export const markAllNotificationsRead = () => http.post("/notifications/read-all");
export const deleteNotification = (id) => http.delete(`/notifications/${id}`);
export const clearNotifications = () => http.delete("/notifications");

export const getStorageSummary = () => http.get("/storage/summary");
export const getStoragePolicy = () => http.get("/storage/policy");
export const setStoragePolicy = (data) => http.put("/storage/policy", data);
export const setOfflineBehavior = (offlineBehavior) => http.put("/storage/offline-behavior", { offlineBehavior });

export const moveFolder = (id, folderId) => http.patch(`/folders/${id}`, { parentId: folderId });
export const registerNode = (data) => http.post("/nodes/register", data);
export const listNodes = () => http.get("/nodes");
export const getSystemNode = () => http.get("/nodes/system");
export const getNodeStorageInfo = (id) => http.get(`/nodes/${id}/storage-info`);
export const setNodeQuota = (id, quotaBytes) => http.post(`/nodes/${id}/quota`, { quotaBytes });
export const setNodeVisibility = (id, visibility) => http.post(`/nodes/${id}/visibility`, { visibility });
export const createPairingCode = () => http.post("/nodes/pairing-code");
export const enableNode = (id) => http.post(`/nodes/${id}/enable`);
export const disableNode = (id) => http.post(`/nodes/${id}/disable`);
export const removeNode = (id, migrate = false) => http.delete(`/nodes/${id}`, { params: { migrate } });
export const migrateNode = (id) => http.post(`/nodes/${id}/migrate`);
export const getNodeSyncStatus = (id) => http.get(`/nodes/${id}/sync-status`);

export const getPermissions = () => http.get("/admin/permissions");
export const setUserQuota = (id, quotaBytes) => http.patch(`/admin/users/${id}/quota`, { quotaBytes: quotaBytes ?? null });

/* ——— RBAC: perfis, recursos e permissões ——— */
export const listPapeis = () => http.get("/admin/papeis");
export const createPapel = (data) => http.post("/admin/papeis", data);
export const updatePapel = (id, data) => http.patch(`/admin/papeis/${id}`, data);
export const deletePapel = (id) => http.delete(`/admin/papeis/${id}`);
export const setPapelPermissoes = (id, permissoes) => http.put(`/admin/papeis/${id}/permissoes`, { permissoes });

export const listRecursos = () => http.get("/admin/recursos");
export const createRecurso = (data) => http.post("/admin/recursos", data);
export const updateRecurso = (id, data) => http.patch(`/admin/recursos/${id}`, data);
export const deleteRecurso = (id) => http.delete(`/admin/recursos/${id}`);

export const listPermissoes = () => http.get("/admin/permissoes");
export const createPermissao = (data) => http.post("/admin/permissoes", data);
export const updatePermissao = (id, data) => http.patch(`/admin/permissoes/${id}`, data);
export const deletePermissao = (id) => http.delete(`/admin/permissoes/${id}`);

export default http;