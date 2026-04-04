import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';// New Structured Component Imports
import Login from './pages/Login/Login';
import Register from './pages/Register/Register';
import Dashboard from './pages/Dashboard/Dashboard';
import Transactions from './pages/Transactions/Transactions';
import Insights from './pages/Insights/Insights';
import AuditLog from './pages/AuditLog/AuditLog';
import Users from './pages/Users/Users';
import Sidebar from './components/Sidebar/Sidebar';

import './index.css';

export const AuthContext = React.createContext();

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [user, setUser] = useState(null);

  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      setIsAuthenticated(true);
      const storedUser = localStorage.getItem('user');
      if (storedUser) setUser(JSON.parse(storedUser));
    }
  }, []);

  const login = (token, userData) => {
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('user', JSON.stringify(userData));
    setIsAuthenticated(true);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user');
    setIsAuthenticated(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      <Toaster position="top-right" />
      <Router>
        {isAuthenticated ? (
          <div style={{ display: 'flex', minHeight: '100vh', backgroundColor: 'var(--bg-dark)' }}>
            <Sidebar />
            <div style={{ flex: 1, padding: '2rem', marginLeft: '260px' }}>
              <Routes>
                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                <Route path="/dashboard" element={<Dashboard />} />
                <Route path="/transactions" element={user?.role === 'ANALYST' || user?.role === 'ADMIN' ? <Transactions /> : <Navigate to="/dashboard" />} />
                <Route path="/insights" element={user?.role === 'ANALYST' || user?.role === 'ADMIN' ? <Insights /> : <Navigate to="/dashboard" />} />
                <Route path="/audit-log" element={user?.role === 'ADMIN' ? <AuditLog /> : <Navigate to="/dashboard" />} />
                <Route path="/users" element={user?.role === 'ADMIN' ? <Users /> : <Navigate to="/dashboard" />} />
                <Route path="*" element={<Navigate to="/dashboard" replace />} />
              </Routes>
            </div>
          </div>
        ) : (
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </Routes>
        )}
      </Router>
    </AuthContext.Provider>
  );
}

export default App;
