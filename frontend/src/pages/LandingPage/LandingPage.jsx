import { useState, useEffect, useRef } from "react";
import { Link } from "react-router";
import {
    FiArrowRight,
    FiCheck,
    FiCloud,
    FiFolder,
    FiHardDrive,
    FiLock,
    FiMenu,
    FiShare2,
    FiShield,
    FiX,
} from "react-icons/fi";
import "./LandingPage.css";
import hero from "../../images/hero.jpg";
import logo from "../../assets/logo.png";
import Card from "./Card.jsx";

/* Secções da navegação — âncoras do próprio documento */
const NAV_LINKS = [
    { href: "#recursos", label: "Recursos" },
    { href: "#como-funciona", label: "Como funciona" },
    { href: "#seguranca", label: "Segurança" },
    { href: "#faq", label: "FAQ" },
];

/* Features reais do sistema (schema + sidebar + backend) */
const RECURSOS = [
    {
        icone: <FiCloud aria-hidden="true" />,
        titulo: "Armazenamento híbrido",
        subtitulo: "Nuvem + o seu computador",
        texto:
            "Guarde os ficheiros na nossa nuvem ou no seu próprio PC de casa. O sistema regista o provedor e o bucket de cada ficheiro, para saber sempre onde está o que é seu.",
    },
    {
        icone: <FiFolder aria-hidden="true" />,
        titulo: "Ficheiros e pastas",
        subtitulo: "Organização sem esforço",
        texto:
            "Crie pastas, carregue, renomeie, mova e organize tudo numa hierarquia simples. Apagou por engano? A lixeira guarda os itens até decidir eliminá-los.",
    },
    {
        icone: <FiShare2 aria-hidden="true" />,
        titulo: "Partilha de ficheiros",
        subtitulo: "Colabore em segundos",
        texto:
            "Partilhe documentos com outros utilizadores e aceda a tudo o que partilharam consigo numa única secção: «Compartilhados comigo».",
    },
    {
        icone: <FiHardDrive aria-hidden="true" />,
        titulo: "Controlo de armazenamento",
        subtitulo: "Espaço sempre à vista",
        texto:
            "Acompanhe o espaço ocupado, o provedor e a chave de armazenamento de cada ficheiro antes de chegar ao limite.",
    },
    {
        icone: <FiShield aria-hidden="true" />,
        titulo: "Perfis e permissões",
        subtitulo: "O certo, para o certo",
        texto:
            "Perfis USER e ADMIN com permissões granulares: cada pessoa vê e executa apenas o que o seu perfil permite — validado no cliente e no servidor.",
    },
    {
        icone: <FiLock aria-hidden="true" />,
        titulo: "Conta segura",
        subtitulo: "JWT + código OTP",
        texto:
            "Sessões protegidas por token JWT e recuperação de password com código OTP enviado por e-mail, com expiração e limite de tentativas.",
    },
];

const PASSOS = [
    {
        titulo: "Crie a sua conta",
        texto: "Registe-se em segundos e receba de imediato o seu espaço pessoal na nuvem.",
    },
    {
        titulo: "Organize e carregue",
        texto: "Estruture pastas, carregue ficheiros e defina com quem partilha cada item.",
    },
    {
        titulo: "Aceda a qualquer lugar",
        texto: "Entre no seu dashboard de qualquer dispositivo, com as suas permissões sempre aplicadas.",
    },
];

const SEGURANCA = [
    "Autenticação com token JWT verificado em cada pedido ao servidor.",
    "Recuperação de password com código OTP por e-mail, com expiração e limite de tentativas.",
    "Perfis e permissões verificados no servidor — nada chega ao ecrã sem autorização.",
    "Painel de administração para gerir utilizadores, perfis e permissões.",
    "Lixeira e confirmação de exclusão: nada se apaga sem a sua confirmação.",
];

