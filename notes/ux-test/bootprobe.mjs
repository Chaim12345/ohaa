// Boot-only probe: does the app reach live data at boot?
// Fails a run on: >=400 (non-404) responses, failed requests, /cloud/ calls
// still unanswered after ~7s (the MTU blackhole used to hang everything and
// this probe would still print ALL OK), or zero successful data-plane 2xx.
import { chromium } from 'playwright';

const RUNS = Number(process.env.RUNS || 3);
const browser = await chromium.launch({
  executablePath: '/usr/local/bin/chromium',
  headless: true,
  args: ['--no-sandbox', '--use-gl=angle', '--use-angle=swiftshader',
         '--enable-unsafe-swiftshader', '--disable-dev-shm-usage'],
});

for (let i = 1; i <= RUNS; i++) {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  const bad = [];
  const started = new Map(); const finished = new Set();
  let dataOk = 0;
  page.on('request', (r) => { if (r.url().includes('/cloud/')) started.set(r.url(), Date.now()); });
  page.on('response', (r) => {
    const u = r.url();
    if (u.includes('/cloud/')) {
      finished.add(u);
      if (r.status() < 300 && (u.includes('/app-conversations/search') || u.includes('/v1/users/me') || u.includes('/billing/credits'))) dataOk++;
    }
    if (r.status() >= 400 && r.status() !== 404) bad.push(`${r.status()} ${u.replace('http://localhost:12001/', '')}`);
  });
  page.on('requestfailed', (r) => {
    const u = r.url();
    if (u.includes('/cloud/')) finished.add(u);
    bad.push(`FAILED ${r.failure()?.errorText} ${u.replace('http://localhost:12001/', '')}`);
  });
  const t0 = Date.now();
  await page.goto('http://localhost:12001', { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(25000);
  const hanging = [...started.entries()]
    .filter(([u, ts]) => !finished.has(u) && ts - t0 < 18000)
    .map(([u]) => u.replace('http://localhost:12001/', ''));
  const verdict = bad.length
    ? `PROBLEM: ${bad.join(' | ')}`
    : hanging.length
      ? `SUSPECT (unanswered calls, likely network blackhole): ${hanging.slice(0, 4).join(' , ')}`
      : dataOk > 0
        ? `ALL OK (${dataOk} data-plane 200s)`
        : 'SUSPECT: no successful data-plane responses (app may be disconnected)';
  console.log(`RUN ${i} (boot ${Math.round((Date.now() - t0) / 1000)}s): ${verdict}`);
  await page.close();
}
await browser.close();
