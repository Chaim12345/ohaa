// Secrets CRUD round-trip vs the LIVE API through the UI (P0-5 proof):
// Settings → Secrets → Add OHAAGUITEST (expect POST 201) → row appears →
// delete it (expect DELETE 200, dialog must name the secret first) → row gone.
// The app is canvas-rendered, so all targeting is OCR-based (tesseract TSV),
// reusing the pass12 helper style. Safety: the delete-confirm dialog must
// contain the test secret's name before the script clicks "Delete".
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';
import fs from 'node:fs';

const SHOTS = '/tmp/opencode/ux-test/shots-secrets';
fs.mkdirSync(SHOTS, { recursive: true });
const W = 1280, H = 900, NAV_Y = 860;
const NAME = 'OHAAGUITEST';
const VALUE = 'temp-value-123';
const log = [], errors = [], net = [], resp = [];

const browser = await chromium.launch({
  executablePath: '/usr/local/bin/chromium',
  headless: true,
  args: ['--no-sandbox', '--use-gl=angle', '--use-angle=swiftshader',
         '--enable-unsafe-swiftshader', '--disable-dev-shm-usage'],
});
const page = await browser.newPage({ viewport: { width: W, height: H } });
page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) errors.push(m.text().slice(0, 200)); });
page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message.slice(0, 200)));
page.on('request', (r) => { if (r.url().includes('/cloud/')) net.push(`${r.method()} ${r.url().replace('http://localhost:12001/', '')}`); });
page.on('response', (r) => {
  const u = r.url();
  if (u.includes('/cloud/')) resp.push(`${r.status()} ${r.request().method()} ${u.replace('http://localhost:12001/', '')}`);
  if (r.status() >= 400 && r.status() !== 404) errors.push(`HTTP ${r.status()} ${u.replace('http://localhost:12001/', '').slice(0, 120)}`);
});

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
  } catch (e) { log.push('OCR fail: ' + e.message.slice(0, 60)); return []; }
}
async function snapWords(tag) {
  const img = `${SHOTS}/w-${String(step).padStart(2, '0')}-${tag}.png`;
  await page.screenshot({ path: img });
  return words(img);
}
const lc = s => s.toLowerCase();
const flat = s => s.toLowerCase().replace(/[^a-z0-9]/g, '');
const isDetail = ws => ws.some(w => w.t === '€' && w.x < 60 && w.y < 60);
const hasAddDialog = ws => ws.some(w => lc(w.t).includes('stored')) || ws.some(w => lc(w.t).includes('letters'));
const hasDeleteDialog = ws => ws.some(w => lc(w.t).includes('removed')) && ws.some(w => lc(w.t).startsWith('delete'));
const isProfileDelete = ws => ws.some(w => lc(w.t).includes('delete') && lc(w.t).includes('profile'));

async function escapeDetail() {
  const ws = await snapWords('escape');
  if (isDetail(ws)) {
    const back = ws.find(w => w.t === '€' && w.x < 60 && w.y < 60);
    await page.mouse.click(back.x, back.y);
    await page.waitForTimeout(2500);
    return true;
  }
  return false;
}
async function navTab(frac, name) {
  await escapeDetail();
  await page.mouse.click(W * frac, NAV_Y);
  await page.waitForTimeout(3500);
  const ws = await snapWords(`nav-${name}`);
  log.push(`   nav ${name}: nav bar visible: ` + ws.some(w => w.t === 'Settings' && w.y > 840));
  return ws;
}
async function clickOn(ws, pred, note) {
  const hit = ws.find(pred);
  if (!hit) { log.push(`   MISS(${note}) — near words: ${ws.slice(0, 24).map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' ')}`); return null; }
  await page.mouse.click(hit.x, hit.y);
  await page.waitForTimeout(1300);
  log.push(`   click(${note}): "${hit.t}" @(${Math.round(hit.x)},${Math.round(hit.y)})`);
  return hit;
}

let ADD_ROW = false, ADD_HTTP = false, DEL_ROW = false, DEL_HTTP = false, DEL_NAME_SAFE = false;

