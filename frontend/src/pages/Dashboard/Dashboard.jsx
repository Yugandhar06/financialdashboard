import React, { useEffect, useState, useContext } from 'react';
import { AuthContext } from '../../App';
import { dashboardService } from '../../api/dashboardService';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { ArrowUpRight, ArrowDownRight, Wallet, Activity } from 'lucide-react';
import './Dashboard.css';

export default function Dashboard() {
  const { user } = useContext(AuthContext);
  const [summary, setSummary] = useState(null);
  const [trends, setTrends] = useState([]);
  const [loading, setLoading] = useState(true);

  // If user is VIEWER, they can't access summary/trends.
  const hasAnalyticsAccess = user?.role === 'ADMIN' || user?.role === 'ANALYST';

  useEffect(() => {
    async function loadDashboard() {
        try {
            if (hasAnalyticsAccess) {
                const [summaryRes, trendsRes] = await Promise.all([
                    dashboardService.getSummary(),
                    dashboardService.getMonthlyTrends(6)
                ]);
                setSummary(summaryRes.data);
                setTrends(trendsRes.data);
            } else {
                // Viewer just loads recent activity
                const recentRes = await dashboardService.getRecentActivity(10);
                setSummary({ recentTransactions: recentRes.data });
            }
        } catch(e) {
            console.error("Dashboard error", e);
        } finally {
            setLoading(false);
        }
    }
    loadDashboard();
  }, [hasAnalyticsAccess]);

  if (loading) return <div className="dash-loading">Loading insights...</div>;

  return (
    <div className="dash-container">
      <header className="dash-header">
        <h1 className="dash-title">Overview</h1>
        <p className="dash-subtitle">Welcome back, {user?.name}. Here is your financial snapshot.</p>
      </header>

      {/* Analytics accessible only to elevated roles */}
      {hasAnalyticsAccess && summary?.totalIncome !== undefined && (
        <div className="kpi-grid">
          <div className="glass-panel kpi-card">
              <div className="kpi-icon kpi-net">
                  <Wallet size={28} />
              </div>
              <div>
                  <p className="kpi-label">Net Balance</p>
                  <p className="kpi-value">${summary.netBalance?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
              </div>
          </div>

          <div className="glass-panel kpi-card">
              <div className="kpi-icon kpi-income">
                  <ArrowUpRight size={28} />
              </div>
              <div>
                  <p className="kpi-label">Total Income</p>
                  <p className="kpi-value">${summary.totalIncome?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
              </div>
          </div>

          <div className="glass-panel kpi-card">
              <div className="kpi-icon kpi-expense">
                  <ArrowDownRight size={28} />
              </div>
              <div>
                  <p className="kpi-label">Total Expenses</p>
                  <p className="kpi-value">${summary.totalExpenses?.toLocaleString(undefined, { minimumFractionDigits: 2 })}</p>
              </div>
          </div>
        </div>
      )}

      <div className={hasAnalyticsAccess ? "dash-grid" : "dash-single"}>
          {hasAnalyticsAccess && (
            <div className="glass-panel chart-panel">
               <h3 className="panel-title">Income vs Expense (6 Months)</h3>
               <div className="chart-wrapper">
                  <ResponsiveContainer width="100%" height="100%" minHeight={300}>
                      <BarChart data={trends} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                          <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.1)" vertical={false} />
                          <XAxis dataKey="month" stroke="var(--text-secondary)" tick={{ fill: 'var(--text-secondary)'}} axisLine={false} tickLine={false} />
                          <YAxis stroke="var(--text-secondary)" tick={{ fill: 'var(--text-secondary)'}} axisLine={false} tickLine={false} tickFormatter={(value) => `$${value}`} />
                          <Tooltip cursor={{ fill: 'rgba(255,255,255,0.05)' }} contentStyle={{ backgroundColor: 'var(--bg-dark)', borderColor: 'var(--border-color)', borderRadius: '8px' }} />
                          <Bar dataKey="income" fill="var(--success)" radius={[4, 4, 0, 0]} name="Income" />
                          <Bar dataKey="expenses" fill="var(--danger)" radius={[4, 4, 0, 0]} name="Expenses" />
                      </BarChart>
                  </ResponsiveContainer>
               </div>
            </div>
          )}

          <div className="glass-panel chart-panel">
             <h3 className="panel-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}><Activity size={18}/> Recent Activity</h3>
             <div className="activity-list">
                 {summary?.recentTransactions?.slice(0, 8).map(tx => (
                     <div key={tx.id} className="activity-item">
                         <div>
                             <p className="activity-cat">{tx.category}</p>
                             <p className="activity-date">{new Date(tx.date).toLocaleDateString()}</p>
                         </div>
                         <p className={tx.type === 'INCOME' ? 'amount-in' : 'amount-out'}>
                             {tx.type === 'INCOME' ? '+' : '-'}${tx.amount.toLocaleString(undefined, {minimumFractionDigits: 2})}
                         </p>
                     </div>
                 ))}
                 {(!summary?.recentTransactions || summary.recentTransactions.length === 0) && (
                     <p className="activity-empty">No recent transactions.</p>
                 )}
             </div>
          </div>
      </div>
    </div>
  );
}
