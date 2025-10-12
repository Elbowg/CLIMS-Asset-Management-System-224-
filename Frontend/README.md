# 🎨 Frontend — CLIMS (Computer & Laptop Asset Management System)

This folder contains the user interface (UI) for CLIMS. It is built using **HTML5, CSS3, Bootstrap, and a small vanilla JS app**.

---

## 🚀 How to Run
1) Start the backend (dev easiest with insecure profile):
   - Windows PowerShell
     
     ```powershell
     cd "c:\Users\amuma\OneDrive\Desktop\CLIMS-Asset-Management-System-224-\Backend\backend"
     .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=insecure
     ```

2) Open the frontend with a static server (recommended):
   - Use **VS Code Live Server** and open `index.html`, or any static server on http://127.0.0.1:5500

3) If your backend is not on http://localhost:8080, pass it via URL param:
   - Example: `http://127.0.0.1:5500/index.html?api=http://localhost:8081`

---

## 📂 Structure
- `index.html` — Main page with navigation, auth form, and views.
- `assets/css/style.css` — Minimal custom styles.
- `assets/js/app.js` — Core logic: auth, API wrapper, view routing, and assets UI.

---

## ✅ Features implemented
- Authentication
  - Login via `POST /api/auth/login`
  - Auto-refresh via `POST /api/auth/refresh`
  - Logout via `POST /api/auth/logout`
  - Tokens stored in `localStorage` with expirations
- Hello check
  - Button calls `GET /api/hello` with Authorization header
- Assets
  - List assets via `GET /api/assets?page=..&size=..&sort=..&status=..&assignedUserId=..`
  - Pagination controls (Prev/Next)
  - Sorting by id, name, status, assignedUserId via clickable headers
  - Simple create form (`POST /api/assets`) with fields: name, serialNumber, status, purchaseDate
  - Toast notifications for success/errors

---

## 🔐 Notes
- In the insecure profile, all endpoints are permitted and CORS is permissive for local hosts.
- In the secure profile, you must authenticate before calling protected endpoints.
- Default dev credentials are typically `admin/admin` (seeded by Flyway for H2 dev).

---

## 🧭 Next steps
- Wire Assignments and Reports views to backend endpoints
- Add update/delete asset actions and details drawer
- Add lookups for users, locations, vendors to assist asset creation