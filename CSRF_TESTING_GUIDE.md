# CSRF Protection - Hướng Dẫn Test

## 1. Testing với cURL

### Bước 1: Lấy CSRF Token

```bash
curl -X GET http://localhost:8080/csrf-token \
  -H "Accept: application/json" \
  -v \
  -c cookies.txt
```

**Output example:**
```json
{
  "token": "abc123def456ghi789...",
  "headerName": "X-CSRF-TOKEN",
  "parameterName": "_csrf"
}
```

Lưu token vào biến:
```bash
TOKEN=$(curl -s http://localhost:8080/csrf-token | jq -r '.token')
echo "Token: $TOKEN"
```

### Bước 2: POST Request với CSRF Token

```bash
curl -X POST http://localhost:8080/test/protected \
  -H "X-CSRF-TOKEN: $TOKEN" \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"name": "test"}'
```

**Success Response:**
```json
{
  "message": "Protected POST request - CSRF token verified!",
  "status": "success",
  "receivedData": "{name=test}",
  "tokenVerified": "true"
}
```

### Bước 3: Test Failure Case - POST mà không có Token

```bash
curl -X POST http://localhost:8080/test/protected \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"name": "test"}'
```

**Error Response:**
```
HTTP/1.1 403 Forbidden
Invalid CSRF token.
```

## 2. Testing với Postman

### Setup:

1. **Tạo Environment Variables** (đâu chọn "Manage Environments")
   - `base_url`: `http://localhost:8080`
   - `csrf_token`: `` (để trống, sẽ tự động fill)

### Request 1: GET CSRF Token

```
Method: GET
URL: {{base_url}}/csrf-token
Headers:
  - Accept: application/json

Script (Tests tab):
var jsonData = pm.response.json();
pm.environment.set("csrf_token", jsonData.token);
pm.environment.set("csrf_header", jsonData.headerName);
console.log("CSRF Token Set:", pm.environment.get("csrf_token"));
```

### Request 2: POST Protected Endpoint

```
Method: POST
URL: {{base_url}}/test/protected
Headers:
  - Content-Type: application/json
  - {{csrf_header}}: {{csrf_token}}

Body (raw):
{
  "name": "test",
  "value": "data"
}
```

### Request 3: PUT Protected Endpoint

```
Method: PUT
URL: {{base_url}}/test/protected/123
Headers:
  - Content-Type: application/json
  - {{csrf_header}}: {{csrf_token}}

Body (raw):
{
  "name": "updated",
  "value": "new data"
}
```

### Request 4: DELETE Protected Endpoint

```
Method: DELETE
URL: {{base_url}}/test/protected/123
Headers:
  - Content-Type: application/json
  - {{csrf_header}}: {{csrf_token}}
```

## 3. Testing với JavaScript (Browser Console)

```javascript
// 1. Lấy CSRF Token
fetch('http://localhost:8080/csrf-token', {
  method: 'GET',
  credentials: 'include'
})
.then(r => r.json())
.then(data => {
  console.log('Token:', data.token);
  window.csrfToken = data.token;
  window.csrfHeader = data.headerName;
});

// 2. POST request với token
setTimeout(() => {
  fetch('http://localhost:8080/test/protected', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [window.csrfHeader]: window.csrfToken
    },
    body: JSON.stringify({ name: 'test' })
  })
  .then(r => r.json())
  .then(data => console.log('Success:', data))
  .catch(e => console.error('Error:', e));
}, 1000);

// 3. Test mà không có token (sẽ fail)
setTimeout(() => {
  fetch('http://localhost:8080/test/protected', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ name: 'test' })
  })
  .then(r => {
    console.log('Status:', r.status);
    return r.text();
  })
  .then(data => console.log('Response:', data))
  .catch(e => console.error('Error:', e));
}, 2000);
```

## 4. Testing với React (Vite + React)

Tạo file `CSRF_TEST.jsx`:

