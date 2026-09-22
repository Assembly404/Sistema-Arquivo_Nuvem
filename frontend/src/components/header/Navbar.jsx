import "./navbar.css"
import logo from "../../assets/logo.png"
import  {Link} from "react-router"

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

          <div className="nav-botoes">.
            <Link to={"/login"}>
              <div className="btn-login">Login</div>
            </Link>
            <Link to={"/register"}>
              <div className="btn-cadastro">Cadastrar</div>
            </Link>
          </div>
        </nav>
      </div>

    </div>
  )
}

export default Navbar