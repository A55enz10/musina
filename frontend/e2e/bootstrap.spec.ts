import { test, expect } from '@playwright/test';

test('login, collegamento reale, logout e rotta SPA dal pacchetto completo', async ({ page, context }) => {
  const username = process.env.MUSINA_E2E_USERNAME;
  const password = process.env.MUSINA_E2E_PASSWORD;
  if (!username || !password) throw new Error('Impostare credenziali sintetiche E2E tramite ambiente');
  await page.goto('/connection');
  await expect(page.getByRole('heading', { name: 'Accedi alla tua famiglia' })).toBeVisible();
  await page.getByLabel('Nome utente').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Accedi', exact: true }).click();
  await expect(page.getByRole('status')).toHaveText('Collegamento al backend verificato');
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Verifica collegamento' }).click();
  await expect(page.getByRole('status')).toHaveText('Collegamento al backend verificato');
  const cookies = await context.cookies();
  expect(cookies.find(cookie => cookie.name === 'JSESSIONID')?.httpOnly).toBe(true);
  const missing = await page.request.get('/api/not-an-endpoint');
  expect(missing.status()).toBe(404);
  expect(missing.headers()['content-type']).not.toContain('text/html');
  await page.getByRole('button', { name: 'Esci', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Accedi', exact: true })).toBeVisible();
  expect((await page.request.get('/api/connection')).status()).toBe(401);
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Accedi alla tua famiglia' })).toBeVisible();
});
