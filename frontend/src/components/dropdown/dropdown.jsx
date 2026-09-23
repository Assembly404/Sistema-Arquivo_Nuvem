import { useEffect, useRef, useState } from 'react'
import './dropdown.css'
function Dropdown({
    options,
    selected,
    onChange,
    placeholder = 'Seleciona uma opcao'
}){
    const [isOpen, setIsOpen] = useState(false);
    const [searchItem, setSearItem] = useState('');
    const dropdownRef = useRef(null);

    useEffect(()=>{
        const handleClick = (event) =>{
            if(dropdownRef.current && !dropdownRef.current.contains(event.target)){
                setIsOpen(false)
            }
        }
        document.addEventListener('mousedown',handleClick);
        return ()=>{
            document.removeEventListener('mousedown',handleClick)
        }
    },[])

    const filteredOptions = options.filter((option) =>
        option.toLowerCase().includes(searchItem.toLowerCase())
    )

    const handleOptionClick = (option) =>{
        onChange(option)
        setIsOpen(false)
    }

    return(
        <>
            <div className="dropdown" ref={dropdownRef}>
                <div className="dropdown-header" onClick={()=> setIsOpen(!isOpen)}>
                    {selected || placeholder}
                    <span className={`dropdown-icon ${isOpen ? 'open': ''}`}>&#x25BC;</span>
                </div>
                {isOpen && (
                    <div className="dropdown-body">
                        <input
                        type="text"
                        className='dropdown-search'
                        value={searchItem}
                        onChange={(e)=>{ setSearItem(e.target.value)}}
                        
                        />
                    <div className="dropdown-options">
                        {filteredOptions.length > 0 ?(
                            filteredOptions.map((option, index) =>(
                                <div
                                    key={index}
                                    className={`dropdown-option ${option === selected ? 'selected':''}`}
                                    onClick={()=> handleOptionClick(option)}
                                >{option}</div>
                            ))

                        ):(
                            <div className="dropdown-no-option">No option found</div>
                        )}
                    </div>
                    </div>
                )}
            </div>
        </>
    )
}

export default Dropdown