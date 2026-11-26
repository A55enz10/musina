import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, connection, login, logout, type Connection } from './api';

type State = { kind: 'loading' } | { kind: 'login'; error?: string }
  | { kind: 'connected'; data: Connection } | { kind: 'error' };

export function App() {
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [busy, setBusy] = useState(false);

  async function verify() {
    setState({ kind: 'loading' });
    try { setState({ kind: 'connected', data: await connection() }); }
    catch (error) { setState(error instanceof ApiError && error.status === 401 ? { kind: 'login' } : { kind: 'error' }); }
  }
  useEffect(() => { void verify(); }, []);

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    setBusy(true);
    try {
      await login(String(values.get('username') ?? ''), String(values.get('password') ?? ''));
      form.reset();
      await verify();
    } catch (error) {
      setState({ kind: 'login', error: error instanceof ApiError ? error.message : 'Backend non raggiungibile. Riprova.' });
    } finally { setBusy(false); }
  }

  async function signOut() {
    setBusy(true);
    try { await logout(); setState({ kind: 'login' }); }
    catch { setState({ kind: 'error' }); }
    finally { setBusy(false); }
  }

  return <main className="shell">
    <header className="brand"><span className="brand-mark" aria-hidden="true">m</span><span>Musina</span></header>
    <section className="card" aria-labelledby="page-title">
      <p className="eyebrow">CONTABILITÀ DI FAMIGLIA</p>
      <h1 id="page-title">Un posto per fare chiarezza.</h1>
      <p className="intro">Il tuo spazio condiviso per i conti di casa.</p>

      {state.kind === 'loading' && <p role="status" className="notice">Verifica del collegamento in corso…</p>}
      {state.kind === 'error' && <div className="notice error" role="alert">
        <h2>Backend non raggiungibile</h2>
        <p>Controlla che Musina sia avviata e riprova.</p>
        <button type="button" onClick={() => void verify()}>Riprova il collegamento</button>
      </div>}
      {state.kind === 'login' && <form onSubmit={event => void signIn(event)}>
        <h2>Accedi alla tua famiglia</h2>
        <label htmlFor="username">Nome utente</label>
        <input id="username" name="username" autoComplete="username" required maxLength={80} autoFocus />
        <label htmlFor="password">Password</label>
        <input id="password" name="password" type="password" autoComplete="current-password" required maxLength={128} />
        {state.error && <p role="alert" className="form-error">{state.error}</p>}
        <button disabled={busy} type="submit">{busy ? 'Accesso in corso…' : 'Accedi'}</button>
        <p className="hint">L’accesso è riservato ai membri della famiglia.</p>
      </form>}
      {state.kind === 'connected' && <div className="connected">
        <p className="notice success" role="status">Collegamento al backend verificato</p>
        <h2>{state.data.familyName}</h2>
        <p>Hai effettuato l’accesso come <strong>{state.data.username}</strong>.</p>
        <p className="hint">Lo spazio è pronto. Conti e movimenti arriveranno nei prossimi aggiornamenti.</p>
        <div className="actions">
          <button type="button" onClick={() => void verify()}>Verifica collegamento</button>
          <button className="secondary" type="button" onClick={() => void signOut()} disabled={busy}>Esci</button>
        </div>
      </div>}
    </section>
    <footer>Musina · Il tuo spazio locale, condiviso in famiglia.</footer>
  </main>;
}
