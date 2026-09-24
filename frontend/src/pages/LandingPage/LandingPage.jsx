import "./LandingPage.css"
import hero from "../../images/hero.jpg"

const LandingPage = () => {
  return (
    <>
      <header>
        <nav>
          <ul>
            <li><a href="#recursos">Section 1</a></li>
            <li><a href="#">Section 2</a></li>
            <li><a href="#">Section 3</a></li>
            <li><a href="#">Section 4</a></li>
          </ul>

          <div className="nav-botoes">
            <a href="#" className="btn-login">
              Login
            </a>

            <a href="#" className="btn-cadastro">
              Cadastrar
            </a>
          </div>
        </nav>
      </header>

      <section className="hero">
        <img src={hero} alt=""
          
          className="hero-img"
        />

        <div className="hero-conteudo">
          <h1>Card Title</h1>

          <p>
            Some quick example text to build on the card title and make up
            the bulk of the card’s content.
          </p>

          <div className="hero-botoes">
            <a href="#" className="btn-cadastro">
              Button 1
            </a>

            <a href="#" className="btn-login">
              Button 2
            </a>
          </div>
        </div>
      </section>

      <main>
        <section className="recursos" id="recursos">
          <h2>Armazenamento híbrido</h2>

          <p className="recursos-subtitulo">
            Tanto pela nossa nuvem ou pelo seu PC de casa
          </p>

          <div className="cards-grid">

            <div className="card">
              <div className="card-body">

                <div className="card-icone">
                  <img src="" alt="" />
                </div>

                <h3 className="card-title">
                  Card title
                </h3>

                <h6 className="card-subtitle">
                  Card subtitle
                </h6>

                <p className="card-text">
                  Some quick example text to build on the card title
                  and make up the bulk of the card’s content.
                </p>

              </div>
            </div>

            <div className="card">
              <div className="card-body">

                <div className="card-icone">
                  <i className="fa-solid fa-lock"></i>
                </div>

                <h3 className="card-title">
                  Card title
                </h3>

                <h6 className="card-subtitle">
                  Card subtitle
                </h6>

                <p className="card-text">
                  Some quick example text to build on the card title
                  and make up the bulk of the card’s content.
                </p>

              </div>
            </div>

            <div className="card">
              <div className="card-body">

                <div className="card-icone">
                  <i className="fa-solid fa-server"></i>
                </div>

                <h3 className="card-title">
                  Card title
                </h3>

                <h6 className="card-subtitle">
                  Card subtitle
                </h6>

                <p className="card-text">
                  Some quick example text to build on the card title
                  and make up the bulk of the card’s content.
                </p>

              </div>
            </div>

          </div>
        </section>
      </main>
    </>
  );
};

export default LandingPage;