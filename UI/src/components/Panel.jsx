export default function Panel({ title, subtitle, children, right }) {
  return (
    <section className="panel">
      <header className="panel-head">
        <div>
          <h3>{title}</h3>
          {subtitle ? <p>{subtitle}</p> : null}
        </div>
        {right ? <div className="panel-right">{right}</div> : null}
      </header>
      <div className="panel-body">{children}</div>
    </section>
  );
}
