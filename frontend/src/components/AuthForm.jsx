import { useState } from 'react'
import { api, describeError } from '../api'

export default function AuthForm({ onLoggedIn }) {
  const [mode, setMode] = useState('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (mode === 'register') {
        await api('/api/auth/register', { method: 'POST', body: { username, password } })
      }
      const login = await api('/api/auth/login', { method: 'POST', body: { username, password } })
      onLoggedIn(login)
    } catch (err) {
      setError(describeError(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" onSubmit={submit}>
      <h2>{mode === 'login' ? 'Log in' : 'Create an account'}</h2>
      <label>
        Username
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
      </label>
      <label>
        Password {mode === 'register' && <span className="muted">(10–64 characters)</span>}
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          required
        />
      </label>
      {error && <p className="error" role="alert">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Register and log in'}</button>
      <p className="muted">
        {mode === 'login' ? 'New here? ' : 'Already registered? '}
        <a href="#" onClick={(e) => { e.preventDefault(); setError(''); setMode(mode === 'login' ? 'register' : 'login') }}>
          {mode === 'login' ? 'Create an account' : 'Log in'}
        </a>
      </p>
    </form>
  )
}
