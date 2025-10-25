# 🧠 GIẢI THÍCH VAI TRÒ CỦA TỪNG COMPONENT AI

## 📋 Tóm tắt nhanh

| Component | Vai trò | Khi nào dùng | Ví dụ |
|-----------|---------|--------------|-------|
| **OpenAI GPT** | Hiểu ngôn ngữ tự nhiên, phân tích ý định, tạo câu trả lời | Mọi request `/assist` | "chào" → phân tích là chitchat |
| **Python ML Service** | Tìm services khớp từ database (TF-IDF scoring) | Chỉ khi user hỏi về booking | "khám răng" → [KHAM RANG: 0.95] |
| **Java Logic** | Điều phối luồng, tạo booking template, quản lý DB | Luôn luôn | Merge kết quả AI + ML |

---

## 🔄 Luồng hoạt động CHI TIẾT (sau khi sửa)

### Ví dụ 1: User gửi "chào"

```
1️⃣ POST /api/assist {"message": "chào"}
   ↓
2️⃣ Java gọi OpenAI để phân tích ý định:
   Prompt: "Analyze this message: 'chào'. Intent?"
   OpenAI trả về: "CHITCHAT"
   ↓
3️⃣ Java thấy intent = CHITCHAT → gọi OpenAI tạo câu trả lời:
   Prompt: "You are a dental receptionist. Respond to: 'chào'"
   OpenAI trả về: "Xin chào! Tôi có thể giúp gì cho bạn hôm nay?"
   ↓
4️⃣ Response về UI:
   {
     "success": true,
     "data": {
       "type": "chitchat",
       "intent": "CHITCHAT",
       "reply": "Xin chào! Tôi có thể giúp gì cho bạn hôm nay?",
       "messageSummary": "Xin chào! Tôi có thể giúp gì cho bạn hôm nay?"
       // KHÔNG có suggestedServices, dentists, dates
     }
   }
```

✅ **Kết quả**: User nhận được câu trả lời tự nhiên, KHÔNG thấy danh sách services/dentists

---

### Ví dụ 2: User gửi "tôi muốn tẩy trắng răng"

```
1️⃣ POST /api/assist {"message": "tôi muốn tẩy trắng răng"}
   ↓
2️⃣ Java gọi OpenAI phân tích ý định:
   OpenAI trả về: "BOOKING"
   ↓
3️⃣ Java thấy intent = BOOKING → gọi OpenAI trích xuất thông tin:
   Prompt: "Extract booking info from: 'tôi muốn tẩy trắng răng'"
   OpenAI trả về: {"service_keywords": ["tẩy trắng", "răng"], "summary": "..."}
   ↓
4️⃣ Java gọi Python ML Service:
   POST http://localhost:5000/recommend
   {
     "query": "tôi muốn tẩy trắng răng",
     "top_k": 5
   }
   ↓
5️⃣ Python ML Service:
   - Gọi GET /api/services (lấy data từ DB)
   - AI phân tích: TF-IDF vector của "tẩy trắng răng"
   - So sánh với mô tả từng service trong DB
   - Tính score (cosine similarity)
   
   Python trả về:
   {
     "results": [
       {"id": 3, "name": "TAY TRANG", "score": 0.856, ...},
       {"id": 1, "name": "KHAM RANG", "score": 0.123, ...}
     ]
   }
   ↓
6️⃣ Java nhận kết quả ML, lấy services từ DB theo ID
   ↓
7️⃣ Java lấy dentists từ DB
   ↓
8️⃣ Java tạo suggested dates/times
   ↓
9️⃣ Java gọi OpenAI tạo messageSummary:
   Prompt: "User asked about tẩy trắng răng. We found 2 services. Write friendly message."
   OpenAI: "Chúng tôi tìm thấy 2 dịch vụ phù hợp với bạn. Vui lòng chọn dịch vụ để đặt lịch."
   ↓
🔟 Response về UI:
   {
     "success": true,
     "data": {
       "type": "booking",
       "intent": "BOOKING",
       "messageSummary": "Chúng tôi tìm thấy 2 dịch vụ...",
       "suggestedServices": [
         {"id": 3, "name": "TAY TRANG", "price": 1500000, ...}
       ],
       "mlRecommendations": [
         {"id": 3, "score": 0.856, ...}
       ],
       "suggestedDentists": [...],
       "suggestedDates": ["10/23/2025", ...],
       "suggestedTimes": ["09:00", ...],
       "quickBookingTemplates": [...]
     }
   }
```

✅ **Kết quả**: User nhận được danh sách services được AI sắp xếp theo độ phù hợp, kèm dentists/dates/times

---

## 🎯 VAI TRÒ CỤ THỂ TỪNG COMPONENT

### 1. OpenAI GPT (ChatUseCase.generate)

