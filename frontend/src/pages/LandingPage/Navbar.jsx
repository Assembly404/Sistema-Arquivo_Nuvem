import "./navbar.css"
import logo from "../../assets/logo.png"

const Navbar = () => {
  return (
    <div className="nav-container">

      <div className="logo">
        <img src={logo} alt="Logo" />
      </div>

      <div className="items">
        <nav>
          <ul>
            <li><a href="#">section1</a></li>
            <li><a href="#">section2</a></li>
            <li><a href="#">section3</a></li>
            <li><a href="#">section4</a></li>
          </ul>

          <div className="nav-botoes">
            <a href="#" className="btn-login">Login</a>
            <a href="#" className="btn-cadastro">Cadastrar</a>
          </div>
        </nav>
      </div>

    </div>
  )
}

export default Navbar