package com.yawmi.sequence

/** مرحلة واحدة ضمن قالب التسلسل (مثال: وضعية تعلمية). المعرّف ثابت ولا يتغير بتغيير الاسم. */
data class StageDef(val id: String, val name: String)

/** إعداد تسلسل واحد (لقسم + مادة). يُؤخذ كنسخة (Snapshot) من القالب فلا يتأثر التقدم بتعديل القالب لاحقاً. */
data class SequenceConfig(val sequenceId: String, val stages: List<StageDef>) {
    init {
        require(stages.isNotEmpty()) { "التسلسل يجب أن يحتوي مرحلة واحدة على الأقل" }
        require(stages.map { it.id }.toSet().size == stages.size) { "معرّفات المراحل يجب أن تكون فريدة" }
    }
}

enum class NotHeldReason { TEACHER_ABSENCE, CLASS_ABSENCE, CANCELLED, OTHER }

/** فرض / اختبار / غير ذلك. لا يستهلك مرحلة إلا إذا قرر الأستاذ ذلك صراحةً. */
enum class AssessmentKind { ASSIGNMENT, EXAM, OTHER }

/** كل ما يستطيع الأستاذ فعله. لا يوجد أي إجراء «تلقائي» مرتبط بالوقت أو بالجدول. */
sealed interface SequenceAction {
    /** إتمام المرحلة الحالية والانتقال للتالية. */
    data object CompleteCurrent : SequenceAction

    /** دمج [count] مراحل متتالية (≥ 2) في حصة واحدة. */
    data class CompleteMerged(val count: Int = 2) : SequenceAction

    /** تكرار المرحلة الحالية: تبقى هي الحالية. */
    data object RepeatCurrent : SequenceAction

    /** تمديد المرحلة الحالية إلى حصة إضافية: تبقى هي الحالية. */
    data object ExtendCurrent : SequenceAction

    /** تجاوز المرحلة الحالية (تُسجَّل «متجاوزة» لا «مكتملة»). */
    data object SkipCurrent : SequenceAction

    /** حصة لم تنجز (غياب/إلغاء): لا تغيّر المرحلة الحالية إطلاقاً، وتُسجَّل في السجل فقط. */
    data class NotHeld(val reason: NotHeldReason) : SequenceAction

    /** فرض أو اختبار. [consumesStage] = false افتراضياً: لا يمس التسلسل. */
    data class RecordAssessment(
        val kind: AssessmentKind,
        val title: String = "",
        val consumesStage: Boolean = false,
    ) : SequenceAction

    /** إعادة ترتيب المراحل المتبقية فقط (من المرحلة الحالية فصاعداً). المنجَز لا يُمس. */
    data class Reorder(val newRemainingOrder: List<String>) : SequenceAction

    /** بدء دورة جديدة بعد اكتمال الدورة الحالية. */
    data object StartNewCycle : SequenceAction

    /** تراجع عن إجراء سابق (يُسجَّل كحدث جديد، ولا يُحذف شيء من السجل). */
    data class Undo(val targetId: String) : SequenceAction
}

/**
 * إدخال في سجل العمليات. [id] فريد عالمياً (UUID) — وهو أساس منع التكرار عند المزامنة بين جهازين.
 */
data class LoggedAction(
    val id: String,
    val timestampMillis: Long,
    val action: SequenceAction,
    val deviceId: String = "",
)

enum class StageOutcome { COMPLETED, SKIPPED, COMPLETED_BY_ASSESSMENT }

data class StageRecord(
    val stageId: String,
    val outcome: StageOutcome,
    val actionId: String,
    /** عدد المراحل التي دُمجت معها في الحصة نفسها (1 = بدون دمج). */
    val mergedGroupSize: Int = 1,
)

data class AssessmentRecord(
    val actionId: String,
    val kind: AssessmentKind,
    val title: String,
    /** المرحلة التي كانت «التالية» وقت التسجيل (موضع الفرض بين المراحل)، أو null إن كانت الدورة مكتملة. */
    val beforeStageId: String?,
    val consumedStage: Boolean,
)

/** الحالة المشتقة من السجل — لا تُخزَّن كمصدر حقيقة، بل تُعاد حسابها من السجل. */
data class SequenceState(
    val config: SequenceConfig,
    val cycle: Int,
    val order: List<StageDef>,
    val cursor: Int,
    val records: List<StageRecord>,
    val assessments: List<AssessmentRecord>,
    val repeatCount: Int,
    val extensionCount: Int,
    val notHeldCount: Int,
    val completedCycles: Int,
) {
    val currentStage: StageDef? get() = order.getOrNull(cursor)
    val nextStage: StageDef? get() = order.getOrNull(cursor + 1)
    val isCycleComplete: Boolean get() = cursor >= order.size
    val completedStageCount: Int get() = records.count { it.outcome != StageOutcome.SKIPPED }
    val skippedStageCount: Int get() = records.count { it.outcome == StageOutcome.SKIPPED }
    val progressPercent: Int get() = if (order.isEmpty()) 0 else (cursor * 100) / order.size
}

enum class RejectReason {
    CYCLE_COMPLETE,
    CYCLE_NOT_COMPLETE,
    NOT_ENOUGH_STAGES,
    INVALID_ARGUMENT,
    INVALID_REORDER,
    INVALID_UNDO_TARGET,
    NOTHING_TO_UNDO,
}
