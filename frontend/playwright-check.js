const { chromium } = require('playwright');
const fs = require('fs');

(async () => {
  const logs = { console: [], requests: [] };
  const browser = await chromium.launch();
  const context = await browser.newContext();
  const page = await context.newPage();

  page.on('console', msg => {
    logs.console.push({ type: msg.type(), text: msg.text() });
  });
  page.on('request', r => {
    logs.requests.push({ url: r.url(), method: r.method() });
  });
  page.on('response', async res => {
    const status = res.status();
    const url = res.url();
    logs.requests.push({ url, status });
  });

  try {
    await page.goto('http://localhost:5173', { waitUntil: 'load', timeout: 10000 });
    // give the app some time to make API calls
    await page.waitForTimeout(2000);
  } catch (e) {
    logs.error = String(e);
  } finally {
    await browser.close();
    fs.writeFileSync('playwright-log.json', JSON.stringify(logs, null, 2));
    console.log('Wrote playwright-log.json');
  }
})();
