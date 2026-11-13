package com.yawmi.sequence

sealed interface ApplyResult {
    val timeline: SequenceTimeline

    data class Applied(override val timeline: SequenceTimeline) : ApplyResult
    data class Rejected(override val timeline: SequenceTimeline, val reason: RejectReason) : ApplyResult

    /** المعرّف موجود مسبقاً (مثلاً وصل من المزامنة مرتين): لا يتغير شيء. */
    data class Duplicate(override val timeline: SequenceTimeline) : ApplyResult
}

/**
 * سجل عمليات تسلسل واحد + الحالة المشتقة منه.
 *
 * - السجل هو مصدر الحقيقة؛ الحالة تُعاد حسابها بإعادة التشغيل (replay).
 * - الترتيب حتمي: (الوقت، ثم المعرّف) — فيعطي جهازان النتيجة نفسها من السجلات نفسها.
 * - التكرار بنفس المعرّف يُتجاهل (idempotent).
 * - التراجع حدث جديد يشير إلى هدفه؛ لا يُحذف شيء من السجل.
 * - إجراء صار غير صالح بسبب تعديلات متزامنة من جهاز آخر يُوسَم «متعارضاً» ويُتجاوز بدل أن يُفسد التسلسل.
 */
class SequenceTimeline private constructor(
    val config: SequenceConfig,
    /** السجل كاملاً بعد الترتيب وإزالة التكرار (يتضمن أحداث Undo). */
    val entries: List<LoggedAction>,
    val state: SequenceState,
    val undoneIds: Set<String>,
    val conflictedIds: Set<String>,
) {
    /** الإجراءات الفعّالة فقط (غير المتراجَع عنها وغير المتعارضة، وبدون أحداث Undo). */
    val effectiveEntries: List<LoggedAction>
        get() = entries.filter {
            it.action !is SequenceAction.Undo && it.id !in undoneIds && it.id !in conflictedIds
        }

    fun apply(entry: LoggedAction): ApplyResult {
        if (entries.any { it.id == entry.id }) return ApplyResult.Duplicate(this)

        val action = entry.action
        if (action is SequenceAction.Undo) {
            val target = entries.firstOrNull { it.id == action.targetId }
            if (target == null || target.action is SequenceAction.Undo || target.id in undoneIds) {
                return ApplyResult.Rejected(this, RejectReason.INVALID_UNDO_TARGET)
            }
        }

        val next = replay(config, entries + entry)
        return if (entry.id in next.conflictedIds) {
            val reason = (SequenceReducer.reduce(state, entry) as? Reduction.Rejected)?.reason
                ?: RejectReason.INVALID_ARGUMENT
            ApplyResult.Rejected(this, reason)
        } else {
            ApplyResult.Applied(next)
        }
    }

    /** يتراجع عن آخر إجراء فعّال. [undoId] معرّف حدث التراجع نفسه. */
    fun undoLast(undoId: String, timestampMillis: Long, deviceId: String = ""): ApplyResult {
        val last = effectiveEntries.lastOrNull()
            ?: return ApplyResult.Rejected(this, RejectReason.NOTHING_TO_UNDO)
        return apply(LoggedAction(undoId, timestampMillis, SequenceAction.Undo(last.id), deviceId))
    }

    /** دمج سجل قادم من جهاز آخر. المعرّفات المكررة تُهمل. */
    fun mergeWith(remote: List<LoggedAction>): SequenceTimeline = replay(config, entries + remote)

    companion object {
        fun create(config: SequenceConfig, entries: List<LoggedAction> = emptyList()): SequenceTimeline =
            replay(config, entries)

        private fun replay(config: SequenceConfig, raw: List<LoggedAction>): SequenceTimeline {
            val ordered = raw
                .sortedWith(compareBy<LoggedAction>({ it.timestampMillis }, { it.id }))
                .distinctBy { it.id }
            val byId = ordered.associateBy { it.id }

            val undone = ordered
                .mapNotNull { (it.action as? SequenceAction.Undo)?.targetId }
                .filter { target -> byId[target]?.action.let { it != null && it !is SequenceAction.Undo } }
                .toSet()

            var state = SequenceReducer.initial(config)
            val conflicted = mutableSetOf<String>()
            for (e in ordered) {
                if (e.action is SequenceAction.Undo || e.id in undone) continue
                when (val r = SequenceReducer.reduce(state, e)) {
                    is Reduction.Applied -> state = r.state
                    is Reduction.Rejected -> conflicted += e.id
                }
            }
            return SequenceTimeline(config, ordered, state, undone, conflicted)
        }
    }
}
