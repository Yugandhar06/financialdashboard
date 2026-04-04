import React, { useContext } from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Receipt, Settings, LogOut, Briefcase, Users, Lightbulb, Shield } from 'lucide-react';
import { AuthContext } from '../../App';
import './Sidebar.css';

export default function Sidebar() {
  const { user, logout } = useContext(AuthContext);

  return (
    <aside className="sidebar">
      <div className="sidebar-header">
        <Briefcase size={28} />
        <h1>FinancePro</h1>
      </div>

      <nav className="sidebar-nav">
        <NavLink 
            to="/dashboard" 
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
        >
          <LayoutDashboard size={20} />
          <span>Dashboard</span>
        </NavLink>

        {(user?.role === 'ANALYST' || user?.role === 'ADMIN') && (
          <NavLink 
              to="/transactions" 
              className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Receipt size={20} />
            <span>Transactions</span>
          </NavLink>
        )}

        {(user?.role === 'ANALYST' || user?.role === 'ADMIN') && (
          <NavLink 
            to="/insights" 
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Lightbulb size={20} />
            <span>Insights</span>
          </NavLink>
        )}

        {user?.role === 'ADMIN' && (
          <NavLink 
            to="/users" 
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Users size={20} />
            <span>Users</span>
          </NavLink>
        )}

        {user?.role === 'ADMIN' && (
          <NavLink 
            to="/audit-log" 
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Shield size={20} />
            <span>Audit Trail</span>
          </NavLink>
        )}

        {user?.role === 'ADMIN' && (
          <NavLink 
            to="/settings" 
            className={({ isActive }) => `sidebar-link ${isActive ? 'active' : ''}`}
          >
            <Settings size={20} />
            <span>Settings</span>
          </NavLink>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="user-profile">
            <div className="user-avatar">
                {user?.name?.charAt(0) || 'U'}
            </div>
            <div>
                <p className="user-name">{user?.name || 'User'}</p>
                <p className="user-role">{user?.role || 'VIEWER'}</p>
            </div>
        </div>

        <button onClick={logout} className="logout-btn">
            <LogOut size={20} />
            <span>Logout</span>
        </button>
      </div>
    </aside>
  );
}
