import { test, expect } from 'vitest';
import fs from 'fs';

function readToken(): string {
  // try frontend/tmp-token.txt then repo root tmp-token.txt
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

test('create asset with department persisted', async () => {
  const token = readToken();
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };

  // get departments
  const depsRes = await fetch(`${API_BASE}/api/lookups/departments`, { headers });
  expect(depsRes.ok).toBe(true);
  const deps = await depsRes.json();
  expect(Array.isArray(deps)).toBe(true);
  expect(deps.length).toBeGreaterThan(0);
  const dept = deps[0];

  const serial = `TEST-VITEST-${Date.now()}`;
  const body = { serialNumber: serial, make: 'Vitest', model: 'V-1', type: 'DESKTOP', purchaseDate: new Date().toISOString().slice(0,10), departmentId: dept.id };

  const createRes = await fetch(`${API_BASE}/api/assets`, { method: 'POST', headers, body: JSON.stringify(body) });
  const created = await createRes.json();
  expect(createRes.ok).toBe(true);
  expect(created).toHaveProperty('id');
  expect(created).toHaveProperty('department');
  expect(created.department).toBe(dept.name);

  // cleanup is optional; we won't delete the created asset here
});
