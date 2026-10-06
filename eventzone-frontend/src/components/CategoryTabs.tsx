import type { EventCategory } from '../types/event';

interface CategoryTabsProps {
  categories: EventCategory[];
  selected: string | null;
  onSelect: (category: string | null) => void;
}

function tabClass(active: boolean) {
  return [
    'rounded-full border px-4 py-2 text-sm font-medium transition',
    active
      ? 'border-brand-600 bg-brand-600 text-white'
      : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'
  ].join(' ');
}

export default function CategoryTabs({ categories, selected, onSelect }: CategoryTabsProps) {
  return (
    <div className="flex flex-wrap gap-2" role="tablist" aria-label="Event categories">
      <button type="button" role="tab" aria-selected={selected === null} className={tabClass(selected === null)} onClick={() => onSelect(null)}>
        All
      </button>
      {categories.map((category) => (
        <button
          key={category.id}
          type="button"
          role="tab"
          aria-selected={selected === category.name}
          className={tabClass(selected === category.name)}
          onClick={() => onSelect(category.name)}
        >
          {category.name}
        </button>
      ))}
    </div>
  );
}
