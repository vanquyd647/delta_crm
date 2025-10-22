import urllib.request
import json

def test_health():
    """Test health check and backend connection."""
    url = 'http://localhost:5000/health'
    try:
        with urllib.request.urlopen(url, timeout=5) as r:
            data = json.loads(r.read().decode())
            print('✓ HEALTH CHECK:', r.status)
            print(f"  Status: {data.get('status')}")
            print(f"  Backend: {data.get('backend_url')}")
            print(f"  Services loaded: {data.get('services_loaded')}")
            print()
    except Exception as e:
        print('✗ Health check failed:', repr(e))
        print()

def test_root():
    """Test root endpoint."""
    url = 'http://localhost:5000/'
    try:
        with urllib.request.urlopen(url, timeout=5) as r:
            data = json.loads(r.read().decode())
            print('✓ ROOT:', r.status)
            print(f"  Service: {data.get('service')}")
            print(f"  Version: {data.get('version')}")
            print()
    except Exception as e:
        print('✗ Root endpoint failed:', repr(e))
        print()

def test_recommend(query, top_k=3):
    """Test recommendation endpoint."""
    url = 'http://localhost:5000/recommend'
    payload = {"query": query, "top_k": top_k}
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            response = json.loads(r.read().decode())
            print(f'✓ RECOMMEND "{query}":', r.status)
            print(f"  Analysis: {response.get('analysis')}")
            print(f"  Results: {response.get('count')} services")
            for idx, svc in enumerate(response.get('results', [])[:2], 1):
                print(f"    {idx}. {svc.get('name')} - {svc.get('price'):,}đ (score: {svc.get('score', 0):.3f})")
            print()
    except Exception as e:
        print(f'✗ Recommend failed for "{query}":', repr(e))
        print()

def test_analyze():
    """Test analytics endpoint."""
    url = 'http://localhost:5000/analyze/services'
    try:
        with urllib.request.urlopen(url, timeout=5) as r:
            data = json.loads(r.read().decode())
            print('✓ ANALYTICS:', r.status)
            stats = data.get('stats', {})
            print(f"  Total services: {stats.get('count')}")
            print(f"  Price range: {stats.get('price_min'):,.0f}đ - {stats.get('price_max'):,.0f}đ")
            print(f"  Average price: {stats.get('price_mean'):,.0f}đ")
            print()
    except Exception as e:
        print('✗ Analytics failed:', repr(e))
        print()

if __name__ == '__main__':
    print("=" * 60)
    print("Testing Dental AI ML Service")
    print("=" * 60)
    print()

    # Test all endpoints
    test_health()
    test_root()
    test_recommend("tẩy trắng răng", top_k=2)
    test_recommend("nhổ răng khôn", top_k=2)
    test_recommend("làm răng sứ", top_k=2)
    test_analyze()

    print("=" * 60)
    print("Testing complete!")
    print("=" * 60)

