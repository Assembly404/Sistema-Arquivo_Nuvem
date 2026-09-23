import { Routes, Route } from 'react-router'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import NotFound from './pages/NotFound.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'
import RequireAuth from './components/utils/RequireAuth.jsx'
import DashboardLayout from './pages/dashboard/DashboardLayout.jsx'
import Dashboard from './pages/dashboard/Dashboard.jsx'
import Placeholder from './pages/dashboard/Placeholder.jsx'
import './App.css';

function App(){
  return(
    <Routes>
      <Route path='/' element={<Home/>}/>
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register />}/>
      <Route path='/forgotpassword' element={<ForgotPassword />}/>

      <Route path='/dashboard' element={<RequireAuth><DashboardLayout /></RequireAuth>}>
        <Route index element={<Dashboard/>}/>
        <Route path='arquivos' element={<Placeholder title="Meus Arquivos"/>}/>
        <Route path='arquivos/compartilhados' element={<Placeholder title="Compartilhados comigo"/>}/>
        <Route path='arquivos/lixeira' element={<Placeholder title="Lixeira"/>}/>
        <Route path='armazenamento' element={<Placeholder title="Armazenamento"/>}/>
        <Route path='admin/usuarios' element={<Placeholder title="Usuários"/>}/>
        <Route path='admin/permissoes' element={<Placeholder title="Perfis & Permissões"/>}/>
        <Route path='notificacoes' element={<Placeholder title="Notificações"/>}/>
        <Route path='suporte' element={<Placeholder title="Suporte"/>}/>
        <Route path='configuracoes' element={<Placeholder title="Configurações"/>}/>
        <Route path='perfil' element={<Placeholder title="Perfil"/>}/>
      </Route>

      <Route path='*' element={<NotFound/>}/>
    </Routes>
  )
}

export default App
