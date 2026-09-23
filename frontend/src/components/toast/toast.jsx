
import { useEffect, useEffectEvent, useRef} from "react"
import styled from 'styled-components'


const ICONS = {
    success: "M8.5 11.5 11 14l4-4m6 2a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z",
    error: "M6 18 17.94 6M18 18 6.06 6",
};
function Toast({
    message,
    type,
    onClose,
    duration
}){

  const onCloseRef = useRef(onClose);

  useEffect(()=>{
    onCloseRef.current = onClose();
  },[onClose])

  useEffect(()=>{
    const timer = setTimeout(()=> onCloseRef.current(), duration)
    return () => clearTimeout(timer)
  },[duration])
    // useEffect(()=>{
    //     const timer = setTimeout(()=>{
    //         onClose();
    //     },duration || 3000)
    //     return ()=> clearTimeout(timer)
    // },[duration, onClose]);

    return(
        <>
            <StyledWrapper>
                <div className="notification-container">
                    <div className={`notification-item ${type}`}>
                        <div className="notification-content">
                            <div className="notification-icon">
                                <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                                    <path
                                    stroke="currentColor"
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d={ICONS[type] || ICONS.success}
                                    ></path>
                                </svg>
                            </div>
                            <div className="notification-text">{message}</div>
                        </div>
                        <button onClick={onClose} className="notification-icon notification-close">
                            <svg aria-hidden="true" fill="none" viewBox="0 0 24 24">
                                <path stroke="currentColor" strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18 17.94 6M18 18 6.06 6" />
                            </svg>
                        </button>
                        <div className="notification-progress-bar" />
                        style={{ animationDuration: `${duration}ms` }}
                    </div>
                </div>
            </StyledWrapper>
            {/* <div className={`toast ${type} `}>
                {message}
                <button className="toastClose" onClick={onClose}>x</button>
            </div> */}
        </>   
    )
}

const StyledWrapper = styled.div`
  /* Notification container */

  .notification-container {
    

    --content-color: black;
    --background-color: #f3f3f3;
    --font-size-content: 0.75em;
    --icon-size: 1em;
    position: fixed;
    top: 20px;        
    right: 20px;       
    color: #fff;
    padding: 15px 20px;
    border-radius: 5px;
    z-index: 1000;   
        
  }

  /* Notification Item */

  .notification-item {
    position: relative;
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-direction: row;
    gap: 1em;
    overflow: hidden;
    padding: 10px 15px;
    border-radius: 6px;
    box-shadow: rgba(111, 111, 111, 0.2) 0px 8px 24px;
    background-color: var(--background-color);
    transition: all 250ms ease;

    /* Background Pattern */

    --grid-color: rgba(225, 225, 225, 0.7);
    background-image: linear-gradient(
        0deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      ),
      linear-gradient(
        90deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      );
    background-size: 55px 55px;
  }

  .notification-item svg {
    transition: 250ms ease;
  }

  .notification-item:hover {
    transform: scale(1.01);
  }

  .notification-item:active {
    transform: scale(1.05);
  }

  .notification-item .notification-close:hover {
    background-color: rgba(204, 204, 204, 0.45);
  }

  .notification-item .notification-close:hover svg {
    color: rgb(0, 0, 0);
  }

  .notification-item .notification-close:active svg {
    transform: scale(1.1);
  }

  /* Notification Icons */

  .notification-item .notification-close {
    padding: 2px;
    border-radius: 5px;
    transition: all 250ms;
  }

  .notification-container svg {
    width: var(--icon-size);
    height: var(--icon-size);
    color: var(--content-color);
  }

  .notification-icon {
    display: flex;
    align-items: center;
  }

  /* Success */

  .success {
    color: #047857;
    background-color: #7dffbc;
    --grid-color: rgba(16, 185, 129, 0.25);
    background-image: linear-gradient(
        0deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      ),
      linear-gradient(
        90deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      );
  }

  .success svg {
    color: #047857;
  }

  .success .notification-progress-bar {
    background-color: #047857;
  }

  .success:hover {
    background-color: #5bffaa;
  }

  /* Info */

  .info {
    color: #1e3a8a;
    background-color: #7eb8ff;
    --grid-color: rgba(59, 131, 246, 0.25);
    background-image: linear-gradient(
        0deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      ),
      linear-gradient(
        90deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      );
  }

  .info svg {
    color: #1e3a8a;
  }

  .info .notification-progress-bar {
    background-color: #1e3a8a;
  }

  .info:hover {
    background-color: #5ba5ff;
  }

  /* Warning */

  .warning {
    color: #78350f;
    background-color: #ffe57e;
    --grid-color: rgba(245, 159, 11, 0.25);
    background-image: linear-gradient(
        0deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      ),
      linear-gradient(
        90deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      );
  }

  .warning svg {
    color: #78350f;
  }

  .warning .notification-progress-bar {
    background-color: #78350f;
  }

  .warning:hover {
    background-color: #ffde59;
  }

  /* Error */

  .error {
    color: #7f1d1d;
    background-color: #ff7e7e;
    --grid-color: rgba(239, 68, 68, 0.25);
    background-image: linear-gradient(
        0deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      ),
      linear-gradient(
        90deg,
        transparent 23%,
        var(--grid-color) 24%,
        var(--grid-color) 25%,
        transparent 26%,
        transparent 73%,
        var(--grid-color) 74%,
        var(--grid-color) 75%,
        transparent 76%,
        transparent
      );
  }

  .error svg {
    color: #7f1d1d;
  }

  .error .notification-progress-bar {
    background-color: #7f1d1d;
  }

  .error:hover {
    background-color: #ff5f5f;
  }

  /* Notification content */

  .notification-content {
    display: flex;
    justify-content: flex-start;
    align-items: center;
    gap: 0.5em;
  }

  .notification-text {
    font-size: var(--font-size-content);
    user-select: none;
  }

  .notification-close {
    cursor: pointer;
  }

  /* Notification progress bar */

  .notification-progress-bar {
    position: absolute;
    bottom: 0;
    left: 0;
    height: 1px;
    background: var(--content-color);
    width: 100%;
    transform: translateX(100%);

    /* Remove the infinite property for your website */

    animation: progressBar 5s linear forwards ;
  }

  /* progressBar Animation */

  @keyframes progressBar {
    0% {
      transform: translateX(0);
    }

    100% {
      transform: translateX(-100%);
    }
  }`;

export default Toast