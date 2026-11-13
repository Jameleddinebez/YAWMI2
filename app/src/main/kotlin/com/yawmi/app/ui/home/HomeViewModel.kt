package com.yawmi.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yawmi.app.YawmiApplication
import com.yawmi.app.di.AppContainer
import com.yawmi.domain.AgendaItem
import com.yawmi.domain.AppointmentType
import com.yawmi.domain.DayType
import com.yawmi.domain.DayTypeResolver
import com.yawmi.domain.TaskItem
import com.yawmi.domain.WorkWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeUiState(
    val date: LocalDate,
    val dayType: DayType,
    val isDayTypeManual: Boolean,
    val holidayName: String?,
    val items: List<AgendaItem>,
    val openTasks: List<TaskItem>,
    val openTaskCount: Int,
    val hijriEnabled: Boolean,
) {
    companion object {
        fun initial(date: LocalDate) = HomeUiState(
            date = date,
            dayType = DayType.UNSPECIFIED,
            isDayTypeManual = false,
            holidayName = null,
            items = emptyList(),
            openTasks = emptyList(),
            openTaskCount = 0,
            hijriEnabled = true,
        )
    }
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val date = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = date.flatMapLatest { d ->
        combine(
            container.agendaRepository.observeDay(d),
            container.taskRepository.observeOpen(),
            container.userPrefs.hijriEnabled,
            container.userPrefs.dayTypeOverrides,
        ) { day, tasks, hijri, overrides ->
            val manual = overrides[d.toString()]
            val auto = DayTypeResolver.resolve(d, day.items, day.holidayName != null, WorkWeek.DEFAULT)
            HomeUiState(
                date = d,
                dayType = manual ?: auto,
                isDayTypeManual = manual != null,
                holidayName = day.holidayName,
                items = day.items,
                openTasks = tasks.take(3),
                openTaskCount = tasks.size,
                hijriEnabled = hijri,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.initial(LocalDate.now()))

    /** تُستدعى من الواجهة عند تغيّر التاريخ (مرور منتصف الليل) دون إعادة فتح الشاشة. */
    fun onDateChanged(newDate: LocalDate) {
        if (date.value != newDate) date.value = newDate
    }

    fun setDayTypeOverride(type: DayType?) {
        container.userPrefs.setDayTypeOverride(date.value, type)
    }

    fun addTask(title: String, description: String?) {
        viewModelScope.launch { container.taskRepository.add(title, description) }
    }

    fun toggleTask(id: String) {
        viewModelScope.launch { container.taskRepository.setDone(id, true) }
    }

    fun addAppointmentToday(title: String, type: AppointmentType, startMinutes: Int?, endMinutes: Int?) {
        viewModelScope.launch {
            container.agendaRepository.addAppointment(title, type, date.value, startMinutes, endMinutes)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YawmiApplication
                HomeViewModel(app.container)
            }
        }
    }
}
