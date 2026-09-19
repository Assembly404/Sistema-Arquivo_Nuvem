import axios from "axios";

const http = axios.create({
    baseURL: "http://localhost:5000",
    headers: {"Content-Type":"application/json"},
    withCredentials: true
})

export const register = (data) =>{
    http.post('/auth/register',data)
}
export const login = (data) =>{
    http.post("/auth/login",data)
}