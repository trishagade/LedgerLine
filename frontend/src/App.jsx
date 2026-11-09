import { useState } from 'react'
import { api, setToken } from './api'
import AuthForm from './components/AuthForm'
import Dashboard from './components/Dashboard'
import AdminPanel from './components/AdminPanel'
import heroImage from './assets/hero.png'
import './App.css'

function App() {
  const [user, setUser] = useState(null)
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
      // Forget the token locally even if logout request fails.
    }
    setToken(null)
    setUser(null)
  }

  if (!user) {
    return (
      <main className="landing-page">
        <header className="landing-header">
          <h1>LedgerLine</h1>
          <span>Secure Digital Banking</span>
        </header>

        <section className="hero-section">
          <div className="hero-content">
            <span className="hero-tag">BANKING MADE TRANSPARENT</span>

            <h2>
              Your Money.
              <br />
              Your Control.
            </h2>

            <p className="hero-quote">
              "Every transaction has a story.
              Make yours secure, transparent and accountable."
            </p>

            <img
              src={heroImage}
              alt="LedgerLine digital banking"
              className="hero-image"
            />
          </div>

          <div className="auth-section">
            <AuthForm onLoggedIn={handleLoggedIn} />
          </div>
        </section>

        <footer className="landing-footer">
          Secure transactions • Transparent records • Trusted banking
        </footer>
      </main>
    )
  }

  const isAdmin = user.role === 'ADMIN'

  return (
    <main className="container">
      <header className="topbar">
        <h1>LedgerLine</h1>
        <nav>
          <button
            className={view === 'accounts' ? 'tab active' : 'tab'}
            onClick={() => setView('accounts')}
          >
            My accounts
          </button>

          {isAdmin && (
            <button
              className={view === 'admin' ? 'tab active' : 'tab'}
              onClick={() => setView('admin')}
            >
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