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
    // Attempt login to obtain JWT token
    try {
      const loginResponse = await context.request.post('http://localhost:8080/api/auth/login', {
        data: { username: 'admin', password: 'Admin@123' }
      });
      if (loginResponse.ok()) {
        const body = await loginResponse.json();
        const token = body.token || body.accessToken || body.jwt || null;
        if (token) {
          // set token in localStorage before page loads
          await context.addInitScript((t) => { window.localStorage.setItem('clims_token', t); }, token);
          logs.login = { success: true };
        } else {
          logs.login = { success: false, body };
        }
      } else {
        logs.login = { success: false, status: loginResponse.status() };
      }
    } catch (le) {
      logs.login = { success: false, error: String(le) };
    }

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
