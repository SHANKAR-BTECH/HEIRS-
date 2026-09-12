import { TrendingUp } from 'lucide-react';
import '../App.css';

export function StatCard({ title, value, icon, change, changeLabel }) {
  const Icon = icon;

  return (
    <div className="stat-card">
      <div className="stat-icon">
        <Icon />
      </div>
      <div className="stat-content">
        <div className="stat-value">{value}</div>
        <div className="stat-label">{title}</div>
        {change && (
          <div className="stat-change">
            <TrendingUp /> {change} {changeLabel}
          </div>
        )}
      </div>
    </div>
  );
}