```javascript
import { useState, useEffect } from 'react';

export default function CsrfTest() {
  const [token, setToken] = useState('');
  const [status, setStatus] = useState('');
  const [response, setResponse] = useState(null);

  useEffect(() => {
    // Fetch token on mount
    fetchToken();
  }, []);

  const fetchToken = async () => {
    try {
      const res = await fetch('http://localhost:8080/csrf-token', {
        method: 'GET',
        credentials: 'include',
        headers: { 'Accept': 'application/json' }
      });
      const data = await res.json();
      setToken(data.token);
      console.log('Token fetched:', data.token);
    } catch (error) {
      console.error('Error fetching token:', error);
      setStatus('Error: ' + error.message);
    }
  };

  const testPost = async () => {
    if (!token) {
      setStatus('Token not available');
      return;
    }

    setStatus('Testing POST request...');
    try {
      const res = await fetch('http://localhost:8080/test/protected', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': token
        },
        body: JSON.stringify({ name: 'React Test' })
      });

      const data = await res.json();
      setResponse(data);
      setStatus(res.ok ? 'Success ✓' : 'Failed ✗');
    } catch (error) {
      setStatus('Error: ' + error.message);
    }
  };

  const testWithoutToken = async () => {
    setStatus('Testing POST without token...');
    try {
      const res = await fetch('http://localhost:8080/test/protected', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name: 'Test' })
      });

      const data = res.status === 403 ? 'CSRF Protection Working!' : await res.json();
      setResponse(data);
      setStatus(res.status === 403 ? 'Protected ✓' : 'Not Protected ✗');
    } catch (error) {
      setStatus('Error: ' + error.message);
    }
  };

  const testPutRequest = async () => {
    if (!token) {
      setStatus('Token not available');
      return;
    }

    setStatus('Testing PUT request...');
    try {
      const res = await fetch('http://localhost:8080/test/protected/123', {
        method: 'PUT',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': token
        },
        body: JSON.stringify({ name: 'Updated' })
      });

      const data = await res.json();
      setResponse(data);
      setStatus(res.ok ? 'Success ✓' : 'Failed ✗');
    } catch (error) {
      setStatus('Error: ' + error.message);
    }
  };

  const testDeleteRequest = async () => {
    if (!token) {
      setStatus('Token not available');
      return;
    }

    setStatus('Testing DELETE request...');
    try {
      const res = await fetch('http://localhost:8080/test/protected/123', {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          'X-CSRF-TOKEN': token
        }
      });

      const data = await res.json();
      setResponse(data);
      setStatus(res.ok ? 'Success ✓' : 'Failed ✗');
    } catch (error) {
      setStatus('Error: ' + error.message);
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'monospace' }}>
      <h2>CSRF Protection Test</h2>
      
      <div>
        <h3>Token: <span style={{color: 'blue'}}>{token ? '✓ Available' : '✗ Loading...'}</span></h3>
        <button onClick={fetchToken}>Refresh Token</button>
      </div>

      <div style={{ marginTop: '20px', border: '1px solid #ddd', padding: '10px' }}>
        <h3>Tests:</h3>
        <button onClick={testPost} style={{marginRight: '10px'}}>Test POST</button>
        <button onClick={testPutRequest} style={{marginRight: '10px'}}>Test PUT</button>
        <button onClick={testDeleteRequest} style={{marginRight: '10px'}}>Test DELETE</button>
        <button onClick={testWithoutToken} style={{marginRight: '10px', background: 'red', color: 'white'}}>
          Test POST (No Token)
        </button>
      </div>

      <div style={{ marginTop: '20px' }}>
        <h3>Status: <span style={{color: status.includes('Error') ? 'red' : 'green'}}>{status}</span></h3>
      </div>

      {response && (
        <div style={{ marginTop: '20px', background: '#f0f0f0', padding: '10px', borderRadius: '5px' }}>
          <h3>Response:</h3>
          <pre>{JSON.stringify(response, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
```

## 5. Testing với Postman Collection

Import collection này vào Postman:

```json
{
  "info": {
    "name": "CSRF Protection Tests",
    "description": "Test collection for CSRF protection"
  },
  "auth": {
    "type": "bearer",
    "bearer": [{"key": "token", "value": "{{csrf_token}}"}]
  },
  "item": [
    {
      "name": "1. Get CSRF Token",
      "request": {
        "method": "GET",
        "url": "{{base_url}}/csrf-token",
        "header": [{"key": "Accept", "value": "application/json"}]
      },
      "event": [{
        "listen": "test",
        "script": {
          "exec": [
            "pm.test('Status is 200', function() {",
            "  pm.response.to.have.status(200);",
            "});",
            "var jsonData = pm.response.json();",
            "pm.environment.set('csrf_token', jsonData.token);",
            "pm.environment.set('csrf_header', jsonData.headerName);"
          ]
        }
      }]
    },
    {
      "name": "2. POST Protected (With Token)",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/test/protected",
        "header": [
          {"key": "Content-Type", "value": "application/json"},
          {"key": "{{csrf_header}}", "value": "{{csrf_token}}"}
        ],
        "body": {"mode": "raw", "raw": "{\"name\": \"test\"}"}
      }
    },
    {
      "name": "3. POST Protected (Without Token) - Should Fail",
      "request": {
        "method": "POST",
        "url": "{{base_url}}/test/protected",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {"mode": "raw", "raw": "{\"name\": \"test\"}"}
      }
    }
  ]
}
```

## 6. Troubleshooting

### Problem: "Invalid CSRF token"
- ✓ Đảm bảo lấy token từ `/csrf-token` trước
- ✓ Kiểm tra header name (mặc định: `X-CSRF-TOKEN`)
- ✓ Gửi credentials: `credentials: 'include'`

### Problem: Token không được cấp
- ✓ Kiểm tra `/csrf-token` endpoint accessible
- ✓ Kiểm tra CORS configuration
- ✓ Xem browser console logs

### Problem: Cookies không được gửi
- ✓ Sử dụng `credentials: 'include'` trong fetch
- ✓ Kiểm tra CORS allow credentials
- ✓ Xem Network tab để kiểm tra cookies

---

**✓ Tất cả tests passed = CSRF Protection hoạt động chính xác!**
