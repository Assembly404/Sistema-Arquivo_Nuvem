import axios from "axios";

const http = axios.create({
    baseURL: "http://127.0.0.1:5000",
    headers: {"Content-Type":"application/json"}
})

export const register = (data) =>{
    return http.post('/auth/register',data)
}
export const login = (data) =>{
    return http.post("/auth/login",data)
}