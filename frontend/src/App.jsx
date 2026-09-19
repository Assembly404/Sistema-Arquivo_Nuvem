import { Routes, Route } from 'react-router'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import NotFound from './pages/NotFound.jsx'
import ForgotPassword from './pages/ForgotPassword.jsx'

import Dashboard from './pages/dashboard/Dashboard.jsx'

import './App.css'

function App() {
  

  return (
    <>
    <Routes>
      {/*Public routes */}
      <Route path='/' element={<Home/>}/>
      <Route path='/login' element={<Login/>} />
      <Route path='/register' element={<Register />}/>
      <Route path='/forgotpassword' element={<ForgotPassword />}/>

      <Route path='/dashboard' element={<Dashboard />}/>

      <Route path='*' element={<NotFound/>}/>
    </Routes>

    

    </>

  )
}

export default App
