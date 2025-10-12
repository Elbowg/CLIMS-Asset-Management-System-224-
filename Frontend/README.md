
# 🎨 Frontend — CLIMS (Computer & Laptop Asset Management System)

This folder contains the **user interface (UI)** for CLIMS. It is built using **HTML5, CSS3, and Bootstrap** to provide a responsive and user-friendly experience.

---

## 🚀 How to Run

### Option 1: Live Server (Recommended)
1. Install the **Live Server** extension in VS Code
2. Right-click on `index.html`
3. Select **Open with Live Server**
4. The frontend will open at `http://127.0.0.1:5500` (or similar)

### Option 2: Direct Browser Access
1. Open `index.html` directly in your browser
2. **Note**: Some browsers may block API calls due to CORS when using `file://` protocol
3. If you encounter CORS issues, use Live Server instead

---

## 🔗 Backend Connection

The frontend connects to the backend API at `http://127.0.0.1:8080` by default.

**Before using the frontend:**
1. Start the backend server (see Backend README)
2. Use either:
   - **Secured mode**: `./mvnw spring-boot:run` (requires login)
   - **Insecure dev mode**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local,insecure`

**Default credentials for testing:**
- Admin: `admin` / `admin`
- User: `user` / `user`

---

## 📂 Folder Structure
- `index.html` → Main landing page of the application
- `assets/css/style.css` → Custom styles and Bootstrap overrides
- `assets/js/app.js` → JavaScript for login, API calls, and client-side logic
- `assets/img/` → Images or logos used in the UI

---

## 📌 Features

### Current Features
- User authentication (login/logout)
- JWT token management
- API testing interface (`/api/hello` endpoint)
- Responsive dashboard with Bootstrap
- Tab-based navigation

### Planned Features
- Asset Registration
- Asset Assignment
- Maintenance Requests
- Reporting & Analytics

---

## 🛠️ Tech Stack
- **HTML5** → Structure
- **CSS3** → Styling
- **Bootstrap 5** → Layout and components
- **JavaScript (Vanilla)** → Interactivity and API calls

---

## 🔧 Development Notes

### CORS Configuration
The backend's `insecure` profile includes CORS configuration to allow requests from:
- `http://localhost:5500` (Live Server default)
- `http://127.0.0.1:5500`
- `file://` origin (for direct browser access)

### API Base URL
The API base URL is configured in `assets/js/app.js`:
```javascript
apiBase: 'http://127.0.0.1:8080'
```

Change this if your backend runs on a different port.

---

## 📖 Usage Guide

1. **Start the backend** (see Backend README)
2. **Open the frontend** using Live Server or direct browser access
3. **Login** with default credentials (`admin`/`admin`)
4. **Test the connection** by clicking "Call /api/hello"
5. **Explore** the different tabs (Assets, Assignments, Maintenance, Reports)

---

## 🐛 Troubleshooting

**Problem**: API calls fail with CORS errors
- **Solution**: Use Live Server instead of opening `index.html` directly

**Problem**: Login fails with network error
- **Solution**: Ensure the backend is running on `http://127.0.0.1:8080`

**Problem**: 401 Unauthorized on API calls
- **Solution**: Login first to get a valid JWT token
