import { useEffect, useRef } from "react";
import "./toast.css";

const ICONS = {
    success: "M8.5 11.5 11 14l4-4m6 2a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z",
    error: "M6 18 17.94 6M18 18 6.06 6",
};

const DEFAULT_DURATION = 4500;

function Toast({
    message,
    type = "success",
    onClose,
    duration = DEFAULT_DURATION
}) {

    const onCloseRef = useRef(onClose);

    /* Mantém a referência mais recente sem reiniciar o temporizador
       (o bug legado invocava onClose() no mount) */
    useEffect(() => {
        onCloseRef.current = onClose;
    }, [onClose]);

    /* O temporizador reinicia quando muda a mensagem (§4.3: 3000–5000ms) */
    useEffect(() => {
        if (!message) return undefined;

        const timer = setTimeout(() => onCloseRef.current?.(), duration);
        return () => clearTimeout(timer);
    }, [duration, message]);

    const isError = type === "error";

    return (
        <div className="toast-container">
            <div
                className={`toast toast--${type}`}
                role={isError ? "alert" : "status"}
            >
                <span className="toast__icon" aria-hidden="true">
                    <svg fill="none" viewBox="0 0 24 24">
                        <path
                            stroke="currentColor"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d={ICONS[type] || ICONS.success}
                        />
                    </svg>
                </span>

                <span className="toast__text">{message}</span>

                <button
                    type="button"
                    className="toast__close"
                    onClick={() => onCloseRef.current?.()}
                    aria-label="Fechar notificação"
                >
                    <svg fill="none" viewBox="0 0 24 24" aria-hidden="true">
                        <path
                            stroke="currentColor"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth="2"
                            d={ICONS.error}
                        />
                    </svg>
                </button>

                <span
                    className="toast__progress"
                    style={{ animationDuration: `${duration}ms` }}
                    aria-hidden="true"
                />
            </div>
        </div>
    );
}

export default Toast
