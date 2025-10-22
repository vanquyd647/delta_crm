import pandas as pd
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import urllib.request
import json
import os

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
        q_vec = self.vectorizer.transform([q])
        sims = cosine_similarity(q_vec, self.service_vectors).flatten()

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

