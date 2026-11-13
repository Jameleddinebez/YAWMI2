# قاعدة البيانات (Room، الإصدار 2)

## قواعد عامة
- المعرّفات `String` (UUID) تُولَّد على الجهاز → لا تصادم بين جهازين.
- `createdAt/updatedAt` (ms) و`deletedAt` (حذف ناعم) في كل جدول تقريباً، تمهيداً للمزامنة.
- التواريخ ISO نصاً، الأوقات دقائق (Int)، الـ enums نصاً. بلا محوّلات أنواع.
- المخططات تُصدَّر إلى `app/schemas/` عند البناء (للترحيلات). لا `fallbackToDestructiveMigration`.

## الجداول (20 = الـ19 المطلوبة + `duration_profiles`)
| الجدول | الغرض |
|---|---|
| `users`, `teacher_profiles` | المستخدم وملفه (المرحلة 2 تملأهما) |
| `institutions`, `class_groups` | المؤسسات والأقسام |
| `schedule_profiles` | جدول أسبوعي (متعدد عند تعدد المؤسسات) |
| `duration_profiles` | أنظمة مدة الحصة (عادي 60 / خاص 45 / مخصص) — غير مرتبطة بأي شهر |
| `schedule_items` | حصص/استراحات/اجتماعات متكررة أسبوعياً (`durationLocked` لحماية الثابت) |
| `schedule_overrides` | تعديل/إلغاء حصة واحدة في تاريخ معيّن دون المساس بالمتكرر |
| `lesson_types` | مكتبة أنواع الحصص (10 افتراضية، قابلة للتخصيص) |
| `sequence_templates` | قوالب التسلسل (3/4/5/مخصص) — المراحل JSON |
| `subject_sequences` | تسلسل مستقل لكل (قسم × مادة) + **نسخة Snapshot** من المراحل |
| `lesson_sessions` | الحصة **المنفَّذة** (منفصلة عن المجدولة): `PLANNED/HELD/NOT_HELD` |
| `lesson_actions` | **سجل العمليات** (للإلحاق فقط) — مصدر حقيقة التسلسل |
| `assessment_events` | الفروض/الاختبارات — منفصلة عن المراحل |
| `appointments`, `holidays`, `tasks`, `notes` | المواعيد، العطل (`isConfirmed`)، المهام، الملاحظات |
| `notifications` | تعريفات المنبهات (تنفيذها في المرحلة 5) |
| `backup_records` | سجل النسخ الاحتياطي |

## العلاقات الأساسية (مفاتيح أجنبية)
`teacher_profiles→users` (CASCADE) · `schedule_items→schedule_profiles` (CASCADE) · `schedule_overrides→schedule_items` (CASCADE) · `subject_sequences→class_groups` (**RESTRICT**) · `lesson_actions→subject_sequences` (**RESTRICT**).
RESTRICT عمداً: لا يُحذف قسم أو تسلسل له سجل إنجاز بالخطأ.

## ضمانات
- **منع تكرار المزامنة:** `lesson_actions.id` فريد + `OnConflictStrategy.IGNORE`.
- **فصل المخطط عن المنجَز:** `schedule_items` (الخطة) ≠ `lesson_sessions` (التنفيذ) ≠ `lesson_actions` (أثره على التسلسل).
- **تعديل الجدول لا يمس الإنجاز:** السجل لا يشير إلى `schedule_items` أصلاً؛ الربط اختياري عبر `lesson_sessions`.

## الإصدار 2 (إضافات فقط، `MIGRATION_1_2`)
| الجدول | الغرض |
|---|---|
| `work_profiles` | ملف عمل: طور + مؤسسة + مدة حصة/استراحة (يتعدد لكل أستاذ؛ `isArchived` بدل الحذف) |
| `subject_catalog` | مكتبة المواد المُصدَّرة بإصدار؛ `isVerified=false` = مسودة؛ `isCustom` = أضافها المستخدم |
| `teacher_subjects` | مواد الأستاذ داخل ملف عمل (`RESTRICT`: لا يُتلف حذفُ مادة السجلاتِ) |
| `work_day_configs` | إعداد كل يوم: راحة/صباحي/مسائي/الاثنان + البدايات وعدد الحصص |
| `schedule_profiles.workProfileId` | عمود جديد يربط الجدول بملف العمل (الصفوف القديمة `NULL`) |

قاعدة: كل DAO لجدول له أبناء `CASCADE/RESTRICT` يستخدم `@Upsert` لا `REPLACE`.
