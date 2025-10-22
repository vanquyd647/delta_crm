from flask import Flask, request, jsonify
from model import ServiceRecommender
import os

app = Flask(__name__)

# Initialize recommender with backend URL from env or default
BACKEND_URL = os.environ.get('BACKEND_URL', 'http://localhost:8080')
rec = ServiceRecommender(backend_url=BACKEND_URL)

@app.route('/')
def index():
    return jsonify({
        'service': 'Dental AI ML Service',
        'description': 'AI-powered service recommender using live database data',
        'version': '2.0',
        'backend_url': BACKEND_URL,
        'endpoints': {
            'POST /recommend': 'Get AI-powered service recommendations',
            'POST /refresh': 'Refresh services data from backend',
            'GET /analyze/services': 'Get statistics about available services',
            'GET /health': 'Check service and backend connection health'
        }
    })

@app.route('/health', methods=['GET'])
def health():
    """Check if ML service and backend connection are healthy."""
    try:
        services = rec.get_all_services()
        return jsonify({
            'success': True,
            'status': 'healthy',
            'backend_url': BACKEND_URL,
            'services_loaded': len(services),
            'message': 'ML service is running and connected to backend'
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'status': 'unhealthy',
            'backend_url': BACKEND_URL,
            'error': str(e),
            'message': 'Cannot connect to backend API'
        }), 503

@app.route('/recommend', methods=['POST'])
def recommend():
    """
    Analyze user query and return AI-powered service recommendations.

    Request JSON:
    {
        "query": "tôi muốn tẩy trắng răng",
        "top_k": 3,
        "refresh": false  // optional: refresh data from backend first
    }

    Response:
    {
        "success": true,
        "query": "...",
        "results": [
            {
                "id": 3,
                "name": "TAY TRANG",
                "description": "Tẩy trắng răng chuyên sâu",
                "price": 1500000,
                "duration_minutes": 60,
                "score": 0.85
            }
        ],
        "analysis": "AI analyzed your query and found 3 matching services"
    }
    """
    payload = request.get_json() or {}
    query = payload.get('query', '')
    top_k = int(payload.get('top_k', 5))
    refresh = payload.get('refresh', False)

    try:
        results = rec.recommend(query, top_k=top_k, refresh=refresh)

        analysis = f"AI analyzed your query"
        if query:
            analysis += f" and found {len(results)} matching services"
        else:
            analysis += " and returned default recommendations"

        return jsonify({
            'success': True,
            'query': query,
            'results': results,
            'count': len(results),
            'analysis': analysis
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Recommendation failed: {str(e)}',
            'query': query
        }), 500

@app.route('/refresh', methods=['POST'])
def refresh():
    """
    Manually refresh services data from backend database.
    Useful when services are updated in the database.
    """
    try:
        rec._refresh_data()
        services = rec.get_all_services()
        return jsonify({
            'success': True,
            'message': 'Successfully refreshed services from backend',
            'services_count': len(services),
            'backend_url': BACKEND_URL
        })
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Refresh failed: {str(e)}',
            'backend_url': BACKEND_URL
        }), 500

@app.route('/analyze/services', methods=['GET'])
def analyze_services():
    """Get statistical analysis of available services."""
    try:
        df = rec.df
        if df is None or len(df) == 0:
            return jsonify({
                'success': False,
                'message': 'No services data available'
            }), 404

        stats = {
            'count': int(len(df)),
            'price_min': float(df['price'].min()) if 'price' in df.columns else 0,
            'price_max': float(df['price'].max()) if 'price' in df.columns else 0,
            'price_mean': float(df['price'].mean()) if 'price' in df.columns else 0,
            'avg_duration_minutes': float(df['duration_minutes'].mean()) if 'duration_minutes' in df.columns else 0,
            'backend_url': BACKEND_URL
        }

        # Add top services by price
        top_affordable = df.nsmallest(3, 'price')[['name', 'price']].to_dict(orient='records') if 'price' in df.columns else []
        stats['top_affordable'] = top_affordable

        return jsonify({'success': True, 'stats': stats})
    except Exception as e:
        return jsonify({
            'success': False,
            'message': f'Analysis failed: {str(e)}'
        }), 500

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 5000))
    print(f"🚀 Starting Dental AI ML Service on port {port}")
    print(f"📡 Backend URL: {BACKEND_URL}")
    print(f"📊 Initializing with live database data...")
    app.run(host='0.0.0.0', port=port, debug=True)

