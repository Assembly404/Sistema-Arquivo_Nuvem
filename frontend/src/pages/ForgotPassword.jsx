import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCircleCheck, faEnvelope, faKey, faLock } from "@fortawesome/free-solid-svg-icons";
import Input from "../components/form/input.jsx";
import Button from "../components/button/button.jsx";
import Toast from "../components/toast/toast.jsx";
import { forgotPassword, verifyOtp, resetPassword } from "../api/axios.js";
import "./ForgotPassword.css";
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
const PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%]).{8,24}$/;
const TOAST_DURATION = 4500;
const SUCCESS_DURATION = 1500;
const RESEND_COOLDOWN = 60;
const checks = (pwd) => ([
  { label: "8 a 24 caracteres", ok: pwd.length >= 8 && pwd.length <= 24 },
  { label: "Uma maiúscula", ok: /[A-Z]/.test(pwd) },
  { label: "Uma minúscula", ok: /[a-z]/.test(pwd) },
  { label: "Um número", ok: /[0-9]/.test(pwd) },
  { label: "Um símbolo (!@#$%)", ok: /[!@#$%]/.test(pwd) }
]);
function ForgotPassword() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState(["", "", "", "", "", ""]);
  const [resetToken, setResetToken] = useState("");
  const [pwd, setPwd] = useState("");
  const [confirm, setConfirm] = useState("");
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState(null);
  const [cooldown, setCooldown] = useState(0);
  const titleRef = useRef(null);
  const otpRefs = useRef([]);
  const timer = useRef(null);
  const closeToast = useCallback(() => setToast(null), []);
  const validEmail = EMAIL_REGEX.test(email);
  const emailError = email && !validEmail ? "Email deve conter @dominio.com" : "";
  const otpValue = otp.join("");
  const validPwd = PWD_REGEX.test(pwd);
  const pwdError = pwd && !validPwd ? "8 a 24 caracteres, com maiúscula, minúscula, número e símbolo." : "";
  const matchError = confirm && pwd !== confirm ? "As passwords não coincidem." : "";
  useEffect(() => { titleRef.current?.focus(); }, [step]);
  useEffect(() => () => clearInterval(timer.current), []);
  useEffect(() => {
    if (cooldown <= 0) return;
    timer.current = setInterval(() => setCooldown((c) => (c <= 1 ? 0 : c - 1)), 1000);
    return () => clearInterval(timer.current);
  }, [cooldown]);
  const startCooldown = () => setCooldown(RESEND_COOLDOWN);
  const handleForgot = async (e) => {
    e?.preventDefault();
    setLoading(true);
    try {
      await forgotPassword({ email });
      setToast({ message: "Se existir conta, enviámos um código.", type: "success" });
      setStep(2); startCooldown();
      setTimeout(() => otpRefs.current[0]?.focus(), 100);
    } catch (err) {
      console.error("[forgot] falha ao solicitar", err);
      setToast({ message: "Não foi possível enviar o código.", type: "error" });
    } finally { setLoading(false); }
  };
  const handleOtpChange = (i, v) => {
    const d = v.replace(/\D/g, "").slice(-1);
    setOtp((prev) => { const n = [...prev]; n[i] = d; return n; });
    if (d && i < 5) otpRefs.current[i + 1]?.focus();
  };
  const handleOtpKey = (i, e) => {
    if (e.key === "Backspace" && !otp[i] && i > 0) otpRefs.current[i - 1]?.focus();
  };
  const handleOtpPaste = (e) => {
    const t = (e.clipboardData.getData("text") || "").replace(/\D/g, "").slice(0, 6).split("");
    if (!t.length) return;
    e.preventDefault();
    setOtp((prev) => prev.map((_, i) => t[i] || ""));
    otpRefs.current[Math.min(t.length, 5)]?.focus();
  };
  const handleVerify = async (e) => {
    e?.preventDefault();
    setLoading(true);
    try {
      const res = await verifyOtp({ email, otp: otpValue });
      setResetToken(res.data?.resetToken || "");
      setToast({ message: "Código verificado.", type: "success" });
      setTimeout(() => setStep(3), SUCCESS_DURATION);
    } catch (err) {
      console.error("[forgot] otp inválido", err);
      const msg = err?.response?.status === 410 ? "Código expirado. Peça um novo." : (err?.response?.data?.message || "Código inválido.");
      setToast({ message: msg, type: "error" });
    } finally { setLoading(false); }
  };
  const handleReset = async (e) => {
    e?.preventDefault();
    setLoading(true);
    try {
      await resetPassword({ resetToken, password: pwd });
      setToast({ message: "Password reposta com sucesso.", type: "success" });
      setTimeout(() => setStep(4), SUCCESS_DURATION);
    } catch (err) {
      console.error("[forgot] falha no reset", err);
      setToast({ message: err?.response?.data?.message || "Não foi possível repor.", type: "error" });
    } finally { setLoading(false); }
  };
  return (
    <>
      <section className="resetPage">
        <div className="reset-card">
          {step === 1 && (<>
            <div className="reset-badge"><FontAwesomeIcon icon={faKey} aria-hidden="true" /></div>
            <h1 className="reset-title" ref={titleRef} tabIndex={-1}>Forgot password?</h1>
            <p className="reset-subtitle">No worries, we will send you reset instructions.</p>
            <form onSubmit={handleForgot} className="reset-form">
              <div className="input_container"><label htmlFor="rp-email">Email</label>
              <Input id="rp-email" type="email" placeholder="O teu email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" errorMessage={emailError} /></div>
              <Button label="Enviar código" variant="primary" type="submit" loading={loading} disabled={!validEmail} />
            </form>
            <Link className="reset-back" to="/login">← Back to log in</Link>
          </>)}
          {step === 2 && (<>
            <div className="reset-badge"><FontAwesomeIcon icon={faEnvelope} aria-hidden="true" /></div>
            <h1 className="reset-title" ref={titleRef} tabIndex={-1}>Check your email</h1>
            <p className="reset-subtitle">Enviámos um código de 6 dígitos para <strong>{email}</strong></p>
            <form onSubmit={handleVerify} className="reset-form">
              <div className="otp-row" onPaste={handleOtpPaste}>
                {otp.map((d, i) => (<input key={i} ref={(el) => (otpRefs.current[i] = el)} className="otp-box" inputMode="numeric" maxLength={1} value={d} aria-label={`Dígito ${i + 1}`} onChange={(e) => handleOtpChange(i, e.target.value)} onKeyDown={(e) => handleOtpKey(i, e)} />))}
              </div>
              <Button label="Verificar código" variant="primary" type="submit" loading={loading} disabled={otpValue.length !== 6} />
            </form>
            <p className="reset-subtitle">{cooldown > 0 ? `Reenviar em ${cooldown}s` : (<>Didn&apos;t receive it? <button type="button" className="link-inline" onClick={handleForgot} disabled={loading}>Click to resend</button></>)}</p>
            <p><button type="button" className="link-inline" onClick={() => setStep(1)}>Usar outro email</button></p>
            <Link className="reset-back" to="/login">← Back to log in</Link>
          </>)}
          {step === 3 && (<>
            <div className="reset-badge"><FontAwesomeIcon icon={faLock} aria-hidden="true" /></div>
            <h1 className="reset-title" ref={titleRef} tabIndex={-1}>Set new password</h1>
            <p className="reset-subtitle">Your new password must be different to previously used passwords.</p>
            <form onSubmit={handleReset} className="reset-form">
              <div className="input_container"><label htmlFor="rp-pwd">Password</label>
              <Input id="rp-pwd" type="password" value={pwd} onChange={(e) => setPwd(e.target.value)} required autoComplete="new-password" errorMessage={pwdError} /></div>
              <div className="input_container"><label htmlFor="rp-confirm">Confirm password</label>
              <Input id="rp-confirm" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required autoComplete="new-password" errorMessage={matchError} /></div>
              <ul className="pwd-checks" aria-live="polite">{checks(pwd).map((c) => (<li key={c.label} className={c.ok ? "pwd-check--ok" : ""}>{c.ok ? "✓" : "•"} {c.label}</li>))}</ul>
              <Button label="Reset password" variant="primary" type="submit" loading={loading} disabled={!validPwd || pwd !== confirm} />
            </form>
            <Link className="reset-back" to="/login">← Back to log in</Link>
          </>)}
          {step === 4 && (<>
            <div className="reset-badge"><FontAwesomeIcon icon={faCircleCheck} aria-hidden="true" /></div>
            <h1 className="reset-title" ref={titleRef} tabIndex={-1}>Password reset</h1>
            <p className="reset-subtitle">Your password has been successfully reset. Click below to log in magically.</p>
            <Button label="Continue" variant="primary" onClick={() => navigate("/login")} />
            <Link className="reset-back" to="/login">← Back to log in</Link>
          </>)}
        </div>
      </section>
      {toast && <Toast key={toast.message} message={toast.message} type={toast.type} duration={toast.type === "success" ? SUCCESS_DURATION : TOAST_DURATION} onClose={closeToast} />}
    </>
  );
}
export default ForgotPassword
