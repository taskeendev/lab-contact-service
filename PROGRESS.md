# lab-contact-service — Progress

ส่วนหนึ่งของ **Feature Lab** — contact form: ใครก็ส่งข้อความได้ (ไม่ต้อง login),
เก็บลง DB ของตัวเอง (contactdb), แอดมินเปิดอ่านในเว็บ + มีอีเมลแจ้งเตือนเด้งหาเจ้าของ
หลักออกแบบ: ส่ง = public / อ่าน-จัดการ = ADMIN เท่านั้น · อีเมลผ่าน SMTP env ล้วน (เจ้าไหนก็ได้)
· ส่งเมลแบบ async — เมลล่มต้องไม่ทำให้ฟอร์มพัง

สถานะ: ⬜ ยังไม่เริ่ม · 🔨 กำลังทำ · ✅ เสร็จ

## บันได 5 ขั้น

- [x] 1. โครง service + schema (messages) + security (POST public / อ่าน ADMIN) + CI — 2026-06-12
- [ ] 2. Contact API: POST validate + แอดมิน list (แบ่งหน้า + นับยังไม่อ่าน)/mark-read/ลบ
- [ ] 3. อีเมลแจ้งเตือน: Spring Mail + @Async — SMTP config จาก env, เมลพังฟอร์มไม่พัง
- [ ] 4. Integration tests (Testcontainers + GreenMail จับอีเมลจริง) + CI เขียว
- [ ] 5. lab-web: หน้า Contact + กล่องขาเข้าใน Admin + ติดป้าย live + redeploy (เกณฑ์เฟส)

## Log การทำงาน

- 2026-06-12 — ขั้น 1 เสร็จ: โครงสูตรมาตรฐาน (env/health/request-id/graceful/CI) + Postgres ที่ :5435;
  V1: messages (name/email/message/read_at, index created_at DESC); security: POST /api/contact
  เปิดสาธารณะ ที่เหลือต้อง token; เจอ+แก้บั๊กลอกโครง: /health ฝังชื่อ "feed-service" → อ่านจาก
  spring.application.name แทน (ฝังชื่อ = ผิดหลัก no-hardcode ของระบบอยู่แล้ว)
