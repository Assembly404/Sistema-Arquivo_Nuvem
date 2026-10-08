import { Routes, Route } from 'react-router'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import NotFound from './pages/NotFound.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import RequireAuth, { RequireRole } from './components/utils/RequireAuth.jsx'
import DashboardLayout from './pages/dashboard/DashboardLayout.jsx'
import Dashboard from './pages/dashboard/Dashboard.jsx'
import Profile from './pages/dashboard/Profile.jsx'
import Placeholder from './pages/dashboard/Placeholder.jsx'
import AdminUsers from './pages/dashboard/AdminUsers.jsx'
import './App.css';

import LandingPage from './pages/LandingPage/LandingPage.jsx'





function App(){ 

  return(
    <Routes>
      <Route path='/' element={<LandingPage/>}/>
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register />}/>
      <Route path='/forgotpassword' element={<ForgotPassword />}/>

      <Route path='/dashboard' element={<RequireAuth><DashboardLayout /></RequireAuth>}>
        <Route index element={<Dashboard/>}/>
        <Route path='arquivos' element={<Placeholder title="Meus Arquivos"/>}/>
        <Route path='arquivos/compartilhados' element={<Placeholder title="Compartilhados comigo"/>}/>
        <Route path='arquivos/lixeira' element={<Placeholder title="Lixeira"/>}/>
        <Route path='armazenamento' element={<Placeholder title="Armazenamento"/>}/>
        <Route path='admin/usuarios' element={<RequireRole role="ADMIN"><AdminUsers/></RequireRole>}/>
        <Route path='admin/permissoes' element={<RequireRole role="ADMIN"><Placeholder title="Perfis & Permissões"/></RequireRole>}/>
        <Route path='notificacoes' element={<Placeholder title="Notificações"/>}/>
        <Route path='suporte' element={<Placeholder title="Suporte"/>}/>
        <Route path='configuracoes' element={<Placeholder title="Configurações"/>}/>
        <Route path='perfil' element={<Profile/>}/>
      </Route>


      <Route path='*' element={<NotFound/>}/>
    </Routes>
  )
}

export default App
