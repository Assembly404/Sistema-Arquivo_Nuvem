/* Card de feature da landing page (§5.5 — superfície + marca).
   Recebe: icone (node), titulo, subtitulo, texto. */
import "./card.css";

const Card = ({ icone, titulo, subtitulo, texto }) => {
    return (
        <article className="landing-card">
            <div className="landing-card__icone">{icone}</div>

            <h3 className="landing-card__titulo">{titulo}</h3>
            <p className="landing-card__subtitulo">{subtitulo}</p>
            <p className="landing-card__texto">{texto}</p>
        </article>
    );
};

export default Card;
