import Spinner from '../utils/spinner'
import './button.css'

/* "secundary" (typo legado) continua aceite como alias de "secondary" (§4.1) */
const VARIANT_ALIASES = {
    secundary: 'secondary'
}

function Button({
    label,
    onClick,
    variant = 'primary',
    customStyles,
    customClass,
    icon,
    loading = false,
    type = 'button',
    disabled = false
}){
    const classes = [
        'btn',
        `btn-${VARIANT_ALIASES[variant] || variant}`,
        loading ? 'is-loading' : '',
        customClass
    ].filter(Boolean).join(' ')

    /* Durante o loading: .btn-label fica visibility:hidden (§4.1), por isso o
       aria-label mantém o nome acessível e o aria-busy anuncia o estado (§3.5). */
    return(
        <button
            className={classes}
            style={customStyles}
            onClick={onClick}
            type={type}
            disabled={disabled || loading}
            aria-busy={loading ? 'true' : undefined}
            aria-label={loading ? `${label}, a processar` : undefined}
        >
            <span className='btn-label'>
                {icon && <span className='icon'>{icon}</span>}
                {label}
            </span>

            <span className='spinner-slot' aria-hidden='true'>
                {loading && <Spinner />}
            </span>
        </button>
    )
}

export default Button
