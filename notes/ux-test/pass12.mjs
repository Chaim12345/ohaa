// Pass12 (final, all-green Oct 12): full UX chain vs live API.
// Calibrations: nav tabs y=860-877 (Dashboard x160 / Conversations x480 / NewTask x800 / Settings x1120);
// list rows "Conversation <id>" @x≈72 y178+; detail back arrow = "€"@(28,32), NO bottom nav on detail;
// prompt placeholder click "checkout"@(164,105); repo field "owner/repo"@(110,338); results y400-700;
// branch chips y400-560 (horizontal strip); click only on STABLE frames (OCR latency 1-2s races list re-render).
import { chromium } from 'playwright';
import { execSync } from 'node:child_process';
import fs from 'node:fs';

const SHOTS = '/tmp/opencode/ux-test/shots12';
fs.mkdirSync(SHOTS, { recursive: true });
const W = 1280, H = 900, NAV_Y = 860;
const log = [], errors = [], net = [];

const browser = await chromium.launch({
  executablePath: '/usr/local/bin/chromium',
  headless: true,
  args: ['--no-sandbox', '--use-gl=angle', '--use-angle=swiftshader',
         '--enable-unsafe-swiftshader', '--disable-dev-shm-usage'],
});
const page = await browser.newPage({ viewport: { width: W, height: H } });
page.on('console', (m) => { if (m.type() === 'error' && !m.text().includes('404')) errors.push(m.text().slice(0, 200)); });
page.on('pageerror', (e) => errors.push('PAGEERROR: ' + e.message.slice(0, 200)));
page.on('response', (r) => { if (r.status() >= 400 && r.status() !== 404) errors.push(`HTTP ${r.status()} ${r.url().slice(27, 130)}`); });
page.on('request', (r) => { if (r.url().includes('/cloud/')) net.push(`${r.method()} ${r.url().slice(27, 150)}`); });

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
async function clickWord(img, pred, note) {
  const ws = words(img);
  const hit = ws.find(pred);
  if (!hit) { log.push(`   MISS(${note}) — words: ${ws.slice(0, 30).map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' ')}`); return false; }
  await page.mouse.click(hit.x, hit.y);
  await page.waitForTimeout(2000);
  log.push(`   clickWord(${note}): "${hit.t}" @(${Math.round(hit.x)},${Math.round(hit.y)})`);
  return true;
}
const lc = s => s.toLowerCase();
const rowPred = w => /^Conversation\b/.test(w.t) && !w.t.startsWith('Conversations') && w.y > 150 && w.y < 800;
const repoHitPred = w => (lc(w.t).includes('chaim') || w.t === 'ohaa') && w.y > 400 && w.y < 800;
const isDetail = ws => ws.some(w => w.t === '€' && w.x < 60 && w.y < 60);

async function escapeDetail() {
  const ws = await snapWords('escape');
  if (isDetail(ws)) {
    const back = ws.find(w => w.t === '€' && w.x < 60 && w.y < 60);
    await page.mouse.click(back.x, back.y);
    await page.waitForTimeout(2500);
    log.push(`   ESCAPED detail via €@(${Math.round(back.x)},${Math.round(back.y)})`);
    return true;
  }
  return false;
}
async function navTab(frac, name) {
  await escapeDetail();
  await page.mouse.click(W * frac, NAV_Y);
  await page.waitForTimeout(3500);
  const ws = await snapWords(`nav-${name}`);
  log.push(`   nav ${name}: screen shows nav bar: ` + ws.some(w => w.t === 'Settings' && w.y > 840));
  return ws;
}