const FAQ = [
    {
        pergunta: "O que é o armazenamento híbrido?",
        resposta:
            "Pode guardar os seus ficheiros na nuvem do sistema ou mantê-los no seu próprio computador, escolhendo o melhor destino para cada ficheiro — sem perder o controlo do que está onde.",
    },
    {
        pergunta: "Os meus ficheiros são privados?",
        resposta:
            "Sim. Cada ficheiro e pasta está associado a um dono e a uma permissão; só quem tem o perfil e as permissões certas consegue aceder, e a verificação é feita também no servidor.",
    },
    {
        pergunta: "Esqueci-me da password, e agora?",
        resposta:
            "Na página «Esqueceu a sua password?» recebe um código OTP por e-mail para definir uma nova password com segurança, sem precisar de falar com ninguém.",
    },
    {
        pergunta: "Preciso de instalar alguma coisa?",
        resposta: "Não. O sistema funciona no navegador, em computador ou telemóvel, sem instalações.",
    },
];

const LandingPage = () => {
    const [menuAberto, setMenuAberto] = useState(false);
    const fecharMenu = () => setMenuAberto(false);
    const heroRef = useRef(null);

    // Efeito parallax no hero
    useEffect(() => {
        const handleScroll = () => {
            if (!heroRef.current) return;
            const scrollY = window.scrollY;
            const heroRect = heroRef.current.getBoundingClientRect();
            // Parallax speed: 0.3x (hero moves slower than scroll)
            const speed = 0.3;
            const offset = scrollY * speed;
            // Only apply if hero is in viewport
            if (heroRect.bottom >= 0 && heroRect.top <= window.innerHeight) {
                heroRef.current.style.transform = `translateY(${offset}px)`;
                // Set CSS variable for image parallax
                heroRef.current.style.setProperty('--parallax-offset', `${offset}px`);
            }
        };

        window.addEventListener("scroll", handleScroll, { passive: true });
        return () => window.removeEventListener("scroll", handleScroll);
    }, []);

    return (
        <div className="landing-page">
            <header>
                <nav className="landing-nav" aria-label="Navegação principal">
                    <a className="landing-logo" href="#topo" onClick={fecharMenu}>
                        <img src={logo} alt="Arquivo Inteligente" />
                    </a>

                    <button
                        type="button"
                        className="landing-menu-btn"
                        onClick={() => setMenuAberto((aberto) => !aberto)}
                        aria-expanded={menuAberto}
                        aria-controls="landing-menu"
                        aria-label={menuAberto ? "Fechar menu" : "Abrir menu"}
                    >
                        {menuAberto ? <FiX aria-hidden="true" /> : <FiMenu aria-hidden="true" />}
                    </button>

                    <div className={`landing-menu${menuAberto ? " aberto" : ""}`} id="landing-menu">
                        <ul className="landing-List">
                            {NAV_LINKS.map(({ href, label }) => (
                                <li key={href}>
                                    <a className="section" href={href} onClick={fecharMenu}>
                                        {label}
                                    </a>
                                </li>
                            ))}
                        </ul>

                        <div className="nav-botoes">
                            <Link to="/login" className="btn-login" onClick={fecharMenu}>
                                Login
                            </Link>
                            <Link to="/register" className="btn-cadastro" onClick={fecharMenu}>
                                Cadastrar
                            </Link>
                        </div>
                    </div>
                </nav>
            </header>


            <section className="hero" id="topo" ref={heroRef}>
                <img src={hero} alt="" className="hero-img" />

                <div className="hero-conteudo">
                    <p className="hero-destaque">Arquivo Inteligente</p>
                    <h1>Os Seus Dados À Distância De Um Clique</h1>

                    <p>
                        Guarde, organize e partilhe os seus ficheiros com a segurança de perfis e
                        permissões — na nossa nuvem ou no seu próprio computador. Sem peso no bolso,
                        tudo na nuvem!
                    </p>

                    <div className="hero-botoes">
                        <Link to="/register" className="btn-cadastro">
                            Criar conta
                        </Link>
                        <Link to="/login" className="btn-login">
                            Já tenho conta
                        </Link>
                    </div>
                </div>
            </section>

            <main className="main">
                <section className="recursos" id="recursos">
                    <h2>O Que O Sistema Oferece</h2>
                    <p className="recursos-subtitulo">
                        Armazenamento híbrido — tanto pela nossa nuvem como pelo seu PC de casa
                    </p>

                    <div className="cards-grid">
                        {RECURSOS.map(({ icone, titulo, subtitulo, texto }) => (
                            <Card
                                key={titulo}
                                icone={icone}
                                titulo={titulo}
                                subtitulo={subtitulo}
                                texto={texto}
                            />
                        ))}
                    </div>
                </section>

                <section className="como-funciona" id="como-funciona">
                    <h2>Como Funciona</h2>
                    <p className="sec-subtitulo">Três passos e a sua nuvem está pronta a usar</p>

                    <div className="passos">
                        {PASSOS.map(({ titulo, texto }, indice) => (
                            <div className="passo" key={titulo}>
                                <span className="passo-numero" aria-hidden="true">
                                    {indice + 1}
                                </span>
                                <h3>{titulo}</h3>
                                <p>{texto}</p>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="seguranca" id="seguranca">
                    <div className="seguranca-grid">
                        <div className="seguranca-texto">
                            <h2>Segurança E Controlo a Sério</h2>
                            <p>
                                Cada ficheiro tem um dono e uma permissão; cada sessão, um token. O
                                que não é seu simplesmente não aparece — no navegador ou no servidor.
                            </p>
                            <Link to="/register" className="btn-cadastro">
                                Começar agora <FiArrowRight aria-hidden="true" />
                            </Link>
                        </div>

                        <ul className="seguranca-lista">
                            {SEGURANCA.map((item) => (
                                <li key={item}>
                                    <span className="seguranca-check" aria-hidden="true">
                                        <FiCheck />
                                    </span>
                                    <span>{item}</span>
                                </li>
                            ))}
                        </ul>
                    </div>
                </section>

                <section className="faq" id="faq">
                    <h2>Perguntas Frequentes</h2>
                    <p className="sec-subtitulo">O essencial, sem rodeios</p>

                    <div className="faq-lista">
                        {FAQ.map(({ pergunta, resposta }) => (
                            <details className="faq-item" key={pergunta}>
                                <summary>{pergunta}</summary>
                                <p>{resposta}</p>
                            </details>
                        ))}
                    </div>
                </section>

                <section className="cta">
                    <div className="cta-caixa">
                        <h2>Comece Hoje</h2>
                        <p>
                            Crie a sua conta e tenha os seus ficheiros seguros, organizados e
                            sempre acessíveis.
                        </p>
                        <div className="hero-botoes">
                            <Link to="/register" className="btn-cadastro">
                                Criar conta grátis
                            </Link>
                            <Link to="/login" className="btn-login">
                                Entrar
                            </Link>
                        </div>
                    </div>
                </section>
            </main>

            <footer className="landing-rodape">
                <div className="rodape-topo">
                    <div className="rodape-marca">
                        <img src={logo} alt="" />
                        <div>
                            <strong>Arquivo Inteligente</strong>
                            <p>Os seus dados à distância de um clique.</p>
                        </div>
                    </div>

                    <nav className="rodape-links" aria-label="Produto">
                        <h3>Produto</h3>
                        <ul>
                            <li><a href="#recursos">Recursos</a></li>
                            <li><a href="#como-funciona">Como funciona</a></li>
                            <li><a href="#seguranca">Segurança</a></li>
                            <li><a href="#faq">FAQ</a></li>
                        </ul>
                    </nav>

                    <nav className="rodape-links" aria-label="Conta">
                        <h3>Conta</h3>
                        <ul>
                            <li><Link to="/login">Entrar</Link></li>
                            <li><Link to="/register">Criar conta</Link></li>
                            <li><Link to="/forgotpassword">Recuperar password</Link></li>
                        </ul>
                    </nav>
                </div>

                <p className="rodape-copy">
                    © 2026 Sistema Arquivo Nuvem — Todos os direitos reservados.
                </p>
            </footer>
        </div>
    );
};

export default LandingPage;


