import { useState, useRef, useEffect } from "react";
import './Register.css'
import Input from "../components/form/input";
import Button from "../components/button/button";
import Toast from "../components/toast/toast";
import video from "../assets/videoUpload.mp4";
import image from "../assets/nuvem.png";
import { Link } from "react-router";
import {MdOutlineMail} from "react-icons/md";
import Axios from 'axios';



function Register(){
    const [email, setEmail] = useState("");
    const [senha, setSenha] = useState("");
    const [nome, setNome] = useState("");
    const [apelido, setApelido] = useState("");
    const [emailError, setEmailError] = useState("")
    const [loading, setLoading] = useState("")
    const [toast, setToast] = useState(null)


    const showToast = (message, type)=>{
        setToast({message, type})
    }
    const removeToast = () =>{
        setToast(null);
    }
    const http = Axios.create({
        baseURL:"http://localhost:5000"
    })
    async function handleSubmit(){ 
        setLoading(true);
            try{
                const response = await http.post('/api/addNewUser',{
                    name: nome,
                    surname: apelido,
                    email: email,
                    password: senha,

                })
            }catch(error){
                showToast("Erro ao cadastrar:", error.response?.data || error.message);
                console.error(error)
            }finally{
                setLoading(false)
                setTimeout(()=>{
                    showToast("Registro efetuado", "success")

                },2000)

            }
            
        

    }


    const handlingEmailChanging = (e)=>{
        setEmail(e.target.value)
        setEmailError(validateEmail(e.target.value) ? '': 'Invalid email adress')
    }
    const validateEmail =(email)=>{
        return /\S+@\S+\.\S+/.test(email)
    }
    return(
        <>  {toast}
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

                        <form action="" className="form grid">
                            <div className="inputDiv">
                                <label htmlFor="username">Nome</label>
                                <Input type="text" 
                                placeholder={"Coloque o seu Nome"}
                                onChange={(e)=>{
                                    setNome(e.target.value)
                                }}
                                
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Apelido</label>
                                <Input type="email" 
                                placeholder={"Coloque o seu Apelido"}
                                onChange={(e)=>{
                                    setApelido(e.target.value)
                                }}
                                
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Email</label>
                                <Input type="email" 
                                placeholder={"Coloque o seu Email"}
                                onChange={(e)=>{
                                    setEmail(e.target.value)
                                }}
                                
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Password</label>
                                <Input type="password" 
                                placeholder="Coloque a sua password"
                                onChange={(e)=>{
                                    setSenha(e.target.value)
                                }}
                                
                                />
                            </div>
                            <div className="inputDiv">
                                <label htmlFor="username">Confirma Password</label>
                                <Input type="password" 
                                placeholder="Confirma a sua password"
                                
                                />
                            </div>

                            <Button label={'Registra'}
                            variant={'secundary'} 
                            type={'submit'}
                            loading={loading}
                            onClick={handleSubmit}/>
                            
                            

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