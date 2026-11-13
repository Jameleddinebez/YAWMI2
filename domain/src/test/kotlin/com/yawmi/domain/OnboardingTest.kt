package com.yawmi.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class OnboardingTest {

    // ---------- WorkSystem ----------

    @Test
    fun periodsAreLessonPlusBreakSpaced() {
        val p = WorkSystem.periods(8 * 60, 3, 60, 10)
        assertEquals(listOf(480, 550, 620), p.map { it.startMinutes })
        assertEquals(listOf(540, 610, 680), p.map { it.endMinutes })
        assertEquals(listOf(1, 2, 3), p.map { it.index })
    }

    @Test
    fun fortyFiveMinuteSystemIsAnyOrdinaryDurationNotTiedToAMonth() {
        val p = WorkSystem.periods(8 * 60, 2, 45, 5)
        assertEquals(480 to 525, p[0].startMinutes to p[0].endMinutes)
        assertEquals(530 to 575, p[1].startMinutes to p[1].endMinutes)
    }

    @Test
    fun defaultWeekIsEditableAndNotForcedUniform() {
        val week = WorkSystem.defaultWeek()
        assertEquals(7, week.size)
        assertEquals(WorkMode.MORNING, week.first { it.day == DayOfWeek.SUNDAY }.mode)
        assertEquals(WorkMode.OFF, week.first { it.day == DayOfWeek.FRIDAY }.mode)
        // لا حصص وهمية: مجرد إعداد فترات
        assertTrue(week.all { it.mode == WorkMode.OFF || it.morningPeriods > 0 })
    }

    @Test
    fun offDayHasNoIssuesEvenWithZeroPeriods() {
        assertTrue(WorkSystem.issues(DayWorkConfig(DayOfWeek.FRIDAY, WorkMode.OFF), 60, 10).isEmpty())
    }

    @Test
    fun morningOnlyNeedsPeriodsAndMustFitInDay() {
        val none = DayWorkConfig(DayOfWeek.SUNDAY, WorkMode.MORNING, morningPeriods = 0)
        assertEquals(listOf(WorkConfigIssue.MORNING_PERIODS_REQUIRED), WorkSystem.issues(none, 60, 10))
        val tooLate = DayWorkConfig(DayOfWeek.SUNDAY, WorkMode.MORNING, morningStart = 23 * 60, morningPeriods = 2)
        assertEquals(listOf(WorkConfigIssue.MORNING_EXCEEDS_DAY), WorkSystem.issues(tooLate, 60, 10))
        val ok = DayWorkConfig(DayOfWeek.SUNDAY, WorkMode.MORNING, morningPeriods = 5)
        assertTrue(WorkSystem.issues(ok, 60, 10).isEmpty())
    }

    @Test
    fun eveningOnlyDayIsSupported() {
        val cfg = DayWorkConfig(DayOfWeek.MONDAY, WorkMode.EVENING, eveningStart = 13 * 60, eveningPeriods = 3)
        assertTrue(WorkSystem.issues(cfg, 60, 10).isEmpty())
        assertTrue(WorkSystem.morningPeriods(cfg, 60, 10).isEmpty())
        assertEquals(3, WorkSystem.eveningPeriods(cfg, 60, 10).size)
    }

    @Test
    fun bothSessionsMustNotOverlap() {
        val overlap = DayWorkConfig(
            DayOfWeek.TUESDAY, WorkMode.BOTH,
            morningStart = 8 * 60, morningPeriods = 5, eveningStart = 12 * 60, eveningPeriods = 2,
        )
        assertEquals(listOf(WorkConfigIssue.SESSIONS_OVERLAP), WorkSystem.issues(overlap, 60, 10))
        val fine = overlap.copy(eveningStart = 14 * 60)
        assertTrue(WorkSystem.issues(fine, 60, 10).isEmpty())
    }

    @Test
    fun invalidDurationsAreRejected() {
        val cfg = DayWorkConfig(DayOfWeek.SUNDAY, WorkMode.MORNING, morningPeriods = 2)
        assertEquals(listOf(WorkConfigIssue.INVALID_DURATION), WorkSystem.issues(cfg, 5, 10))
        assertEquals(listOf(WorkConfigIssue.INVALID_DURATION), WorkSystem.issues(cfg, 60, 200))
        assertTrue(WorkSystem.durationValid(35, 0))
        assertTrue(WorkSystem.durationValid(75, 15))
    }

    // ---------- OnboardingValidator ----------

    @Test
    fun nameAndLevelAreRequired() {
        val errs = OnboardingValidator.infoErrors(OnboardingDraft())
        assertEquals(listOf(OnboardingError.NAME_REQUIRED, OnboardingError.LEVEL_REQUIRED), errs)
        val ok = OnboardingDraft(name = "أحمد", level = EducationLevel.MIDDLE)
        assertTrue(OnboardingValidator.infoErrors(ok).isEmpty())
    }

    @Test
    fun optionalFieldsAreNotForcedButCheckedWhenProvided() {
        val base = OnboardingDraft(name = "س", level = EducationLevel.SECONDARY)
        assertTrue(OnboardingValidator.infoErrors(base.copy(email = "", age = "")).isEmpty())
        assertEquals(listOf(OnboardingError.INVALID_EMAIL), OnboardingValidator.infoErrors(base.copy(email = "abc")))
        assertEquals(listOf(OnboardingError.INVALID_AGE), OnboardingValidator.infoErrors(base.copy(age = "7")))
        assertEquals(listOf(OnboardingError.INVALID_AGE), OnboardingValidator.infoErrors(base.copy(age = "xx")))
        assertTrue(OnboardingValidator.infoErrors(base.copy(email = "a@b.dz", age = "35")).isEmpty())
    }

    @Test
    fun atLeastOneSubjectIsRequired() {
        assertEquals(listOf(OnboardingError.SUBJECTS_REQUIRED), OnboardingValidator.subjectErrors(OnboardingDraft()))
        assertTrue(OnboardingValidator.subjectErrors(OnboardingDraft(subjectIds = setOf("sub_mid_math"))).isEmpty())
    }

    @Test
    fun workErrorsDetectNoWorkDaysAndBadConfig() {
        val allOff = OnboardingDraft(week = WorkSystem.defaultWeek().map { it.copy(mode = WorkMode.OFF) })
        assertTrue(OnboardingError.NO_WORK_DAYS in OnboardingValidator.workErrors(allOff))
        assertTrue(OnboardingValidator.workErrors(OnboardingDraft()).isEmpty())
        val bad = OnboardingDraft(week = WorkSystem.defaultWeek().map { it.copy(morningPeriods = 0) })
        assertTrue(OnboardingError.WORK_CONFIG_INVALID in OnboardingValidator.workErrors(bad))
    }

    // ---------- SubjectCatalogSeed ----------

    @Test
    fun subjectIdsAreUniqueAndLevelsAreNotMixed() {
        val all = SubjectCatalogSeed.all()
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(SubjectCatalogSeed.middle.all { it.level == EducationLevel.MIDDLE && it.id.startsWith("sub_mid_") })
        assertTrue(SubjectCatalogSeed.secondary.all { it.level == EducationLevel.SECONDARY && it.id.startsWith("sub_sec_") })
        assertTrue(all.all { it.nameAr.isNotBlank() })
    }

    @Test
    fun catalogIsHonestlyMarkedAsDraft() {
        assertTrue(SubjectCatalogSeed.CATALOG_VERSION.startsWith("draft"))
        assertFalse(SubjectCatalogSeed.SOURCE_NOTE.isBlank())
        assertEquals(SubjectCatalogSeed.middle, SubjectCatalogSeed.forLevel(EducationLevel.MIDDLE))
    }
}
