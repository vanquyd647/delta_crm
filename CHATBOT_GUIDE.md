# 🎯 HƯỚNG DẪN SỬ DỤNG DENTAL AI CHATBOT

## 📊 Kiến trúc hệ thống

```
┌─────────────┐
│   Browser   │ 
│  (Postman)  │
└──────┬──────┘
       │ HTTP Request
       ↓
┌─────────────────────────────────────────┐
│     Java Backend (Spring Boot)          │
│     Port: 8080                          │
│  ┌──────────────┐    ┌──────────────┐  │
│  │ ChatController│    │ ServiceCtrl  │  │
│  └──────┬───────┘    └──────┬───────┘  │
│         │                    │          │
│         │                    ↓          │
│         │              ┌──────────┐     │
│         │              │ Database │     │
│         │              │(MariaDB) │     │
│         │              └──────────┘     │
└─────────┼────────────────────┬──────────┘
          │                    │
          │ REST API           │ /api/services
          ↓                    │
┌─────────────────────────┐    │
│  Python ML Service      │    │
│  Port: 5000            │←───┘
│  ┌─────────────────┐   │
│  │ AI Model        │   │
│  │ (TF-IDF +       │   │
│  │  Cosine Sim)    │   │
│  └─────────────────┘   │
└─────────────────────────┘
```

## 🔄 Luồng hoạt động

### Luồng 1: User hỏi chatbot
```
1. User: "Tôi muốn tẩy trắng răng, giá bao nhiêu?"
   ↓
2. POST /api/assist {"message": "Tôi muốn tẩy trắng răng..."}
   ↓
3. Java Backend:
   - Lấy danh sách services từ database
   - Lấy danh sách nha sĩ từ database
   - Tạo gợi ý ngày/giờ
   ↓
4. Java gọi Python ML Service:
   POST http://localhost:5000/recommend
   {"query": "Tôi muốn tẩy trắng răng...", "top_k": 5}
   ↓
5. Python ML Service:
   - Gọi GET http://localhost:8080/api/services (lấy data từ DB)
   - AI phân tích câu hỏi (TF-IDF)
   - Tính độ tương đồng (Cosine Similarity)
   - Xếp hạng services theo score
   ↓
6. Python trả kết quả về Java
   ↓
7. Java merge kết quả:
   {
     "suggestedServices": [...],      // Từ DB
     "suggestedDentists": [...],      // Từ DB
     "mlRecommendations": [...],      // Từ AI
     "quickBookingTemplates": [...]
   }
   ↓
8. Response trả về UI
```

### Luồng 2: User đặt lịch
```
1. User chọn service, dentist, date, time
   ↓
2. POST /api/book {
     "fullName": "...",
     "email": "...",
     "serviceId": 3,
     "date": "01/15/2026",
     "time": "09:00",
     ...
   }
   ↓
3. Java Backend lưu vào database
   ↓
4. Response: appointment đã tạo
```

## 🚀 Cách chạy hệ thống

### Bước 1: Khởi động Java Backend
```cmd
cd D:\Project\dental-backend\dental-backend
mvn spring-boot:run
```
✅ Backend sẽ chạy trên `http://localhost:8080`

### Bước 2: Khởi động Python ML Service
```cmd
cd D:\Project\dental-backend\dental-backend
.venv\Scripts\python.exe ml_service\app.py
```
✅ ML Service sẽ chạy trên `http://localhost:5000`

### Bước 3: Test kết nối
```cmd
.venv\Scripts\python.exe ml_service\test_request.py
```

## 📡 API Endpoints để test trên Postman

### 1️⃣ Health Check ML Service
```http
GET http://localhost:5000/health
```
**Mục đích**: Kiểm tra Python có kết nối được với Java backend không

**Response mẫu**:
```json
{
  "success": true,
  "status": "healthy",
  "backend_url": "http://localhost:8080",
  "services_loaded": 15,
  "message": "ML service is running and connected to backend"
}
```

### 2️⃣ Lấy danh sách dịch vụ (từ database)
```http
GET http://localhost:8080/api/services
```
**Response mẫu**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "KHAM RANG",
      "description": "Khám tổng quát răng miệng",
      "price": 120000,
      "durationMinutes": 30
    }
  ]
}
```

### 3️⃣ Chat Assistant (gợi ý dịch vụ)
```http
POST http://localhost:8080/api/assist
Content-Type: application/json

{
  "message": "Tôi muốn tẩy trắng răng, giá bao nhiêu?"
}
```

**Response mẫu**:
```json
{
  "success": true,
  "data": {
    "messageSummary": "Tôi đã phân tích yêu cầu của bạn...",
    "suggestedServices": [
      {
        "id": 3,
        "name": "TAY TRANG",
        "price": 1500000,
        "durationMinutes": 60
      }
    ],
    "suggestedDentists": [...],
    "suggestedDates": ["01/23/2026", "01/24/2026", "01/25/2026"],
    "suggestedTimes": ["09:00", "11:00", "14:00", "16:00"],
    "mlRecommendations": [
      {
        "id": 3,
        "name": "TAY TRANG",
        "score": 0.856,
        "price": 1500000
      }
    ],
    "quickBookingTemplates": [...]
  }
}
```

### 4️⃣ Đặt lịch nhanh
```http
POST http://localhost:8080/api/book
Content-Type: application/json

