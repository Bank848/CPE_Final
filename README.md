Raw Game for make another one.
https://github.com/ArmmyC/BookWorm/tree/main

# Bookworm Puzzle RPG

เกมแนว Puzzle RPG ที่ผสมผสานการสร้างคำจากตัวอักษรบนกระดาน (Bookworm) เข้ากับระบบต่อสู้แบบ RPG Turn base ทั้งในโหมด 1 Player และ 2 Players

---

## 📖 ภาพรวม (Overview)

- **ชื่อเกม**: Bookworm Puzzle RPG  
- **ภาษา**: Java + Swing (+ JavaFX สำหรับวีดีโอ)  
- **Grid**: 8×8 ตัวอักษร  
- **Objective**: สร้างคำจากตัวอักษรเพื่อทำดาเมจใส่ศัตรู เก็บ Gems นำไปซื้อไอเท็ม สกิล หรืออัปเกรดตัวละคร  
- **โหมดเกม**:  
  - Single-player (1 Player): ผ่านด่าน 1–20 ปะทะมอนสเตอร์และบอสทุก 5 ด่าน  
  - Two-player (2 Player): แข่งกันแพ้ชนะ 2 ใน 3 รอบ เลือกแชมเปี้ยนต่อสู้แบบ Turn-based  

---

## 🎮 โหมดเล่นคนเดียว (1 Player Mode)

### การเริ่มเกม
1. รันคลาส `BookwormUI`  
2. ระบบจะสุ่มสร้างกระดาน 8×8 และมอนสเตอร์เริ่มต้น (Lv.1)  
3. ทุกด่าน (Level) จะมี HP และดาเมจของมอนสเตอร์จะเพิ่มขึ้น  
4. ทุก 5 ด่านจะเป็นห้องบอสพิเศษ พร้อมสกิลและ HP สูงกว่าปกติ

### กลไกหลัก
- **สร้างคำ**: คลิกเลือกตัวอักษรติดกัน (แนวตั้ง/นอน/ทแยง) อย่างน้อย 3 ตัว  
- **คำนวณดาเมจ**:  
  - พลังโจมตีพื้นฐาน + คะแนนจาก Tile  
  - ถ้ามีไอคอน “*” (special) ดาเมจจะคูณ 2 และรีเซ็ตกระดานใหม่  
- **เก็บ Gems**: ใช้ซื้อไอเท็ม/สกิล ที่ร้านค้า (Shop)  
- **ร้านค้า**:  
  - ซื้อ Heal, Shield, Shuffle, Mana Potion ได้ตั้งแต่ Lv.1  
  - ด่าน 11+ จะมี Antidote (ล้างพิษ)  
  - ด่าน 16+ จะมี Bandage (หยุด Bleeding + ฟื้น HP)  
- **Loot Box**:  
  - หลังบอส มีโอกาสได้ Loot Box (ได้แบบสุ่ม)
- **Secret Merchant**:
  - พ่อค้าลับจะปรากฏที่ด่าน 5 โอกาสเกิด 50% และ 10, 15 เสมอ
  - ไอเท็มถาวรเช่น Legendary Book, Necklace, Potion พิเศษ ฯลฯ  

### ฟีเจอร์เสริม
- **ระบบ Ally “Kitsune”** (เฉพาะเหตุการณ์บางด่าน) ช่วยโจมตี หรือรับดีบัฟแทน  
- **วีดีโอฉากจบลับ** (JavaFX) เมื่อใช้ Legendary Book ได้ในด่านสุดท้าย  (คำใบ้:แม้ตัวเราจะอ่านLegendary Bookไม่ออกแต่ดูเหมือนจะมีคนอ่านมันได้นะ)
- **Progression**: รับ Skill Point 2 แต้มต่อเลเวลอัปสกิล Hero ได้  

---

## ⚔️ โหมดสองผู้เล่น (2 Player Mode)

### การเริ่มเกม
1. รันคลาส `Bookworm2PlayerWithMana`  
2. แต่ละผู้เล่นเลือก **Champion** (Warrior / Mage / Rogue)  
3. แข่งกัน 3 รอบ (Best-of-3)  

### Champion แต่ละฝ่าย
| แชมเปี้ยน | HP   | Mana  | Speed/Crit | สกิลพิเศษ (Skill)                      |
| ---------- | ---- | ----- | ---------- | -------------------------------------- |
| Warrior    | สูง  | ต่ำ   | ต่ำ        | Power Strike (โจมตีแรง), Fortify (ป้องกัน) |
| Mage       | ต่ำ  | สูง   | สูง        | Fireball (เวทแรง), Arcane Shield (ป้องกัน) |
| Rogue      | กลาง | กลาง | สูง        | Quick Slash (โจมตีเร็ว), Shadow Step (บัฟโจมตี) |

### กลไกหลัก
- **Turn-based**: สลับกันสร้างคำและกด Submit  
- **คำถูกต้อง**: ≥3 ตัวอักษร และมีใน Dictionary  
- **ดาเมจ & เก็บ Gems**: เหมือนโหมด 1 Player แต่มี Mana Cost สำหรับสกิล  
- **ใช้สกิล**:  
  - ต้องมี Mana เพียงพอ + ไม่อยู่ใน Cooldown  
  - แต่ละสกิลมีค่า Mana/Cooldown ต่างกัน  
- **Shop**:  
  - ใช้ Gems ซื้อ Heal, Shield, Buff (บูสต์พลังโจมตี), Shuffle, Mana Potion  
- **Win Condition**:  
  - ถ้า HP ฝ่ายใดหมด หากชนะรวด2ตาติดก็จะถือว่าPlayerคนนั้นเป็นผู้ชนะไปเลย หากคะแนนออกมาเสมอกัน1-1 จะให้สู้ต่อในตาสุดท้ายเพื่อหาผู้ชนะ

---

## 🚀 วิธีใช้งาน (Getting Started)

1. ติดตั้ง JDK 11+  
2. คอมไพล์โค้ดและรันเกม
# คอมไพล์
javac --module-path lib/javafx-sdk-21.0.7/lib --add-modules javafx.base,javafx.controls,javafx.graphics,javafx.swing,javafx.media -d out src/*.java

# รัน
java --module-path lib/javafx-sdk-21.0.7/lib --add-modules javafx.base,javafx.controls,javafx.graphics,javafx.swing,javafx.media -cp out BookwormUI
