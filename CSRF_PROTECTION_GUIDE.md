# CSRF Protection - Hướng Dẫn Sử Dụng

## Tổng Quan

CSRF (Cross-Site Request Forgery) protection đã được cấu hình cho dự án của bạn. Nó sẽ bảo vệ các endpoint POST, PUT, DELETE khỏi các tấn công CSRF.

## Các Thành Phần CSRF

### 1. **SecurityConfig.java**
- Cấu hình Spring Security với CSRF protection
- CSRF token được lưu trữ trong HTTP-only cookies
- Các endpoint exempted: `/auth/register`, `/auth/login`, `/csrf-token`

### 2. **CsrfController.java**
- Cung cấp hai endpoints:
  - `GET /csrf-token` - Lấy CSRF token
  - `POST /csrf-token/refresh` - Refresh CSRF token

### 3. **CsrfCookieFilter.java**
- Filter đảm bảo CSRF token luôn có sẵn trong response headers

### 4. **CorsConfig.java** (Cập nhật)
- Cho phép header `X-CSRF-TOKEN` được truyền từ frontend

## Cách Sử Dụng từ Frontend

### Bước 1: Lấy CSRF Token

```javascript
// Gọi endpoint để lấy CSRF token
fetch('http://localhost:8080/csrf-token', {
  method: 'GET',
  credentials: 'include' // Quan trọng! Để gửi cookies
})
.then(response => response.json())
.then(data => {
  const csrfToken = data.token;
  const headerName = data.headerName; // 'X-CSRF-TOKEN'
  localStorage.setItem('csrfToken', csrfToken);
})
```

### Bước 2: Sử dụng CSRF Token trong Requests

```javascript
// Lấy token từ localStorage
const csrfToken = localStorage.getItem('csrfToken');

// POST request với CSRF token
fetch('http://localhost:8080/api/resource', {
  method: 'POST',
  credentials: 'include', // Quan trọng! Để gửi cookies
  headers: {
    'Content-Type': 'application/json',
    'X-CSRF-TOKEN': csrfToken
  },
  body: JSON.stringify({
    // dữ liệu của bạn
  })
})
```

### Bước 3: Tự động Interceptor (React/Vue/Angular)

#### React với Axios:

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true
});

// Interceptor để tự động thêm CSRF token
api.interceptors.request.use(config => {
  const csrfToken = localStorage.getItem('csrfToken');
  if (csrfToken) {
    config.headers['X-CSRF-TOKEN'] = csrfToken;
  }
  return config;
});

export default api;
```

#### Vue 3:

```javascript
// main.js
import { createApp } from 'vue'
import axios from 'axios'
import App from './App.vue'

const app = createApp(App)

const api = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true
})

// Lấy CSRF token khi app khởi động
axios.get('http://localhost:8080/csrf-token', {
  withCredentials: true
})
.then(response => {
  localStorage.setItem('csrfToken', response.data.token)
})

// Thêm interceptor
api.interceptors.request.use(config => {
  const csrfToken = localStorage.getItem('csrfToken')
  if (csrfToken) {
    config.headers['X-CSRF-TOKEN'] = csrfToken
  }
  return config
})

app.config.globalProperties.$api = api
app.mount('#app')
```

## Exempted Endpoints (Không cần CSRF Token)

```
POST /auth/register
POST /auth/login
GET /csrf-token
POST /csrf-token/refresh
```

## Endpoints Cần CSRF Token

```
POST /api/**           (Tạo resource)
PUT /api/**            (Cập nhật resource)
DELETE /api/**         (Xóa resource)
PATCH /api/**          (Partial update)
POST /test/**          (Test endpoints)
```

## Cấu Hình Bổ Sung (Tùy Chọn)

### Nếu muốn exempted thêm endpoints:

Chỉnh sửa file `SecurityConfig.java`:

```java
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
    .ignoringRequestMatchers(
        "/auth/register",
        "/auth/login",
        "/csrf-token",
        "/your-endpoint"  // Thêm endpoint khác
    )
)
```

### Nếu muốn tùy chỉnh header name:

Tạo custom CSRF token repository:

```java
// Sẽ được hướng dẫn nếu cần
```

## Testing với Postman/cURL

```bash
# 1. Lấy CSRF token
curl -X GET http://localhost:8080/csrf-token \
  -b cookies.txt \
  -c cookies.txt

# 2. Sử dụng token trong POST request
curl -X POST http://localhost:8080/api/resource \
  -H "X-CSRF-TOKEN: <token-từ-bước-1>" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"key":"value"}'
```

## Khắc Phục Sự Cố

### 1. Lỗi "Invalid CSRF token"

- Đảm bảo bạn đã lấy token mới
- Kiểm tra `X-CSRF-TOKEN` header được gửi đúng
- Kiểm tra cookies được gửi (credentials: 'include')

### 2. Token không được cấp

- Kiểm tra endpoint `/csrf-token` có accessible
- Kiểm tra CORS configuration

### 3. POST/PUT/DELETE không hoạt động

- Kiểm tra token được gửi trong header
- Kiểm tra endpoint không phải exempted

## Độ An Toàn

✅ CSRF token lưu trong HTTP-only cookies (không thể truy cập qua JS)
✅ Token tự động rotate sau mỗi request
✅ Header `X-CSRF-TOKEN` yêu cầu truy cập từ cùng origin
✅ Kết hợp với rate limiting và auth

---

**Ghi Chú:** Trong môi trường production, cần kiểm tra thêm các biện pháp bảo mật khác như:
- HTTPS
- Secure cookies
- SameSite cookie attributes
- Additional CORS restrictions
