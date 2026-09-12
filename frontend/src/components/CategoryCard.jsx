import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import '../App.css';

export function CategoryCard({ category, count, description, icon: InjectedIcon }) {
  const to = `/search?category=${encodeURIComponent(category)}`;

  return (
    <article className={`category-card ${category.toLowerCase()}`}>
      <Link
        className="category-card-link"
        to={to}
        aria-label={`Browse ${category} records (${count} records)`}
      >
        <span className="sr-only">Browse {category} records</span>
      </Link>
      <div className="category-icon">
        {InjectedIcon && <InjectedIcon aria-hidden="true" />}
      </div>
      <h3 className="category-name">{category}</h3>
      <p className="category-description">{description}</p>
      <div className="category-meta">
        <span className="category-count">{count} record{count === 1 ? '' : 's'}</span>
        <span className="category-browse">
          Browse <ArrowRight className="btn-icon" aria-hidden="true" />
        </span>
      </div>
    </article>
  );
}