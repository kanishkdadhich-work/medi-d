export default function Pagination({ page, pageSize, totalItems, totalPages: totalPagesProp, onPageChange }) {
  const totalPages = Math.max(1, totalPagesProp || Math.ceil((totalItems || 0) / pageSize));
  const canPrev = page > 1;
  const canNext = page < totalPages;

  if (totalPages <= 1) return null;

  return (
    <div className="mt-4 flex flex-wrap items-center justify-between gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm">
      <p className="text-slate-600">Page {page} of {totalPages}{typeof totalItems === 'number' ? ` (${totalItems} items)` : ''}</p>
      <div className="flex gap-2">
        <button className="btn-ghost" type="button" disabled={!canPrev} onClick={() => onPageChange(page - 1)}>
          Previous
        </button>
        <button className="btn-ghost" type="button" disabled={!canNext} onClick={() => onPageChange(page + 1)}>
          Next
        </button>
      </div>
    </div>
  );
}
