import React, { useState, useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../App';
import { authService } from '../../api/authService';
import './Register.css'; // Mapped exactly to Login variables

export default function Register() {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('VIEWER');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
        const response = await authService.register({ name, email, password, role });
        // The backend returns a JWT + user object on successful registration implicitly
        login(response.data.token, response.data);
        navigate('/dashboard');
    } catch (err) {
        setError(err.message || 'Registration failed.');
    } finally {
        setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="glass-panel auth-panel">
        <h2 className="auth-title">Create Account</h2>
        <p className="auth-subtitle">Join us to manage your finances professionally</p>
        
        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <div>
            <label className="auth-label">Full Name</label>
            <input 
              type="text" 
              className="input-base" 
              placeholder="Alice Smith" 
              value={name}
              onChange={(e) => setName(e.target.value)}
              required 
            />
          </div>

          <div>
            <label className="auth-label">Email address</label>
            <input 
              type="email" 
              className="input-base" 
              placeholder="alice@example.com" 
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required 
            />
          </div>
          
          <div>
            <label className="auth-label">Secure Password</label>
            <input 
              type="password" 
              className="input-base" 
              placeholder="••••••••" 
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={6}
              required 
            />
          </div>

          <div>
            <label className="auth-label">Account Type</label>
            <select 
              className="input-base" 
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="VIEWER">Viewer (Read Only)</option>
              <option value="ANALYST">Analyst (Access Insights)</option>
            </select>
          </div>

          <button type="submit" className="btn-primary" disabled={loading} style={{ opacity: loading ? 0.7 : 1 }}>
            {loading ? 'Creating account...' : 'Sign Up'}
          </button>
        </form>

        <p className="auth-footer">
          Already have an account? <Link to="/login" className="auth-link">Sign in instead</Link>
        </p>
      </div>
    </div>
  );
}
