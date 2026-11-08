import { useEffect, useState } from 'react'
import { api, describeError, money } from '../api'

// This screen is only a convenience. The server enforces ADMIN on every /api/admin/** call,
// so a non-admin who somehow opened this screen would just get 403 errors.
export default function AdminPanel() {
  const [accounts, setAccounts] = useState([])
  const [logs, setLogs] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    async function load() {
      try {
        setAccounts(await api('/api/admin/accounts'))
        setLogs(await api('/api/admin/audit-logs?size=100'))
      } catch (err) {
        setError(describeError(err))
      }
    }
    load()
  }, [])

  return (
    <div>
      {error && <p className="error" role="alert">{error}</p>}
      <section className="card">
        <h2>All accounts</h2>
        <div className="scroll">
          <table>
            <thead><tr><th>Number</th><th>Owner name</th><th>Owner user id</th><th>Balance</th></tr></thead>
            <tbody>
              {accounts.map((a) => (
                <tr key={a.accountNumber}>
                  <td>{a.accountNumber}{a.system && ' (system)'}</td>
                  <td>{a.ownerName}</td>
                  <td>{a.ownerUserId ?? '—'}</td>
                  <td>{money(a.balance)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <h2>Audit log (append-only, latest first)</h2>
        <div className="scroll">
          <table>
            <thead><tr><th>Time</th><th>Actor</th><th>Action</th><th>Outcome</th><th>Target</th><th>Details</th><th>IP</th></tr></thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.id}>
                  <td>{new Date(l.createdAt).toLocaleString()}</td>
                  <td>{l.actorUsername ?? '—'}</td>
                  <td>{l.action}</td>
                  <td>{l.outcome}</td>
                  <td>{l.targetType ? `${l.targetType} ${l.targetId ?? ''}` : '—'}</td>
                  <td>{l.details ?? ''}</td>
                  <td>{l.ipAddress ?? ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  )
}
