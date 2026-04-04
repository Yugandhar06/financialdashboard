import React, { useEffect, useState, useContext } from 'react';
import { AuthContext } from '../../App';
import { auditLogService } from '../../api/auditLogService';
import { Clock, User, Trash2, Edit3, Plus } from 'lucide-react';
import './AuditLog.css';

export default function AuditLog() {
  const { user } = useContext(AuthContext);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filter, setFilter] = useState('all');
  const [page, setPage] = useState(0);

  useEffect(() => {
    loadAuditLogs();
  }, [filter, page]);

  async function loadAuditLogs() {
    try {
      setLoading(true);
      setError(null);

      let response;
      if (filter === 'recent') {
        response = await auditLogService.getRecentChanges(24);
      } else if (filter === 'deleted') {
        response = await auditLogService.getDeletedRecords(page, 20);
      } else {
        // Default to recent changes
        response = await auditLogService.getRecentChanges(24);
      }

      // Handle response structure
      const data = response.data ? response.data : response;
      setLogs(Array.isArray(data) ? data : data.content || []);
    } catch (e) {
      console.error('Error loading audit logs:', e);
      setError('Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  }

  function getActionIcon(action) {
    switch (action) {
      case 'CREATE':
        return <Plus size={16} className="action-icon create" />;
      case 'UPDATE':
        return <Edit3 size={16} className="action-icon update" />;
      case 'DELETE':
        return <Trash2 size={16} className="action-icon delete" />;
      default:
        return null;
    }
  }

  function getActionColor(action) {
    switch (action) {
      case 'CREATE':
        return 'create';
      case 'UPDATE':
        return 'update';
      case 'DELETE':
        return 'delete';
      default:
        return '';
    }
  }

  function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleString();
  }

  if (!user?.role === 'ADMIN') {
    return <div className="audit-error">Access Denied. Admin only.</div>;
  }

  return (
    <div className="audit-container">
      <header className="audit-header">
        <h1 className="audit-title">Audit Trail</h1>
        <p className="audit-subtitle">Track all system changes and user actions</p>
      </header>

      <div className="audit-filters">
        <button
          className={`filter-btn ${filter === 'all' ? 'active' : ''}`}
          onClick={() => { setFilter('all'); setPage(0); }}
        >
          All Changes
        </button>
        <button
          className={`filter-btn ${filter === 'recent' ? 'active' : ''}`}
          onClick={() => { setFilter('recent'); setPage(0); }}
        >
          Last 24 Hours
        </button>
        <button
          className={`filter-btn ${filter === 'deleted' ? 'active' : ''}`}
          onClick={() => { setFilter('deleted'); setPage(0); }}
        >
          Deleted Records
        </button>
      </div>

      {loading && <div className="audit-loading">Loading audit logs...</div>}
      {error && <div className="audit-error">{error}</div>}

      {!loading && logs.length === 0 && (
        <div className="audit-empty">No audit logs found for this filter</div>
      )}

      {!loading && logs.length > 0 && (
        <div className="audit-logs">
          {logs.map((log, idx) => (
            <div key={log.id || idx} className={`audit-entry ${getActionColor(log.action)}`}>
              <div className="entry-icon">
                {getActionIcon(log.action)}
              </div>

              <div className="entry-content">
                <div className="entry-header">
                  <span className={`action-badge ${log.action.toLowerCase()}`}>
                    {log.action}
                  </span>
                  <span className="entity-type">{log.entityType}</span>
                  <span className="entity-id">#{log.entityId}</span>
                </div>

                <div className="entry-details">
                  {log.fieldName && (
                    <div className="change-detail">
                      <span className="field-name">Field: {log.fieldName}</span>
                      <span className="change-arrow">→</span>
                      <span className="old-value">{log.oldValue || '(empty)'}</span>
                      <span className="new-value">{log.newValue || '(empty)'}</span>
                    </div>
                  )}
                </div>

                <div className="entry-footer">
                  <div className="by-user">
                    <User size={14} />
                    <span>{log.changedBy}</span>
                  </div>
                  <div className="timestamp">
                    <Clock size={14} />
                    <span>{formatTimestamp(log.timestamp)}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {!loading && logs.length > 0 && (
        <div className="audit-pagination">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="pagination-btn"
          >
            Previous
          </button>
          <span className="page-number">Page {page + 1}</span>
          <button
            onClick={() => setPage(page + 1)}
            disabled={logs.length < 20}
            className="pagination-btn"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
