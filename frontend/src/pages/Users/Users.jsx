import React, { useEffect, useState } from 'react';
import { userService } from '../../api/userService';
import { Loader2, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import './Users.css';

export default function Users() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const resData = await userService.getUsers();
      setUsers(resData.data || []);
    } catch (err) {
      toast.error("Failed to load users");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleRoleChange = async (userId, newRole) => {
    try {
      await userService.updateRole(userId, newRole);
      toast.success("User role updated successfully");
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u));
    } catch (err) {
      toast.error(err.message || "Failed to update role");
    }
  };

  const handleStatusToggle = async (userId, currentStatus) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await userService.updateStatus(userId, newStatus);
      toast.success(`User set to ${newStatus}`);
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, status: newStatus } : u));
    } catch (err) {
      toast.error(err.message || "Failed to update status");
    }
  };

  const handleDelete = async (userId, userEmail) => {
    if (!window.confirm(`Are you certain you want to permanently obliterate ${userEmail} and all their traces?`)) return;
    try {
      await userService.deleteUser(userId);
      toast.success("User successfully terminated.");
      setUsers(prev => prev.filter(u => u.id !== userId));
    } catch (err) {
      toast.error(err.message || "Failed to delete user");
    }
  };

  return (
    <div className="users-container">
      <header className="users-header">
        <div>
          <h1 className="users-title">User Management</h1>
          <p className="users-subtitle">Add, remove, and define access levels for team members.</p>
        </div>
      </header>

      <div className="glass-panel users-panel">
        {loading ? (
          <div className="tx-loading">
            <Loader2 className="spinner" size={32} />
            <p>Loading users...</p>
          </div>
        ) : (
          <div className="tx-table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th style={{ textAlign: 'right' }}>Status</th>
                  <th style={{ textAlign: 'center' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {(!users || users.length === 0) ? (
                  <tr>
                    <td colSpan="4" className="tx-empty">No users found.</td>
                  </tr>
                ) : (
                  users.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontWeight: 500 }}>{u.name}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.email}</td>
                      <td>
                        <select 
                          className="input-base users-select"
                          value={u.role}
                          onChange={(e) => handleRoleChange(u.id, e.target.value)}
                        >
                          <option value="VIEWER">Viewer</option>
                          <option value="ANALYST">Analyst</option>
                          <option value="ADMIN">Admin</option>
                        </select>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button 
                          className={`status-btn ${u.status === 'ACTIVE' ? 'active-btn' : 'inactive-btn'}`}
                          onClick={() => handleStatusToggle(u.id, u.status)}
                        >
                          {u.status}
                        </button>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <button className="action-btn" onClick={() => handleDelete(u.id, u.email)} title="Delete user">
                           <Trash2 size={16} />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
