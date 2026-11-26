import { spawn } from 'node:child_process';
import { createWriteStream, existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { setTimeout as delay } from 'node:timers/promises';

// Uses only the dedicated Musina configuration. Credentials never enter command arguments or reports.
const root = fileURLToPath(new URL('../', import.meta.url));
const local = {};
if (existsSync(join(root, '.env'))) {
  for (const line of readFileSync(join(root, '.env'), 'utf8').split(/\r?\n/)) {
    const split = line.indexOf('=');
    if (split > 0 && !line.startsWith('#')) local[line.slice(0, split)] = line.slice(split + 1);
  }
}
const environment = { ...local, ...process.env };
const username = environment.MUSINA_E2E_USERNAME ?? environment.MUSINA_BOOTSTRAP_USERNAME;
const password = environment.MUSINA_E2E_PASSWORD ?? environment.MUSINA_BOOTSTRAP_PASSWORD;
if (!username || !password) throw new Error('Configurare credenziali sintetiche nella .env locale o nell’ambiente E2E');
if ((environment.MUSINA_DB_NAME ?? 'musina_dev') !== 'musina_dev') {
  throw new Error('Lo smoke usa esclusivamente il database dedicato musina_dev');
}
const existing = process.argv.includes('--existing');
const jar = join(root, 'backend/build/libs/musina.jar');
if (!existing && !existsSync(jar)) throw new Error('Eseguire prima la build Gradle');
const port = environment.MUSINA_SMOKE_PORT ?? '18080';
const baseURL = existing ? (environment.MUSINA_E2E_URL ?? 'http://127.0.0.1:5173') : `http://127.0.0.1:${port}`;
if (!existing) {
  try {
    await fetch(`${baseURL}/api/auth/csrf`, { signal: AbortSignal.timeout(1000) });
    throw new Error('Porta smoke già in uso: non viene toccato il processo esistente');
  } catch (error) {
    if (error instanceof Error && error.message.startsWith('Porta smoke')) throw error;
  }
}
const java = environment.JAVA_HOME ? join(environment.JAVA_HOME, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java';
const log = existing ? null : createWriteStream(join(root, 'backend/build/smoke-application.log'), { flags: 'w' });
const app = existing ? null : spawn(java, ['-jar', jar], {
  cwd: root, env: { ...environment, MUSINA_PORT: port, MUSINA_BIND_ADDRESS: '127.0.0.1' },
  stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true,
});
if (app && log) {
  app.stdout.pipe(log, { end: false });
  app.stderr.pipe(log, { end: false });
}
let spawnError;
app?.on('error', error => { spawnError = error; });

async function runBrowser() {
  for (let i = 0; i < 120; i++) {
    if (spawnError || (app && app.exitCode !== null)) throw new Error('Avvio JAR fallito: consultare backend/build/smoke-application.log');
    try {
      if ((await fetch(`${baseURL}/api/auth/csrf`, { signal: AbortSignal.timeout(1000) })).ok) break;
    } catch { /* Application still starting. */ }
    if (i === 119) throw new Error('Timeout avvio JAR: consultare il log locale');
    await delay(500);
  }
  if ((await fetch(`${baseURL}/api/connection`)).status !== 401) throw new Error('API anonima non protetta');
  const index = await fetch(`${baseURL}/`);
  if (!index.ok || !(await index.text()).includes('<div id="root">')) throw new Error('React non incluso nel pacchetto');
  console.log(existing ? 'Server esistenti raggiungibili; avvio smoke browser.' : 'Pacchetto completo avviato; UI inclusa e API anonima protetta. Avvio smoke browser.');
  const cli = join(root, 'frontend/node_modules/@playwright/test/cli.js');
  const result = await new Promise((resolve, reject) => {
    const test = spawn(process.execPath, [cli, 'test'], {
      cwd: join(root, 'frontend'), windowsHide: true, stdio: 'inherit',
      env: { ...environment, MUSINA_E2E_URL: baseURL, MUSINA_E2E_USERNAME: username, MUSINA_E2E_PASSWORD: password },
    });
    test.on('error', reject);
    test.on('exit', code => resolve(code ?? 1));
  });
  if (result !== 0) throw new Error('Smoke browser fallito');
}

try {
  await runBrowser();
  console.log('Smoke completato: login, verifica collegamento, sessione, rotta SPA, errori API e logout.');
} finally {
  if (app) {
    const exited = new Promise(resolve => app.once('exit', resolve));
    if (app.exitCode === null) { app.kill(); await Promise.race([exited, delay(5000)]); }
  }
  log?.end();
}
