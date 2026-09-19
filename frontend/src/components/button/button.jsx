import Spinner from '../utils/spinner'
import './button.css'

function Button({
    label,
    onClick,
    variant,
    customStyles,
    customClass,
    icon,
    loading,
    type,
    disabled
}){
    return(
        <>
            <button
                className={`btn btn-${variant} ${customClass}`}
                style={customStyles}
                onClick={onClick}
                type={type}
                disabled={disabled}
            >
                {loading ? (
                    <Spinner />
                ): (
                    <>
                        {icon && <span className='icon'>{icon}</span>}
                        {label}
                    </>
                )}
            
            </button>    
        </>
    )
}

export default Button