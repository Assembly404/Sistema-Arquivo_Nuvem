import { useState, useRef, useEffect, useCallback } from "react";
import './Register.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
import video from "../assets/videoUpload.mp4";
import image from "../assets/nuvem.png";
import { Link , useNavigate} from "react-router";
import Toast from "../components/toast/toast";

import { register } from "../api/axios";

// import { faCheck, faTimes, faInfoCircle} from '@fortawesome/react-fontawesome';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faXmark,} from "@fortawesome/free-solid-svg-icons";
// import { byPrefixAndName } from '@awesome.me/kit-KIT_CODE/icons'

const  USER_REGEX = /^[a-zA-z][a-zA-Z0-9-_]{3,23}$/;
const PWD_REGEx = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const EMAIl_REGEX = /^[a-zA-z0-9._%+-]+@[a-zA-z0-9.-]+\.[a-zA-Z0-9]{2,}$/;

function Register(){
    const [name, setName] = useState("");
    const [surname, setSurname] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPwd] = useState("");
    const [matchPwd, setmatchPwd] = useState("");

    const validName = USER_REGEX.test(name);
    const validSurname = USER_REGEX.test(surname);
    const validEmail = EMAIl_REGEX.test(email);
    const validPwd = PWD_REGEx.test(password);
    const validmatch = password === matchPwd;
    // const [errorMessage, setErrorMessage] = useState(false);
    const [success, setSuccess] = useState(false);
    const navigate = useNavigate();

    const [loading, setLoading] = useState(false)
    const userRef = useRef();

    // const [toast, setToast] = useState(null);
    // const showToast = (message, type) => setToast({ message, type, id: Date.now() });
    // const closeToast = useCallback(() => setToast(null), []); 
    // const errRef = useRef();

    const nameError = name && !validName ? "minimo 4 caracteres e deve comecar com uma letra" : "";
    const apelidoError = surname && !validSurname ? "minimo 4 caracteres e deve comecar com uma letra" : "";
    const emailError = email && !validEmail ? "email deve conter @dominio.com":"";
    const pwdError = password && !validPwd
    ? "8 a 24 caracteres.Deve ter maiúscula, minúscula,\n número e um símbolo (!@#$%)."
    : "";
    const matchError = matchPwd && !validmatch ? "As passwords não coincidem." : "";


    useEffect(()=>{
        userRef.current.focus();
    },[]);

    const handleSubmit = async function (e) {
        e.preventDefault();

        

        try{
            const payload = {
                email: email,
                name: name,
                surname: surname,
                password: password
            }
            const response =  await register(payload)
            console.log(response.data)
            console.log(response.status)
            console.log(JSON.stringify(response))
            console.log(response)

            console.log('COnta criada com sucesso')
            navigate('/login')
            
        }catch(err){
            
            if(!err?.response){
                console.log('No server Response')
            }else {
                console.log(err.response.status)
                console.log(err.response.data)
                console.log("Registration Failed")
            }
        }


        
    }
    return(
        <>  
            <div className="registerPage flex">
                <div className="container flex">
                    <div className="videoDiv">
                        <video src={video} autoPlay muted loop></video>

                        <div className="textDiv">
                            <h2 className="title">Os Seus Dados A Distancia De Um Clique </h2>
                            <p>Sem peso no bolso, tudo na nuvem!</p>
                        </div>

                        <div className="footerDiv flex">
                            <span className="text">Ja tem uma conta?</span>
                            <Link to={'/login'}>
                                <button className="btn-account">Login</button>
                            </Link>
                        </div>
                    </div>
                    <div className="formDiv flex">
                        <div className="headerDiv">
                            {/* <img src={image} alt="Logo de imagem" /> */}
                            <h3>Crie a sua Conta</h3>
                        </div>

                        <form onSubmit={handleSubmit} className="form grid">
                            <div className="inputDiv">
                                <label htmlFor="name">
                                    Nome:
                                    {name && (
                                        <FontAwesomeIcon 
                                        icon={validName ? faCheck : faXmark} 
                                        style={validName ? {color: "rgb(45, 246, 0)"} :{color:"rgb(255,0,24)"}}/>
                                    )}

                                   
                                    
                                </label>
                                <Input type="text" 
                                placeholder={"Coloque o seu Nome"}
                                errorMessage={nameError}
                                id={"name"}
                                ref={userRef}
                                value={name}
                                autoComplete={"off"}
                                onChange={(e)=>{setName(e.target.value)}}
                                required={true}
                                
                                                             
                                />
                            
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="apelido">
                                    Apelido: 
                                    {surname && (
                                        <FontAwesomeIcon 
                                        icon={validSurname ? faCheck : faXmark} 
                                        style={validSurname ? {color: "rgb(45, 246, 0)"} :{color:"rgb(255,0,24)"}}/>
                                    )}
                                </label>
                                <Input type="text" 
                                placeholder={"Coloque o seu Apelido"}
                                id={"apelido"}
                                // ref={userRef}
                                value={surname}
                                autoComplete={"off"}
                                onChange={(e)=>{setSurname(e.target.value)}}
                                required={true}
                                errorMessage={apelidoError}
                                 
                                                               
                                />
                                
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="email">
                                    Email:
                                    {email && (
                                        <FontAwesomeIcon 
                                        icon={validEmail ? faCheck : faXmark} 
                                        style={validEmail ? {color: "rgb(45, 246, 0)"} :{color:"rgb(255,0,24)"}}/>
                                    )}

                                </label>
                                <Input type="email" 
                                placeholder={"Coloque o seu Email"}
                                id={"email"}
                                value={email}
                                autoComplete={"off"}
                                onChange={(e)=>{setEmail(e.target.value)}}
                                required={true}
                                errorMessage={emailError}
                                
                                                                
                                />
                                
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="password">
                                    Password:
                                    {password && (
                                        <FontAwesomeIcon 
                                        icon={validPwd ? faCheck : faXmark} 
                                        style={validPwd ? {color: "rgb(45, 246, 0)"} :{color:"rgb(255,0,24)"}}/>
                                    )}

                                </label>
                                <Input type="password" 
                                placeholder="Coloque a sua password"
                                id={"password"}
                                value={password}
                                autoComplete={"off"}
                                onChange={(e)=>{setPwd(e.target.value)}}
                                required={true}
                                errorMessage={pwdError}             
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="matchpwd">
                                    Confirmar Password:
                                    {matchPwd && (
                                        <FontAwesomeIcon 
                                        icon={validmatch ? faCheck : faXmark} 
                                        style={validmatch ? {color: "rgb(45, 246, 0)"} :{color:"rgb(255,0,24)"}}/>
                                    )}
                                </label>
                                <Input type="password" 
                                placeholder="Confirma a sua password"
                                id={'matchpwd'}
                                value={matchPwd}
                                onChange={(e)=>{setmatchPwd(e.target.value)}}
                                required={true}
                                errorMessage={matchError}
                                
                                />
                            </div>

                            <Button label={'Registra'}
                            variant={'secundary'} 
                            loading={loading}
                            type="submit"
                            disabled={
                                !validName || !validSurname || !validEmail || !validPwd || !validmatch ? true : false
                            }
                            />
                            
                            

                            <span className="forgotPassword">
                                Esqueceu a sua password? <a href="/forgotpassword">Clique Aqui</a>
                            </span>
                        </form>
                    </div>
                </div>
            </div>
        </>
    )
}

export default Register;