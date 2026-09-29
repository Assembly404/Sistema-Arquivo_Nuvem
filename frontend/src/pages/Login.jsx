import { useCallback, useContext, useEffect, useRef, useState } from "react";
import './Login.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
import Toast from "../components/toast/toast";
import hero from "../images/hero.jpg";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faRightToBracket } from "@fortawesome/free-solid-svg-icons";
import { Link, useNavigate } from "react-router";
import AuthContext from "../context/AuthProvider.jsx";
import { login } from "../api/axios";

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

            setAuth({email, password, token})

            /* §5.3 — sucesso: Toast antes de redireccionar */
            setToast({ message: "Sessão iniciada com sucesso.", type: "success" });
            navigateTimer.current = setTimeout(() => navigate("/dashboard"), SUCCESS_TOAST_DURATION);

        }catch(err){
            /* trilha de erro (diagnóstico) — o utilizador é informado pelo Toast */
            console.error("[login] falha na autenticação", err);

            if(!err?.response){
                console.error("[login] sem resposta do servidor");
                setToast({ message: "Sem resposta do servidor. Tente novamente.", type: "error" });
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
            <section className="loginPage flex">
                <div className="container flex">
                    <div className="videoDiv">
                        <img src={hero} alt="" />

                        <div className="textDiv">
                            <p className="title">Os Seus Dados A Distancia De Um Clique</p>
                            <p>Sem peso no bolso, tudo na nuvem!</p>
                        </div>

                        <div className="footerDiv flex">
                            <span className="text">Nao tem conta?</span>
                            <Link to={'/register'} className="btn btn-primary btn-account">
                                Criar conta
                            </Link>
                        </div>
                    </div>

                    <div className="formDiv flex">
                        <div className="headerDiv">
                            <FontAwesomeIcon className="headIcon" icon={faRightToBracket} aria-hidden="true" />
                            <h1 className="authTitle">Bem vindo ao Arquivo Inteligente</h1>
                        </div>

                        <form onSubmit={handleSubmit} className="form grid">
                            <span className="formHint">Ligue se a sua nuvem</span>

                            <div className="inputDiv">
                                <label htmlFor="email">
                                    Email:
                                </label>
                                <Input type="email"
                                id="email"
                                placeholder="Coloque o seu Email"
                                autoComplete="email"
                                value={email}
                                ref={userRef}
                                onChange={(e)=>{setEmail(e.target.value)}}
                                required={true}
                                errorMessage={emailError} />
                            </div>

                            <div className="inputDiv">
                                <label htmlFor="password">Password</label>
                                <Input type="password"
                                id="password"
                                placeholder="Coloque a sua senha"
                                autoComplete="current-password"
                                onChange={(e)=>{ setPassword(e.target.value)}}
                                value={password}
                                required={true} />
                            </div>

                            <Button label={'Login'}
                            variant={'primary'}
                            type={'submit'}
                            loading={loading}
                            disabled={!validEmail} />

                            <span className="forgotPassword">
                                Esqueceu a sua password? <Link to="/forgotpassword">Clique Aqui</Link>
                            </span>
                        </form>
                    </div>
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
