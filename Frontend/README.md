
# 🎨 Frontend — CLIMS (Computer & Laptop Asset Management System)

This folder contains the **user interface (UI)** for CLIMS. It is built using **HTML5, CSS3, Bootstrap 5, and vanilla JavaScript** to provide a responsive and user-friendly experience.

---

## 🚀 How to Run

### Recommended: Use Live Server (VS Code)
1. Install the **Live Server** extension in VS Code
2. Right-click on `index.html`
3. Select **Open with Live Server**
4. The app will open at `http://127.0.0.1:5500` (or similar)

### Alternative: Open Directly
1. Open `index.html` in your browser directly
2. **Note**: Some browsers restrict CORS for `file://` protocol. If you encounter issues calling the backend API, use Live Server instead.

---

## 🔗 Backend Connection

The frontend connects to the backend API at `http://localhost:8080` by default.

### Prerequisites
Before using the frontend, ensure the backend is running:

#### Option 1: Secure Mode (JWT Required)
```bash
cd Backend/backend
./mvnw spring-boot:run
```
- Login with credentials to get a JWT token
- Token is automatically included in API requests

#### Option 2: Insecure Dev Mode (No Auth Required)
```bash
cd Backend/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,insecure
```
- **WARNING**: Only use for local development!
- All endpoints permit unauthenticated access
- CORS is enabled for common localhost ports

---

## 📂 Folder Structure
- `index.html` → Main landing page with login and navigation
- `assets/css/style.css` → Custom styles and Bootstrap overrides
- `assets/js/app.js` → Authentication, JWT management, and API calls
- `assets/img/` → Images or logos (optional)

---

## 🔐 Authentication & Features

### Login
- The app starts with a login form
- **Demo credentials**:
  - Admin: `admin` / `admin` (ROLE_ADMIN)
  - User: `user` / `user` (ROLE_USER)
- On successful login:
  - JWT tokens are stored in localStorage
  - User info is displayed in the navbar
  - Main app content becomes accessible

### JWT Token Management
- Access tokens are automatically included in API requests via `Authorization: Bearer <token>` header
- Tokens persist across page refreshes (stored in localStorage)
- Logout clears all stored tokens

### Available Tabs
- **Home**: Welcome screen with backend API test button (`/api/hello`)
- **Assets**: Asset management (placeholder)
- **Assignments**: Asset assignments (placeholder)
- **Reports**: Reporting features (placeholder)

---

## 🎨 UI Features
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Bootstrap 5**: Modern component library
- **Toast Notifications**: Success/error messages
- **Tab Navigation**: Organized content sections
- **Loading States**: Visual feedback during API calls

---

## 🛠️ Tech Stack
- **HTML5** → Semantic structure
- **CSS3** → Custom styling with CSS variables
- **Bootstrap 5** → Responsive layout and components
- **JavaScript (ES6+)** → Authentication, API calls, and interactivity
- **localStorage** → Client-side JWT token persistence

---

## 🧪 Testing the Connection

1. Start the backend (see above)
2. Open the frontend in Live Server
3. Login with `admin` / `admin`
4. Click **"Call /api/hello"** button
5. You should see: `✅ Success: ✅ CLIMS Backend is running!`

---

## 🔒 Security Notes

### For Development (Insecure Profile)
- Backend binds to `127.0.0.1` only (loopback)
- CORS allows common localhost origins
- **Never use insecure profile in production!**

### For Production
- Use the secure profile with JWT authentication
- Configure CORS to allow only your production frontend origin
- Use HTTPS for all communication
- Set strong JWT secrets via environment variables

---

## 📌 Future Enhancements
- Asset registration forms
- Asset assignment workflow
- Maintenance request tracking
- Reporting dashboards
- Real-time updates
- File uploads for asset images
- Advanced filtering and search
