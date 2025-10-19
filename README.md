# Dental Backend — Chat AI & Booking API

README này mô tả cách chạy và tích hợp tính năng Chat AI (tư vấn + gợi ý đặt lịch) có trong project, cùng ví dụ Postman và các lưu ý quan trọng để frontend dùng an toàn.

## Mục tiêu
- Cung cấp endpoint để:
  - Tư vấn hội thoại (`/api/chat/generate`) — gọi LLM hoặc trả simulated reply khi không có API key
  - Gợi ý đặt lịch (suggest-only) (`/api/chat/assist`) — trả danh sách dịch vụ, nha sĩ, ngày/giờ và mẫu đặt nhanh
  - Thực hiện đặt lịch khi người dùng xác nhận (`/api/chat/book`) — chỉ tạo appointment khi frontend explicit gửi yêu cầu
  - Xem preview system prompt (`/api/chat/debug`)

> QUAN TRỌNG: theo thiết kế, chatbot chỉ gợi ý. Việc tạo lịch chỉ xảy ra khi frontend gọi `/api/chat/book` (tức người dùng đã xác nhận).

## Yêu cầu & cài đặt
- Java 17
- Maven
- (Tùy) OpenAI API key nếu muốn gọi LLM thật

Chạy server local:

```bat
cd /d D:\Project\dental-backend\dental-backend
mvnw.cmd spring-boot:run
```

Hoặc chạy jar sau khi `mvn package`:

```bat
cd /d D:\Project\dental-backend\dental-backend\target
java -jar Ai_search-0.0.1-SNAPSHOT.jar
```

Server mặc định lắng nghe: `http://localhost:8080`.

## Cấu hình chính
Các cấu hình quan trọng có thể đặt trong `application.properties` hoặc các biến môi trường:
- `OPENAI_API_KEY` hoặc `spring.ai.openai.api-key` — nếu có, `/api/chat/generate` sẽ gọi OpenAI
- `OPENAI_BASE_URL` hoặc `spring.ai.openai.base-url` — base URL cho provider
- `app.chat.default-system` — default system prompt cho LLM
- `app.chat.include-services` (true/false) — nếu true backend thêm tóm tắt services vào system prompt
- `app.chat.include-services-full` (true/false) — nếu true backend thêm JSON đầy đủ của services

## Endpoint chính (tóm tắt)
1. POST /api/chat/assist
   - Mục đích: Phân tích user message -> trả gợi ý đặt lịch (không tạo appointment)
   - Request body: `{"message":"...","system":""}`
   - Response data: `suggestedServices`, `suggestedDentists`, `suggestedDates`, `suggestedTimes`, `quickBookingTemplates`

2. POST /api/chat/book
   - Mục đích: Tạo appointment (chỉ khi user xác nhận)
   - Request body (QuickBookingRequest):
     ```json
     {
       "fullName": "Nguyen Van A",
       "email": "a@example.com",
       "phone": "0900123456",
       "serviceId": 2,
       "date": "10/20/2025",
       "time": "09:00",
       "dentistId": 6,      // optional
       "notes": "..."
     }
     ```
   - Chú ý: server chấp nhận `date` ở các format `MM/dd/yyyy`, `dd/MM/yyyy`, hoặc `yyyy-MM-dd`. `time` cần `HH:mm` (24h).

3. POST /api/chat/generate
   - Mục đích: Gửi message tới LLM (OpenAI) hoặc nhận simulated reply.
   - Request body: `{"message":"...","system":""}`
   - Nếu không có API key, reply là simulated (cho dev test).

4. POST /api/chat/debug
   - Trả preview `systemToSend` (system prompt + services summary/json) để debug.

## Ví dụ raw JSON dùng trong Postman
- /api/chat/assist
```json
{
  "message": "Tôi muốn đặt lịch cạo vôi, cho tôi biết giá và thời gian",
  "system": ""
}
```

