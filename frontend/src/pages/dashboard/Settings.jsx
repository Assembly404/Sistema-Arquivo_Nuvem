import { useCallback, useContext, useEffect, useState } from "react";
import { FiBell, FiHardDrive, FiShield } from "react-icons/fi";
import AuthContext from "../../context/AuthProvider.jsx";
import Toast from "../../components/toast/toast.jsx";
import { getStoragePolicy, setAuthToken, setStoragePolicy } from "../../api/axios.js";
import "./Settings.css";

const TOAST_DURATION = 4500;

const POLICY_OPTIONS = [
    { value: "SYSTEM_FIRST", label: "Nuvem do sistema primeiro", hint: "Os carregamentos vão para a nuvem do sistema, exceto quando escolhes outro destino." },
    { value: "DEVICE_FIRST", label: "Dispositivo pessoal primeiro", hint: "Se tiveres um dispositivo online, os carregamentos usam-no antes da nuvem do sistema." },
    { value: "MANUAL", label: "Escolher manualmente no upload", hint: "O sistema pede-te para escolher o destino em cada carregamento." }
];

function Settings() {
    const { auth } = useContext(AuthContext);
    const [policy, setPolicy] = useState("SYSTEM_FIRST");
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState(null);
    const closeToast = useCallback(() => setToast(null), []);

    useEffect(() => { if (auth?.token) setAuthToken(auth.token); }, [auth?.token]);

    useEffect(() => {
        if (!auth?.token) return undefined;
        getStoragePolicy()
            .then((res) => { if (res.data?.policy) setPolicy(res.data.policy); })
            .catch((err) => console.error("[settings] falha ao carregar política", err));
        return undefined;
    }, [auth?.token]);

    const handleSave = async () => {
        setSaving(true);
        try {
            await setStoragePolicy({ policy });
            setToast({ message: "Configurações guardadas.", type: "success" });
        } catch (err) {
            setToast({ message: err?.response?.data?.message || "Não foi possível guardar.", type: "error" });
        } finally {
            setSaving(false);
        }
    };

    return (
        <section className="dashboard-page settings-page">
            <h1 className="dashboard-page__title">Configurações</h1>
            <p className="dashboard-page__text">Preferências da tua conta e do teu espaço.</p>

            <div className="card settings-card">
                <h2 className="card__title"><FiHardDrive aria-hidden="true" /> Política de armazenamento</h2>
                <p className="dashboard-page__text">
                    Define o destino preferido por omissão nos carregamentos de ficheiros.
                </p>
                <div className="settings-options" role="radiogroup" aria-label="Política de armazenamento">
                    {POLICY_OPTIONS.map((opt) => (
                        <label key={opt.value} className={`settings-option${policy === opt.value ? " settings-option--active" : ""}`}>
                            <input
                                type="radio"
                                name="storage-policy"
                                value={opt.value}
                                checked={policy === opt.value}
                                onChange={() => setPolicy(opt.value)}
                            />
                            <span className="settings-option__label">{opt.label}</span>
                            <span className="settings-option__hint">{opt.hint}</span>
                        </label>
                    ))}
                </div>
                <div>
                    <button type="button" className="btn btn-primary" onClick={handleSave} disabled={saving}>
                        {saving ? "A guardar..." : "Guardar preferências"}
                    </button>
                </div>
            </div>

            <div className="card settings-card">
                <h2 className="card__title"><FiShield aria-hidden="true" /> Segurança</h2>
                <p className="dashboard-page__text">
                    Gere a tua password e os teus dados pessoais na página{' '}
                    <a href="/dashboard/perfil">Perfil</a>.
                </p>
            </div>

            <div className="card settings-card">
                <h2 className="card__title"><FiBell aria-hidden="true" /> Notificações</h2>
                <p className="dashboard-page__text">
                    Recebe alertas sobre partilhas, dispositivos offline e espaço próximo do limite.
                    A gestão detalhada estará disponível em breve.
                </p>
            </div>

            {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={TOAST_DURATION} onClose={closeToast} />}
        </section>
    );
}

export default Settings;