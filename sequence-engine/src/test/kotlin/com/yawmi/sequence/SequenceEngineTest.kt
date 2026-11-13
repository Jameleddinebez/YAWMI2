package com.yawmi.sequence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SequenceEngineTest {

    private var t = 0L
    private var n = 0

    @Before
    fun reset() {
        t = 0L
        n = 0
    }

    private fun config(id: String = "seq-A", stages: List<StageDef> = SequenceTemplates.fourStages()) =
        SequenceConfig(id, stages)

    private fun entry(a: SequenceAction, device: String = "D1") =
        LoggedAction("act-${++n}", ++t, a, device)

    private fun SequenceTimeline.doIt(a: SequenceAction): SequenceTimeline {
        val r = apply(entry(a))
        assertTrue("الإجراء $a يجب أن يُقبل لكن النتيجة: $r", r is ApplyResult.Applied)
        return r.timeline
    }

    private fun SequenceTimeline.undo(): SequenceTimeline {
        val r = undoLast("undo-${++n}", ++t)
        assertTrue("التراجع يجب أن ينجح: $r", r is ApplyResult.Applied)
        return r.timeline
    }

    private fun stageId(tl: SequenceTimeline) = tl.state.currentStage?.id

    // ---------- الحالة الابتدائية ----------

    @Test
    fun initialStateStartsAtFirstStage() {
        val tl = SequenceTimeline.create(config())
        assertEquals("stage_learning", stageId(tl))
        assertEquals("stage_simple", tl.state.nextStage?.id)
        assertEquals(0, tl.state.progressPercent)
        assertEquals(1, tl.state.cycle)
        assertFalse(tl.state.isCycleComplete)
    }

    // ---------- إتمام عادي ----------

    @Test
    fun completeCurrentAdvancesOneStage() {
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        assertEquals("stage_simple", stageId(tl))
        assertEquals(1, tl.state.completedStageCount)
        assertEquals(25, tl.state.progressPercent)
    }

    // ---------- غياب / إلغاء ----------

    @Test
    fun notHeldSessionKeepsSameStage() {
        var tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val before = stageId(tl)
        tl = tl.doIt(SequenceAction.NotHeld(NotHeldReason.TEACHER_ABSENCE))
        tl = tl.doIt(SequenceAction.NotHeld(NotHeldReason.CLASS_ABSENCE))
        assertEquals(before, stageId(tl))
        assertEquals(2, tl.state.notHeldCount)
        assertEquals(1, tl.state.completedStageCount)
    }

    @Test
    fun emptyLogNeverAdvancesNoMatterHowMuchTimePasses() {
        // لا تقدم تلقائي: بلا إجراءات من الأستاذ، تبقى الحالة كما هي.
        val tl = SequenceTimeline.create(config(), emptyList())
        assertEquals(0, tl.state.cursor)
        assertEquals(0, tl.state.records.size)
    }

    // ---------- تكرار / تمديد ----------

    @Test
    fun repeatKeepsCurrentStageAndCounts() {
        val tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.RepeatCurrent)
            .doIt(SequenceAction.RepeatCurrent)
        assertEquals("stage_learning", stageId(tl))
        assertEquals(2, tl.state.repeatCount)
    }

    @Test
    fun extendTwoSessionsThenCompleteMovesOnce() {
        var tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.ExtendCurrent)
        assertEquals("stage_learning", stageId(tl))
        tl = tl.doIt(SequenceAction.CompleteCurrent)
        assertEquals("stage_simple", stageId(tl))
        assertEquals(1, tl.state.extensionCount)
        assertEquals(1, tl.state.completedStageCount)
    }

    // ---------- دمج ----------

    @Test
    fun mergeTwoStagesCompletesBothInOneAction() {
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteMerged(2))
        assertEquals("stage_integration", stageId(tl))
        assertEquals(2, tl.state.completedStageCount)
        assertTrue(tl.state.records.all { it.mergedGroupSize == 2 })
        assertEquals(1, tl.state.records.map { it.actionId }.toSet().size)
    }

    @Test
    fun mergeRejectedWhenNotEnoughStagesOrInvalidCount() {
        val tl = SequenceTimeline.create(config())
        val tooMany = tl.apply(entry(SequenceAction.CompleteMerged(5)))
        assertTrue(tooMany is ApplyResult.Rejected)
        assertEquals(RejectReason.NOT_ENOUGH_STAGES, (tooMany as ApplyResult.Rejected).reason)
        val tooFew = tl.apply(entry(SequenceAction.CompleteMerged(1)))
        assertEquals(RejectReason.INVALID_ARGUMENT, (tooFew as ApplyResult.Rejected).reason)
        assertEquals(0, tl.state.cursor)
    }

    // ---------- تجاوز ----------

    @Test
    fun skipMarksStageSkippedNotCompleted() {
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.SkipCurrent)
        assertEquals("stage_simple", stageId(tl))
        assertEquals(1, tl.state.skippedStageCount)
        assertEquals(0, tl.state.completedStageCount)
        assertEquals(StageOutcome.SKIPPED, tl.state.records.single().outcome)
    }

    // ---------- الفروض والاختبارات ----------

    @Test
    fun assignmentBetweenStagesDoesNotConsumeStageByDefault() {
        val tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.CompleteCurrent)
            .doIt(SequenceAction.RecordAssessment(AssessmentKind.ASSIGNMENT, "الفرض الأول"))
        assertEquals("stage_simple", stageId(tl))
        assertEquals(1, tl.state.completedStageCount)
        val rec = tl.state.assessments.single()
        assertEquals("stage_simple", rec.beforeStageId)
        assertFalse(rec.consumedStage)
    }

    @Test
    fun examConsumesStageOnlyWhenTeacherChoosesSo() {
        val tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.RecordAssessment(AssessmentKind.EXAM, "اختبار", consumesStage = true))
        assertEquals("stage_simple", stageId(tl))
        assertEquals(StageOutcome.COMPLETED_BY_ASSESSMENT, tl.state.records.single().outcome)
        assertTrue(tl.state.assessments.single().consumedStage)
    }

    // ---------- إعادة الترتيب ----------

    @Test
    fun reorderChangesOnlyRemainingStages() {
        val tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.CompleteCurrent)
            .doIt(SequenceAction.Reorder(listOf("stage_evaluation", "stage_integration", "stage_simple")))
        assertEquals(
            listOf("stage_learning", "stage_evaluation", "stage_integration", "stage_simple"),
            tl.state.order.map { it.id },
        )
        assertEquals("stage_evaluation", stageId(tl))
        // سجل الإنجاز لم يتغير
        assertEquals("stage_learning", tl.state.records.single().stageId)
    }

    @Test
    fun reorderRejectsCompletedStagesOrBadPermutation() {
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val withCompleted = tl.apply(
            entry(SequenceAction.Reorder(listOf("stage_learning", "stage_simple", "stage_integration", "stage_evaluation"))),
        )
        assertEquals(RejectReason.INVALID_REORDER, (withCompleted as ApplyResult.Rejected).reason)
        val duplicate = tl.apply(entry(SequenceAction.Reorder(listOf("stage_simple", "stage_simple", "stage_evaluation"))))
        assertEquals(RejectReason.INVALID_REORDER, (duplicate as ApplyResult.Rejected).reason)
        val missing = tl.apply(entry(SequenceAction.Reorder(listOf("stage_simple", "stage_integration"))))
        assertEquals(RejectReason.INVALID_REORDER, (missing as ApplyResult.Rejected).reason)
    }

    // ---------- استقلالية الأقسام والمواد ----------

    @Test
    fun sameTemplateOnThreeClassesProgressesIndependently() {
        val class1 = SequenceTimeline.create(config("class1-math"))
        val class2 = SequenceTimeline.create(config("class2-math"))
        val class3 = SequenceTimeline.create(config("class3-math", SequenceTemplates.threeStages()))

        val c1 = class1.doIt(SequenceAction.CompleteCurrent).doIt(SequenceAction.CompleteCurrent)
            .doIt(SequenceAction.CompleteCurrent)
        val c3 = class3.doIt(SequenceAction.CompleteCurrent).doIt(SequenceAction.CompleteCurrent)
            .doIt(SequenceAction.CompleteCurrent)

        assertEquals("stage_evaluation", stageId(c1))          // القسم 1: المرحلة الرابعة
        assertEquals("stage_learning", stageId(class2))         // القسم 2 لم يتأثر
        assertTrue(c3.state.isCycleComplete)                    // القسم 3 أكمل التسلسل
        assertEquals(0, class2.state.records.size)
    }

    @Test
    fun differentSubjectsOfTheSameClassAreIndependent() {
        val math = SequenceTimeline.create(config("class1-math")).doIt(SequenceAction.CompleteCurrent)
        val physics = SequenceTimeline.create(config("class1-physics"))
        assertEquals("stage_simple", stageId(math))
        assertEquals("stage_learning", stageId(physics))
    }

    // ---------- إكمال التسلسل والدورة الجديدة ----------

    @Test
    fun completingAllStagesThenStartNewCycle() {
        var tl = SequenceTimeline.create(config(stages = SequenceTemplates.threeStages()))
        repeat(3) { tl = tl.doIt(SequenceAction.CompleteCurrent) }
        assertTrue(tl.state.isCycleComplete)
        assertNull(tl.state.currentStage)
        assertEquals(100, tl.state.progressPercent)

        val afterComplete = tl.apply(entry(SequenceAction.CompleteCurrent))
        assertEquals(RejectReason.CYCLE_COMPLETE, (afterComplete as ApplyResult.Rejected).reason)

        tl = tl.doIt(SequenceAction.StartNewCycle)
        assertEquals(2, tl.state.cycle)
        assertEquals(1, tl.state.completedCycles)
        assertEquals("stage_learning", stageId(tl))
        assertEquals(0, tl.state.records.size)
    }

    @Test
    fun startNewCycleRejectedWhileCycleIsIncomplete() {
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val r = tl.apply(entry(SequenceAction.StartNewCycle))
        assertEquals(RejectReason.CYCLE_NOT_COMPLETE, (r as ApplyResult.Rejected).reason)
    }

    // ---------- التراجع Undo ----------

    @Test
    fun undoMergeRestoresPreviousState() {
        var tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val before = tl.state
        tl = tl.doIt(SequenceAction.CompleteMerged(2))
        assertEquals("stage_evaluation", stageId(tl))
        tl = tl.undo()
        assertEquals(before.cursor, tl.state.cursor)
        assertEquals(before.records, tl.state.records)
        assertEquals("stage_simple", stageId(tl))
    }

    @Test
    fun undoSkipRestoresStageAsCurrent() {
        var tl = SequenceTimeline.create(config()).doIt(SequenceAction.SkipCurrent)
        tl = tl.undo()
        assertEquals("stage_learning", stageId(tl))
        assertEquals(0, tl.state.skippedStageCount)
    }

    @Test
    fun undoTwiceUndoesTwoActionsInOrder() {
        var tl = SequenceTimeline.create(config())
            .doIt(SequenceAction.CompleteCurrent)
            .doIt(SequenceAction.CompleteCurrent)
        assertEquals("stage_integration", stageId(tl))
        tl = tl.undo()
        assertEquals("stage_simple", stageId(tl))
        tl = tl.undo()
        assertEquals("stage_learning", stageId(tl))
    }

    @Test
    fun undoKeepsLogAppendOnly() {
        var tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val sizeBefore = tl.entries.size
        tl = tl.undo()
        assertEquals(sizeBefore + 1, tl.entries.size) // حدث التراجع أُضيف ولم يُحذف شيء
        assertEquals(0, tl.effectiveEntries.size)
    }

    @Test
    fun undoWithNothingToUndoIsRejected() {
        val r = SequenceTimeline.create(config()).undoLast("u1", 1L)
        assertEquals(RejectReason.NOTHING_TO_UNDO, (r as ApplyResult.Rejected).reason)
    }

    @Test
    fun undoOfNotHeldSessionRestoresCounter() {
        var tl = SequenceTimeline.create(config()).doIt(SequenceAction.NotHeld(NotHeldReason.CANCELLED))
        tl = tl.undo()
        assertEquals(0, tl.state.notHeldCount)
    }

    // ---------- المزامنة ومنع التكرار ----------

    @Test
    fun sameActionDeliveredTwiceIsNotCountedTwice() {
        val tl = SequenceTimeline.create(config())
        val e = LoggedAction("same-id", 10L, SequenceAction.CompleteCurrent, "D1")
        val once = tl.apply(e).timeline
        val twice = once.apply(e)
        assertTrue(twice is ApplyResult.Duplicate)
        assertEquals(1, twice.timeline.state.completedStageCount)
        assertEquals(1, twice.timeline.entries.size)
    }

    @Test
    fun twoDevicesMergingSameLogsDoNotDuplicateRecords() {
        val a = LoggedAction("a1", 1L, SequenceAction.CompleteCurrent, "phone")
        val b = LoggedAction("b1", 2L, SequenceAction.CompleteCurrent, "tablet")
        val phone = SequenceTimeline.create(config(), listOf(a))
        val tablet = SequenceTimeline.create(config(), listOf(a, b)) // الجهاز اللوحي رأى a ثم أضاف b

        val merged = phone.mergeWith(tablet.entries).mergeWith(tablet.entries).mergeWith(listOf(a, b, a))
        assertEquals(2, merged.entries.size)
        assertEquals(2, merged.state.completedStageCount)
        assertEquals("stage_integration", merged.state.currentStage?.id)
    }

    @Test
    fun replayIsDeterministicRegardlessOfArrivalOrder() {
        val es = listOf(
            LoggedAction("e1", 1L, SequenceAction.CompleteCurrent),
            LoggedAction("e2", 2L, SequenceAction.ExtendCurrent),
            LoggedAction("e3", 3L, SequenceAction.CompleteMerged(2)),
            LoggedAction("e4", 4L, SequenceAction.NotHeld(NotHeldReason.CANCELLED)),
        )
        val forward = SequenceTimeline.create(config(), es).state
        val shuffled = SequenceTimeline.create(config(), es.reversed()).state
        assertEquals(forward, shuffled)
    }

    @Test
    fun concurrentConflictingActionIsFlaggedNotCorrupting() {
        // جهازان أتمّا الدورة كلٌّ على حدة ثم تزامنا: الإجراء الزائد يُوسَم متعارضاً ولا يُفسد الحالة.
        val cfg = config(stages = SequenceTemplates.threeStages())
        val base = (1..3).map { LoggedAction("p$it", it.toLong(), SequenceAction.CompleteCurrent, "phone") }
        val extra = LoggedAction("t1", 10L, SequenceAction.CompleteCurrent, "tablet")
        val tl = SequenceTimeline.create(cfg, base + extra)
        assertTrue(tl.state.isCycleComplete)
        assertEquals(3, tl.state.completedStageCount)
        assertTrue("t1" in tl.conflictedIds)
    }

    // ---------- الجدول لا يمس التقدم ----------

    @Test
    fun timetableChangesAreInvisibleToTheEngine() {
        // المحرك لا يعرف الجدول أصلاً: تغيير الجدول لا ينتج أي إجراء، فلا تتغير بيانات الإنجاز.
        val tl = SequenceTimeline.create(config()).doIt(SequenceAction.CompleteCurrent)
        val snapshot = tl.state
        val afterScheduleEdit = SequenceTimeline.create(config(), tl.entries)
        assertEquals(snapshot, afterScheduleEdit.state)
    }

    // ---------- صحة الإعداد ----------

    @Test
    fun configRejectsEmptyOrDuplicateStages() {
        var failed = 0
        try { SequenceConfig("x", emptyList()) } catch (e: IllegalArgumentException) { failed++ }
        try { SequenceConfig("x", listOf(StageDef("a", "A"), StageDef("a", "B"))) } catch (e: IllegalArgumentException) { failed++ }
        assertEquals(2, failed)
    }
}
