import { useCallback, useEffect, useRef, useState } from 'react'
import { api, describeError, money } from '../api'

const AMOUNT_PATTERN = /^\d{1,7}(\.\d{1,2})?$/

export default function AccountPanel({ accountNumber, onChanged }) {
  const [account, setAccount] = useState(null)
  const [ledger, setLedger] = useState([])
  const [action, setAction] = useState('deposit')
  const [amount, setAmount] = useState('')
  const [toAccount, setToAccount] = useState('')
  const [message, setMessage] = useState(null) // { type: 'ok' | 'error', text }
  const [busy, setBusy] = useState(false)

  // The Idempotency-Key is reused for an identical retry (e.g. after a network error),
  // and replaced when the form content changes or after a success.
  const keyRef = useRef({ signature: '', key: '' })

  const load = useCallback(async () => {
    try {
      const [acc, entries] = await Promise.all([
        api(`/api/accounts/${encodeURIComponent(accountNumber)}`),
        api(`/api/accounts/${encodeURIComponent(accountNumber)}/ledger`),
      ])
      setAccount(acc)
      setLedger(entries)
    } catch (err) {
      setMessage({ type: 'error', text: describeError(err) })
    }
  }, [accountNumber])

  useEffect(() => {
    load()
  }, [load])

  function keyFor(signature) {
    if (keyRef.current.signature !== signature) {
      keyRef.current = { signature, key: crypto.randomUUID() }
    }
    return keyRef.current.key
  }

  async function submit(e) {
    e.preventDefault()
    setMessage(null)
    if (!AMOUNT_PATTERN.test(amount) || Number(amount) <= 0) {
      setMessage({ type: 'error', text: 'Enter an amount like 25 or 25.50 (max 2 decimals).' })
      return
    }
    if (action === 'transfer' && !toAccount.trim()) {
      setMessage({ type: 'error', text: 'Enter the destination account number.' })
      return
    }

    const dest = toAccount.trim().toUpperCase()
    const signature = `${action}|${accountNumber}|${dest}|${amount}`
    const idempotencyKey = keyFor(signature)
    const numeric = Number(amount)

    setBusy(true)
    try {
      let result
      if (action === 'transfer') {
        result = await api('/api/transfers', {
          method: 'POST',
          idempotencyKey,
          body: { fromAccountNumber: accountNumber, toAccountNumber: dest, amount: numeric },
        })
      } else {
        result = await api(`/api/accounts/${encodeURIComponent(accountNumber)}/${action}`, {
          method: 'POST',
          idempotencyKey,
          body: { amount: numeric },
        })
      }
      setMessage({
        type: 'ok',
        text: `${result.type} of ${money(result.amount)} done (transaction #${result.transactionId})${result.replayed ? ' — this was a repeat of an earlier request, not applied twice' : ''}.`,
      })
      keyRef.current = { signature: '', key: '' } // success: next submission is a new payment
      setAmount('')
      await load()
      onChanged()
    } catch (err) {
      setMessage({ type: 'error', text: describeError(err) })
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card">
      <h2>{accountNumber}</h2>
      {account && <p className="balance">Balance: <strong>{money(account.balance)}</strong></p>}

      <form onSubmit={submit}>
        <div className="tabs">
          {['deposit', 'withdraw', 'transfer'].map((a) => (
            <button type="button" key={a} className={action === a ? 'tab active' : 'tab'} onClick={() => setAction(a)}>
              {a[0].toUpperCase() + a.slice(1)}
            </button>
          ))}
        </div>
        {action === 'transfer' && (
          <label>
            To account number
            <input value={toAccount} onChange={(e) => setToAccount(e.target.value)} maxLength={40} />
          </label>
        )}
        <label>
          Amount
          <input value={amount} onChange={(e) => setAmount(e.target.value)} inputMode="decimal" placeholder="0.00" />
        </label>
        <button type="submit" disabled={busy}>{busy ? 'Working…' : 'Submit'}</button>
        {message && <p className={message.type === 'ok' ? 'success' : 'error'} role="status">{message.text}</p>}
      </form>

      <h3>Ledger (latest first)</h3>
      <div className="scroll">
        <table>
          <thead>
            <tr><th>Tx</th><th>Type</th><th>Side</th><th>Amount</th><th>Balance after</th><th>Time</th></tr>
          </thead>
          <tbody>
            {ledger.length === 0 && <tr><td colSpan="6" className="muted">No entries yet.</td></tr>}
            {ledger.map((l, i) => (
              <tr key={`${l.transactionId}-${i}`}>
                <td>#{l.transactionId}</td>
                <td>{l.type}</td>
                <td>{l.direction}</td>
                <td>{money(l.amount)}</td>
                <td>{money(l.balanceAfter)}</td>
                <td>{new Date(l.createdAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}