**Công dụng:**
- 🧠 **Hiểu ngôn ngữ tự nhiên** (NLU - Natural Language Understanding)
- 🎯 **Phân tích ý định** (Intent Classification)
- 📝 **Trích xuất thông tin** (Entity Extraction)
- 💬 **Tạo câu trả lời** (Natural Language Generation)

**Khi nào dùng:**
- ✅ Phân biệt "chào" vs "tôi muốn khám răng"
- ✅ Trích xuất từ khóa: "tẩy trắng răng" → ["tẩy trắng", "răng"]
- ✅ Tạo câu trả lời thân thiện cho user
- ✅ Hiểu câu hỏi phức tạp: "con tôi bị sâu răng, bao nhiêu tiền?"

**Ví dụ gọi:**
```java
String reply = chatService.generate(
    "Analyze intent: 'chào'", 
    null
);
// Output: "CHITCHAT"
```

**Cấu hình:** `application.yml`
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-3.5-turbo
```

---

### 2. Python ML Service (TF-IDF + Cosine Similarity)

**Công dụng:**
- 🔍 **Tìm kiếm thông minh** (Semantic Search)
- 📊 **Xếp hạng độ liên quan** (Relevance Scoring)
- 🎯 **Match query với database** (Query-Document Matching)

**Khi nào dùng:**
- ✅ Khi đã biết user muốn booking (intent = BOOKING)
- ✅ Tìm services khớp với câu hỏi trong database
- ✅ Xếp hạng services theo độ liên quan (score 0.0-1.0)

**KHÔNG dùng khi:**
- ❌ User chỉ chào hỏi
- ❌ User hỏi giờ mở cửa, địa chỉ (không liên quan services)

**Ví dụ gọi:**
```http
POST http://localhost:5000/recommend
{
  "query": "tẩy trắng răng",
  "top_k": 5
}
```

**Response:**
```json
{
  "results": [
    {"id": 3, "name": "TAY TRANG", "score": 0.856},
    {"id": 4, "name": "NHAN RANG", "score": 0.234}
  ]
}
```

**Cách hoạt động:**
```
1. Lấy tất cả services từ DB qua API
2. Chuyển query + descriptions thành vectors (TF-IDF)
3. Tính cosine similarity:
   
   Query: "tẩy trắng răng"
   Service 1: "TAY TRANG - Tẩy trắng răng chuyên sâu"
   → Similarity = 0.856 (rất cao)
   
   Service 2: "KHAM RANG - Khám tổng quát"
   → Similarity = 0.123 (thấp)
   
4. Sắp xếp theo score, trả về top_k
```

---

### 3. Java ChatController (Orchestrator)

**Vai trò:**
- 🎭 **Điều phối luồng** (Orchestration)
- 🔀 **Quyết định logic** (Decision Making)
- 🗃️ **Quản lý database** (Data Management)
- 🔧 **Kết hợp kết quả** (Result Aggregation)

**Nhiệm vụ:**
1. Nhận request từ UI
2. Gọi OpenAI phân tích intent
3. **Nếu CHITCHAT**: Gọi OpenAI trả lời → end
4. **Nếu BOOKING**: 
   - Gọi ML Service tìm services
   - Lấy dentists từ DB
   - Tạo suggested dates/times
   - Gọi OpenAI tạo messageSummary
   - Merge tất cả → response

---

## 🆚 SO SÁNH OpenAI vs Python ML Service

| Tiêu chí | OpenAI GPT | Python ML Service |
|----------|------------|-------------------|
| **Nhiệm vụ** | Hiểu ngôn ngữ, tạo text | Tìm kiếm database |
| **Input** | Text tự do | Query + database |
| **Output** | Text tự nhiên | Danh sách scored items |
| **Chi phí** | $$ (API call) | Free (tự host) |
| **Tốc độ** | ~1-2s | ~100ms |
| **Độ chính xác** | Rất cao (GPT-3.5/4) | Trung bình (TF-IDF) |
| **Use case** | Chitchat, NLU, NLG | Search, ranking |

**Kết hợp cả hai = Chatbot mạnh nhất!**

---

## 🧪 TEST SAU KHI SỬA

### Test 1: Chitchat (không trả services)
```http
POST http://localhost:8080/api/assist
{
  "message": "chào"
}
```

**Kỳ vọng:**
```json
{
  "success": true,
  "data": {
    "type": "chitchat",
    "intent": "CHITCHAT",
    "reply": "Xin chào! Tôi có thể giúp gì cho bạn?",
    "messageSummary": "Xin chào! Tôi có thể giúp gì cho bạn?"
  }
}
```
✅ **KHÔNG có** `suggestedServices`, `dentists`, `dates`

---

### Test 2: Hỏi về dịch vụ
```http
POST http://localhost:8080/api/assist
{
  "message": "tôi muốn khám răng"
}
```

**Kỳ vọng:**
```json
{
  "success": true,
  "data": {
    "type": "booking",
    "intent": "BOOKING",
    "messageSummary": "Chúng tôi tìm thấy các dịch vụ khám răng phù hợp...",
    "suggestedServices": [
      {"id": 1, "name": "KHAM RANG", ...}
    ],
    "mlRecommendations": [
      {"id": 1, "score": 0.95}
    ],
    "suggestedDentists": [...],
    "suggestedDates": [...],
    "quickBookingTemplates": [...]
  }
}
```
✅ **CÓ** đầy đủ thông tin booking

---

### Test 3: Câu hỏi phức tạp
```http
POST http://localhost:8080/api/assist
{
  "message": "con tôi 5 tuổi bị sâu răng, chi phí bao nhiêu?"
}
```

**OpenAI sẽ:**
1. Phân tích: intent = BOOKING
2. Trích xuất: service_keywords = ["sâu răng", "trẻ em"]

**ML Service sẽ:**
- Tìm services liên quan "sâu răng" trong DB
- Score cao nếu description có "trị sâu", "hàn răng"

**Response:**
- Services phù hợp + giá
- Dentists chuyên nha khoa trẻ em (nếu có)
- Suggested dates/times

---

## 🔧 Troubleshooting

### ❌ Lỗi: Vẫn trả services khi hỏi "chào"
**Nguyên nhân:** OpenAI API key chưa config hoặc sai

**Giải pháp:**
1. Kiểm tra `application.yml`:
```yaml
spring:
  ai:
    openai:
      api-key: sk-proj-xxxxx  # Phải có key hợp lệ
