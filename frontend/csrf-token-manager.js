/**
 * CSRF Token Manager - JavaScript/TypeScript
 * 
 * Sử dụng để quản lý CSRF token phía frontend
 * Hỗ trợ: Vanilla JS, React, Vue, Angular
 */

class CsrfTokenManager {
  constructor(storageKey = 'csrfToken', apiBaseUrl = 'http://localhost:8080') {
    this.storageKey = storageKey;
    this.apiBaseUrl = apiBaseUrl;
    this.token = null;
    this.headerName = 'X-CSRF-TOKEN';
    this.parameterName = '_csrf';
  }

  /**
   * Lấy CSRF token từ server
   * @returns {Promise<Object>} Token info
   */
  async fetchToken() {
    try {
      const response = await fetch(`${this.apiBaseUrl}/csrf-token`, {
        method: 'GET',
        credentials: 'include', // Gửi cookies
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch CSRF token: ${response.statusText}`);
      }

      const data = await response.json();
      
      // Lưu token
      this.token = data.token;
      this.headerName = data.headerName || 'X-CSRF-TOKEN';
      this.parameterName = data.parameterName || '_csrf';
      
      // Lưu vào localStorage
      localStorage.setItem(this.storageKey, JSON.stringify({
        token: this.token,
        headerName: this.headerName,
        parameterName: this.parameterName,
        timestamp: Date.now()
      }));

      console.log('[CSRF] Token fetched and stored successfully');
      return data;
    } catch (error) {
      console.error('[CSRF] Error fetching token:', error);
      throw error;
    }
  }

  /**
   * Lấy token từ localStorage hoặc fetch nếu chưa có
   * @returns {Promise<string>} CSRF token
   */
  async getToken() {
    // Kiểm tra token đã lưu
    const stored = localStorage.getItem(this.storageKey);
    if (stored) {
      const data = JSON.parse(stored);
      this.token = data.token;
      this.headerName = data.headerName;
      this.parameterName = data.parameterName;
      return this.token;
    }

    // Nếu chưa có, fetch từ server
    const data = await this.fetchToken();
    return data.token;
  }

  /**
   * Refresh CSRF token
   * @returns {Promise<Object>} New token info
   */
  async refreshToken() {
    try {
      const response = await fetch(`${this.apiBaseUrl}/csrf-token/refresh`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`Failed to refresh CSRF token: ${response.statusText}`);
      }

      const data = await response.json();
      
      this.token = data.token;
      this.headerName = data.headerName || 'X-CSRF-TOKEN';
      this.parameterName = data.parameterName || '_csrf';
      
      localStorage.setItem(this.storageKey, JSON.stringify({
        token: this.token,
        headerName: this.headerName,
        parameterName: this.parameterName,
        timestamp: Date.now()
      }));

      console.log('[CSRF] Token refreshed successfully');
      return data;
    } catch (error) {
      console.error('[CSRF] Error refreshing token:', error);
      throw error;
    }
  }

  /**
   * Thêm CSRF token vào request headers
   * @param {Object} headers - Headers object
   * @returns {Object} Headers với CSRF token
   */
  addToHeaders(headers = {}) {
    if (this.token) {
      headers[this.headerName] = this.token;
    }
    return headers;
  }

  /**
   * Thêm CSRF token vào FormData (cho form submissions)
   * @param {FormData} formData - FormData object
   * @returns {FormData} FormData với CSRF token
   */
  addToFormData(formData) {
    if (this.token && this.parameterName) {
      formData.append(this.parameterName, this.token);
    }
    return formData;
  }

  /**
   * Xóa token từ storage
   */
  clearToken() {
    this.token = null;
    localStorage.removeItem(this.storageKey);
    console.log('[CSRF] Token cleared');
  }
}

// ============================================
// AXIOS INTERCEPTOR EXAMPLE
// ============================================

/**
 * Cấu hình Axios với CSRF protection
 * @param {Object} axiosInstance - Axios instance
 * @param {CsrfTokenManager} csrfManager - CSRF manager instance
 */
function setupAxiosCsrf(axiosInstance, csrfManager) {
  // Request interceptor
  axiosInstance.interceptors.request.use(async (config) => {
    // Chỉ thêm CSRF token cho POST, PUT, DELETE, PATCH
    if (['post', 'put', 'delete', 'patch'].includes(config.method?.toLowerCase())) {
      try {
        await csrfManager.getToken();
        csrfManager.addToHeaders(config.headers);
      } catch (error) {
        console.error('[CSRF] Failed to add token to request:', error);
      }
    }
    return config;
  });

  // Response interceptor
  axiosInstance.interceptors.response.use(
    response => response,
    error => {
      // Nếu lỗi 403 (Forbidden), có thể là CSRF token hết hạn
      if (error.response?.status === 403) {
        console.warn('[CSRF] Possible invalid token, refreshing...');
        return csrfManager.refreshToken().then(() => {
          // Retry request
          return axiosInstance.request(error.config);
        }).catch(err => Promise.reject(err));
      }
      return Promise.reject(error);
    }
  );
}

// ============================================
// FETCH API WRAPPER EXAMPLE
// ============================================

/**
 * Wrapper cho fetch API với CSRF protection
 * @param {string} url - URL
 * @param {Object} options - Fetch options
 * @param {CsrfTokenManager} csrfManager - CSRF manager
 * @returns {Promise<Response>}
 */
async function secureFetch(url, options = {}, csrfManager) {
  // Merge default options
  const config = {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  };

  // Thêm CSRF token cho các method cần thiết
  if (['POST', 'PUT', 'DELETE', 'PATCH'].includes(config.method?.toUpperCase())) {
    try {
      await csrfManager.getToken();
      csrfManager.addToHeaders(config.headers);
    } catch (error) {
      console.error('[CSRF] Failed to add token:', error);
      throw error;
    }
  }

  return fetch(url, config);
}

// ============================================
// USAGE EXAMPLES
// ============================================

/**
 * VANILLA JAVASCRIPT EXAMPLE
 */
/*
// Khởi tạo
const csrfManager = new CsrfTokenManager();

// Lấy token khi page load
document.addEventListener('DOMContentLoaded', async () => {
  await csrfManager.fetchToken();
});

// Sử dụng trong fetch
async function createItem(data) {
  const response = await fetch('http://localhost:8080/api/items', {
    method: 'POST',
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      [csrfManager.headerName]: await csrfManager.getToken()
    },
    body: JSON.stringify(data)
  });
  return response.json();
}
*/

/**
 * REACT EXAMPLE
 */
/*
import { useEffect, useRef } from 'react';
import axios from 'axios';

function App() {
  const csrfManagerRef = useRef(new CsrfTokenManager());

  useEffect(() => {
    // Fetch token on mount
    csrfManagerRef.current.fetchToken();

    // Setup axios
    setupAxiosCsrf(axios, csrfManagerRef.current);
  }, []);

  async function handleSubmit(data) {
    try {
      const response = await axios.post(
        'http://localhost:8080/api/items',
        data
      );
      console.log('Success:', response.data);
    } catch (error) {
      console.error('Error:', error);
    }
  }

  return (
    <div>
      <button onClick={() => handleSubmit({ name: 'test' })}>
        Submit
      </button>
    </div>
  );
}
*/

/**
 * VUE 3 EXAMPLE
 */
/*
import { onMounted, ref } from 'vue';

export default {
  setup() {
    const csrfManager = ref(new CsrfTokenManager());
    const items = ref([]);

    onMounted(async () => {
      await csrfManager.value.fetchToken();
    });

    const createItem = async (data) => {
      try {
        const token = await csrfManager.value.getToken();
        const response = await fetch('http://localhost:8080/api/items', {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': token
          },
          body: JSON.stringify(data)
        });
        return response.json();
      } catch (error) {
        console.error('Error:', error);
      }
    };

    return { items, createItem };
  }
};
*/

// Export cho CommonJS/ES Modules
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { CsrfTokenManager, setupAxiosCsrf, secureFetch };
}
