import { Routes, Route } from 'react-router'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import NotFound from './pages/NotFound.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import RequireAuth, { RequireRole } from './components/utils/RequireAuth.jsx'
import DashboardLayout from './pages/dashboard/DashboardLayout.jsx'
import Dashboard from './pages/dashboard/Dashboard.jsx'
import Profile from './pages/dashboard/Profile.jsx'
import Notifications from './pages/dashboard/Notifications.jsx'
import MyFiles from './pages/dashboard/MyFiles.jsx'
import Trash from './pages/dashboard/Trash.jsx'
import SharedWith from './pages/dashboard/SharedWith.jsx'
import Storage from './pages/dashboard/Storage.jsx'
import AdminPermissions from './pages/dashboard/AdminPermissions.jsx'
import Settings from './pages/dashboard/Settings.jsx'
import Support from './pages/dashboard/Support.jsx'
import AdminUsers from './pages/dashboard/AdminUsers.jsx'
import PublicAccess from './pages/PublicAccess.jsx'
import './App.css';

import LandingPage from './pages/LandingPage/LandingPage.jsx'





function App(){ 

  return(
    <Routes>
      <Route path='/' element={<LandingPage/>}/>
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register />}/>
      <Route path='/forgotpassword' element={<ForgotPassword />}/>
      <Route path='/s/:token' element={<PublicAccess/>}/>

      <Route path='/dashboard' element={<RequireAuth><DashboardLayout /></RequireAuth>}>
        <Route index element={<Dashboard/>}/>
        <Route path='arquivos' element={<MyFiles/>}/>
        <Route path='arquivos/compartilhados' element={<SharedWith/>}/>
        <Route path='arquivos/lixeira' element={<Trash/>}/>
        <Route path='armazenamento' element={<Storage/>}/>
        <Route path='admin/usuarios' element={<RequireRole permission="ADMIN_USUARIOS"><AdminUsers/></RequireRole>}/>
        <Route path='admin/permissoes' element={<RequireRole permission="ADMIN_PERMISSOES"><AdminPermissions/></RequireRole>}/>
        <Route path='notificacoes' element={<Notifications/>}/>
        <Route path='suporte' element={<Support/>}/>
        <Route path='configuracoes' element={<Settings/>}/>
        <Route path='perfil' element={<Profile/>}/>
      </Route>


      <Route path='*' element={<NotFound/>}/>
    </Routes>
  )
}

export default App
