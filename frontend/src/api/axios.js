import axios from "axios";

const http = axios.create({
    baseURL: "http://192.168.108.79:5000",
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
export const patchMe = (data) => http.patch("/users/me", data);
export const changePassword = (data) => http.post("/users/me/password", data);
export const deleteMe = () => http.delete("/users/me");

export const listUsers = () => http.get("/admin/users");
export const createUser = (data) => http.post("/admin/users", data);
export const changeUserRole = (id, data) => http.patch(`/admin/users/${id}/role`, data);
export const deleteUser = (id) => http.delete(`/admin/users/${id}`);

export const forgotPassword = (data) => http.post("/auth/password/forgot", data);
export const verifyOtp = (data) => http.post("/auth/password/verify-otp", data);
export const resetPassword = (data) => http.post("/auth/password/reset", data);

export default http;