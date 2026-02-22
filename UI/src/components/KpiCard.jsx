export default function KpiCard({ label, value, tone = 'neutral' }) {
  return (
    <article className={`kpi kpi-${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
