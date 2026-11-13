# إعداد Firebase (المرحلة 6 — لم يبدأ)

حالياً **لا توجد أي تبعية لـ Firebase ولا مفاتيح** في المشروع، عن قصد: إضافة إضافة `google-services` بلا ملف `google-services.json` تكسر البناء، ولا نضع مفاتيح وهمية. الواجهات الجاهزة: `AuthRepository`, `SyncRepository` (التنفيذ الحالي `NoOp*` يُرجع «غير مُعدّة»).

## ما تحتاج فعله أنت عندما نصل
1. إنشاء مشروع على console.firebase.google.com.
2. إضافة تطبيق Android بالحزمة `com.yawmi.app`، وتنزيل `google-services.json` ووضعه في `app/` (**لا يُرفع إلى GitHub**: محمي في `.gitignore`).
3. تفعيل Authentication (Email/Password)، وCloud Firestore، وStorage، وCloud Messaging.
4. إضافة قواعد Firestore: كل مستخدم يقرأ ويكتب `users/{uid}/**` فقط.
5. بصمة SHA-1 للتوقيع إن لزمت.

ما أضيفه أنا حينها: تبعيات Firebase، تنفيذ الواجهتين، قواعد الأمان، مزامنة سجل العمليات (Outbox) مع اختبارات.
