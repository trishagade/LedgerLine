
import { useState } from 'react'
import { api, describeError } from '../api'

export default function AuthForm({ onLoggedIn }) {
  const [mode, setMode] = useState('login')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [mobileNumber, setMobileNumber] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')

    if (mode === 'register') {
      if (password.length < 12 || password.length > 64) {
        setError('Password must be 12-64 characters.')
        return
      }

      if (password !== confirmPassword) {
        setError('Passwords do not match.')
        return
      }
    }

    setBusy(true)

    try {
      if (mode === 'register') {
        await api('/api/auth/register', {
          method: 'POST',
          body: {
            fullName: fullName.trim(),
            email: email.trim(),
            mobileNumber: mobileNumber.trim() || null,
            password
          }
        })
      }

      const login = await api('/api/auth/login', {
        method: 'POST',
        body: {
          username: email.trim(),
          password
        }
      })

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

      {mode === 'register' && (
        <>
          <label>
            Full Name
            <input
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              autoComplete="name"
              maxLength={100}
              required
            />
          </label>

          <label>
            Mobile Number <span className="muted">(Optional)</span>
            <input
              type="tel"
              value={mobileNumber}
              onChange={(e) => setMobileNumber(e.target.value)}
              autoComplete="tel"
              maxLength={20}
            />
          </label>
        </>
      )}

      <label>
        {mode === 'login' ? 'Email or Username' : 'Email Address'}
        <input
          type={mode === 'login' ? 'text' : 'email'}
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="username"
          maxLength={254}
          required
        />
      </label>

      <label>
        Password {mode === 'register' && (
          <span className="muted">(12-64 characters)</span>
        )}
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
          maxLength={64}
          required
        />
      </label>

      {mode === 'register' && (
        <label>
          Confirm Password
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            autoComplete="new-password"
            maxLength={64}
            required
          />
        </label>
      )}

      {error && <p className="error" role="alert">{error}</p>}

      <button type="submit" disabled={busy}>
        {busy
          ? 'Please wait…'
          : mode === 'login'
            ? 'Log in'
            : 'Register and log in'}
      </button>

      <p className="muted">
        {mode === 'login' ? 'New here? ' : 'Already registered? '}
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault()
            setError('')
            setMode(mode === 'login' ? 'register' : 'login')
          }}
        >
          {mode === 'login' ? 'Create an account' : 'Log in'}
        </a>
      </p>
    </form>
  )
}
