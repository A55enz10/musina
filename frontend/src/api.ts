export type Connection = {
  message: string;
  username: string;
  familyId: string;
  familyName: string;
  role: 'MEMBER' | 'ADMIN';
};

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); }
}

async function ensureSuccess(response: Response): Promise<Response> {
  if (!response.ok) {
    const message = response.status === 401 ? 'Credenziali non valide o sessione scaduta.'
      : response.status === 429 ? 'Troppi tentativi. Riprova più tardi.'
      : 'Operazione non riuscita. Riprova.';
    throw new ApiError(response.status, message);
  }
  return response;
}

async function csrf(): Promise<{ token: string; headerName: string }> {
  return (await ensureSuccess(await fetch('/api/auth/csrf', { credentials: 'same-origin', cache: 'no-store' }))).json();
}

export async function connection(): Promise<Connection> {
  return (await ensureSuccess(await fetch('/api/connection', { credentials: 'same-origin', cache: 'no-store' }))).json();
}

export async function login(username: string, password: string): Promise<void> {
  const token = await csrf();
  await ensureSuccess(await fetch('/api/auth/login', {
    method: 'POST', credentials: 'same-origin',
    headers: { [token.headerName]: token.token, 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({ username, password }),
  }));
}

export async function logout(): Promise<void> {
  const token = await csrf();
  await ensureSuccess(await fetch('/api/auth/logout', {
    method: 'POST', credentials: 'same-origin', headers: { [token.headerName]: token.token },
  }));
}
