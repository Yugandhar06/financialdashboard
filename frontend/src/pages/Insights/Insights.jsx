import React, { useEffect, useState, useContext } from 'react';
import { AuthContext } from '../../App';
import { insightsService } from '../../api/insightsService';
import { TrendingUp, TrendingDown, Zap, AlertCircle, Lightbulb } from 'lucide-react';
import './Insights.css';

export default function Insights() {
  const { user } = useContext(AuthContext);
  const [insights, setInsights] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadInsights() {
      try {
        setLoading(true);
        const response = await insightsService.getInsights();
        
        // Handle both wrapped (with data field) and unwrapped responses
        const data = response.data ? response.data : response;
        setInsights(data);
        setError(null);
      } catch (e) {
        console.error('Error loading insights:', e);
        setError('Failed to load insights. Please try again.');
      } finally {
        setLoading(false);
      }
    }
    loadInsights();
  }, []);

  if (loading) return <div className="insights-loading">Loading insights...</div>;
  if (error) return <div className="insights-error">{error}</div>;
  if (!insights) return <div className="insights-empty">No insights available</div>;

  const { spendingTrend, categoryAnalysis, budgetStatus, recommendations } = insights;

  return (
    <div className="insights-container">
      <header className="insights-header">
        <h1 className="insights-title">Advanced Insights</h1>
        <p className="insights-subtitle">Comprehensive analysis of your spending patterns and budget</p>
      </header>

      {/* Spending Trend Section */}
      <div className="insights-grid">
        <div className="glass-panel trend-panel">
          <div className="trend-header">
            <h3 className="panel-title">Spending Trend</h3>
            {spendingTrend.trendDirection === 'UP' && <TrendingUp className="trend-icon up" size={24} />}
            {spendingTrend.trendDirection === 'DOWN' && <TrendingDown className="trend-icon down" size={24} />}
            {spendingTrend.trendDirection === 'STABLE' && <Zap className="trend-icon stable" size={24} />}
          </div>

          <div className="trend-cards">
            <div className="trend-stat">
              <label>This Month</label>
              <value className="stat-value">${parseFloat(spendingTrend.thisMonth).toFixed(2)}</value>
            </div>
            <div className="trend-stat">
              <label>Last Month</label>
              <value className="stat-value">${parseFloat(spendingTrend.lastMonth).toFixed(2)}</value>
            </div>
            <div className="trend-stat">
              <label>Trend</label>
              <value className={`stat-value ${spendingTrend.trendDirection.toLowerCase()}`}>
                {spendingTrend.trendPercentage}
              </value>
            </div>
            <div className="trend-stat">
              <label>Next Month (Predicted)</label>
              <value className="stat-value">${parseFloat(spendingTrend.predictedNextMonth).toFixed(2)}</value>
            </div>
          </div>

          <p className="trend-insight">{spendingTrend.insight}</p>
        </div>

        {/* Category Analysis */}
        <div className="glass-panel category-panel">
          <h3 className="panel-title">Top Spending Category</h3>
          
          <div className="category-content">
            <div className="category-highlight">
              <h4 className="category-name">{categoryAnalysis.topCategory}</h4>
              <p className="category-amount">${parseFloat(categoryAnalysis.topCategoryAmount).toFixed(2)}</p>
              <p className="category-status">{categoryAnalysis.topCategoryTrend}</p>
            </div>

            <div className="category-breakdown">
              <h4 className="breakdown-title">Category Breakdown</h4>
              <div className="breakdown-list">
                {Object.entries(categoryAnalysis.categoryBreakdown).map(([key, detail]) => (
                  <div key={key} className="breakdown-item">
                    <div className="breakdown-info">
                      <span className="category-label">{detail.name}</span>
                      <span className={`category-status-badge ${detail.status}`}>{detail.status}</span>
                    </div>
                    <div className="breakdown-values">
                      <span>${parseFloat(detail.totalSpent).toFixed(2)}</span>
                      <span className="percent">({detail.percentOfTotal}%)</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>

        {/* Budget Status */}
        <div className="glass-panel budget-panel">
          <h3 className="panel-title">Budget Status</h3>

          <div className="budget-summary">
            <div className="budget-meter">
              <div className="progress-bar">
                <div 
                  className="progress-fill"
                  style={{
                    width: `${Math.min((budgetStatus.totalSpent / budgetStatus.totalBudget * 100), 100)}%`
                  }}
                ></div>
              </div>
              <div className="budget-text">
                <span>${parseFloat(budgetStatus.totalSpent).toFixed(2)} / ${parseFloat(budgetStatus.totalBudget).toFixed(2)}</span>
                <span className={`status-label ${budgetStatus.overallStatus}`}>{budgetStatus.overallStatus}</span>
              </div>
            </div>
          </div>

          <div className="budget-categories">
            <h4>By Category</h4>
            {Object.entries(budgetStatus.categories).map(([key, item]) => (
              <div key={key} className="budget-category">
                <div className="budget-cat-info">
                  <span>{item.category}</span>
                  <span className={`budget-status ${item.status}`}>{item.percentUsed}</span>
                </div>
                <div className="budget-bar">
                  <div 
                    className={`budget-bar-fill ${item.status}`}
                    style={{ width: `${Math.min(parseFloat(item.percentUsed), 100)}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recommendations */}
        <div className="glass-panel recommendations-panel">
          <h3 className="panel-title">Recommendations</h3>

          {recommendations.suggestions.length > 0 && (
            <div className="recommendations-group">
              <h4 className="rec-subtitle">💡 Suggestions</h4>
              <ul className="rec-list">
                {recommendations.suggestions.map((suggestion, idx) => (
                  <li key={idx}>{suggestion}</li>
                ))}
              </ul>
            </div>
          )}

          {recommendations.warnings.length > 0 && (
            <div className="recommendations-group">
              <h4 className="rec-subtitle warning">⚠️ Warnings</h4>
              <ul className="rec-list warning">
                {recommendations.warnings.map((warning, idx) => (
                  <li key={idx}>{warning}</li>
                ))}
              </ul>
            </div>
          )}

          {recommendations.opportunities.length > 0 && (
            <div className="recommendations-group">
              <h4 className="rec-subtitle opportunity">✨ Opportunities</h4>
              <ul className="rec-list opportunity">
                {recommendations.opportunities.map((opportunity, idx) => (
                  <li key={idx}>{opportunity}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
