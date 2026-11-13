package com.yawmi.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yawmi.app.YawmiApplication
import com.yawmi.app.data.local.entity.SubjectCatalogEntity
import com.yawmi.app.di.AppContainer
import com.yawmi.domain.DayWorkConfig
import com.yawmi.domain.EducationLevel
import com.yawmi.domain.Gender
import com.yawmi.domain.OnboardingDraft
import com.yawmi.domain.OnboardingValidator
import com.yawmi.domain.WorkMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class OnboardingFlags(
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
)

data class OnboardingUiState(
    /** 0 ترحيب، 1 معلومات، 2 مواد، 3 نظام العمل، 4 ملخص */
    val step: Int,
    val draft: OnboardingDraft,
    val subjects: List<SubjectCatalogEntity>,
    val flags: OnboardingFlags,
)

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {

    private val store = container.onboardingDraftStore
    private val repo = container.onboardingRepository

    private val stepFlow = MutableStateFlow(store.loadStep())
    private val draftFlow = MutableStateFlow(store.load())
    private val flagsFlow = MutableStateFlow(OnboardingFlags())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val subjectsFlow = draftFlow.map { it.level }.distinctUntilChanged().flatMapLatest { level ->
        if (level == null) flowOf(emptyList()) else repo.subjectsFor(level)
    }

    val uiState: StateFlow<OnboardingUiState> = combine(stepFlow, draftFlow, subjectsFlow, flagsFlow) { step, draft, subjects, flags ->
        OnboardingUiState(step, draft, subjects, flags)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        OnboardingUiState(stepFlow.value, draftFlow.value, emptyList(), OnboardingFlags()),
    )

    private fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        val next = transform(draftFlow.value)
        draftFlow.value = next
        store.save(next)
    }

    // ---- الخطوة 1
    fun setName(v: String) = update { it.copy(name = v) }
    fun setInstitution(v: String) = update { it.copy(institutionName = v) }
    fun setEmail(v: String) = update { it.copy(email = v) }
    fun setAge(v: String) = update { it.copy(age = v.filter { c -> c.isDigit() }.take(2)) }
    fun setGender(g: Gender?) = update { it.copy(gender = g) }

    /** تغيير الطور يفرغ المواد المختارة: لا خلط بين مواد المتوسط والثانوي. */
    fun setLevel(level: EducationLevel) = update {
        if (it.level == level) it else it.copy(level = level, subjectIds = emptySet())
    }

    // ---- الخطوة 2
    fun toggleSubject(id: String) = update {
        it.copy(subjectIds = if (id in it.subjectIds) it.subjectIds - id else it.subjectIds + id)
    }

    fun addCustomSubject(name: String) {
        val level = draftFlow.value.level ?: return
        viewModelScope.launch {
            val id = repo.addCustomSubject(name, level)
            if (id != null) update { it.copy(subjectIds = it.subjectIds + id) }
        }
    }

    // ---- الخطوة 3
    fun setLessonMinutes(v: Int) = update { it.copy(lessonMinutes = v) }
    fun setBreakMinutes(v: Int) = update { it.copy(breakMinutes = v) }

    fun updateDay(day: DayOfWeek, transform: (DayWorkConfig) -> DayWorkConfig) = update { d ->
        d.copy(week = d.week.map { if (it.day == day) transform(it) else it })
    }

    fun setDayMode(day: DayOfWeek, mode: WorkMode) = updateDay(day) { c ->
        // عند تفعيل فترة لأول مرة نقترح عدداً بدئياً بدل صفر (قابل للتعديل)
        c.copy(
            mode = mode,
            morningPeriods = if (mode.hasMorning && c.morningPeriods == 0) 5 else c.morningPeriods,
            eveningPeriods = if (mode.hasEvening && c.eveningPeriods == 0) 3 else c.eveningPeriods,
        )
    }

    // ---- التنقل
    fun next() {
        val s = uiState.value
        val errors = when (s.step) {
            1 -> OnboardingValidator.infoErrors(s.draft)
            2 -> OnboardingValidator.subjectErrors(s.draft)
            3 -> OnboardingValidator.workErrors(s.draft)
            else -> emptyList()
        }
        if (errors.isNotEmpty()) {
            flagsFlow.value = flagsFlow.value.copy(showErrors = true)
            return
        }
        flagsFlow.value = OnboardingFlags()
        val n = (s.step + 1).coerceAtMost(4)
        stepFlow.value = n
        store.saveStep(n)
    }

    fun back() {
        val n = (stepFlow.value - 1).coerceAtLeast(0)
        flagsFlow.value = OnboardingFlags()
        stepFlow.value = n
        store.saveStep(n)
    }

    fun finish() {
        if (flagsFlow.value.saving) return
        val draft = draftFlow.value
        flagsFlow.value = OnboardingFlags(saving = true)
        viewModelScope.launch {
            repo.complete(draft).fold(
                onSuccess = {
                    container.userPrefs.saveProfileExtras(draft.gender?.name, draft.age.ifBlank { null })
                    store.clear()
                    flagsFlow.value = OnboardingFlags()
                },
                onFailure = { e ->
                    flagsFlow.value = OnboardingFlags(saveError = e.message ?: "تعذّر الحفظ")
                },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YawmiApplication
                OnboardingViewModel(app.container)
            }
        }
    }
}
