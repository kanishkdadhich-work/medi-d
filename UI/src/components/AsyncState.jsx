export default function AsyncState({ loading, error, empty, emptyMessage = 'No data found.' }) {
  if (loading) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-6 text-sm text-slate-600">
        Loading data...
      </div>
    );
  }

  if (error) {
    return (
      <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">
        <p className="font-semibold">Request failed</p>
        <p>{error}</p>
      </div>
    );
  }

  if (empty) {
    return (
      <div className="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
        {emptyMessage}
      </div>
    );
  }

  return null;
}
