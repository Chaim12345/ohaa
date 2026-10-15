// Error-path probe on the UNKEYED server (12002): boot 401 banners + bogus key in Settings.
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';
import fs from 'node:fs';

const SHOTS = '/tmp/opencode/ux-test/shots-err';
fs.mkdirSync(SHOTS, { recursive: true });
const W = 1280, H = 900, NAV_Y = 860;
const log = [], errors = [];

const browser = await chromium.launch({
  executablePath: '/usr/local/bin/chromium',
  headless: true,
  args: ['--no-sandbox', '--use-gl=angle', '--use-angle=swiftshader',
         '--enable-unsafe-swiftshader', '--disable-dev-shm-usage'],
});
const page = await browser.newPage({ viewport: { width: W, height: H } });
page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) errors.push(m.text().slice(0, 250)); });
page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message.slice(0, 250)));
page.on('response', (r) => { if (r.status() >= 400 && r.status() !== 404) errors.push(`HTTP ${r.status()} ${r.url().slice(27, 130)}`); });

let step = 0;
async function shot(name) {
  step++;
  const f = `${SHOTS}/${String(step).padStart(2, '0')}-${name}.png`;
  await page.screenshot({ path: f });
  log.push(`${String(step).padStart(2, '0')} ${name}`);
  return f;
}
function words(img) {
  try {
    const tsv = execSync(`tesseract "${img}" stdout tsv 2>/dev/null`, { encoding: 'utf8', maxBuffer: 1 << 24 });
    return tsv.split('\n').slice(1).map(l => l.split('\t'))
      .filter(c => c.length >= 12 && Number(c[10]) >= 30 && c[11]?.trim())
      .map(c => ({ t: c[11].trim(), x: (Number(c[6]) + Number(c[8]) / 2), y: (Number(c[7]) + Number(c[9]) / 2) }));
  } catch { return []; }
}
const lc = s => s.toLowerCase();

try {
  await page.goto('http://localhost:12002', { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(25000);
  let img = await shot('boot-unkeyed');
  let ws = words(img);
  const errWords = ws.filter(w => lc(w.t).includes('http') || lc(w.t).includes('401') || lc(w.t).includes('unauthorized') || lc(w.t).includes('bad gateway') || lc(w.t).includes('fail'));
  log.push('BOOT ERROR BANNER words: ' + errWords.map(w => `${w.t}@(${Math.round(w.y)})`).join(' '));

  await page.mouse.click(W * 0.875, NAV_Y);
  await page.waitForTimeout(4000);
  img = await shot('settings');
  ws = words(img);
  log.push('SETTINGS top words: ' + ws.slice(0, 40).map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' '));

  const keyLabel = ws.find(w => lc(w.t).includes('key') && w.y > 60);
  if (keyLabel) {
    await page.mouse.click(keyLabel.x + 80, keyLabel.y + 45);
    await page.waitForTimeout(800);
    await page.keyboard.type('sk-bogus-key-for-error-path-test', { delay: 6 });
    log.push('typed bogus key near label: ' + keyLabel.t);
  } else {
    log.push('KEY LABEL NOT FOUND — full words above');
  }
  img = await shot('key-typed');
  ws = words(img);
  const testBtn = ws.find(w => /^test$/i.test(w.t) || lc(w.t).startsWith('test connection') || lc(w.t) === 'save');
  if (testBtn) {
    await page.mouse.click(testBtn.x, testBtn.y);
    await page.waitForTimeout(6000);
    log.push('clicked button: ' + testBtn.t);
  } else {
    log.push('TEST/SAVE BUTTON NOT FOUND');
  }
  img = await shot('after-test');
  ws = words(img);
  const post = ws.filter(w => lc(w.t).includes('http') || lc(w.t).includes('401') || lc(w.t).includes('403') || lc(w.t).includes('unauthorized') || lc(w.t).includes('invalid') || lc(w.t).includes('fail') || lc(w.t).includes('error'));
  log.push('AFTER-TEST error words: ' + post.map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' '));
  log.push('FULL after-test words: ' + ws.slice(0, 45).map(w => w.t).join(' '));
} catch (e) {
  log.push('FATAL: ' + e.message);
}

console.log('=== FLOW ===');
console.log(log.join('\n'));
console.log('=== ERRORS (' + errors.length + ') ===');
console.log([...new Set(errors)].slice(0, 15).join('\n'));
await browser.close();