```

2. Hoặc set env variable:
```cmd
set OPENAI_API_KEY=sk-proj-xxxxx
```

3. Test OpenAI hoạt động:
```http
POST http://localhost:8080/api/generate
{
  "message": "hello"
}
```

---

### ❌ Lỗi: mlRecommendations trống
**Nguyên nhân:** Python ML service chưa chạy hoặc `ml.service.url` chưa config

**Giải pháp:**
1. Chạy Python service:
```cmd
.venv\Scripts\python.exe ml_service\app.py
```

2. Config `application.yml`:
```yaml
ml:
  service:
    url: http://localhost:5000
```

3. Test ML service:
```http
GET http://localhost:5000/health
```

---

### ❌ Lỗi: Intent luôn là BOOKING
**Nguyên nhân:** OpenAI model không hiểu prompt hoặc temperature quá cao

**Giải pháp:** Giảm temperature trong config:
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          temperature: 0.3  # Thấp = ổn định hơn
```

---

## 📊 Flow Chart đầy đủ

```
┌─────────────┐
│ User Input  │
└──────┬──────┘
       │
       ↓
┌──────────────────────────────┐
│ OpenAI: Analyze Intent       │
│ Prompt: "Is this BOOKING or  │
│         CHITCHAT?"           │
└──────┬────────────┬──────────┘
       │            │
   CHITCHAT      BOOKING
       │            │
       ↓            ↓
┌──────────────┐  ┌─────────────────────┐
│ OpenAI:      │  │ OpenAI: Extract     │
│ Generate     │  │ Keywords            │
│ Friendly     │  └──────┬──────────────┘
│ Response     │         │
└──────┬───────┘         ↓
       │          ┌──────────────────────┐
       │          │ Python ML Service:   │
       │          │ - Fetch services     │
       │          │ - TF-IDF matching    │
       │          │ - Score & rank       │
       │          └──────┬───────────────┘
       │                 │
       │                 ↓
       │          ┌──────────────────────┐
       │          │ Java: Fetch DB       │
       │          │ - Dentists           │
       │          │ - Generate dates     │
       │          │ - Create templates   │
       │          └──────┬───────────────┘
       │                 │
       │                 ↓
       │          ┌──────────────────────┐
       │          │ OpenAI: Generate     │
       │          │ Summary Message      │
       │          └──────┬───────────────┘
       │                 │
       └────────┬────────┘
                ↓
        ┌───────────────┐
        │ Final Response│
        │ to UI         │
        └───────────────┘
```

---

## 🎓 Kết luận

**3 components hoạt động như 1 team:**

1. **OpenAI (Brain)** - Não bộ: Hiểu user, phân tích, tạo câu trả lời
2. **Python ML (Librarian)** - Thủ thư: Tìm tài liệu (services) phù hợp trong DB
3. **Java (Manager)** - Quản lý: Điều phối, quyết định, kết hợp kết quả

→ **Kết quả:** Chatbot thông minh, trả lời đúng ngữ cảnh, gợi ý chính xác!
