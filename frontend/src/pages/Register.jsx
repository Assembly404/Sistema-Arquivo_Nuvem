import { useCallback, useEffect, useRef, useState } from "react";
import './Register.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
import Toast from "../components/toast/toast";
import hero from "../images/hero.jpg";
import { Link, useNavigate } from "react-router";
import { register } from "../api/axios";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faXmark, faUserPlus } from "@fortawesome/free-solid-svg-icons";

const USER_REGEX = /^[a-zA-Z][a-zA-Z0-9_-]{2,23}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

/* §5.3 — o toast tem de ser visível antes do redireccionamento */
const TOAST_DURATION = 4500;
const SUCCESS_TOAST_DURATION = 1500;

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

    const nameError = name && !validName ? "minimo 3 caracteres e deve comecar com uma letra" : "";
    const apelidoError = surname && !validSurname ? "minimo 4 caracteres e deve comecar com uma letra" : "";
    const emailError = email && !validEmail ? "email deve conter @dominio.com" : "";
    const pwdError = password && !validPwd
        ? "8 a 24 caracteres.Deve ter maiúscula, minúscula,\n número e um símbolo (!@#$%)."
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
            <div className="registerPage flex">
                <div className="container flex">
                    <div className="videoDiv">
                        <img src={hero} alt="" />

                        <div className="textDiv">
                            <p className="title">Os Seus Dados A Distancia De Um Clique</p>
                            <p>Sem peso no bolso, tudo na nuvem!</p>
                        </div>

                        <div className="footerDiv flex">
                            <span className="text">Ja tem uma conta?</span>
                            <Link to={'/login'} className="btn btn-primary btn-account">
                                Login
                            </Link>
                        </div>
                    </div>

                    <div className="formDiv flex">
                        <div className="headerDiv">
                            <FontAwesomeIcon className="headIcon" icon={faUserPlus} aria-hidden="true" />
                            <h1 className="authTitle">Crie a sua Conta</h1>
                        </div>

                        <form onSubmit={handleSubmit} className="form grid">
                            <div className="inputDiv">
                                <label htmlFor="name">
                                    Nome:
                                    {name && (
                                        <FontAwesomeIcon
                                        aria-hidden="true"
                                        className={`fieldIcon ${validName ? 'fieldIcon--valid' : 'fieldIcon--invalid'}`}
                                        icon={validName ? faCheck : faXmark} />
                                    )}
                                </label>
                                <Input type="text"
                                id="name"
                                placeholder="Coloque o seu Nome"
                                autoComplete="given-name"
                                value={name}
                                ref={userRef}
                                onChange={(e)=>{setName(e.target.value)}}
                                required={true}
                                errorMessage={nameError} />
                            </div>

                            <div className="inputDiv">
                                <label htmlFor="apelido">
                                    Apelido:
                                    {surname && (
                                        <FontAwesomeIcon
                                        aria-hidden="true"
                                        className={`fieldIcon ${validSurname ? 'fieldIcon--valid' : 'fieldIcon--invalid'}`}
                                        icon={validSurname ? faCheck : faXmark} />
                                    )}
                                </label>
                                <Input type="text"
                                id="apelido"
                                placeholder="Coloque o seu Apelido"
                                autoComplete="family-name"
                                value={surname}
                                onChange={(e)=>{setSurname(e.target.value)}}
                                required={true}
                                errorMessage={apelidoError} />
                            </div>

                            <div className="inputDiv">
                                <label htmlFor="email">
                                    Email:
                                    {email && (
                                        <FontAwesomeIcon
                                        aria-hidden="true"
                                        className={`fieldIcon ${validEmail ? 'fieldIcon--valid' : 'fieldIcon--invalid'}`}
                                        icon={validEmail ? faCheck : faXmark} />
                                    )}
                                </label>
                                <Input type="email"
                                id="email"
                                placeholder="Coloque o seu Email"
                                autoComplete="email"
                                value={email}
                                onChange={(e)=>{setEmail(e.target.value)}}
                                required={true}
                                errorMessage={emailError} />
                            </div>

                            <div className="inputDiv">
                                <label htmlFor="password">
                                    Password:
                                    {password && (
                                        <FontAwesomeIcon
                                        aria-hidden="true"
                                        className={`fieldIcon ${validPwd ? 'fieldIcon--valid' : 'fieldIcon--invalid'}`}
                                        icon={validPwd ? faCheck : faXmark} />
                                    )}
                                </label>
                                <Input type="password"
                                id="password"
                                placeholder="Coloque a sua password"
                                autoComplete="new-password"
                                value={password}
                                onChange={(e)=>{setPwd(e.target.value)}}
                                required={true}
                                errorMessage={pwdError} />
                            </div>

                            <div className="inputDiv">
                                <label htmlFor="matchpwd">
                                    Confirmar Password:
                                    {matchPwd && (
                                        <FontAwesomeIcon
                                        aria-hidden="true"
                                        className={`fieldIcon ${validmatch ? 'fieldIcon--valid' : 'fieldIcon--invalid'}`}
                                        icon={validmatch ? faCheck : faXmark} />
                                    )}
                                </label>
                                <Input type="password"
                                id="matchpwd"
                                placeholder="Confirma a sua password"
                                autoComplete="new-password"
                                value={matchPwd}
                                onChange={(e)=>{setmatchPwd(e.target.value)}}
                                required={true}
                                errorMessage={matchError} />
                            </div>

                            <Button label={'Registrar'}
                            variant={'primary'}
                            type={'submit'}
                            loading={loading}
                            disabled={
                                !validName || !validSurname || !validEmail || !validPwd || !validmatch
                            } />

                            <span className="forgotPassword">
                                Esqueceu a sua password? <Link to="/forgotpassword">Clique Aqui</Link>
                            </span>
                        </form>
                    </div>
                </div>
            </div>

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
