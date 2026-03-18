import React, { useState } from 'react';
import './App.css';

const API_BASE = '/api';

export default function App() {
  const [activeTab, setActiveTab] = useState('analyze');
  const [inputText, setInputText] = useState('');
  const [selectedFile, setSelectedFile] = useState(null);
  const [result, setResult] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // ── Submit text for analysis ──────────────────────────────────
  const handleTextSubmit = async (e) => {
    e.preventDefault();
    if (!inputText.trim()) { setError('Please enter some text to analyse.'); return; }
    setLoading(true); setError(''); setResult(null);
    try {
      const res = await fetch(`${API_BASE}/analyze/text`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: inputText }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Analysis failed');
      setResult(data);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  };

  // ── Submit file for analysis ──────────────────────────────────
  const handleFileSubmit = async (e) => {
    e.preventDefault();
    if (!selectedFile) { setError('Please select a file.'); return; }
    setLoading(true); setError(''); setResult(null);
    try {
      const form = new FormData();
      form.append('file', selectedFile);
      const res = await fetch(`${API_BASE}/analyze/file`, { method: 'POST', body: form });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'File analysis failed');
      setResult(data);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  };

  // ── Load history ──────────────────────────────────────────────
  const loadHistory = async () => {
    try {
      const res = await fetch(`${API_BASE}/analyze/history`);
      const data = await res.json();
      setHistory(data);
    } catch { setHistory([]); }
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setResult(null); setError('');
    if (tab === 'history') loadHistory();
  };

  // ── Export result to S3 ───────────────────────────────────────
  const handleExport = async (requestId) => {
    try {
      const res = await fetch(`${API_BASE}/analyze/${requestId}/export`, { method: 'POST' });
      const data = await res.json();
      alert(`Exported to S3!\nKey: ${data.s3Key}`);
    } catch { alert('Export failed.'); }
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-inner">
          <div className="logo">
            <span className="logo-icon">📄</span>
            <div>
              <h1>AI Document Analysis</h1>
              <p className="tagline">Pilot Project · AWS Learning</p>
            </div>
          </div>
          <nav className="nav">
            {['analyze', 'history'].map(tab => (
              <button key={tab} className={`nav-btn ${activeTab === tab ? 'active' : ''}`}
                onClick={() => handleTabChange(tab)}>
                {tab === 'analyze' ? '🔍 Analyse' : '📋 History'}
              </button>
            ))}
          </nav>
        </div>
      </header>

      <main className="main">
        {activeTab === 'analyze' && (
          <div className="analyze-page">
            <div className="input-panel">
              <div className="mode-tabs">
                <button className="mode-tab active" id="text-mode">Text Input</button>
                <span className="divider">or</span>
                <label className="mode-tab file-label" htmlFor="file-input">
                  Upload File
                  <input id="file-input" type="file" accept=".txt,.md,.csv"
                    onChange={e => setSelectedFile(e.target.files[0])}
                    style={{ display: 'none' }} />
                </label>
              </div>

              {selectedFile ? (
                <form onSubmit={handleFileSubmit}>
                  <div className="file-chosen">
                    <span className="file-icon">📎</span>
                    <span>{selectedFile.name}</span>
                    <button type="button" className="clear-btn"
                      onClick={() => setSelectedFile(null)}>✕</button>
                  </div>
                  <button type="submit" className="btn-primary" disabled={loading} id="submit-file-btn">
                    {loading ? <span className="spinner" /> : '⚡ Analyse File'}
                  </button>
                </form>
              ) : (
                <form onSubmit={handleTextSubmit}>
                  <textarea
                    id="text-input"
                    className="text-input"
                    placeholder="Paste or type your document content here..."
                    value={inputText}
                    onChange={e => setInputText(e.target.value)}
                    rows={10}
                  />
                  <div className="input-footer">
                    <span className="word-hint">{inputText.split(/\s+/).filter(Boolean).length} words</span>
                    <button type="submit" className="btn-primary" disabled={loading} id="submit-text-btn">
                      {loading ? <span className="spinner" /> : '⚡ Analyse Text'}
                    </button>
                  </div>
                </form>
              )}

              {error && <div className="alert error">⚠️ {error}</div>}
            </div>

            {result && <ResultCard result={result} onExport={handleExport} />}
          </div>
        )}

        {activeTab === 'history' && (
          <div className="history-page">
            <h2 className="section-title">Analysis History</h2>
            {history.length === 0
              ? <div className="empty-state">No analyses yet. Submit a document to get started.</div>
              : history.map(item => (
                <div key={item.requestId} className="history-card">
                  <div className="history-header">
                    <span className="req-id">#{item.requestId}</span>
                    <span className={`badge badge-${item.status}`}>{item.status}</span>
                    <span className="timestamp">{new Date(item.createdAt).toLocaleString()}</span>
                  </div>
                  {item.fileName && <div className="file-label-text">📎 {item.fileName}</div>}
                  {item.summary && <p className="history-summary">{item.summary}</p>}
                  <div className="history-meta">
                    {item.wordCount && <span>📝 {item.wordCount} words</span>}
                    {item.classification && <ClassBadge label={item.classification} />}
                    <button className="export-btn" onClick={() => handleExport(item.requestId)}>
                      ↑ Export to S3
                    </button>
                  </div>
                </div>
              ))
            }
          </div>
        )}
      </main>
    </div>
  );
}

function ResultCard({ result, onExport }) {
  return (
    <div className="result-card">
      <div className="result-header">
        <h2>Analysis Result</h2>
        <span className="badge badge-completed">✓ completed</span>
      </div>

      <div className="result-grid">
        <Stat icon="📝" label="Word Count" value={result.wordCount?.toLocaleString()} />
        <Stat icon="🏷️" label="Classification" value={<ClassBadge label={result.classification} />} />
        <Stat icon="⚡" label="Processing" value={`${result.processingMs}ms`} />
        {result.fileName && <Stat icon="📎" label="File" value={result.fileName} />}
      </div>

      <div className="summary-box">
        <h3>📋 Summary</h3>
        <p>{result.summary}</p>
      </div>

      <div className="result-actions">
        <button className="btn-secondary" id="export-btn" onClick={() => onExport(result.requestId)}>
          ↑ Export to S3
        </button>
        <span className="req-id-small">Request #{result.requestId}</span>
      </div>
    </div>
  );
}

function Stat({ icon, label, value }) {
  return (
    <div className="stat">
      <span className="stat-icon">{icon}</span>
      <div>
        <div className="stat-label">{label}</div>
        <div className="stat-value">{value}</div>
      </div>
    </div>
  );
}

const classColors = {
  technical: '#3b82f6',
  legal:     '#8b5cf6',
  financial: '#f59e0b',
  general:   '#6b7280',
};

function ClassBadge({ label }) {
  return (
    <span className="class-badge" style={{ backgroundColor: classColors[label] || '#6b7280' }}>
      {label}
    </span>
  );
}
