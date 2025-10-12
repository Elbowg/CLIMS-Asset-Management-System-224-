// CLIMS Frontend App
// Handles auth (JWT), simple view routing, and API calls to the Spring Boot backend.

(function () {
  const API_BASE = getApiBase();

  // App state persisted in localStorage
  const state = {
    accessToken: localStorage.getItem('clims.access') || null,
    refreshToken: localStorage.getItem('clims.refresh') || null,
    accessExp: parseInt(localStorage.getItem('clims.accessExp') || '0', 10),
    refreshExp: parseInt(localStorage.getItem('clims.refreshExp') || '0', 10),
  };

  // Elements
  const el = {
    authStatus: document.getElementById('authStatus'),
    btnLogout: document.getElementById('btnLogout'),
    loginForm: document.getElementById('loginForm'),
    username: document.getElementById('username'),
    password: document.getElementById('password'),
    loginSpinner: document.getElementById('loginSpinner'),
    btnHello: document.getElementById('btnHello'),
    helloSpinner: document.getElementById('helloSpinner'),
    helloResult: document.getElementById('helloResult'),
    views: document.querySelectorAll('.view'),
    navLinks: document.querySelectorAll('[data-view]'),
    alertContainer: document.getElementById('alertContainer'),
  };

  // ASSETS MODULE
  const assetsState = {
    page: 0,
    size: 10,
    sort: 'id,asc',
    statusFilter: null,
    assignedUserId: null,
  };

  const assetsEl = {
    tbody: null,
    pageInfo: null,
    prev: null,
    next: null,
    reload: null,
    search: null,
    spinner: null,
    form: null,
    name: null,
    serial: null,
    status: null,
    purchaseDate: null,
  };

  // one-time init flags to avoid duplicate event binding
  let inited = { assets: false, reports: false, assignments: false };

  // Decode JWT payload safely (no validation, just parsing)
  function decodeJwt(token) {
    try {
      const payload = token.split('.')[1];
      const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(decodeURIComponent(escape(json)));
    } catch {
      return null;
    }
  }

  async function resolveCurrentUserId() {
    if (!state.accessToken) return null;
    const payload = decodeJwt(state.accessToken);
    const username = payload?.sub || payload?.username;
    if (!username) return null;
    // Try to find matching user via /api/users (admin-only; in secure mode this may 403)
    try {
      const res = await api('/api/users');
      if (!res.ok) return null;
      const users = await res.json();
      const found = users.find(u => u.username === username || u.email === username);
      return found?.id ?? null;
    } catch { return null; }
  }

  // Export helpers for Assets/Assignments
  function exportCsvFromRows(name, headers, rows) {
    if (!rows || rows.length === 0) { showToast('Nothing to export', 'warning'); return; }
    let csv = headers.join(',') + '\\n';
    rows.forEach(r => { csv += r.map(v => (v==null?'':String(v))).join(',') + '\\n'; });
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const date = new Date().toISOString().slice(0,10);
    a.download = `${name}_${date}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // Track last loaded pages for export
  let lastAssetsPage = null;
  let lastAsgnPage = null;

  // ASSIGNMENTS MODULE (search by userId and list assigned assets)
  const asgnState = { userId: null, page: 0, size: 10 };
  const asgnEl = { userId: null, search: null, clear: null, tbody: null, prev: null, next: null, pageInfo: null };

  function initAssignmentsElements() {
    if (inited.assignments) return;
    asgnEl.userId = document.getElementById('asgnUserId');
    asgnEl.search = document.getElementById('asgnSearch');
    asgnEl.clear = document.getElementById('asgnClear');
    asgnEl.tbody = document.getElementById('asgnTbody');
    asgnEl.prev = document.getElementById('asgnPrev');
    asgnEl.next = document.getElementById('asgnNext');
    asgnEl.pageInfo = document.getElementById('asgnPageInfo');

    asgnEl.search?.addEventListener('click', () => {
      const val = (asgnEl.userId.value || '').trim();
      if (!/^\d+$/.test(val)) { showToast('Enter a numeric userId', 'warning'); return; }
      asgnState.userId = Number(val);
      asgnState.page = 0;
      loadAssignments();
    });
    asgnEl.clear?.addEventListener('click', () => {
      asgnEl.userId.value = '';
      asgnState.userId = null;
      asgnEl.tbody.innerHTML = '<tr><td colspan="6" class="text-muted">Enter a userId and Search</td></tr>';
      asgnEl.pageInfo.textContent = 'Page 0';
    });
    asgnEl.prev?.addEventListener('click', () => { if (asgnState.page > 0) { asgnState.page -= 1; loadAssignments(); } });
    asgnEl.next?.addEventListener('click', () => { asgnState.page += 1; loadAssignments(); });

    document.getElementById('asgnMy')?.addEventListener('click', async () => {
      const uid = await resolveCurrentUserId();
      if (!uid) { showToast('Cannot resolve current userId', 'warning'); return; }
      asgnEl.userId.value = String(uid);
      asgnState.userId = uid;
      asgnState.page = 0;
      loadAssignments();
    });

    document.getElementById('expAsgnList')?.addEventListener('click', () => {
      if (!lastAsgnPage || !Array.isArray(lastAsgnPage.content)) { showToast('Nothing to export', 'warning'); return; }
      const rows = lastAsgnPage.content.map(r => [r.id, r.name, r.status, r.serialNumber, r.assignedUserId]);
      exportCsvFromRows('assignments_list', ['id','name','status','serialNumber','assignedUserId'], rows);
    });

    inited.assignments = true;
  }

  async function loadAssignments() {
    if (asgnState.userId == null) return;
    const params = new URLSearchParams();
    params.set('page', String(asgnState.page));
    params.set('size', String(asgnState.size));
    params.set('assignedUserId', String(asgnState.userId));
    // Optionally only show actively assigned statuses
    const res = await api(`/api/assets?${params.toString()}`);
    if (!res.ok) {
      asgnEl.tbody.innerHTML = `<tr><td colspan="6" class="text-danger">Load failed (${res.status})</td></tr>`;
      return;
    }
    const page = await res.json();
    lastAsgnPage = page;
    renderAssignments(page);
  }

  // Render assignments page content
  function renderAssignments(page) {
    const content = Array.isArray(page.content) ? page.content : [];
    if (content.length === 0) {
      asgnEl.tbody.innerHTML = '<tr><td colspan="7" class="text-muted">No data</td></tr>';
    } else {
      asgnEl.tbody.innerHTML = content.map(row => {
        const id = row.id ?? '';
        const name = sanitize(row.name);
        const status = sanitize(row.status);
        const serial = sanitize(row.serialNumber);
        const assigned = row.assignedUserId != null ? row.assignedUserId : '';
        const pd = sanitize(row.purchaseDate);
        return `<tr data-asset-id="${id}">
          <td>${id}</td>
          <td>${name}</td>
          <td>${status}</td>
          <td>${serial}</td>
          <td>${assigned}</td>
          <td>${pd}</td>
          <td><button class="btn btn-sm btn-outline-warning" data-action="unassign">Unassign</button></td>
        </tr>`;
      }).join('');

      // Wire unassign buttons
      asgnEl.tbody.querySelectorAll('tr[data-asset-id]').forEach(tr => {
        const id = tr.getAttribute('data-asset-id');
        const btn = tr.querySelector('[data-action="unassign"]');
        btn?.addEventListener('click', async () => {
          try {
            const res = await api(`/api/assets/${id}/unassign`, { method: 'POST' });
            if (!res.ok) throw new Error(`Unassign failed (${res.status})`);
            showToast('Unassigned', 'success');
            loadAssignments();
          } catch (e) {
            showToast(e.message || 'Unassign failed', 'danger');
          }
        });
      });
    }
    const pageNum = (page.number ?? asgnState.page) + 1;
    const totalPages = (page.totalPages ?? pageNum);
    asgnEl.pageInfo.textContent = `Page ${pageNum} of ${totalPages}`;
    if (asgnEl.prev) asgnEl.prev.disabled = pageNum <= 1;
    if (asgnEl.next) asgnEl.next.disabled = totalPages && pageNum >= totalPages;
  }

  // Extend assets hooks
  function initAssetsElements() {
    if (inited.assets) return;
    assetsEl.tbody = document.getElementById('assetTbody');
    assetsEl.pageInfo = document.getElementById('assetPageInfo');
    assetsEl.prev = document.getElementById('assetPrev');
    assetsEl.next = document.getElementById('assetNext');
    assetsEl.reload = document.getElementById('btnReloadAssets');
    assetsEl.search = document.getElementById('assetSearch');
    assetsEl.spinner = document.getElementById('assetSpinner');
    assetsEl.form = document.getElementById('assetForm');
    assetsEl.name = document.getElementById('assetName');
    assetsEl.serial = document.getElementById('assetSerial');
    assetsEl.status = document.getElementById('assetStatus');
    assetsEl.purchaseDate = document.getElementById('assetPurchaseDate');

    const table = document.getElementById('assetTable');
    table?.querySelectorAll('button[data-sort]')?.forEach(btn => {
      btn.addEventListener('click', () => {
        const key = btn.getAttribute('data-sort');
        const [curKey, curDir] = (assetsState.sort || 'id,asc').split(',');
        const nextDir = key === curKey && curDir === 'asc' ? 'desc' : 'asc';
        assetsState.sort = `${key},${nextDir}`;
        loadAssets();
      });
    });

    assetsEl.prev?.addEventListener('click', () => {
      if (assetsState.page > 0) { assetsState.page -= 1; loadAssets(); }
    });
    assetsEl.next?.addEventListener('click', () => { assetsState.page += 1; loadAssets(); });
    assetsEl.reload?.addEventListener('click', () => { assetsState.page = 0; loadAssets(); });
    assetsEl.search?.addEventListener('change', () => {
      const val = (assetsEl.search.value || '').trim();
      assetsState.statusFilter = null;
      assetsState.assignedUserId = null;
      if (/^\d+$/.test(val)) {
        assetsState.assignedUserId = Number(val);
      } else if (val) {
        assetsState.statusFilter = val;
      }
      assetsState.page = 0;
      loadAssets();
    });
    // My Assets button
    document.getElementById('btnMyAssets')?.addEventListener('click', async () => {
      const uid = await resolveCurrentUserId();
      if (!uid) { showToast('Cannot resolve current userId', 'warning'); return; }
      assetsState.assignedUserId = uid;
      assetsState.statusFilter = null;
      assetsState.page = 0;
      loadAssets();
    });
    // Export current list
    document.getElementById('expAssetsList')?.addEventListener('click', () => {
      if (!lastAssetsPage || !Array.isArray(lastAssetsPage.content)) { showToast('Nothing to export', 'warning'); return; }
      const rows = lastAssetsPage.content.map(r => [r.id, r.name, r.type, r.status, r.serialNumber, r.assignedUserId, r.purchaseDate]);
      exportCsvFromRows('assets_list', ['id','name','type','status','serialNumber','assignedUserId','purchaseDate'], rows);
    });

    assetsEl.form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (!assetsEl.form.checkValidity()) {
        assetsEl.form.classList.add('was-validated');
        return;
      }
      setLoading(assetsEl.spinner, true);
      try {
        const payload = {
          name: assetsEl.name.value.trim(),
          serialNumber: assetsEl.serial.value.trim() || null,
          status: assetsEl.status.value || null,
          purchaseDate: assetsEl.purchaseDate.value || null,
        };
        const res = await api('/api/assets', { method: 'POST', body: JSON.stringify(payload) });
        if (!res.ok) throw new Error(`Create failed (${res.status})`);
        showToast('Asset created', 'success');
        assetsEl.form.reset();
        assetsEl.form.classList.remove('was-validated');
        assetsState.page = 0;
        loadAssets();
      } catch (err) {
        showToast(err.message || 'Create failed', 'danger');
      } finally {
        setLoading(assetsEl.spinner, false);
      }
    });

    inited.assets = true;
  }

  async function loadAssets() {
    if (!assetsEl.tbody) return;
    try {
      const params = new URLSearchParams();
      params.set('page', String(assetsState.page));
      params.set('size', String(assetsState.size));
      if (assetsState.sort) params.set('sort', assetsState.sort);
      if (assetsState.statusFilter) params.set('status', assetsState.statusFilter);
      if (assetsState.assignedUserId != null) params.set('assignedUserId', String(assetsState.assignedUserId));

      const res = await api(`/api/assets?${params.toString()}`);
      if (!res.ok) throw new Error(`Load failed (${res.status})`);
      const page = await res.json();
      lastAssetsPage = page;
      renderAssets(page);
      wireAssetRowActions();
    } catch (err) {
      assetsEl.tbody.innerHTML = `<tr><td colspan="8" class="text-danger">${err.message || 'Error loading assets'}</td></tr>`;
    }
  }

  function renderAssets(page) {
    const content = Array.isArray(page.content) ? page.content : [];
    if (content.length === 0) {
      assetsEl.tbody.innerHTML = '<tr><td colspan="8" class="text-muted">No data</td></tr>';
    } else {
      assetsEl.tbody.innerHTML = content.map(row => {
         const id = row.id ?? '';
         const name = sanitize(row.name);
         const type = sanitize(row.type);
         const status = sanitize(row.status);
         const serial = sanitize(row.serialNumber);
         const assigned = row.assignedUserId != null ? row.assignedUserId : '';
         const pd = sanitize(row.purchaseDate);
         return `<tr data-asset-id="${id}">
           <td>${id}</td>
           <td>${name}</td>
           <td>${type}</td>
           <td>${status}</td>
           <td>${serial}</td>
           <td>${assigned}</td>
           <td>${pd}</td>
           <td>
             <div class="d-flex flex-wrap gap-1 align-items-center">
               <input class="form-control form-control-sm" placeholder="userId" style="width:90px" data-role="assignUserId" />
               <button class="btn btn-sm btn-outline-primary" data-action="assign">Assign</button>
               <button class="btn btn-sm btn-outline-warning" data-action="unassign">Unassign</button>
               <span class="vr mx-1"></span>
               <button class="btn btn-sm btn-outline-secondary" data-action="edit">Edit</button>
               <button class="btn btn-sm btn-success d-none" data-action="save">Save</button>
               <button class="btn btn-sm btn-light d-none" data-action="cancel">Cancel</button>
               <button class="btn btn-sm btn-outline-danger ms-auto" data-action="delete">Delete</button>
             </div>
           </td>
         </tr>`;
       }).join('');
     }
     const pageNum = (page.number ?? assetsState.page) + 1;
     const totalPages = (page.totalPages ?? pageNum);
     assetsEl.pageInfo.textContent = `Page ${pageNum} of ${totalPages}`;
     assetsEl.prev.disabled = pageNum <= 1;
     assetsEl.next.disabled = totalPages && pageNum >= totalPages;
   }

   function wireAssetRowActions() {
     assetsEl.tbody.querySelectorAll('tr[data-asset-id]').forEach(tr => {
       const id = tr.getAttribute('data-asset-id');
       const input = tr.querySelector('[data-role="assignUserId"]');
       const btnAssign = tr.querySelector('[data-action="assign"]');
       const btnUnassign = tr.querySelector('[data-action="unassign"]');
       const btnEdit = tr.querySelector('[data-action="edit"]');
       const btnSave = tr.querySelector('[data-action="save"]');
       const btnCancel = tr.querySelector('[data-action="cancel"]');
       const btnDelete = tr.querySelector('[data-action="delete"]');

       btnAssign.addEventListener('click', async () => {
         const val = (input?.value || '').trim();
         if (!/^\d+$/.test(val)) {
           showToast('Enter a numeric userId', 'warning');
           return;
         }
         try {
           const payload = { userId: Number(val) };
           const res = await api(`/api/assets/${id}/assign`, { method: 'POST', body: JSON.stringify(payload) });
           if (!res.ok) throw new Error(`Assign failed (${res.status})`);
           showToast(`Assigned to user ${val}`, 'success');
           loadAssets();
         } catch (e) {
           showToast(e.message || 'Assign failed', 'danger');
         }
       });

       btnUnassign.addEventListener('click', async () => {
         try {
           const res = await api(`/api/assets/${id}/unassign`, { method: 'POST' });
           if (!res.ok) throw new Error(`Unassign failed (${res.status})`);
           showToast('Unassigned', 'success');
           loadAssets();
         } catch (e) {
           showToast(e.message || 'Unassign failed', 'danger');
         }
       });

       btnEdit.addEventListener('click', () => enterEditMode(tr));
       btnCancel.addEventListener('click', () => loadAssets());
       btnSave.addEventListener('click', async () => {
         try {
           const payload = collectRowPayload(tr);
           const res = await api(`/api/assets/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
           if (!res.ok) throw new Error(`Update failed (${res.status})`);
           showToast('Asset updated', 'success');
           loadAssets();
         } catch (e) {
           showToast(e.message || 'Update failed', 'danger');
         }
       });

       btnDelete.addEventListener('click', async () => {
         if (!confirm('Delete this asset?')) return;
         try {
           const res = await api(`/api/assets/${id}`, { method: 'DELETE' });
           if (!res.ok && res.status !== 204) throw new Error(`Delete failed (${res.status})`);
           showToast('Asset deleted', 'success');
           // If deleting last item on page, step back a page if possible
           if (assetsEl.tbody.children.length === 1 && assetsState.page > 0) {
             assetsState.page -= 1;
           }
           loadAssets();
         } catch (e) {
           showToast(e.message || 'Delete failed', 'danger');
         }
       });
     });
   }

   function enterEditMode(tr) {
     if (tr.getAttribute('data-editing') === 'true') return;
     tr.setAttribute('data-editing', 'true');
     const tds = tr.querySelectorAll('td');
     // Column indices: 0=id,1=name,2=type,3=status,4=serial,5=assignedUserId,6=purchaseDate,7=actions
     const nameTd = tds[1];
     const statusTd = tds[3];
     const serialTd = tds[4];
     const pdTd = tds[6];
     const actionsTd = tds[7];

     const curName = nameTd.textContent.trim();
     const curStatus = statusTd.textContent.trim();
     const curSerial = serialTd.textContent.trim();
     const curPd = pdTd.textContent.trim();

     nameTd.innerHTML = `<input class="form-control form-control-sm" value="${curName.replace(/"/g, '&quot;')}" data-edit="name" />`;
     statusTd.innerHTML = `<select class="form-select form-select-sm" data-edit="status">
       ${['IN_STOCK','ASSIGNED','REPAIR','RETIRED'].map(s => `<option value="${s}" ${s===curStatus?'selected':''}>${s}</option>`).join('')}
     </select>`;
     serialTd.innerHTML = `<input class="form-control form-control-sm" value="${curSerial.replace(/"/g, '&quot;')}" data-edit="serial" />`;
     pdTd.innerHTML = `<input type="date" class="form-control form-control-sm" value="${curPd}" data-edit="purchaseDate" />`;

     const btnEdit = actionsTd.querySelector('[data-action="edit"]');
     const btnSave = actionsTd.querySelector('[data-action="save"]');
     const btnCancel = actionsTd.querySelector('[data-action="cancel"]');
     const btnDelete = actionsTd.querySelector('[data-action="delete"]');
     const btnAssign = actionsTd.querySelector('[data-action="assign"]');
     const btnUnassign = actionsTd.querySelector('[data-action="unassign"]');

     btnEdit.classList.add('d-none');
     btnDelete.classList.add('d-none');
     btnAssign.classList.add('d-none');
     btnUnassign.classList.add('d-none');
     btnSave.classList.remove('d-none');
     btnCancel.classList.remove('d-none');
   }

   function collectRowPayload(tr) {
     const id = Number(tr.getAttribute('data-asset-id'));
     const name = tr.querySelector('[data-edit="name"]').value.trim();
     const status = tr.querySelector('[data-edit="status"]').value || null;
     const serial = tr.querySelector('[data-edit="serial"]').value.trim() || null;
     const purchaseDate = tr.querySelector('[data-edit="purchaseDate"]').value || null;
     return {
       id,
       name,
       status,
       serialNumber: serial,
       purchaseDate,
     };
   }

  // REPORTS MODULE
  let charts = { maint: null, assets: null, audit: null };
  const repEl = {
    from: null, to: null, run: null, spinner: null,
    chartMaint: null, chartAssets: null, chartAudit: null,
    tblMaint: null,
  };

  // Keep last fetched report datasets for export
  let lastReportData = { maint: [], assets: [], audit: [] };

  function initReportsElements() {
    if (inited.reports) return;
    repEl.from = document.getElementById('repFrom');
    repEl.to = document.getElementById('repTo');
    repEl.run = document.getElementById('repRun');
    repEl.spinner = document.getElementById('repSpinner');
    repEl.chartMaint = document.getElementById('chartMaint');
    repEl.chartAssets = document.getElementById('chartAssets');
    repEl.chartAudit = document.getElementById('chartAudit');
    repEl.tblMaint = document.querySelector('#tblMaint tbody');

    // Default range: last 7 days
    if (!repEl.from.value || !repEl.to.value) {
      const today = new Date();
      const to = today.toISOString().slice(0,10);
      const fromDt = new Date(today); fromDt.setDate(today.getDate() - 6);
      const from = fromDt.toISOString().slice(0,10);
      repEl.from.value = from;
      repEl.to.value = to;
    }

    repEl.run?.addEventListener('click', () => runReports());
    inited.reports = true;
   }

  async function runReports() {
    const from = repEl.from.value;
    const to = repEl.to.value;
    if (!from || !to) { showToast('Select From/To dates', 'warning'); return; }
    setLoading(repEl.spinner, true);
    try {
      const [maint, assets, audit] = await Promise.all([
        fetchReport(`/api/reports/maintenance/workload`, from, to),
        fetchReport(`/api/reports/assets/daily-status`, from, to),
        fetchReport(`/api/reports/audit/daily-actions`, from, to),
      ]);
      lastReportData.maint = maint;
      lastReportData.assets = assets;
      lastReportData.audit = audit;
      renderMaint(maint);
      renderAssetsDaily(assets);
      renderAuditDaily(audit);
      wireExports();
    } catch (e) {
      showToast(e.message || 'Report load failed', 'danger');
    } finally {
      setLoading(repEl.spinner, false);
    }
  }

  // Fetch a report dataset with from/to filters
  async function fetchReport(path, from, to) {
    const params = new URLSearchParams();
    params.set('from', from);
    params.set('to', to);
    const res = await api(`${path}?${params.toString()}`);
    if (!res.ok) throw new Error(`Report request failed (${res.status})`);
    return res.json();
  }

  // Render maintenance workload: [{ day, status, count }]
  function renderMaint(rows) {
    if (!window.Chart) return;
    const labels = Array.from(new Set(rows.map(r => r.day))).sort();
    const statuses = Array.from(new Set(rows.map(r => r.status)));
    const colorMap = buildColorMap(statuses);
    const datasets = statuses.map(s => ({
      label: s,
      backgroundColor: colorMap[s],
      data: labels.map(d => sumBy(rows.filter(r => r.day === d && r.status === s), 'count')),
      stack: 'maint'
    }));
    charts.maint?.destroy?.();
    charts.maint = new Chart(repEl.chartMaint, {
      type: 'bar',
      data: { labels, datasets },
      options: { responsive: true, scales: { x: { stacked: true }, y: { stacked: true, beginAtZero: true } } }
    });

    // Table
    if (repEl.tblMaint) {
      repEl.tblMaint.innerHTML = rows.map(r => `<tr><td>${sanitize(r.day)}</td><td>${sanitize(r.status)}</td><td>${r.count}</td></tr>`).join('');
    }
  }

  // Render assets daily status: [{ id: { bucketDate, assetStatus }, assetCount }]
  function renderAssetsDaily(rows) {
    if (!window.Chart) return;
    const labels = Array.from(new Set(rows.map(r => r.id?.bucketDate))).sort();
    const statuses = Array.from(new Set(rows.map(r => r.id?.assetStatus)));
    const colorMap = buildColorMap(statuses);
    const datasets = statuses.map(s => ({
      label: s,
      borderColor: colorMap[s],
      backgroundColor: colorMap[s] + '55',
      tension: 0.2,
      data: labels.map(d => sumBy(rows.filter(r => r.id?.bucketDate === d && r.id?.assetStatus === s), 'assetCount')),
    }));
    charts.assets?.destroy?.();
    charts.assets = new Chart(repEl.chartAssets, {
      type: 'line', data: { labels, datasets }, options: { responsive: true, scales: { y: { beginAtZero: true } } }
    });
  }

  // Render audit daily actions: [{ id: { bucketDate, auditAction }, actionCount }]
  function renderAuditDaily(rows) {
    if (!window.Chart) return;
    const labels = Array.from(new Set(rows.map(r => r.id?.bucketDate))).sort();
    const actions = Array.from(new Set(rows.map(r => r.id?.auditAction)));
    const colorMap = buildColorMap(actions);
    const datasets = actions.map(s => ({
      label: s,
      borderColor: colorMap[s],
      backgroundColor: colorMap[s] + '55',
      tension: 0.2,
      data: labels.map(d => sumBy(rows.filter(r => r.id?.bucketDate === d && r.id?.auditAction === s), 'actionCount')),
    }));
    charts.audit?.destroy?.();
    charts.audit = new Chart(repEl.chartAudit, {
      type: 'line', data: { labels, datasets }, options: { responsive: true, scales: { y: { beginAtZero: true } } }
    });
  }

  // helpers for charts
  function sumBy(arr, key) { return arr.reduce((a, b) => a + (Number(b[key]) || 0), 0); }
  function buildColorMap(keys) {
    const palette = [
      '#0d6efd', '#6f42c1', '#198754', '#dc3545', '#fd7e14', '#20c997', '#0dcaf0', '#6610f2', '#6c757d'
    ];
    const map = {};
    keys.forEach((k, i) => { map[k] = palette[i % palette.length]; });
    return map;
  }

  function wireExports() {
    // Guard against duplicate listeners on repeated runs
    if (wireExports._wired) return;
    document.getElementById('expMaint')?.addEventListener('click', () => exportCsv('maintenance', lastReportData.maint));
    document.getElementById('expAssets')?.addEventListener('click', () => exportCsv('assets_daily_status', lastReportData.assets));
    document.getElementById('expAudit')?.addEventListener('click', () => exportCsv('audit_daily_actions', lastReportData.audit));
    wireExports._wired = true;
  }

  function exportCsv(name, rows) {
    if (!rows || rows.length === 0) { showToast('Nothing to export', 'warning'); return; }
    let csv = '';
    if (name === 'maintenance') {
      csv += 'day,status,count\n';
      rows.forEach(r => { csv += `${r.day},${r.status},${r.count}\n`; });
    } else if (name === 'assets_daily_status') {
      csv += 'bucketDate,assetStatus,assetCount\n';
      rows.forEach(r => { csv += `${r.id.bucketDate},${r.id.assetStatus},${r.assetCount}\n`; });
    } else if (name === 'audit_daily_actions') {
      csv += 'bucketDate,auditAction,actionCount\n';
      rows.forEach(r => { csv += `${r.id.bucketDate},${r.id.auditAction},${r.actionCount}\n`; });
    }
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    const date = new Date().toISOString().slice(0,10);
    a.download = `${name}_${date}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  // Assignments: optional user autocomplete
  async function loadUsersForDatalist() {
    // Only admins can call /api/users; in insecure profile anyone can
    try {
      const res = await api('/api/users');
      if (!res.ok) return; // silently skip if forbidden
      const users = await res.json();
      const datalistId = 'userList';
      let dl = document.getElementById(datalistId);
      if (!dl) {
        dl = document.createElement('datalist');
        dl.id = datalistId;
        document.body.appendChild(dl);
      }
      dl.innerHTML = users.map(u => `<option value="${u.id}">${sanitize(u.fullName || u.username)} (${sanitize(u.email||'')})</option>`).join('');
      const input = document.getElementById('asgnUserId');
      if (input) input.setAttribute('list', datalistId);
    } catch {}
  }

  // Extend showView to run once and add datalist
  const origShowView4 = showView;
  showView = function(name) {
    origShowView4(name);
    if (name === 'assignments') {
      initAssignmentsElements();
      loadUsersForDatalist();
    } else if (name === 'assets') {
      initAssetsElements();
      if (lastAssetsPage == null) loadAssets();
    } else if (name === 'reports') {
      initReportsElements();
    }
  };

  // Utils
  function getApiBase() {
    // Default to localhost backend; allow override via ?api= URL param
    const urlParam = new URLSearchParams(window.location.search).get('api');
    if (urlParam) return urlParam.replace(/\/$/, '');
    return 'http://localhost:8080';
  }

  // Basic HTML sanitizer to avoid injecting HTML into tables
  function sanitize(value) {
    if (value == null) return '';
    const div = document.createElement('div');
    div.innerText = String(value);
    return div.innerHTML;
  }

  function nowSec() { return Math.floor(Date.now() / 1000); }
  function setLoading(spinnerEl, isLoading) {
    if (!spinnerEl) return;
    spinnerEl.classList.toggle('d-none', !isLoading);
  }
  function showToast(message, variant = 'info', timeout = 3500) {
    const id = 't' + Math.random().toString(36).slice(2);
    const html = `
      <div id="${id}" class="toast align-items-center text-bg-${variant} border-0 position-absolute end-0 mt-2" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">${message}</div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
      </div>`;
    el.alertContainer.insertAdjacentHTML('beforeend', html);
    const toastEl = document.getElementById(id);
    const toast = new bootstrap.Toast(toastEl, { delay: timeout });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
  }

  function saveTokens({ accessToken, refreshToken, expiresIn, refreshExpiresIn }) {
    state.accessToken = accessToken;
    state.refreshToken = refreshToken;
    // store absolute expiry in seconds
    state.accessExp = nowSec() + (expiresIn || 0);
    state.refreshExp = nowSec() + (refreshExpiresIn || 0);

    localStorage.setItem('clims.access', accessToken);
    localStorage.setItem('clims.refresh', refreshToken);
    localStorage.setItem('clims.accessExp', String(state.accessExp));
    localStorage.setItem('clims.refreshExp', String(state.refreshExp));
    updateAuthUI();
  }

  function clearTokens() {
    state.accessToken = null;
    state.refreshToken = null;
    state.accessExp = 0;
    state.refreshExp = 0;
    localStorage.removeItem('clims.access');
    localStorage.removeItem('clims.refresh');
    localStorage.removeItem('clims.accessExp');
    localStorage.removeItem('clims.refreshExp');
    updateAuthUI();
  }

  function isAccessValidSoon(bufferSec = 15) {
    return state.accessToken && (state.accessExp - nowSec() > bufferSec);
  }

  async function login(username, password) {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
      credentials: 'include',
    });
    if (!res.ok) throw new Error('Invalid credentials');
    const data = await res.json();
    saveTokens(data);
  }

  async function refreshIfNeeded() {
    if (isAccessValidSoon()) return;
    if (!state.refreshToken || state.refreshExp <= nowSec()) {
      clearTokens();
      throw new Error('Session expired');
    }
    const res = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: state.refreshToken }),
      credentials: 'include',
    });
    if (!res.ok) {
      clearTokens();
      throw new Error('Refresh failed');
    }
    const data = await res.json();
    saveTokens(data);
  }

  async function logout() {
    try {
      if (state.refreshToken) {
        await fetch(`${API_BASE}/api/auth/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: state.refreshToken }),
          credentials: 'include',
        });
      }
    } finally {
      clearTokens();
    }
  }

  // Generic API wrapper with auto-refresh on 401
  async function api(path, options = {}) {
    await refreshIfNeeded().catch(() => { /* swallow to allow 401 flow */ });
    const headers = new Headers(options.headers || {});
    if (state.accessToken) headers.set('Authorization', `Bearer ${state.accessToken}`);
    if (!headers.has('Content-Type') && options.body) headers.set('Content-Type', 'application/json');

    let res = await fetch(`${API_BASE}${path}`, { ...options, headers, credentials: 'include' });
    if (res.status === 401 && state.refreshToken) {
      await refreshIfNeeded().catch(() => {});
      if (state.accessToken) {
        headers.set('Authorization', `Bearer ${state.accessToken}`);
        res = await fetch(`${API_BASE}${path}`, { ...options, headers, credentials: 'include' });
      }
    }
    return res;
  }

  // View routing
  function showView(name) {
    el.views.forEach(v => v.classList.add('d-none'));
    const section = document.getElementById(`view-${name}`);
    if (section) section.classList.remove('d-none');
    document.querySelectorAll('.nav-link').forEach(a => a.classList.remove('active'));
    const active = document.querySelector(`a[href="#${name}"]`);
    if (active) active.classList.add('active');
  }

  function updateAuthUI() {
    const signedIn = !!state.accessToken && state.refreshExp > nowSec();
    el.authStatus.textContent = signedIn ? 'Signed in' : 'Signed out';
    el.authStatus.className = 'badge ' + (signedIn ? 'text-bg-success' : 'text-bg-secondary');
    el.btnLogout.classList.toggle('d-none', !signedIn);
    el.btnHello.disabled = !signedIn;
  }

  // Event handlers
  el.loginForm?.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!el.loginForm.checkValidity()) {
      el.loginForm.classList.add('was-validated');
      return;
    }
    setLoading(el.loginSpinner, true);
    try {
      await login(el.username.value.trim(), el.password.value);
      showToast('Login successful', 'success');
      updateAuthUI();
    } catch (err) {
      showToast(err.message || 'Login failed', 'danger');
    } finally {
      setLoading(el.loginSpinner, false);
    }
  });

  el.btnLogout?.addEventListener('click', async () => {
    await logout();
    showToast('Logged out', 'secondary');
  });

  el.btnHello?.addEventListener('click', async () => {
    setLoading(el.helloSpinner, true);
    try {
      const res = await api('/api/hello');
      const text = await res.text();
      el.helloResult.textContent = `${res.status} ${res.statusText}:\n${text}`;
    } catch (e) {
      el.helloResult.textContent = 'Request failed: ' + (e.message || e);
    } finally {
      setLoading(el.helloSpinner, false);
    }
  });

  // Nav routing
  el.navLinks.forEach(a => a.addEventListener('click', (evt) => {
    const hash = a.getAttribute('href')?.slice(1) || 'home';
    showView(hash);
  }));

  // Initial load
  updateAuthUI();
  const startView = (location.hash || '#home').slice(1);
  showView(startView);
})();
