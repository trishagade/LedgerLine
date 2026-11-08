import { useState } from 'react'
import { api, setToken } from './api'
import AuthForm from './components/AuthForm'
import Dashboard from './components/Dashboard'
import AdminPanel from './components/AdminPanel'
import './App.css'

function App() {
  const [user, setUser] = useState(null) // { id, username, role }
  const [view, setView] = useState('accounts')

  function handleLoggedIn(loginResponse) {
    setToken(loginResponse.token)
    setUser(loginResponse.user)
    setView('accounts')
  }

  async function handleLogout() {
    try {
      await api('/api/auth/logout', { method: 'POST' })
    } catch {
      // even if the call fails, forget the token locally
    }
    setToken(null)
    setUser(null)
  }

  if (!user) {
    return (
      <main className="container narrow">
        <h1>LedgerLine</h1>
        <p className="muted">Accounts, deposits, withdrawals and transfers with a double-entry ledger.</p>
        <AuthForm onLoggedIn={handleLoggedIn} />
      </main>
    )
  }

  const isAdmin = user.role === 'ADMIN'

  return (
    <main className="container">
      <header className="topbar">
        <h1>LedgerLine</h1>
        <nav>
          <button className={view === 'accounts' ? 'tab active' : 'tab'} onClick={() => setView('accounts')}>
            My accounts
          </button>
          {isAdmin && (
            <button className={view === 'admin' ? 'tab active' : 'tab'} onClick={() => setView('admin')}>
              Admin
            </button>
          )}
        </nav>
        <span className="who">
          {user.username} <span className="badge">{user.role}</span>
        </span>
        <button onClick={handleLogout}>Log out</button>
      </header>

      {view === 'accounts' && <Dashboard />}
      {view === 'admin' && isAdmin && <AdminPanel />}
    </main>
  )
}

export default App
