"""
Enhanced ML module for Dental AI chatbot
- Keeps existing ServiceRecommender behavior (TF-IDF on services)
- Adds Vietnamese text preprocessing (normalization, punctuation cleaning, abbreviation expansion)
- Adds a light-weight IntentClassifier (rule-based) for common dental intents
- Adds Chatbot class that composes IntentClassifier + ServiceRecommender and returns
  structured conversational responses (intent, entities, suggestions, next steps)
"""

import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import urllib.request
import json
import os
import re
import unicodedata
from typing import List, Dict, Optional, Any


class ServiceRecommender:
    """AI-powered service recommender that fetches live data from backend API.

    Features:
    - Fetches services from Java backend API (live database data)
    - Uses TF-IDF + cosine similarity for intelligent text matching
    - Analyzes Vietnamese text queries
    - Returns ranked services with confidence scores
    """

    def __init__(self, backend_url=None):
        """
        Initialize recommender with backend URL.

        Args:
            backend_url: Java backend URL (e.g. http://localhost:8080)
                        Falls back to env var BACKEND_URL or localhost:8080
        """
        self.backend_url = backend_url or os.getenv('BACKEND_URL', 'http://localhost:8080')
        self.services_endpoint = f"{self.backend_url}/api/services"
        self.df = None
        self.vectorizer = None
        self.service_vectors = None
        self._refresh_data()

    def _refresh_data(self):
        """Fetch latest services from backend API and rebuild vectors."""
        try:
            req = urllib.request.Request(self.services_endpoint)
            req.add_header('Accept', 'application/json')

            with urllib.request.urlopen(req, timeout=10) as response:
                data = json.loads(response.read().decode('utf-8'))

                # Handle different response formats
                if isinstance(data, dict) and 'data' in data:
                    services = data['data']
                elif isinstance(data, list):
                    services = data
                else:
                    services = []

                if not services:
                    raise ValueError("No services returned from backend API")

                # Convert to DataFrame
                self.df = pd.DataFrame(services)

                # Ensure required columns exist
                if 'name' not in self.df.columns:
                    self.df['name'] = ''
                if 'description' not in self.df.columns:
                    self.df['description'] = ''

                # Create combined text field for better matching
                self.df['text'] = (
                    self.df['name'].fillna('') + ' ' +
                    self.df['description'].fillna('')
                ).str.lower()

                # Build TF-IDF vectors
                # ngram_range=(1,3) captures Vietnamese multi-word phrases
                self.vectorizer = TfidfVectorizer(
                    ngram_range=(1, 3),
                    min_df=1,
                    max_df=0.9,
                    lowercase=True
                )
                self.service_vectors = self.vectorizer.fit_transform(self.df['text'])

                print(f"✓ Loaded {len(self.df)} services from backend API")

        except Exception as e:
            print(f"✗ Failed to fetch services from {self.services_endpoint}: {e}")
            # Initialize with empty dataframe as fallback
            self.df = pd.DataFrame(columns=['id', 'name', 'description', 'price', 'duration_minutes', 'text'])
            self.vectorizer = TfidfVectorizer(ngram_range=(1,3))
            self.service_vectors = self.vectorizer.fit_transform([''])

    def recommend(self, query, top_k=5, refresh=False):
        """
        Analyze query and return ranked service recommendations.

        Args:
            query: User's question/request (Vietnamese or English)
            top_k: Number of recommendations to return
            refresh: Whether to refresh data from backend before recommending

        Returns:
            List of service dicts with 'score' field indicating relevance
        """
        if refresh or self.df is None or len(self.df) == 0:
            self._refresh_data()

        if self.df is None or len(self.df) == 0:
            return []

        if not query or not query.strip():
            # No query - return top services by price (affordable first)
            subset = self.df.sort_values(by='price').head(top_k)
            results = subset.to_dict(orient='records')
            for r in results:
                r['score'] = 0.0
                r.pop('text', None)  # Remove internal field
            return results

        # Analyze query with TF-IDF + cosine similarity
        q = query.lower().strip()
        try:
            q_vec = self.vectorizer.transform([q])
            sims = cosine_similarity(q_vec, self.service_vectors).flatten()
        except Exception:
            # If vectorizer cannot transform (e.g., empty vocabulary), return empty
            sims = np.zeros((len(self.df),), dtype=float)

        # Get top_k indices sorted by similarity
        idx = np.argsort(-sims)[:top_k]

        results = []
        for i in idx:
            r = self.df.iloc[i].to_dict()
            r['score'] = float(sims[i])
            r.pop('text', None)  # Remove internal field
            results.append(r)

        return results

    def get_all_services(self):
        """Get all available services (useful for debugging)."""
        if self.df is None or len(self.df) == 0:
            self._refresh_data()

        services = self.df.to_dict(orient='records')
        for s in services:
            s.pop('text', None)
        return services


