README - Dental Backend (tích hợp ML service)

Mục đích cập nhật
- Thêm một Python-based ML microservice (ml_service) dùng NumPy, Pandas, scikit-learn để hỗ trợ gợi ý dịch vụ và phân tích dữ liệu.

Vị trí
- ml_service/ (cùng cấp với mã Java)

Các bước nhanh để chạy toàn bộ hệ thống (dev)
1) Chạy backend Java Spring Boot (mặc định port 8080)
   - mvn spring-boot:run
2) Chạy Python ML microservice
   - python -m venv .venv
   - on Windows (cmd.exe): .\.venv\Scripts\activate
   - pip install -r ml_service\requirements.txt
   - python ml_service\app.py

Tích hợp vào UI / Chatbot
- Khi người dùng nhập truy vấn trên UI, luồng gợi ý:
  1. UI gửi message đến endpoint chat assist: POST /api/chat/assist (Java app)
  2. Backend Java có thể gọi ML service để tăng cường đề xuất: POST http://localhost:5000/recommend với body {"query": "<user message>", "top_k": 3}
  3. Kết quả trả về sẽ được hiển thị trong UI (gợi ý dịch vụ, thời gian, nha sĩ)

Ví dụ RestTemplate (Java) - mẫu
```
String url = "http://localhost:5000/recommend";
Map<String,Object> body = Map.of("query", userMessage, "top_k", 3);
ResponseEntity<String> resp = restTemplate.postForEntity(url, body, String.class);
```

Postman - mẫu raw JSON cho /api/chat/book (từ UI -> Java -> public booking)
```
POST http://localhost:8080/api/chat/book
Content-Type: application/json

{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0900123456",
  "serviceId": 1,
  "date": "10/21/2025",
  "time": "09:00",
  "dentistId": 6,
  "notes": "Đặt qua chatbot"
}
```

Ghi chú về định dạng ngày
- Hiện backend mong đợi định dạng `MM/dd/yyyy` (ví dụ: 10/21/2025). Lỗi sẽ xảy ra nếu chuỗi ngày không khớp (ví dụ 10/20/2025 with invalid month 20)

Tiếp theo (tự động hoá)
- Tạo Dockerfile cho ml_service
- Tạo endpoint /train để huấn luyện từ dữ liệu lịch sử (appointments)
- Thêm unit tests cho pipeline ML

Nếu bạn muốn, tôi có thể:
- Thêm Dockerfile + docker-compose để chạy Java + Python cùng nhau
- Viết ví dụ RestTemplate/WebClient cụ thể và sửa `ChatController` để tự gọi ml_service và trả kết quả enrich cho UI
flask==2.3.2
numpy==1.26.2
pandas==2.2.2
scikit-learn==1.3.2
joblib==1.3.2
gunicorn==20.1.0

