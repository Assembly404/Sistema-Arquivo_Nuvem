import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCheck, faXmark, faInfoCircle } from "@fortawesome/free-solid-svg-icons";
import './input.css'


function Input({
    type,
    placeholder,
    id,
    value,
    onChange,
    errorMessage,
    customSytles,
    icon,
    ref,
    autoComplete,
    required,
    // aria_invalid,
    // aria_describedby,
    onFocus,
    onBlur
}){

    const errorID = id ? `${id}-error` : undefined;

    return(
        <>

            
            <div className='input_container' style={customSytles}>
                
                {icon && <span className='inputIcon'>{icon}</span>}
                <input 
                type={type}
                placeholder={placeholder}
                id={id}
                value={value}
                ref={ref}
                autoComplete={autoComplete}
                onChange={onChange}
                required={required}
                aria-invalid={errorMessage ? "true" : "false"}
                aria-describedby={errorMessage ? errorID : undefined}
                onFocus={onFocus}
                onBlur={onBlur}
                className={`inputField ${errorMessage ? 'inputError' : ''}`}

                />
                {errorMessage && (
                    <>
                        <span id={errorID} className='errorText'>
                            <FontAwesomeIcon className="icon-info" icon={faInfoCircle} />
                            {errorMessage}
                        </span>

                    </>
                    
                )}
            </div>
        </>
    )
}

export default Input;