try {
  await page.goto('http://localhost:12001', { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(25000);
  await shot('dashboard');

  let ws = [], hasRows = false;
  for (let attempt = 1; attempt <= 3 && !hasRows; attempt++) {
    await navTab(0.375, 'conversations');
    await page.waitForTimeout(1500);
    await shot(`conversations-try${attempt}`);
    ws = await snapWords(`conv${attempt}`);
    hasRows = ws.some(rowPred);
  }
  log.push('LIST ROWS VISIBLE: ' + hasRows);
  if (hasRows) {
    const row = ws.find(rowPred);
    await page.mouse.click(row.x, row.y);
    await page.waitForTimeout(3000);
    await shot('detail');
    const dws = await snapWords('detail');
    log.push('DETAIL CONFIRMED (€ back + title, no bottom nav): '
      + (isDetail(dws) && dws.some(w => w.t === 'Conversation') && !dws.some(w => w.t === 'Settings' && w.y > 840)));
    const back = dws.find(w => w.t === '€' && w.x < 60 && w.y < 60);
    if (back) { await page.mouse.click(back.x, back.y); log.push(`   BACK CLICKED €@(${Math.round(back.x)},${Math.round(back.y)})`); }
    else { log.push('   € NOT FOUND: ' + dws.slice(0, 20).map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' ')); }
    await page.waitForTimeout(3000);
    await shot('after-back');
    const aws = await snapWords('afterback');
    const verified = aws.some(rowPred) && aws.some(w => w.t === 'Settings' && w.y > 840) && !isDetail(aws);
    log.push('BACK-NAV VERIFIED (rows + nav + not detail): ' + verified);
  }

  await navTab(0.625, 'newtask');
  let img = await shot('newtask');
  let ws2 = words(img);
  log.push('ON NEW TASK (shows "Describe"): ' + ws2.some(w => lc(w.t).includes('describe')));
  await clickWord(img, w => lc(w.t).includes('checkout') || w.t === 'PR', 'prompt placeholder');
  await page.keyboard.type('Add a README badge to the repo', { delay: 8 });
  img = await shot('prompt-typed');
  log.push('PROMPT typed: ' + words(img).some(w => lc(w.t).includes('readme')));

  await clickWord(img, w => lc(w.t).includes('owner/repo'), 'repo field');
  await page.keyboard.type('ohaa', { delay: 8 });
  // STABILITY-GATED click: the last keystroke's response can land during OCR latency
  // (1-2s) and shift the list — only click when two consecutive frames are identical.
  let repoClicked = false, prevSig = '', stable = 0;
  for (let i = 0; i < 20 && !repoClicked; i++) {
    await page.waitForTimeout(1000);
    const cws = await snapWords(`repo-p${i}`);
    const sig = cws.filter(w => w.y > 400 && w.y < 700).map(w => w.t).join('|');
    const hasResults = cws.some(w => lc(w.t).includes('found'));
    stable = (hasResults && sig && sig === prevSig) ? stable + 1 : 0;
    prevSig = sig;
    if (hasResults && stable >= 1) {
      const hit = cws.find(w => lc(w.t).includes('/ohaa') && w.y > 400 && w.y < 700)
        || cws.find(w => lc(w.t).includes('chaim') && w.y > 400 && w.y < 700);
      if (hit) {
        await page.mouse.click(hit.x, hit.y);
        await page.waitForTimeout(2000);
        log.push(`   REPO CLICKED stable-frame p${i}: "${hit.t}" @(${Math.round(hit.x)},${Math.round(hit.y)})`);
        repoClicked = true;
      }
    }
  }
  log.push('REPO CLICKED: ' + repoClicked);
  img = await shot('repo-after-click');
  const rws = words(img);
  const fieldWords = rws.filter(w => w.y > 300 && w.y < 370);
  log.push('FIELD text after click: ' + fieldWords.map(w => `${w.t}@(${Math.round(w.x)})`).join(','));
  log.push('ON_SELECT_REPOSITORY FIRED (field has owner/repo): ' + fieldWords.some(w => lc(w.t).includes('chaim')));
  log.push('branches/search in net trace: ' + net.some(u => u.includes('branches/search')));
  log.push('chaim tokens post-click: ' + rws.filter(w => lc(w.t).includes('chaim')).map(w => `${w.t}@(${Math.round(w.x)},${Math.round(w.y)})`).join(' '));

  let branchClicked = false;
  for (let i = 0; i < 10 && !branchClicked; i++) {
    await page.waitForTimeout(1500);
    const bws = await snapWords(`branch-${i}`);
    // chips are a horizontal strip ~y400-560; OCR is noisy: "main." w/ period,
    // "_redesign/..." w/ underscore — use contains, not strict equality
    const hit = bws.find(w => w.y > 400 && w.y < 560
      && !lc(w.t).includes('github-actions') && !lc(w.t).includes('describe')
      && (w.t.includes('main') || lc(w.t).includes('canvas') || lc(w.t).includes('cloud-v1') || lc(w.t).includes('redesign')));
    if (hit) {
      await page.mouse.click(hit.x, hit.y);
      await page.waitForTimeout(1500);
      log.push(`   BRANCH CHIP CLICKED: "${hit.t}" @(${Math.round(hit.x)},${Math.round(hit.y)})`);
      branchClicked = true;
      const aws = await snapWords('branch-selected');
      log.push('   after select, header region: ' + aws.filter(w => w.y > 420 && w.y < 570).map(w => w.t).join(','));
    }
  }
  log.push('BRANCH CLICKED: ' + branchClicked);
  await shot('branch-final');

  await navTab(0.875, 'settings');
  await shot('settings-top');
  await page.mouse.move(W / 2, 600);
  await page.mouse.wheel(0, 800);
  await page.waitForTimeout(2000);
  img = await shot('settings-scrolled');
  const sw = words(img);
  log.push('SETTINGS SECTIONS: ' + sw.filter(w => /profile|secret|cloud/i.test(w.t)).map(w => w.t).join(','));
} catch (e) {
  log.push('FATAL: ' + e.message);
}

console.log('=== FLOW ===');
console.log(log.join('\n'));
console.log('=== NETWORK TRACE (' + net.length + ' /cloud/ calls) ===');
console.log([...new Set(net)].join('\n'));
console.log('=== ERRORS (' + errors.length + ') ===');
console.log([...new Set(errors)].slice(0, 12).join('\n'));
await browser.close();
