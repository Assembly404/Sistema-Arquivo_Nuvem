import { useState } from "react";
import './Login.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
import Toast from "../components/toast/toast";
import video from "../assets/videoUpload.mp4";
import image from "../assets/nuvem.png";
import { Link } from "react-router";
import {MdOutlineMail} from "react-icons/md"


function Login(){
    const [email, setEmail] = useState("")
    const [senha, setSenha] = useState("");
    const [emailError, setEmailError] = useState("")
    const [loading, setLoading] = useState("")
    const [toast, setToast] = useState(null)

    const showToast = (message, type)=>{
        setToast({message, type})
    }
    const removeToast = () =>{
        setToast(null);
    }

    const handleSubmit = () =>{
        setLoading(true)
        setTimeout(()=>{
            setLoading(false)
            showToast("Muitoas informacoes que vao aparecer no toast notication", "success")
        },2000)
        

    }


    const handlingEmailChanging = (e)=>{
        setEmail(e.target.value)
        setEmailError(validateEmail(e.target.value) ? '': 'Invalid email adress')
    }
    const validateEmail =(email)=>{
        return /\S+@\S+\.\S+/.test(email)
    }
    return(
        <>
            <div className="loginPage flex">
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

                        <form action="" className="form grid">
                            <span>Ligue se a sua nuvem</span>
                            <div className="inputDiv">
                                <label htmlFor="username">Email</label>
                                <Input type="email" 
                                placeholder={"Coloque o seu Email"}
                                
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Password</label>
                                <Input type="password" 
                                placeholder="Coloque a sua senha"
                                
                                />
                            </div>

                            <Button label={'Login'}
                            variant={'secundary'} 
                            type={'submit'}/>

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

export default Login;