- /api/chat/book (có dentist)
```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0900123456",
  "serviceId": 2,
  "date": "10/20/2025",
  "time": "09:00",
  "dentistId": 6,
  "notes": "Đặt qua chatbot - user confirmed"
}
```

- /api/chat/book (không dentist)
```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "0900123456",
  "serviceId": 1,
  "date": "20/10/2025",
  "time": "09:00",
  "notes": "Đặt qua chatbot - user confirmed"
}
```

- /api/chat/generate
```json
{
  "message": "Cho tôi biết cạo vôi gồm những bước nào?",
  "system": ""
}
```

- /api/chat/debug
```json
{
  "message": "preview",
  "system": ""
}
```

## Flow UI gợi ý (recommendation — suggest-only)
1. User nhập câu hỏi trong chat UI.
2. Frontend gọi `POST /api/chat/assist` để lấy `quickBookingTemplates` và gợi ý.
3. Hiển thị các gợi ý (dịch vụ, nha sĩ, ngày giờ). Người dùng chọn 1 mẫu và chỉnh thông tin (fullName, email, phone).
4. Hiển thị màn xác nhận cuối cùng (summary). Khi người dùng nhấn "Xác nhận", frontend gọi `POST /api/chat/book`.
5. Hiển thị kết quả thành công/không thành công.

> Bảo đảm an toàn: KHÔNG gọi `/api/chat/book` tự động khi nhận `assist` trả về; bắt buộc người dùng phải xác nhận.

## Gợi ý tích hợp frontend (fetch / axios)
- Assist (fetch):
```js
await fetch("http://localhost:8080/api/chat/assist", {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message: 'Tôi muốn đặt cạo vôi', system: '' })
});
```
- Book (axios):
```js
await axios.post('http://localhost:8080/api/chat/book', payload, { headers: { 'X-CHAT-CONFIRMED':'true' } });
```

## Khuyến nghị bảo mật & audit
- Luôn yêu cầu người dùng xác nhận trước khi tạo booking. Backend có thể kiểm tra header `X-CHAT-CONFIRMED: true` hoặc field `confirmedByUser:true` để chặt chẽ hơn.
- Nếu bắt buộc, yêu cầu người dùng đăng nhập để tạo booking (liên kết booking với user id).
- Ghi log/audit: lưu nguồn gợi ý (chat message/template id), user/email, IP, user-agent và timestamp khi tạo booking.

## Xử lý lỗi thường gặp
- `Unable to parse date`: kiểm tra format `date` (dùng MM/dd/yyyy hoặc dd/MM/yyyy hoặc yyyy-MM-dd)
- Validation 400: kiểm tra các trường required (fullName,email,phone,serviceId,date,time)
- Nếu `/api/chat/generate` trả simulated reply: kiểm tra biến môi trường `OPENAI_API_KEY` và restart server

## Thêm tài liệu / Postman
- Bạn có thể tạo Postman Collection gồm 4 request: `/api/chat/assist`, `/api/chat/book`, `/api/chat/generate`, `/api/chat/debug`.
- Sử dụng Test/Pre-request scripts để map `quickBookingTemplates[0]` -> biến environment và build payload `/book` an toàn.

## Muốn mở rộng/tao tự động (lưu ý)
- Nếu muốn cho bot hỏi từng field (slot-filling) và tự động gửi `/api/chat/book` khi user trả lời "Có" trong chat, cần thêm stateful session management và guard xác nhận (khuyến nghị: luôn show confirm button trên UI thay vì let bot auto-book).

---

Nếu bạn muốn, tôi có thể tiếp tục và:
- Tạo file Postman Collection (.json) trong repo để import nhanh
- Thêm guard server-side (yêu cầu header `X-CHAT-CONFIRMED`) trong `ChatController.book()`
- Viết React demo component (chat -> assist -> confirm -> book)

Chọn 1 trong các tuỳ chọn trên hoặc yêu cầu sửa nội dung README này thêm chi tiết/tiếng Anh — tôi sẽ cập nhật ngay.
