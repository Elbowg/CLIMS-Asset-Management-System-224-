const http = require('http');
const https = require('https');
const urls = [
  'http://localhost:5173/',
  'http://127.0.0.1:5173/',
  'http://localhost:8080/api/reports/kpis',
  'http://127.0.0.1:8080/api/reports/kpis'
];

function fetch(url) {
  return new Promise((resolve) => {
    const lib = url.startsWith('https') ? https : http;
    const req = lib.get(url, (res) => {
      let body = '';
      res.setEncoding('utf8');
      res.on('data', (chunk) => body += chunk);
      res.on('end', () => resolve({ url, status: res.statusCode, headers: res.headers, body: body.slice(0, 2000) }));
    });
    req.on('error', (err) => resolve({ url, error: err.message }));
    req.setTimeout(3000, () => { req.abort(); resolve({ url, error: 'timeout' }); });
  });
}

(async () => {
  for (const u of urls) {
    const r = await fetch(u);
    console.log('---', u, '---');
    if (r.error) console.log('ERROR:', r.error);
    else {
      console.log('STATUS:', r.status);
      console.log('HEADERS:', JSON.stringify(r.headers));
      console.log('BODY:', r.body ? r.body.substring(0, 1000) : '(empty)');
    }
  }
})();