# ------------------- New helper utilities -------------------

def normalize_vietnamese_text(text: str) -> str:
    """Normalize Vietnamese text: unicode normalize, lower, remove punctuation and extra whitespace."""
    if not text:
        return ''
    # Unicode normalization
    text = unicodedata.normalize('NFC', text)
    # Lowercase
    text = text.lower()
    # Replace common punctuation with spaces
    text = re.sub(r"[\"'“”‘’()\[\]{}<>:;,.!?\\/\\|@#\$%\^&\*-_=+~`]+", ' ', text)
    # Collapse whitespace
    text = re.sub(r"\s+", ' ', text).strip()
    return text


# Small abbreviation and synonym expansion tuned for Vietnamese dental terms
ABBREVIATIONS = {
    'tẩy': 'tẩy trắng',
    'tẩy tr': 'tẩy trắng',
    'trắng': 'tẩy trắng',
    'nha khoa': 'nhakhoa',  # temporary token so we can match multiword
}

SYNONYMS = {
    'tẩy trắng': ['tẩy trắng răng', 'tẩy', 'trắng răng', 'trắng'],
    'trám': ['trám răng', 'trám'],
    'nhổ': ['nhổ răng', 'nhổ'],
    'cạo vôi': ['cạo vôi răng', 'cạo vôi'],
    'bọc răng sứ': ['bọc răng', 'bọc răng sứ'],
}


def expand_abbreviations(text: str) -> str:
    """Expand abbreviations and apply simple synonym mapping."""
    if not text:
        return ''
    for k, v in ABBREVIATIONS.items():
        # word boundary replacement
        text = re.sub(rf"\b{k}\b", v, text)
    return text


def find_service_keywords(text: str) -> List[str]:
    """Return list of service keywords detected in text using SYNONYMS map."""
    found = []
    for canonical, variants in SYNONYMS.items():
        for var in variants:
            if re.search(rf"\b{re.escape(var)}\b", text):
                found.append(canonical)
                break
    return found


class IntentClassifier:
    """Lightweight rule-based intent classifier for dental chatbot."""

    INTENTS = {
        'book_appointment': [r'đặt', r'hẹn', r'book', r'đặt lịch', r'đặt hẹn'],
        'ask_price': [r'giá', r'bao nhiêu', r'chi phí', r'cost', r'price'],
        'service_inquiry': [r'tẩy trắng', r'trám', r'nhổ', r'bọc', r'cạo vôi', r'nhức', r'đau'],
        'greeting': [r'chào', r'xin chào', r'hello', r'hi'],
        'goodbye': [r'cảm ơn', r'thông cảm ơn', r'tạm biệt', r'bye'],
        'contact': [r'điện thoại', r'phone', r'liên hệ', r'địa chỉ', r'địa chỉ của'],
    }

    def __init__(self):
        # compile patterns
        self.patterns = {intent: [re.compile(p, re.I) for p in pats] for intent, pats in self.INTENTS.items()}

    def predict(self, text: str) -> Dict[str, Any]:
        """Return predicted intent and a crude confidence score."""
        text_norm = normalize_vietnamese_text(text)
        scores = {}
        for intent, patterns in self.patterns.items():
            score = 0
            for p in patterns:
                if p.search(text_norm):
                    score += 1
            if score:
                scores[intent] = float(score)

        if not scores:
            return {'intent': 'unknown', 'confidence': 0.0}

        # choose highest score, normalize to 0..1 by dividing by number of patterns
        best_intent = max(scores.items(), key=lambda x: x[1])[0]
        max_possible = len(self.patterns.get(best_intent, [])) or 1
        confidence = min(1.0, scores[best_intent] / max_possible)
        return {'intent': best_intent, 'confidence': confidence}


