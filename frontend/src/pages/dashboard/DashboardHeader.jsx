import { useContext, useEffect, useRef, useState } from "react";
import { FiHelpCircle, FiLogOut, FiSettings, FiUser } from "react-icons/fi";
import { Link, useNavigate, useSearchParams } from "react-router";
import AuthContext from "../../context/AuthProvider.jsx";
import Logo from "../../assets/logo.png";
import "./DashboardHeader.css";

const SEARCH_DEBOUNCE = 350;

function DashboardHeader({ onLogout }) {
    const { auth } = useContext(AuthContext);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [query, setQuery] = useState(searchParams.get("q") ?? "");
    const [syncedQuery, setSyncedQuery] = useState(() => searchParams.get("q") ?? "");
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef(null);

    const name = auth?.user?.name && auth?.user?.surname
        ? `${auth.user.name} ${auth.user.surname}`
        : auth?.user?.name || auth?.user?.email?.split("@")[0] || "Utilizador";
    const initials = name
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0))
        .join("")
        .toUpperCase() || "U";

    const urlQuery = searchParams.get("q") ?? "";
    if (urlQuery !== syncedQuery) {
        setSyncedQuery(urlQuery);
        setQuery(urlQuery);
    }

    useEffect(() => {
        const term = query.trim();
        if (term === urlQuery) return undefined;
        const timer = window.setTimeout(() => {
            const target = term
                ? `/dashboard/arquivos?q=${encodeURIComponent(term)}`
                : "/dashboard/arquivos";
            navigate(target);
        }, SEARCH_DEBOUNCE);
        return () => window.clearTimeout(timer);
    }, [query, urlQuery, navigate]);

    useEffect(() => {
        if (!menuOpen) return undefined;
        const handlePointerDown = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) setMenuOpen(false);
        };
        const handleKeyDown = (event) => {
            if (event.key === "Escape") setMenuOpen(false);
        };
        document.addEventListener("mousedown", handlePointerDown);
        document.addEventListener("keydown", handleKeyDown);
        return () => {
            document.removeEventListener("mousedown", handlePointerDown);
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [menuOpen]);

    return (
        <header className="dash-header">
            <div className="dash-header__brand" aria-hidden="true">
                <img src={Logo} alt="" />
                <span>Arquivo Nuvem</span>
            </div>

            <label className="dash-header__search">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15.5 14h-.8l-.3-.3a6.5 6.5 0 1 0-.7.7l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9z" /></svg>
                <input
                    type="search"
                    placeholder="Pesquisar no Arquivo Nuvem"
                    aria-label="Pesquisar no Arquivo Nuvem"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                />
            </label>

            <div className="dash-header__spacer" />

            <Link
                to="/dashboard/configuracoes"
                className="dash-header__icon"
                aria-label="Configurações"
                title="Configurações"
            >
                <FiSettings aria-hidden="true" />
            </Link>

            <Link
                to="/dashboard/suporte"
                className="dash-header__icon"
                aria-label="Suporte"
                title="Suporte"
            >
                <FiHelpCircle aria-hidden="true" />
            </Link>

            <div className="dash-header__account" ref={menuRef}>
                <button
                    type="button"
                    className="dash-header__avatar"
                    onClick={() => setMenuOpen((open) => !open)}
                    aria-expanded={menuOpen}
                    aria-haspopup="menu"
                    aria-label={`Conta de ${name}`}
                >
                    {initials}
                </button>

                {menuOpen && (
                    <ul className="dash-header__menu" role="menu">
                        <li className="dash-header__menu-head">
                            <span className="dash-header__menu-name">{name}</span>
                            <span className="dash-header__menu-email">{auth?.user?.email || ""}</span>
                        </li>
                        <li>
                            <Link to="/dashboard/perfil" className="dash-header__menu-item" role="menuitem" onClick={() => setMenuOpen(false)}>
                                <span className="dash-header__menu-icon"><FiUser aria-hidden="true" /></span>
                                Perfil
                            </Link>
                        </li>
                        <li>
                            <button
                                type="button"
                                className="dash-header__menu-item dash-header__menu-item--danger"
                                role="menuitem"
                                onClick={() => {
                                    setMenuOpen(false);
                                    onLogout?.();
                                }}
                            >
                                <span className="dash-header__menu-icon"><FiLogOut aria-hidden="true" /></span>
                                Terminar sessão
                            </button>
                        </li>
                    </ul>
                )}
            </div>
        </header>
    );
}

export default DashboardHeader;