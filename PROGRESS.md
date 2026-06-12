# lab-contact-service — Progress

ส่วนหนึ่งของ **Feature Lab** — contact form: ใครก็ส่งข้อความได้ (ไม่ต้อง login),
เก็บลง DB ของตัวเอง (contactdb), แอดมินเปิดอ่านในเว็บ + มีอีเมลแจ้งเตือนเด้งหาเจ้าของ
หลักออกแบบ: ส่ง = public / อ่าน-จัดการ = ADMIN เท่านั้น · อีเมลผ่าน SMTP env ล้วน (เจ้าไหนก็ได้)
· ส่งเมลแบบ async — เมลล่มต้องไม่ทำให้ฟอร์มพัง

สถานะ: ⬜ ยังไม่เริ่ม · 🔨 กำลังทำ · ✅ เสร็จ

## บันได 5 ขั้น

- [x] 1. โครง service + schema (messages) + security (POST public / อ่าน ADMIN) + CI — 2026-06-12
- [x] 2. Contact API: POST validate + แอดมิน list (แบ่งหน้า + นับยังไม่อ่าน)/mark-read/ลบ — 2026-06-12
- [x] 3. อีเมลแจ้งเตือน: Spring Mail + @Async — SMTP config จาก env, เมลพังฟอร์มไม่พัง — 2026-06-12
- [x] 4. Integration tests (Testcontainers + GreenMail จับอีเมลจริง) + CI เขียว — 2026-06-12
- [ ] 5. lab-web: หน้า Contact + กล่องขาเข้าใน Admin + ติดป้าย live + redeploy (เกณฑ์เฟส)

## Log การทำงาน

- 2026-06-12 — ขั้น 4 เสร็จ: ContactFlowIntegrationTest — Testcontainers Postgres + GreenMail SMTP
  ในเทสต์เดียว: ส่งฟอร์มแล้ว "แกะเมลจริง" ดู to/subject/เนื้อ (waitForIncomingEmail รอ async),
  validate 3 field, สิทธิ์ 401/403/200, mark-read idempotent + unread นับถูก, ลบ/404;
  ติดกับดัก 3 จุด: TestRestTemplate ค่า default ยิง PATCH ไม่ได้ (เสียบ httpclient5) /
  @DynamicPropertySource ถาม port จาก GreenMail ก่อน start → NPE (ใช้ ServerSetupTest.SMTP::getPort) /
  ผ่านบนเครื่องแต่พังบน CI: readAt นาโนวิจากหน่วยความจำ vs ไมโครวิจาก Postgres — truncate ตอนเขียน

- 2026-06-12 — ขั้น 3 เสร็จ: MailNotifier (@Async + @EnableAsync) — submit ตอบ 201 ใน 45ms ไม่รอ SMTP;
  ทดสอบกับ MailHog: เมลเด้งจริง (to/subject/เนื้อครบ); เคส SMTP ล่ม: ฟอร์มยัง 201 + log error +
  ข้อความอยู่ใน DB ครบ; บั๊กที่เจอ: append บล็อก mail ลง yml แล้วไปซ้อนใต้ jwt: แทน spring: →
  spring.mail.host ไม่ถูกตั้ง → ไม่มี bean JavaMailSender → boot พัง — จัด indent ใหม่ให้อยู่ใต้ spring:

- 2026-06-12 — ขั้น 2 เสร็จ: POST /api/contact (นิรนาม, validate name/email/message ราย field,
  ผู้ส่งได้แค่ 201 — เนื้อกล่องเป็นของแอดมิน); GET กล่องขาเข้า ADMIN (แบ่งหน้า + total + unread),
  PATCH /{id}/read (ยิงซ้ำ readAt ไม่ขยับ), DELETE; เทสต์ 11 เคสผ่าน: 401/403/200 ตามสิทธิ์ครบ

- 2026-06-12 — ขั้น 1 เสร็จ: โครงสูตรมาตรฐาน (env/health/request-id/graceful/CI) + Postgres ที่ :5435;
  V1: messages (name/email/message/read_at, index created_at DESC); security: POST /api/contact
  เปิดสาธารณะ ที่เหลือต้อง token; เจอ+แก้บั๊กลอกโครง: /health ฝังชื่อ "feed-service" → อ่านจาก
  spring.application.name แทน (ฝังชื่อ = ผิดหลัก no-hardcode ของระบบอยู่แล้ว)
