import { useCallback, useEffect, useState } from 'react'
import { api, describeError, money } from '../api'
import AccountPanel from './AccountPanel'

export default function Dashboard() {
  const [accounts, setAccounts] = useState([])
  const [selected, setSelected] = useState(null) // account number
  const [error, setError] = useState('')
  const [accountNumber, setAccountNumber] = useState('')

  const load = useCallback(async () => {
    try {
      setAccounts(await api('/api/accounts'))
    } catch (err) {
      setError(describeError(err))
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function createAccount(e) {
    e.preventDefault()
    setError('')
    try {
      const body = accountNumber.trim() ? { accountNumber: accountNumber.trim().toUpperCase() } : {}
      const created = await api('/api/accounts', { method: 'POST', body })
      setAccountNumber('')
      await load()
      setSelected(created.accountNumber)
    } catch (err) {
      setError(describeError(err))
    }
  }

  return (
    <div className="grid">
      <section className="card">
        <h2>My accounts</h2>
        {accounts.length === 0 && <p className="muted">No accounts yet. Create one below.</p>}
        <ul className="list">
          {accounts.map((a) => (
            <li key={a.accountNumber}>
              <button className={selected === a.accountNumber ? 'row active' : 'row'} onClick={() => setSelected(a.accountNumber)}>
                <span>{a.accountNumber}</span>
                <strong>{money(a.balance)}</strong>
              </button>
            </li>
          ))}
        </ul>
        <form onSubmit={createAccount}>
          <label>
            New account number <span className="muted">(optional, e.g. SAVINGS01 — leave blank to auto-generate)</span>
            <input value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} maxLength={20} />
          </label>
          <button type="submit">Create account</button>
        </form>
        {error && <p className="error" role="alert">{error}</p>}
      </section>

      {selected ? (
        <AccountPanel key={selected} accountNumber={selected} onChanged={load} />
      ) : (
        <section className="card"><p className="muted">Select an account to deposit, withdraw, transfer and see its ledger.</p></section>
      )}
    </div>
  )
}