class Chatbot:
    """High level chatbot that wraps recommender + intent classifier + simple dialog actions."""

    def __init__(self, recommender: ServiceRecommender):
        self.recommender = recommender
        self.intent_classifier = IntentClassifier()

    def process(self, message: str, session: Optional[Dict] = None, top_k: int = 3) -> Dict[str, Any]:
        """Process a user message and return a structured response.

        Response includes:
          - intent, confidence
          - entities (detected service keywords)
          - suggestions (list of recommended services)
          - reply: a user-facing message in Vietnamese
        """
        session = session or {}
        raw = message or ''
        text = normalize_vietnamese_text(raw)
        text = expand_abbreviations(text)

        # Intent detection
        intent_res = self.intent_classifier.predict(text)
        intent = intent_res['intent']
        confidence = intent_res['confidence']

        # Detect service keywords
        service_keywords = find_service_keywords(text)

        # If user asked about services or price, run recommender
        suggestions = []
        if intent in ('service_inquiry', 'ask_price') or service_keywords:
            # build a query favoring detected keywords + original message
            query = ' '.join(service_keywords + [text]) if service_keywords else text
            suggestions = self.recommender.recommend(query, top_k=top_k)

        # Compose reply text
        reply = ''
        if intent == 'greeting':
            reply = 'Xin chào! Tôi có thể giúp bạn về dịch vụ nha khoa, đặt lịch, hoặc báo giá. Bạn cần gì giúp đỡ?'
        elif intent == 'book_appointment':
            # Offer top suggested services and ask for booking details
            if suggestions:
                svc_names = ', '.join([s.get('name', '') for s in suggestions])
                reply = f"Bạn muốn đặt lịch cho dịch vụ: {svc_names}. Vui lòng cho biết tên, số điện thoại, và ngày giờ mong muốn."
            else:
                reply = 'Bạn muốn đặt lịch hẹn. Xin cung cấp tên, số điện thoại và thời gian mong muốn để chúng tôi hỗ trợ.'
        elif intent == 'ask_price':
            if suggestions:
                # Provide prices if available
                lines = []
                for s in suggestions:
                    price = s.get('price')
                    price_str = f"{int(price):,} VND" if price is not None and price != '' else 'Liên hệ để biết giá'
                    lines.append(f"- {s.get('name')}: {price_str}")
                # Use explicit newline joining to avoid unterminated-string issues
                reply = 'Giá tham khảo cho các dịch vụ:\n' + '\n'.join(lines)
            else:
                reply = 'Bạn muốn biết giá. Vui lòng cho biết dịch vụ cụ thể hoặc mô tả triệu chứng để tôi hỗ trợ chính xác hơn.'
        elif intent == 'service_inquiry':
            if suggestions:
                lines = []
                for s in suggestions:
                    desc = s.get('description', '') or ''
                    score = s.get('score', 0.0)
                    lines.append(f"- {s.get('name')} (độ phù hợp {score:.2f}): {desc}")
                # Use explicit newline joining to avoid unterminated-string issues
                reply = 'Mình tìm thấy các dịch vụ phù hợp:\n' + '\n'.join(lines) + '\nBạn muốn biết thêm thông tin hay đặt lịch?'
            else:
                reply = 'Mình chưa tìm thấy dịch vụ phù hợp. Bạn có thể mô tả rõ hơn triệu chứng hoặc mong muốn không?'
        elif intent == 'contact':
            reply = 'Thông tin liên hệ: Nha khoa Hoàng Bình - Số điện thoại: 0123-456-789 (ví dụ). Bạn có muốn chúng tôi gọi lại?'
        elif intent == 'goodbye':
            reply = 'Cảm ơn bạn! Chúc bạn một ngày tốt lành. Nếu cần hỗ trợ thêm, hãy nhắn cho chúng tôi.'
        else:
            # Unknown intent: fallback using suggestions or generic message
            if suggestions:
                reply = 'Mình không rõ lắm nhưng có thể bạn đang quan tâm tới các dịch vụ sau:'
                short = ', '.join([s.get('name', '') for s in suggestions])
                reply += ' ' + short + '. Bạn muốn biết thêm về dịch vụ nào?'
            else:
                reply = 'Xin lỗi, mình chưa hiểu. Bạn có thể mô tả lại hoặc hỏi về dịch vụ/giá/đặt lịch không?'

        # Build response payload
        response = {
            'success': True,
            'query': raw,
            'intent': intent,
            'confidence': confidence,
            'entities': {'services': service_keywords},
            'suggestions': suggestions,
            'reply': reply
        }

        return response


# End of file
