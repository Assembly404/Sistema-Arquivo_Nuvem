import { useCallback, useEffect, useRef, useState } from "react";
import './Register.css'
import Input from "../components/form/input.jsx";
import Button from "../components/button/button.jsx";
import Toast from "../components/toast/toast.jsx";
import { Link, useNavigate } from "react-router";
import { register } from "../api/axios.js";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faUserPlus } from "@fortawesome/free-solid-svg-icons";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

/* §5.3 — o toast tem de ser visível antes do redireccionamento */
const TOAST_DURATION = 4500;
const SUCCESS_TOAST_DURATION = 1500;

const checks = (pwd) => ([
    { label: "8 a 24 caracteres", ok: pwd.length >= 8 && pwd.length <= 24 },
    { label: "Uma maiúscula", ok: /[A-Z]/.test(pwd) },
    { label: "Uma minúscula", ok: /[a-z]/.test(pwd) },
    { label: "Um número", ok: /[0-9]/.test(pwd) },
    { label: "Um símbolo (!@#$%)", ok: /[!@#$%]/.test(pwd) }
]);

function Register(){
    const [name, setName] = useState("");
    const [surname, setSurname] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPwd] = useState("");
    const [matchPwd, setmatchPwd] = useState("");
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState(null);

    const navigate = useNavigate();
    const userRef = useRef();
    const navigateTimer = useRef();

    const closeToast = useCallback(() => setToast(null), []);

    const validName = USER_REGEX.test(name);
    const validSurname = USER_REGEX.test(surname);
    const validEmail = EMAIL_REGEX.test(email);
    const validPwd = PWD_REGEX.test(password);
    const validmatch = password === matchPwd;

    const nameError = name && !validName ? "Mínimo 3 caracteres, deve começar com uma letra" : "";
    const surnameError = surname && !validSurname ? "Mínimo 3 caracteres, deve começar com uma letra" : "";
    const emailError = email && !validEmail ? "Email deve conter @dominio.com" : "";
    const pwdError = password && !validPwd
        ? "8 a 24 caracteres. Deve ter maiúscula, minúscula, número e um símbolo (!@#$%)."
        : "";
    const matchError = matchPwd && !validmatch ? "As passwords não coincidem." : "";

    useEffect(()=>{
        userRef.current?.focus();
    },[]);

    useEffect(()=> () => clearTimeout(navigateTimer.current), []);

    const handleSubmit = async function (e) {
        e.preventDefault();

        setLoading(true);

        try{
            const payload = {
                email: email,
                name: name,
                surname: surname,
                password: password
            }
            await register(payload)

            /* §5.3 — sucesso: Toast antes de redireccionar */
            setToast({ message: "Conta criada com sucesso.", type: "success" });
            navigateTimer.current = setTimeout(() => navigate('/login'), SUCCESS_TOAST_DURATION);

        }catch(err){
            /* trilha de erro (diagnóstico) — o utilizador é informado pelo Toast */
            console.error("[register] falha no registo", err);

            if(!err?.response){
                console.error("[register] sem resposta do servidor");
                setToast({ message: "Sem resposta do servidor. Tente novamente.", type: "error" });
            }else if(err.response.status === 409){
                console.error("[register] email já registado", err.response.status);
                setToast({ message: "Já existe uma conta com este email.", type: "error" });
            }else if(err.response.status === 429){
                setToast({ message: "Demasiadas tentativas. Tente novamente mais tarde.", type: "error" });
            }else{
                console.error("[register] dados inválidos", err.response.status, err.response.data);
                setToast({ message: "Não foi possível criar a conta. Verifique os dados.", type: "error" });
            }
        }finally{
            setLoading(false);
        }
    }

    return(
        <>
            <section className="registerPage">
                <div className="register-card">
                    <div className="register-badge">
                        <FontAwesomeIcon icon={faUserPlus} aria-hidden="true" />
                    </div>
                    <h1 className="register-title" ref={userRef} tabIndex={-1}>Crie a sua Conta</h1>
                    <p className="register-subtitle">Junte-se ao Arquivo Inteligente</p>
                    <form onSubmit={handleSubmit} className="register-form">
                        <div className="input_container">
                            <label htmlFor="name">Nome</label>
                            <Input
                                type="text"
                                id="name"
                                placeholder="O teu nome"
                                autoComplete="given-name"
                                value={name}
                                onChange={(e)=>{setName(e.target.value)}}
                                required
                                errorMessage={nameError} />
                        </div>

                        <div className="input_container">
                            <label htmlFor="apelido">Apelido</label>
                            <Input
                                type="text"
                                id="apelido"
                                placeholder="O teu apelido"
                                autoComplete="family-name"
                                value={surname}
                                onChange={(e)=>{setSurname(e.target.value)}}
                                required
                                errorMessage={surnameError} />
                        </div>

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
                                autoComplete="new-password"
                                value={password}
                                onChange={(e)=>{setPwd(e.target.value)}}
                                required
                                errorMessage={pwdError} />
                        </div>

                        <div className="input_container">
                            <label htmlFor="matchpwd">Confirmar Password</label>
                            <Input
                                type="password"
                                id="matchpwd"
                                placeholder="Confirmar password"
                                autoComplete="new-password"
                                value={matchPwd}
                                onChange={(e)=>{setmatchPwd(e.target.value)}}
                                required
                                errorMessage={matchError} />
                        </div>

                        <ul className="pwd-checks" aria-live="polite">
                            {checks(password).map((c) => (
                                <li key={c.label} className={c.ok ? "pwd-check--ok" : ""}>
                                    <span>{c.ok ? "✓" : "•"}</span> {c.label}
                                </li>
                            ))}
                        </ul>

                        <Button
                            label="Registrar"
                            variant="primary"
                            type="submit"
                            loading={loading}
                            disabled={!validName || !validSurname || !validEmail || !validPwd || !validmatch} />

                        <Link className="register-back" to="/login">Já tem conta? Entrar</Link>
                        <Link className="register-back" to="/forgotpassword">Esqueceu a password?</Link>
                    </form>
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

export default Register;