try {
  await page.goto('http://localhost:12001', { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(25000);
  await shot('boot');

  await navTab(0.875, 'settings');

  // Locate the Secrets section (scroll progressively).
  let sx = 0, sy = 0, found = false, sw = [];
  for (let i = 0; i < 5 && !found; i++) {
    sw = await snapWords(`settings-${i}`);
    const hit = sw.find(w => lc(w.t) === 'secrets' || lc(w.t) === 'secrets.');
    if (hit) { sx = hit.x; sy = hit.y; found = true; break; }
    await page.mouse.move(W / 2, 600);
    await page.mouse.wheel(0, 700);
    await page.waitForTimeout(1800);
  }
  if (!found) throw new Error('Secrets section not found after 5 scrolls');
  log.push(`SECRETS SECTION @(${Math.round(sx)},${Math.round(sy)})`);
  await shot('secrets-section');

  // --- open the Add-secret dialog (the +.iconbutton is right of the header)
  let dialogOpen = false;
  for (const [cx, cy] of [[1240, sy], [1240, sy + 18], [1230, sy], [1250, sy]]) {
    await page.mouse.click(cx, cy);
    await page.waitForTimeout(1300);
    const ws = await snapWords('after-add-click');
    if (hasAddDialog(ws)) { dialogOpen = true; log.push(`ADD DIALOG OPEN via (${Math.round(cx)},${Math.round(cy)})`); break; }
    if (isProfileDelete(ws)) {
      const cancel = ws.find(w => lc(w.t) === 'cancel');
      if (cancel) { await page.mouse.click(cancel.x, cancel.y); await page.waitForTimeout(800); log.push('   (cancelled unexpected profile-delete dialog)'); }
    }
  }
  log.push('ADD DIALOG OPEN: ' + dialogOpen);

  if (dialogOpen) {
    await shot('add-dialog');
    let ws = await snapWords('dlg-name');
    await clickOn(ws, w => lc(w.t) === 'name' || lc(w.t) === 'name:', 'name field');
    await page.keyboard.type(NAME, { delay: 30 });
    await page.waitForTimeout(600);
    ws = await snapWords('dlg-value');
    await clickOn(ws, w => lc(w.t) === 'value' || lc(w.t) === 'value:', 'value field');
    await page.keyboard.type(VALUE, { delay: 20 });
    await page.waitForTimeout(900);
    await shot('add-filled');

    ws = await snapWords('dlg-confirm');
    const adds = ws.filter(w => w.t === 'Add');
    const btn = adds.sort((a, b) => b.y - a.y)[0]; // bottom-most 'Add' = confirm button
    if (!btn) { log.push('   MISS(Add confirm button) — words: ' + ws.map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' ')); }
    else { await page.mouse.click(btn.x, btn.y); await page.waitForTimeout(4500); log.push(`   click(Add confirm) @(${Math.round(btn.x)},${Math.round(btn.y)})`); }

    await shot('after-add');
    const aws = await snapWords('after-add-w');
    ADD_ROW = aws.some(w => flat(w.t).includes('guitest'));
    ADD_HTTP = resp.some(r => (r.startsWith('201 POST') || r.startsWith('200 POST')) && r.includes('/secrets'));
    log.push('ROW VISIBLE AFTER ADD: ' + ADD_ROW);
  }

  // --- delete the row (only after the confirm dialog names it)
  if (ADD_ROW) {
    const ws = await snapWords('pre-delete');
    const row = ws.find(w => flat(w.t).includes('guitest'));
    let opened = false;
    if (!row) log.push('   MISS(guitest row for delete)');
    else {
      for (const [cx, cy] of [[1240, row.y], [1240, row.y + 10], [1240, row.y - 10]]) {
        await page.mouse.click(cx, cy);
        await page.waitForTimeout(1300);
        const dws = await snapWords('after-del-click');
        if (hasDeleteDialog(dws)) {
          opened = true;
          DEL_NAME_SAFE = dws.some(w => flat(w.t).includes('guitest'));
          log.push(`DELETE DIALOG open (names the secret: ${DEL_NAME_SAFE})`);
          if (DEL_NAME_SAFE) {
            await shot('delete-dialog');
            const dels = dws.filter(w => lc(w.t).startsWith('delete'));
            const delBtn = dels.sort((a, b) => b.y - a.y)[0]; // bottom-most 'Delete' = confirm
            if (delBtn) { await page.mouse.click(delBtn.x, delBtn.y); await page.waitForTimeout(4500); log.push(`   click(Delete confirm) @(${Math.round(delBtn.x)},${Math.round(delBtn.y)})`); }
          } else {
            const cancel = dws.find(w => lc(w.t) === 'cancel');
            if (cancel) { await page.mouse.click(cancel.x, cancel.y); await page.waitForTimeout(800); log.push('   CANCELLED: dialog did not name the test secret'); }
          }
          break;
        }
      }
      log.push('DELETE DIALOG opened: ' + opened);
    }

    if (DEL_NAME_SAFE) {
      await shot('after-delete');
      const final = await snapWords('after-delete-w');
      DEL_ROW = !final.some(w => flat(w.t).includes('guitest'));
      DEL_HTTP = resp.some(r => r.startsWith('200 DELETE') && r.includes('/secrets/'));
      log.push('ROW GONE AFTER DELETE: ' + DEL_ROW);
    }
  }
} catch (e) {
  log.push('FATAL: ' + e.message);
}

console.log('=== FLOW ===');
console.log(log.join('\n'));
console.log('=== RESULT ===');
console.log('SECRETS-UI ADD  : ' + (ADD_ROW && ADD_HTTP ? 'PASS' : 'FAIL') + ` (row=${ADD_ROW}, http=${ADD_HTTP})`);
console.log('SECRETS-UI DEL  : ' + (DEL_ROW && DEL_HTTP ? 'PASS' : 'FAIL') + ` (row-gone=${DEL_ROW}, http=${DEL_HTTP}, dialog-named=${DEL_NAME_SAFE})`);
console.log('=== SECRETS HTTP ===');
console.log(resp.filter(r => r.includes('/secrets')).join('\n') || '(none)');
console.log('=== ERRORS (' + errors.length + ') ===');
console.log([...new Set(errors)].slice(0, 12).join('\n') || '(none)');
console.log('=== NET (' + net.length + ' /cloud/ calls, first 14) ===');
console.log([...new Set(net)].slice(0, 14).join('\n'));
await browser.close();
