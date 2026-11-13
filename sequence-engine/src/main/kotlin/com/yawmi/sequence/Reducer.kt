package com.yawmi.sequence

internal sealed interface Reduction {
    data class Applied(val state: SequenceState) : Reduction
    data class Rejected(val reason: RejectReason) : Reduction
}

/** دالة نقية: (حالة، إجراء) → حالة جديدة أو رفض. لا تقرأ الساعة ولا الجدول ولا أي مصدر خارجي. */
internal object SequenceReducer {

    fun initial(config: SequenceConfig): SequenceState = SequenceState(
        config = config,
        cycle = 1,
        order = config.stages,
        cursor = 0,
        records = emptyList(),
        assessments = emptyList(),
        repeatCount = 0,
        extensionCount = 0,
        notHeldCount = 0,
        completedCycles = 0,
    )

    fun reduce(s: SequenceState, entry: LoggedAction): Reduction = when (val a = entry.action) {
        is SequenceAction.CompleteCurrent -> advance(s, entry, StageOutcome.COMPLETED, 1)
        is SequenceAction.CompleteMerged ->
            if (a.count < 2) Reduction.Rejected(RejectReason.INVALID_ARGUMENT)
            else advance(s, entry, StageOutcome.COMPLETED, a.count)
        is SequenceAction.SkipCurrent -> advance(s, entry, StageOutcome.SKIPPED, 1)
        is SequenceAction.RepeatCurrent ->
            if (s.isCycleComplete) Reduction.Rejected(RejectReason.CYCLE_COMPLETE)
            else Reduction.Applied(s.copy(repeatCount = s.repeatCount + 1))
        is SequenceAction.ExtendCurrent ->
            if (s.isCycleComplete) Reduction.Rejected(RejectReason.CYCLE_COMPLETE)
            else Reduction.Applied(s.copy(extensionCount = s.extensionCount + 1))
        is SequenceAction.NotHeld -> Reduction.Applied(s.copy(notHeldCount = s.notHeldCount + 1))
        is SequenceAction.RecordAssessment -> assessment(s, entry, a)
        is SequenceAction.Reorder -> reorder(s, a)
        is SequenceAction.StartNewCycle ->
            if (!s.isCycleComplete) Reduction.Rejected(RejectReason.CYCLE_NOT_COMPLETE)
            else Reduction.Applied(
                initial(s.config).copy(cycle = s.cycle + 1, completedCycles = s.completedCycles + 1),
            )
        // التراجع يُعالَج على مستوى السجل (SequenceTimeline) وليس هنا.
        is SequenceAction.Undo -> Reduction.Applied(s)
    }

    private fun advance(s: SequenceState, e: LoggedAction, outcome: StageOutcome, count: Int): Reduction {
        if (s.isCycleComplete) return Reduction.Rejected(RejectReason.CYCLE_COMPLETE)
        if (s.cursor + count > s.order.size) return Reduction.Rejected(RejectReason.NOT_ENOUGH_STAGES)
        val newRecords = s.order.subList(s.cursor, s.cursor + count)
            .map { StageRecord(it.id, outcome, e.id, count) }
        return Reduction.Applied(s.copy(cursor = s.cursor + count, records = s.records + newRecords))
    }

    private fun assessment(s: SequenceState, e: LoggedAction, a: SequenceAction.RecordAssessment): Reduction {
        val record = AssessmentRecord(e.id, a.kind, a.title, s.currentStage?.id, a.consumesStage)
        if (!a.consumesStage) {
            return Reduction.Applied(s.copy(assessments = s.assessments + record))
        }
        if (s.isCycleComplete) return Reduction.Rejected(RejectReason.CYCLE_COMPLETE)
        val consumed = StageRecord(s.order[s.cursor].id, StageOutcome.COMPLETED_BY_ASSESSMENT, e.id, 1)
        return Reduction.Applied(
            s.copy(
                cursor = s.cursor + 1,
                records = s.records + consumed,
                assessments = s.assessments + record,
            ),
        )
    }

    private fun reorder(s: SequenceState, a: SequenceAction.Reorder): Reduction {
        val remaining = s.order.drop(s.cursor)
        val byId = remaining.associateBy { it.id }
        val valid = a.newRemainingOrder.size == remaining.size &&
            a.newRemainingOrder.toSet() == byId.keys &&
            a.newRemainingOrder.toSet().size == a.newRemainingOrder.size
        if (!valid) return Reduction.Rejected(RejectReason.INVALID_REORDER)
        val newOrder = s.order.take(s.cursor) + a.newRemainingOrder.map { byId.getValue(it) }
        return Reduction.Applied(s.copy(order = newOrder))
    }
}
