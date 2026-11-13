# YAWMI | يومي — المساعد اليومي للأستاذ

> **هذا التطبيق من أستاذ إلى أستاذ.**

تطبيق Android أصلي (Kotlin + Jetpack Compose) لتنظيم الحياة المهنية اليومية للأستاذ: الجدول، الحصص، المواعيد، العطل، المنبهات، والتسلسل البيداغوجي. واجهته عربية بالكامل (RTL).

## حالة المشروع: المرحلة 2 (الإصدار 0.2.0 قيد الاختبار)

| الجزء | الحالة |
|---|---|
| هيكل المشروع (Gradle، وحدات، CI) | ✅ مكتوب |
| ثيم الهوية البصرية + RTL + شريط التنقل (5 أقسام) | ✅ مكتوب |
| الصفحة الرئيسية (ساعة بالثواني، عدّ تنازلي، أجندة اليوم، مهام، لون حسب حالة اليوم) | ✅ مكتوبة |
| قاعدة بيانات Room (20 جدولاً) | ✅ مكتوبة |
| **محرك التسلسل البيداغوجي** (وحدة مستقلة) | ✅ مكتوب، **29 اختباراً ناجحاً** |
| منطق اليوم والأجندة والتواريخ (وحدة `domain`) | ✅ مكتوب، **17 اختباراً ناجحاً** |
| الإعداد الأولي + ملفات العمل + المواد (المرحلة 2) | ✅ مكتوبة — **بانتظار تأكيد البناء عبر CI** |
| بناء APK المرحلة 1 | ✅ نجح عبر GitHub Actions (تحقّقت من محتوى الـ APK) |
| الجدول، المواعيد، العطل، المنبهات، OCR، Firebase، PDF، الطقس | ⏳ مراحل لاحقة |

تفاصيل ما اختُبر فعلاً وما لم يُختبر: [`docs/PHASE1_REPORT.md`](docs/PHASE1_REPORT.md).

## البنية

```
YAWMI/
├── app/               تطبيق Android: واجهات Compose + Room + Repositories + DI يدوي
├── domain/            Kotlin خالص: الأجندة، حالة اليوم، التواريخ العربية، الوقت   (اختبارات JVM)
├── sequence-engine/   Kotlin خالص: محرك التسلسل البيداغوجي (Event-sourced)       (اختبارات JVM)
├── docs/              PRD، المعمارية، قاعدة البيانات، Firebase، تقرير المرحلة
└── .github/workflows/ CI: اختبارات + بناء APK
```

لماذا الوحدتان `domain` و`sequence-engine` بلا Android؟ كي تُختبر منطقيتهما على أي JVM بسرعة ودون محاكي، وكي لا تتأثر بتغيّر واجهات Android.

## فتح المشروع وتشغيله (Android Studio)

1. ثبّت **Android Studio** حديثاً (Ladybug أو أحدث) — يأتي مع JDK 17+ وAndroid SDK.
2. `File ▸ Open` واختر مجلد `YAWMI`.
3. انتظر مزامنة Gradle (أول مرة تنزّل التبعيات من الإنترنت).
4. شغّل على جهاز حقيقي أو محاكي: ▶ `app`.

من سطر الأوامر:

```bash
./gradlew :sequence-engine:test :domain:test      # اختبارات المحركات (سريعة)
./gradlew :app:assembleDebug                      # بناء APK للتجربة
# الناتج: app/build/outputs/apk/debug/app-debug.apk
```

> **تنبيه صادق:** كود تطبيق Android لم يُترجَم بعد في بيئة الإنشاء (لم يكن فيها Android SDK). إن ظهر خطأ ترجمة عند أول بناء فهو متوقع احتمالاً — أرسل لي نص الخطأ وأصلحه في الحال.

## رفع المشروع إلى GitHub

```bash
cd YAWMI
git init -b main
git add .
git commit -m "feat: phase 1 — project foundation, home dashboard, sequence engine"

# أنشئ مستودعاً Private فارغاً باسم YAWMI على github.com ثم:
git remote add origin https://github.com/<حسابك>/YAWMI.git
git push -u origin main
```

بعد الدفع، افتح تبويب **Actions**: سيعمل `CI` تلقائياً، يشغّل الاختبارات ويبني APK ويرفعه كـ Artifact باسم `yawmi-debug-apk`.

**لا ترفع أبداً:** `google-services.json`، ملفات التوقيع (`*.jks`)، أي مفاتيح. الملف `.gitignore` يحميك منها.

## الوثائق

- [`docs/PRD.md`](docs/PRD.md) — متطلبات المنتج ونطاق كل مرحلة
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — المعمارية وقراراتها
- [`docs/DATABASE.md`](docs/DATABASE.md) — الكيانات والعلاقات
- [`docs/SEQUENCE_ENGINE.md`](docs/SEQUENCE_ENGINE.md) — محرك التسلسل البيداغوجي
- [`docs/FIREBASE_SETUP.md`](docs/FIREBASE_SETUP.md) — ما تحتاج إعداده أنت لاحقاً
- [`docs/PHASE1_REPORT.md`](docs/PHASE1_REPORT.md) — تقرير المرحلة الأولى
- [`docs/PHASE2_REPORT.md`](docs/PHASE2_REPORT.md) — تقرير المرحلة الثانية وجدول حالة الميزات
- [`docs/OFL-NotoSansArabic.txt`](docs/OFL-NotoSansArabic.txt) — رخصة الخط المضمَّن

## الترخيص

انظر [`LICENSE`](LICENSE) — حقوق محفوظة افتراضياً إلى أن تقرر الترخيص المناسب.
