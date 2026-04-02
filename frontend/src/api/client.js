import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach token automatically
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Response interceptor to handle data extraction and 401 redirects
apiClient.interceptors.response.use((response) => {
  // Check if API wrapped data in a success envelope, else return raw
  const data = response.data;
  if (data && data.success === false) {
    throw new Error(data.message || "An error occurred");
  }
  return data; // returning data directly matches the old fetchApi behavior
}, (error) => {
  const isAuthEndpoint = error.config?.url?.includes('/auth/login') || error.config?.url?.includes('/auth/register');

  if (error.response && error.response.status === 401 && !isAuthEndpoint) {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user');
    
    // Only redirect if we aren't already on the login page
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
    return Promise.reject(new Error("Session expired. Please log in again."));
  }
  
  const errorMessage = error.response?.data?.message || error.message || "An error occurred";
  return Promise.reject(new Error(errorMessage));
});
