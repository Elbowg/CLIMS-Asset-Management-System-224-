import { test, expect } from 'vitest';
import fs from 'fs';

function readToken(): string {
  const candidates = ['tmp-token.txt', '../tmp-token.txt', '../../tmp-token.txt', '../../../tmp-token.txt'];
  for (const p of candidates) {
    try {
      const raw = fs.readFileSync(p, 'utf8');
      const m = raw.match(/[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+\.[A-Za-z0-9-_]+/);
      if (m) return m[0];
    } catch (e) {
      // ignore
    }
  }
  throw new Error('No token found in expected tmp-token.txt locations');
}

const API_BASE = process.env.TEST_API_BASE || 'http://localhost:8080';

test('rejects malicious SQL input on create', async () => {
  const token = readToken();
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  // fetch a valid department to attach
  const depsRes = await fetch(`${API_BASE}/api/lookups/departments`, { headers });
  expect(depsRes.ok).toBe(true);
  const deps = await depsRes.json();
  expect(Array.isArray(deps)).toBe(true);
  expect(deps.length).toBeGreaterThan(0);
  const dept = deps[0];

  // malicious payload in serial number
  const serial = `TEST-SQL-'; DROP TABLE assets; ---${Date.now()}`;
  const body = { serialNumber: serial, make: 'Vitest', model: 'SQL', type: 'DESKTOP', purchaseDate: new Date().toISOString().slice(0,10), departmentId: dept.id };

  const createRes = await fetch(`${API_BASE}/api/assets`, { method: 'POST', headers, body: JSON.stringify(body) });

  // We expect the server to reject obviously malicious SQL-like payloads.
  // At minimum it should NOT return ok (2xx). If it does accept it, the test will fail and surface that.
  expect(createRes.ok).toBe(false);

  const txt = await createRes.text();
  // Ensure some kind of error or message is returned
  expect(txt.length).toBeGreaterThan(0);
});
