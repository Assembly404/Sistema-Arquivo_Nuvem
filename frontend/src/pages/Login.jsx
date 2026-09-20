import { useContext, useEffect, useRef, useState } from "react";
import './Login.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
// import Toast from "../components/toast/toast";
import video from "../assets/videoUpload.mp4";
import image from "../assets/nuvem.png";
import { Link, useNavigate } from "react-router";
// import {MdOutlineMail} from "react-icons/md"
import AuthContext from "../context/AuthProvider.jsx";
import { login } from "../api/axios";


const EMAIl_REGEX = /^[a-zA-z0-9._%+-]+@[a-zA-z0-9.-]+\.[a-zA-Z0-9]{2,}$/;

function Login(){
    const {setAuth} = useContext(AuthContext)
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("");
    // const [loading, setLoading] = useState("")
    // const [toast, setToast] = useState(null)

    const userRef = useRef();
    const navigate = useNavigate();

    
    const validEmail = EMAIl_REGEX.test(email);

    const emailError = email && !validEmail ? "email deve conter @dominio.com":"";
    
    const handleSubmit = async function (e) {
        e.preventDefault();

        try{
            const payload = {
                email,
                password
            }
            const response = await login(payload);
            const token = response.data?.token


            console.log(response.data);
            console.log(response.data.token);

            setAuth({email, password, token})

            alert("login efectuado com sucesso")

            navigate("/dashboard")

            
            
            
            console.log(err.response.status)
            console.log(err.response.data)
            console.log("Login Failed")
            
        }catch(err){
            if(!err?.response){
                console.log('No server response')
            }else if(err.response.status === 403){
                console.log("User not authotired")
            }
        }
        
        

    }
    useEffect(()=>{
        userRef.current.focus();
    },[])

    return(
        <>
            
            <section className="loginPage flex">
                <div className="container flex">
                    <div className="videoDiv">
                        <video src={video} autoPlay muted loop></video>

                        <div className="textDiv">
                            <h2 className="title">Os Seus Dados A Distancia De Um Clique </h2>
                            <p>Sem peso no bolso, tudo na nuvem!</p>
                        </div>

                        <div className="footerDiv flex">
                            <span className="text">Nao tem conta?</span>
                            <Link to={'/register'}>
                                <button className="btn-account">Entre</button>
                            </Link>
                        </div>
                    </div>
                    <div className="formDiv flex">
                        <div className="headerDiv">
                            <img src={image} alt="Logo de imagem" />
                            <h3>Bem vindo ao Arquivo Inteligente</h3>
                        </div>

                        <form onSubmit={handleSubmit} className="form grid">
                            <span>Ligue se a sua nuvem</span>
                            <div className="inputDiv">
                                <label htmlFor="email">
                                    Email:
                                </label>
                                <Input type="email" 
                                placeholder={"Coloque o seu Email"}
                                id={"email"}
                                value={email}
                                ref={userRef}
                                onChange={(e)=>{setEmail(e.target.value)}}
                                required={true}
                                errorMessage={emailError} />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Password</label>
                                <Input type="password" 
                                placeholder="Coloque a sua senha"
                                onChange={(e)=>{ setPassword(e.target.value)}}
                                value={password}
                                required={true}

                                
                                />
                            </div>

                            <Button label={'Login'}
                            variant={'secundary'} 
                            type={'submit'}
                            disabled={!validEmail ? true:false}/>

                            

                            <span className="forgotPassword">
                                Esqueceu a sua password? <a href="/forgotpassword">Clique Aqui</a>
                            </span>
                        </form>
                    </div>
                </div>
            </section>
        </>
    )
}

export default Login;