# CLIMS Setup Guide

This guide will help you get the CLIMS (Computer & Laptop Asset Management System) up and running quickly.

## Prerequisites

- **Java 17** or higher
- **PowerShell** (Windows) or **Bash** (Linux/Mac)
- **Modern web browser** (Chrome, Firefox, Edge)
- **VS Code** with Live Server extension (recommended for frontend)

---

## Quick Start (Windows)

### 1. Start the Backend

```powershell
# Navigate to the Backend folder
cd Backend

# Run the startup script
.\start-backend.ps1
```

**Wait for the startup message:**
```
Tomcat started on port(s): 8080
Started BackendApplication
```

The backend is now running at `http://localhost:8080`

### 2. Open the Frontend

**Option A: Using VS Code Live Server (Recommended)**
1. Open the `Frontend` folder in VS Code
2. Right-click on `index.html`
3. Select "Open with Live Server"
4. Your browser will open at `http://127.0.0.1:5500`

**Option B: Direct Browser Access**
1. Open `Frontend/index.html` in your browser
2. Note: Some CORS issues may occur with file:// protocol

### 3. Login and Test

1. In the frontend, enter:
   - Username: `admin`
   - Password: `admin`
2. Click **Login**
3. Once logged in, click **"Call /api/hello"** to test the backend connection
4. You should see: `✅ CLIMS Backend is running!`

---

## Manual Setup (All Platforms)

### Backend (Spring Boot)

```bash
# Navigate to backend module
cd Backend/backend

# Linux/Mac
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

The backend will start on port 8080 with:
- H2 in-memory database
- JWT authentication enabled
- Swagger UI at http://localhost:8080/swagger-ui/index.html

### Frontend (Static HTML/CSS/JS)

The frontend is a simple static web application. You can serve it using:

1. **VS Code Live Server** (Recommended)
   - Install the "Live Server" extension
   - Right-click `index.html` → "Open with Live Server"

2. **Python HTTP Server**
   ```bash
   cd Frontend
   python -m http.server 5500
   ```

3. **Node.js HTTP Server**
   ```bash
   cd Frontend
   npx http-server -p 5500
   ```

---

## Authentication

### Default Users

The application comes with seeded users (via Flyway migrations):
- **Admin**: `admin` / `admin` (ROLE_ADMIN)
- **User**: `user` / `user` (ROLE_USER)

### Getting a JWT Token (PowerShell)

```powershell
# Login
$loginBody = @{ username = 'admin'; password = 'admin' } | ConvertTo-Json
$tokens = Invoke-RestMethod -Method POST -Uri 'http://localhost:8080/api/auth/login' -Body $loginBody -ContentType 'application/json'

# View your token
$tokens.accessToken

# Use the token in API calls
$headers = @{ Authorization = "Bearer $($tokens.accessToken)" }
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:8080/api/hello'
```

### Using Swagger UI (Easier)

1. Open http://localhost:8080/swagger-ui/index.html
2. Find and execute `POST /api/auth/login`
3. Use request body: `{"username":"admin","password":"admin"}`
4. Copy the `accessToken` from the response
5. Click **"Authorize"** button at the top
6. Enter: `Bearer YOUR_ACCESS_TOKEN` (include the word "Bearer")
7. Now you can try any endpoint with authentication!

---

## Development Modes

### Secured Mode (Default)

Full JWT authentication and authorization:
```bash
cd Backend/backend
./mvnw spring-boot:run
```

### Insecure Mode (Development Only)

No authentication required for rapid development:
```bash
cd Backend/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,insecure
```

**⚠️ Note**: The insecure profile is experimental and may have startup issues due to missing database migrations. Use secured mode with Swagger UI for the best experience.

---

## API Endpoints

### Public Endpoints (No Authentication Required)
- `GET /actuator/health` - Health check
- `POST /api/auth/login` - Login
- `GET /swagger-ui/index.html` - API documentation

### Protected Endpoints (Require JWT Token)
- `GET /api/hello` - Test endpoint
- `GET /api/assets` - List assets
- `POST /api/assets` - Create asset (Admin only)
- And many more...

---

## Troubleshooting

### Backend won't start

**Problem**: `mvnw command not found`
- **Solution**: Navigate to `Backend/backend` directory first
- **Solution**: On Windows, use `.\mvnw.cmd` instead of `./mvnw`

**Problem**: Port 8080 already in use
- **Solution**: Stop other applications using port 8080, or change the port:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
  ```

**Problem**: Java version error
- **Solution**: Ensure Java 17 or higher is installed:
  ```bash
  java -version
  ```

### Frontend issues

**Problem**: CORS errors when opening index.html directly
- **Solution**: Use Live Server or another local web server

**Problem**: Login fails
- **Solution**: Make sure the backend is running on port 8080
- **Solution**: Check browser console for error messages

**Problem**: API calls return 401 Unauthorized
- **Solution**: Login first to get a valid JWT token
- **Solution**: Check that the token hasn't expired

### General Tips

1. **Clear browser cache** if you see old content
2. **Check browser console (F12)** for JavaScript errors
3. **Verify backend logs** in the PowerShell/terminal window
4. **Use Swagger UI** for easier API testing

---

## Next Steps

Once you have the system running:

1. **Explore the API** using Swagger UI
2. **Test the frontend features**:
   - Login/Logout
   - API testing with /api/hello
   - Navigation between tabs
3. **Check the documentation**:
   - Backend README: `Backend/README.md`
   - Frontend README: `Frontend/README.md`
   - Security details: `SECURITY.md`

---

## Support

For issues or questions:
1. Check the README files in Backend/ and Frontend/ folders
2. Review the application logs
3. Open an issue on GitHub with:
   - Steps to reproduce
   - Error messages
   - Your environment (OS, Java version, browser)

---

## Summary

**Fastest way to get started:**
1. Run `Backend\start-backend.ps1` (Windows)
2. Open `Frontend/index.html` with Live Server
3. Login with `admin/admin`
4. Click "Call /api/hello" to test

That's it! You're ready to explore CLIMS. 🎉