{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0900123456",
  "serviceId": 1,
  "date": "01/23/2026",
  "time": "09:00",
  "dentistId": 6,
  "notes": "Đặt qua chatbot"
}
```

⚠️ **Lưu ý định dạng ngày**: `MM/dd/yyyy` (tháng/ngày/năm)
- ✅ Đúng: `01/23/2026` (23 tháng 1)
- ❌ Sai: `23/01/2026` (sẽ báo lỗi)

### 5️⃣ Test AI trực tiếp (Python service)
```http
POST http://localhost:5000/recommend
Content-Type: application/json

{
  "query": "tẩy trắng răng",
  "top_k": 3
}
```

**Response**:
```json
{
  "success": true,
  "query": "tẩy trắng răng",
  "count": 3,
  "analysis": "AI analyzed your query and found 3 matching services",
  "results": [
    {
      "id": 3,
      "name": "TAY TRANG",
      "score": 0.856,
      "price": 1500000
    }
  ]
}
```

### 6️⃣ Làm mới dữ liệu AI (khi database thay đổi)
```http
POST http://localhost:5000/refresh
```

## 🧪 Test Cases

### Test 1: Câu hỏi về dịch vụ cụ thể
```json
{
  "message": "Tôi muốn tẩy trắng răng"
}
```
✅ Kỳ vọng: AI trả về services liên quan đến "tẩy trắng"

### Test 2: Hỏi về giá
```json
{
  "message": "Dịch vụ nào rẻ nhất?"
}
```
✅ Kỳ vọng: Services được sắp xếp theo giá tăng dần

### Test 3: Hỏi chung chung
```json
{
  "message": "Phòng khám có dịch vụ gì?"
}
```
✅ Kỳ vọng: Hiển thị top 3-5 services phổ biến

### Test 4: Hỏi bằng tiếng Anh
```json
{
  "message": "teeth whitening service"
}
```
✅ Kỳ vọng: AI vẫn nhận diện được (TF-IDF works với cả English)

## 🔧 Troubleshooting

### ❌ Lỗi: "Cannot connect to backend"
**Nguyên nhân**: Java backend chưa chạy

**Giải pháp**:
```cmd
mvn spring-boot:run
```
Đợi log: `Started DentalBackendApplication`

---

### ❌ Lỗi: "No services loaded" hoặc services_loaded = 0
**Nguyên nhân**: Database chưa có dữ liệu services

**Giải pháp**:
1. Kiểm tra database có records trong table `services`
2. Gọi API để xem: `GET http://localhost:8080/api/services`
3. Nếu cần, thêm dữ liệu mẫu qua Postman hoặc SQL

---

### ❌ Lỗi: "booking_failed: Invalid date format"
**Nguyên nhân**: Format ngày sai

**Giải pháp**: Dùng format `MM/dd/yyyy`
- ✅ `01/23/2026` (23 Jan 2026)
- ❌ `23/01/2026` (sai)

---

### ❌ Python không gọi được Java API
**Nguyên nhân**: Port hoặc URL sai

**Giải pháp**:
```cmd
set BACKEND_URL=http://localhost:8080
.venv\Scripts\python.exe ml_service\app.py
```

## 📊 Giám sát hệ thống

### Check Java Backend
```http
GET http://localhost:8080/actuator/health
```

### Check Python ML Service
```http
GET http://localhost:5000/health
```

### Check AI có data chưa
```http
GET http://localhost:5000/analyze/services
```

## 🎓 Cách AI hoạt động

### 1. TF-IDF (Term Frequency - Inverse Document Frequency)
- Chuyển text thành số (vector)
- Từ quan trọng có trọng số cao
- Hỗ trợ n-grams: "tẩy trắng răng" = 1 phrase

### 2. Cosine Similarity
- So sánh độ tương đồng giữa câu hỏi và mô tả service
- Score từ 0.0 (không liên quan) đến 1.0 (rất liên quan)

### 3. Ranking
- Services được xếp hạng theo score
- Trả về top_k services phù hợp nhất

### Ví dụ:
```
Query: "tẩy trắng răng"

Service 1: "TAY TRANG - Tẩy trắng răng chuyên sâu"
→ Score: 0.856 (rất liên quan)

Service 2: "KHAM RANG - Khám tổng quát"
→ Score: 0.123 (ít liên quan)

Service 3: "CAO CAO RANG"
→ Score: 0.089 (không liên quan)
```

## 📝 Note quan trọng

1. **Data là LIVE từ database**: Mỗi lần gọi API, Python sẽ lấy dữ liệu mới nhất
2. **Không cần restart Python** khi thêm services vào database
3. Có thể gọi `/refresh` để force update ngay lập tức
4. AI model sẽ tự động rebuild vectors khi data thay đổi

## 🚀 Deploy Production

### Docker Compose (recommend)
```yaml
services:
  backend:
    image: dental-backend:latest
    ports: ["8080:8080"]
  
  ml-service:
    image: dental-ml:latest
    ports: ["5000:5000"]
    environment:
      BACKEND_URL: http://backend:8080
```

---

**🎉 Hoàn thành! Hệ thống đã sẵn sàng sử dụng với AI phân tích dữ liệu realtime từ database.**
