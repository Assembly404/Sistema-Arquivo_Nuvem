import { FiAlertCircle, FiHelpCircle, FiMail, FiMessageCircle } from "react-icons/fi";
import "./Support.css";

function Support() {
    return (
        <section className="dashboard-page support-page">
            <h1 className="dashboard-page__title">Suporte</h1>
            <p className="dashboard-page__text">Precisas de ajuda? Estamos cá para isso.</p>

            <div className="support-grid">
                <div className="card support-card">
                    <h2 className="card__title"><FiHelpCircle aria-hidden="true" /> Central de ajuda</h2>
                    <p className="dashboard-page__text">
                        Consulta o guia de início rápido e as perguntas frequentes sobre a plataforma
                        na página <a href="/dashboard/arquivos">Arquivos</a> ou nos documento de requisitos.
                    </p>
                </div>

                <div className="card support-card">
                    <h2 className="card__title"><FiMail aria-hidden="true" /> Email</h2>
                    <p className="dashboard-page__text">
                        Envia-nos um email para <strong>suporte@arquivonuvem.local</strong> — respondemos em até 1 dia útil.
                    </p>
                </div>

                <div className="card support-card">
                    <h2 className="card__title"><FiMessageCircle aria-hidden="true" /> Reportar problema</h2>
                    <p className="dashboard-page__text">
                        Se encontraste um erro, descreve o que aconteceu e partilha connosco o máximo de contexto
                        (ecrã, navegador, hora aproximada).
                    </p>
                </div>

                <div className="card support-card">
                    <h2 className="card__title"><FiAlertCircle aria-hidden="true" /> Estado do serviço</h2>
                    <p className="dashboard-page__text">
                        O estado dos teus dispositivos pessoais pode ser consultado em{' '}
                        <a href="/dashboard/armazenamento">Armazenamento</a>.
                    </p>
                </div>
            </div>
        </section>
    );
}

export default Support;