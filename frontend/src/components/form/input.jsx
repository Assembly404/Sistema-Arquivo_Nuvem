import './input.css'


function Input({
    type,
    placeholder,
    value,
    onChange,
    errorMessage,
    customSytles,
    icon
}){
    return(
        <>
            <div className='input_container' style={customSytles}>
                {icon && <span className='inputIcon'>{icon}</span>}
                <input 
                type={type}
                placeholder={placeholder}
                value={value}
                onChange={onChange}
                className={`inputField ${errorMessage ? 'inputError' : ''}`}

                />
                {errorMessage && <span className='errorText'>{errorMessage}</span>}
            </div>
        </>
    )
}

export default Input;
