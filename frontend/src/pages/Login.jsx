import { useCallback, useContext, useEffect, useRef, useState } from "react";
import './Login.css'
import Input from "../components/form/input.jsx";
import Button from "../components/button/button.jsx";
import Toast from "../components/toast/toast.jsx";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRightToBracket } from "@fortawesome/free-solid-svg-icons";
import { Link, useNavigate } from "react-router";
import AuthContext from "../context/AuthProvider.jsx";
import { login, setAuthToken } from "../api/axios.js";

const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

/* §5.3 — o toast tem de ser visível antes do redireccionamento */
const TOAST_DURATION = 4500;
const SUCCESS_TOAST_DURATION = 1500;

function Login(){
    const {setAuth} = useContext(AuthContext)
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState(null);

    const userRef = useRef();
    const navigateTimer = useRef();
    const navigate = useNavigate();

    const closeToast = useCallback(() => setToast(null), []);

    const validEmail = EMAIL_REGEX.test(email);

    const emailError = email && !validEmail ? "Email deve conter @dominio.com" : "";

    useEffect(()=>{
        userRef.current?.focus();
    },[])

    useEffect(()=> () => clearTimeout(navigateTimer.current), [])

    const handleSubmit = async function (e) {
        e.preventDefault();

        setLoading(true);

        try{
            const payload = {
                email,
                password
            }
            const response = await login(payload);
            const token = response.data?.token
            const user = response.data?.user || { email }

            setAuthToken(token);
            setAuth({ user, token })

            /* §5.3 — sucesso: Toast antes de redireccionar */
            setToast({ message: "Sessão iniciada com sucesso.", type: "success" });
            navigateTimer.current = setTimeout(() => navigate("/dashboard"), SUCCESS_TOAST_DURATION);

        }catch(err){
            /* trilha de erro (diagnóstico) — o utilizador é informado pelo Toast */
            console.error("[login] falha na autenticação", err);

            if(!err?.response){
                console.error("[login] sem resposta do servidor");
                setToast({ message: "Sem resposta do servidor. Tente novamente.", type: "error" });
            }else if(err.response.status === 401){
                console.error("[login] credenciais inválidas", err.response.status);
                /* erro de servidor nunca apaga o que o utilizador digitou (§5.3) */
                setToast({ message: "Email ou password incorrectos.", type: "error" });
            }else if(err.response.status === 403){
                console.error("[login] utilizador sem autorização", err.response.status);
                setToast({ message: "Não tem autorização para aceder a esta conta.", type: "error" });
            }else if(err.response.status === 429){
                setToast({ message: "Demasiadas tentativas. Tente novamente mais tarde.", type: "error" });
            }else{
                console.error("[login] credenciais inválidas", err.response.status);
                /* erro de servidor nunca apaga o que o utilizador digitou (§5.3) */
                setToast({ message: "Email ou password incorrectos.", type: "error" });
            }
        }finally{
            setLoading(false);
        }
    }

    return(
        <>
            <section className="loginPage">
                <div className="login-card">
                    <div className="login-badge">
                        <FontAwesomeIcon icon={faRightToBracket} aria-hidden="true" />
                    </div>
                    <h1 className="login-title" ref={userRef} tabIndex={-1}>Bem vindo ao Arquivo Inteligente</h1>
                    <p className="login-subtitle">Ligue-se à sua nuvem</p>
                    <form onSubmit={handleSubmit} className="login-form">
                        <div className="input_container">
                            <label htmlFor="email">Email</label>
                            <Input
                                type="email"
                                id="email"
                                placeholder="O teu email"
                                autoComplete="email"
                                value={email}
                                onChange={(e)=>{setEmail(e.target.value)}}
                                required
                                errorMessage={emailError} />
                        </div>

                        <div className="input_container">
                            <label htmlFor="password">Password</label>
                            <Input
                                type="password"
                                id="password"
                                placeholder="A tua password"
                                autoComplete="current-password"
                                value={password}
                                onChange={(e)=>{ setPassword(e.target.value)}}
                                required />
                        </div>

                        <Button
                            label="Login"
                            variant="primary"
                            type="submit"
                            loading={loading}
                            disabled={!validEmail} />
                    </form>
                    <Link className="login-back" to="/forgotpassword">Esqueceu a password?</Link>
                    <Link className="login-back" to="/register">Não tem conta? Criar uma</Link>
                </div>
            </section>

            {toast && (
                <Toast
                key={toast.message}
                message={toast.message}
                type={toast.type}
                duration={toast.type === 'success' ? SUCCESS_TOAST_DURATION : TOAST_DURATION}
                onClose={closeToast} />
            )}
        </>
    )
}

export default Login;