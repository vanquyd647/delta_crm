# Dental AI ML Service - Python Microservice

## 🎯 Mục tiêu
Microservice AI phân tích câu hỏi người dùng và gợi ý dịch vụ nha khoa chính xác nhất, sử dụng dữ liệu **LIVE từ database** (không dùng CSV tĩnh).

## 🏗️ Kiến trúc
```
User → Java Backend → Database
              ↓
         ML Service (Python)
              ↓
      AI Analysis (TF-IDF + Cosine Similarity)
              ↓
         Recommendations
```

### Luồng dữ liệu:
1. **Java Backend** quản lý database và expose API `/api/services`
2. **Python ML Service** gọi API để lấy dữ liệu services **realtime**
3. **Model AI** phân tích câu hỏi tiếng Việt/English và tính toán độ tương đồng
4. Trả về danh sách dịch vụ được xếp hạng theo độ phù hợp (score)

## 📁 Cấu trúc file

### Core files:
- **app.py**: Flask REST API server với các endpoint:
  - `GET /` - Thông tin service
  - `GET /health` - Kiểm tra kết nối backend
  - `POST /recommend` - AI phân tích và gợi ý dịch vụ
  - `POST /refresh` - Làm mới dữ liệu từ backend
  - `GET /analyze/services` - Thống kê dữ liệu
  
- **model.py**: AI Service Recommender
  - Lấy dữ liệu từ Java backend API
  - TF-IDF vectorization (hỗ trợ tiếng Việt)
  - Cosine similarity matching
  - Ranking và scoring

- **requirements.txt**: Python dependencies
- **test_request.py**: Test script tự động

### Deprecated:
- ~~data/services.csv~~ - Không còn dùng, thay bằng API call

## 🚀 Cài đặt và chạy

### 1. Tạo virtualenv và cài packages
```cmd
python -m venv .venv
.\.venv\Scripts\activate
pip install -r ml_service\requirements.txt
```

### 2. Cấu hình Backend URL (optional)
Mặc định: `http://localhost:8080`

Để thay đổi, set biến môi trường:
```cmd
set BACKEND_URL=http://your-backend:8080
```

### 3. Chạy ML Service
```cmd
.venv\Scripts\python.exe ml_service\app.py
```

Service sẽ chạy trên `http://0.0.0.0:5000`

### 4. Test kết nối
```cmd
.venv\Scripts\python.exe ml_service\test_request.py
```

## 📡 API Endpoints

### 1. Health Check
```http
GET http://localhost:5000/health
```
Response:
```json
{
  "success": true,
  "status": "healthy",
  "backend_url": "http://localhost:8080",
  "services_loaded": 15,
  "message": "ML service is running and connected to backend"
}
```

### 2. AI Recommendations
```http
POST http://localhost:5000/recommend
Content-Type: application/json

{
  "query": "tôi muốn tẩy trắng răng, giá bao nhiêu?",
  "top_k": 3,
  "refresh": false
}
```

Response:
```json
{
  "success": true,
  "query": "tôi muốn tẩy trắng răng, giá bao nhiêu?",
  "count": 3,
  "analysis": "AI analyzed your query and found 3 matching services",
  "results": [
    {
      "id": 3,
      "name": "TAY TRANG",
      "description": "Tẩy trắng răng chuyên sâu",
      "price": 1500000,
      "duration_minutes": 60,
      "score": 0.856
    }
  ]
}
```

### 3. Refresh Data
```http
POST http://localhost:5000/refresh
```
Làm mới dữ liệu từ database khi có thay đổi.

### 4. Analytics
```http
GET http://localhost:5000/analyze/services
```

## 🔗 Tích hợp với Java Backend

**Đã tích hợp sẵn** trong `ChatController.java`:
- Tự động gọi ML service khi có config `ml.service.url`
- Enrich response với `mlRecommendations`
- Xử lý lỗi tự động (fallback nếu ML service không khả dụng)

## 🧪 Test scenarios

### Test 1: Câu hỏi tiếng Việt
```json
{"query": "tẩy trắng răng", "top_k": 3}
```

### Test 2: Câu hỏi về giá
```json
{"query": "dịch vụ rẻ nhất", "top_k": 5}
```

### Test 3: Không có query (gợi ý mặc định)
```json
{"query": "", "top_k": 3}
```

## 🔧 Troubleshooting

### Lỗi: Cannot connect to backend
- **Nguyên nhân**: Java backend chưa chạy hoặc sai URL
- **Giải pháp**: 
  1. Chạy Java backend trước: `mvn spring-boot:run`
  2. Kiểm tra `BACKEND_URL` environment variable
  3. Test endpoint: `curl http://localhost:8080/api/services`

### Lỗi: No services loaded
- **Nguyên nhân**: Database chưa có dữ liệu hoặc API trả về format sai
- **Giải pháp**: 
  1. Kiểm tra database có records trong table `services`
  2. Gọi `/refresh` để reload data
  3. Xem logs của Java backend

## 📊 Model AI Details

### Algorithm: TF-IDF + Cosine Similarity
- **TF-IDF**: Chuyển text thành vectors, tăng trọng số từ quan trọng
- **N-grams**: (1,3) - hỗ trợ cụm từ tiếng Việt (vd: "tẩy trắng răng")
- **Cosine Similarity**: Tính độ tương đồng giữa query và mô tả dịch vụ
- **Ranking**: Xếp hạng theo score (0.0 - 1.0)

### Future Improvements:
- [ ] Thêm machine learning model (trained on booking history)
- [ ] Word embeddings (Word2Vec, FastText) cho tiếng Việt
- [ ] Collaborative filtering (dựa trên lịch sử đặt lịch)
- [ ] Deep learning (BERT Vietnamese)

