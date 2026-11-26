import { randomBytes } from 'node:crypto';
import { existsSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const target = fileURLToPath(new URL('../.env', import.meta.url));
if (existsSync(target)) {
  console.log('.env esiste già: nessuna configurazione modificata.');
} else {
  const secret = () => randomBytes(32).toString('base64');
  writeFileSync(target, [
    'MUSINA_DB_NAME=musina_dev', 'MUSINA_DB_USER=musina_dev', 'MUSINA_DB_PORT=15432',
    `MUSINA_DB_PASSWORD=${secret()}`, 'MUSINA_BOOTSTRAP_USERNAME=admin.local',
    `MUSINA_BOOTSTRAP_PASSWORD=${secret()}`, 'MUSINA_BOOTSTRAP_FAMILY=Famiglia', '',
  ].join('\n'), { flag: 'wx', mode: 0o600 });
  console.log('Creato .env locale con segreti casuali. Leggere le credenziali iniziali solo localmente.');
}
