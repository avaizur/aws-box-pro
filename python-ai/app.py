"""
AI Document Analysis Service — Python Flask AI Backend
=======================================================
Runs on port 5000. Nginx routes /ai/* to this service.
Called exclusively by the Java API (not directly by the frontend).

Endpoints:
  POST /analyze   → Analyse text: returns summary, word_count, classification
  GET  /health    → Health check

API Contract with Java API:
  Request:
    POST /analyze
    Content-Type: application/json
    { "text": "Your document content..." }

  Response:
    {
      "summary":        "Short summary of the document...",
      "word_count":     142,
      "classification": "technical",
      "processing_ms":  18
    }

Classification labels:
  - "technical"   → contains code, specs, or engineering terminology
  - "legal"       → contracts, terms, compliance language
  - "financial"   → numbers, budgets, reports, revenue
  - "general"     → everything else
"""

import os
import re
import time
from flask import Flask, request, jsonify

app = Flask(__name__)

# ── Health Check ─────────────────────────────────────────────────────────────

@app.route("/health")
def health():
    return jsonify({"status": "ok", "service": "python-ai", "version": "1.0.0"})


# ── Main Analysis Endpoint ────────────────────────────────────────────────────

@app.route("/analyze", methods=["POST"])
def analyze():
    """
    Receives text from the Java API.
    Returns: summary, word_count, classification, processing_ms.
    """
    data = request.get_json(force=True)
    text = data.get("text", "").strip()

    if not text:
        return jsonify({"error": "Field 'text' is required and cannot be empty."}), 400

    start_time = time.time()

    word_count = _count_words(text)
    classification = _classify(text)
    summary = _summarize(text)
    pii_entities = _detect_pii(text)

    processing_ms = int((time.time() - start_time) * 1000)

    return jsonify({
        "summary":        summary,
        "word_count":     word_count,
        "classification": classification,
        "pii_entities":   pii_entities,
        "processing_ms":  processing_ms,
    })


# ── Analysis Functions ────────────────────────────────────────────────────────

def _count_words(text: str) -> int:
    """Count words by splitting on whitespace."""
    return len(text.split())


def _classify(text: str) -> str:
    """
    Keyword-based document classification.
    Simple and transparent — easy to explain and extend.
    Replace with an ML model in a future phase.
    """
    text_lower = text.lower()

    technical_keywords = [
        "function", "class", "algorithm", "api", "server", "database",
        "software", "hardware", "network", "code", "system", "protocol",
        "interface", "module", "deployment", "infrastructure", "endpoint"
    ]
    legal_keywords = [
        "agreement", "contract", "clause", "liability", "indemnify",
        "jurisdiction", "breach", "party", "parties", "terms", "conditions",
        "obligations", "compliance", "regulation", "pursuant", "herein"
    ]
    financial_keywords = [
        "revenue", "profit", "loss", "budget", "forecast", "invoice",
        "payment", "tax", "expense", "cost", "balance", "financial",
        "quarterly", "annual report", "earnings", "roi", "cashflow"
    ]

    scores = {
        "technical":  sum(1 for kw in technical_keywords  if kw in text_lower),
        "legal":      sum(1 for kw in legal_keywords      if kw in text_lower),
        "financial":  sum(1 for kw in financial_keywords  if kw in text_lower),
    }

    best = max(scores, key=scores.get)
    return best if scores[best] > 0 else "general"


def _summarize(text: str, max_sentences: int = 3) -> str:
    """
    Extractive summarisation — picks the first N sentences.
    A simple, explainable baseline. Replace with an LLM/ML model later.
    """
    # Split into sentences
    sentences = re.split(r'(?<=[.!?])\s+', text.strip())
    sentences = [s.strip() for s in sentences if len(s.strip()) > 10]

    if not sentences:
        return "No meaningful content found to summarise."

    # Take up to max_sentences
    selected = sentences[:max_sentences]
    summary = " ".join(selected)

    # Truncate if very long (safety net)
    if len(summary) > 500:
        summary = summary[:497] + "..."

    return summary


def _detect_pii(text: str) -> list:
    """
    Identifies sensitive Personally Identifiable Information (PII).
    Returns a list of detected entity types and counts.
    """
    patterns = {
        "email":       r'[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}',
        "phone":       r'(?:\+44|0)7\d{9}|\b\d{3}[-.\s]??\d{3}[-.\s]??\d{4}\b',
        "credit_card": r'\b(?:\d{4}[ -]?){3}\d{4}\b',
    }

    results = []
    for label, pattern in patterns.items():
        matches = re.findall(pattern, text)
        if matches:
            results.append({
                "type": label,
                "count": len(matches)
            })

    return results


# ── Entry Point ───────────────────────────────────────────────────────────────

if __name__ == "__main__":
    port = int(os.environ.get("AI_SERVICE_PORT", 5000))
    app.run(host="0.0.0.0", port=port, debug=False)
