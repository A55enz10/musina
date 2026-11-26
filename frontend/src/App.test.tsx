import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import { App } from './App';
import * as api from './api';

vi.mock('./api', async original => ({ ...await original<typeof api>(), connection: vi.fn(), login: vi.fn(), logout: vi.fn() }));
const connected = { message: 'ok', username: 'utente.sintetico', familyId: 'synthetic', familyName: 'Famiglia di prova', role: 'MEMBER' as const };

describe('Collegamento Musina', () => {
  it('mostra il caricamento senza inventare uno stato connesso', () => {
    vi.mocked(api.connection).mockReturnValue(new Promise(() => {}));
    render(<App />);
    expect(screen.getByRole('status')).toHaveTextContent('Verifica del collegamento in corso');
  });

  it('mostra il risultato autenticato restituito dal backend', async () => {
    vi.mocked(api.connection).mockResolvedValue(connected);
    render(<App />);
    expect(await screen.findByText('Famiglia di prova')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Collegamento al backend verificato');
  });

  it('se il backend non risponde offre un retry che verifica nuovamente', async () => {
    vi.mocked(api.connection).mockRejectedValueOnce(new TypeError('network')).mockResolvedValueOnce(connected);
    render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Backend non raggiungibile');
    await userEvent.click(screen.getByRole('button', { name: 'Riprova il collegamento' }));
    expect(await screen.findByText('Famiglia di prova')).toBeInTheDocument();
  });

  it('richiede login, non registra utenti e permette logout', async () => {
    vi.mocked(api.connection).mockRejectedValueOnce(new api.ApiError(401, 'session')).mockResolvedValueOnce(connected);
    vi.mocked(api.login).mockResolvedValue();
    vi.mocked(api.logout).mockResolvedValue();
    render(<App />);
    await userEvent.type(await screen.findByLabelText('Nome utente'), 'utente.sintetico');
    await userEvent.type(screen.getByLabelText('Password'), 'synthetic-password');
    await userEvent.click(screen.getByRole('button', { name: 'Accedi' }));
    expect(await screen.findByText('Famiglia di prova')).toBeInTheDocument();
    expect(api.login).toHaveBeenCalledWith('utente.sintetico', 'synthetic-password');
    await userEvent.click(screen.getByRole('button', { name: 'Esci' }));
    await waitFor(() => expect(screen.getByLabelText('Nome utente')).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: /registr/i })).not.toBeInTheDocument();
  });
});
