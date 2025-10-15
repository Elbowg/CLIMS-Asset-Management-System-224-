const API = 'http://localhost:8080';

async function login() {
  const res = await fetch(`${API}/api/auth/login`, { method: 'POST', body: JSON.stringify({ username: 'admin', password: 'Admin@123' }), headers: { 'Content-Type': 'application/json' } });
  if (!res.ok) throw new Error('login failed ' + res.status);
  return (await res.json()).token;
}

async function lookup(token, path) {
  const res = await fetch(`${API}${path}`, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) throw new Error(`lookup ${path} failed ${res.status}`);
  return res.json();
}

async function createAsset(token, body) {
  const res = await fetch(`${API}/api/assets`, { method: 'POST', body: JSON.stringify(body), headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } });
  if (!res.ok) throw new Error('create asset failed ' + res.status);
  return res.json();
}

(async () => {
  try {
    const token = await login();
    const locations = await lookup(token, '/api/lookups/locations');
    const vendors = await lookup(token, '/api/lookups/vendors');
    const locationId = locations[0].id;
    const vendorId = vendors[0].id;
    const body = {
      serialNumber: `NODE-SN-${Math.floor(Math.random()*900)+100}`,
      make: 'NodeTest',
      model: 'NT-1',
      purchaseDate: '2025-10-15',
      warrantyExpiryDate: '2028-10-15',
      locationId,
      vendorId
    };
    const created = await createAsset(token, body);
    console.log('created', created);
    const assets = await lookup(token, '/api/assets?page=0&size=20');
    console.log('assets count', assets.totalElements);
    process.exit(0);
  } catch (err) {
    console.error(err);
    process.exit(2);
  }
})();
