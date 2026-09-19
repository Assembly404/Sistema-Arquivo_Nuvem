import { useContext, createContext, useCallback, useState,} from "react";
import Toast from "../components/toast/toast";

const ToastContext = createContext(null);

export default function ToastProvider({children}){   
    const [toast, setToast] = useState(null);

    const showToast = useCallback((message, type='sucess', duration = 3000)=>{
        setToast(message,type,duration);

    },[]);
    const closeToast = useCallback(()=>{ setToast(null)},[])

    return(
        <ToastContext.Provider value={{ showToast }}>
            {children}
            {toast && (
                <Toast
                    key={toast.id}
                    message={toast.message}
                    type={toast.type}
                    duration={toast.duration}
                    onClose={closeToast}
                />
            )}
        </ToastContext.Provider>
    )
}

export const useToast = () => useContext(ToastContext) ;