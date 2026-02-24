import React from 'react';

export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, message: error?.message || 'Unexpected UI error' };
  }

  componentDidCatch(error) {
    console.error('UI runtime error:', error);
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="min-h-screen bg-slate-50 p-6">
          <section className="mx-auto max-w-3xl rounded-2xl border border-rose-200 bg-white p-6">
            <h1 className="text-xl font-semibold text-rose-700">Application Error</h1>
            <p className="mt-2 text-sm text-slate-700">A runtime error occurred in the UI. Use refresh after fix.</p>
            <p className="mt-3 rounded bg-rose-50 px-3 py-2 font-mono text-sm text-rose-700">{this.state.message}</p>
            <button className="btn-primary mt-4" onClick={() => window.location.reload()}>Reload</button>
          </section>
        </main>
      );
    }

    return this.props.children;
  }
}
