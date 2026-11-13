package com.yawmi.app.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yawmi.app.ui.common.YawmiLogo
import com.yawmi.domain.ArabicDates
import com.yawmi.domain.DayWorkConfig
import com.yawmi.domain.EducationLevel
import com.yawmi.domain.Gender
import com.yawmi.domain.OnboardingDraft
import com.yawmi.domain.OnboardingError
import com.yawmi.domain.OnboardingValidator
import com.yawmi.domain.TimeFormat
import com.yawmi.domain.WorkMode
import com.yawmi.domain.WorkSystem

private const val LAST_STEP = 4

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.step == 0) {
        WelcomeStep(onStart = viewModel::next)
        return
    }

    val errors: List<OnboardingError> = if (!state.flags.showErrors) emptyList() else when (state.step) {
        1 -> OnboardingValidator.infoErrors(state.draft)
        2 -> OnboardingValidator.subjectErrors(state.draft)
        3 -> OnboardingValidator.workErrors(state.draft)
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "الخطوة ${state.step} من $LAST_STEP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { state.step / LAST_STEP.toFloat() }, modifier = Modifier.fillMaxWidth())
            }
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = viewModel::back, modifier = Modifier.weight(1f).height(52.dp)) { Text("السابق") }
                Button(
                    onClick = if (state.step == LAST_STEP) viewModel::finish else viewModel::next,
                    enabled = !state.flags.saving,
                    modifier = Modifier.weight(2f).height(52.dp),
                ) { Text(if (state.step == LAST_STEP) "إنشاء جدولي" else "التالي") }
            }
        },
    ) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.step) {
                1 -> InfoStep(state.draft, errors, viewModel)
                2 -> SubjectsStep(state, errors, viewModel)
                3 -> WorkStep(state.draft, errors, viewModel)
                else -> SummaryStep(state, viewModel)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------- الترحيب

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(700), label = "welcomeAlpha")

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.weight(2f).fillMaxWidth().graphicsLayer {
                this.alpha = alpha
                translationY = (1f - alpha) * 48f
            },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            YawmiLogo(size = 96.dp)
            Spacer(Modifier.height(24.dp))
            Text("مرحباً بك", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("يومي", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(
                "مساعدك اليومي لتنظيم الجدول والحصص والمواعيد ومتابعة تقدّمك البيداغوجي.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        // الزر في الثلث السفلي
        Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("ابدأ") }
        }
    }
}

// ---------------------------------------------------------------- الخطوة 1: المعلومات

@Composable
private fun InfoStep(draft: OnboardingDraft, errors: List<OnboardingError>, vm: OnboardingViewModel) {
    StepTitle("معلوماتك", "الاسم والطور إلزاميان، وكل ما عداهما اختياري ويمكن تعديله لاحقاً.")

    OutlinedTextField(
        value = draft.name, onValueChange = vm::setName,
        label = { Text("الاسم") }, singleLine = true,
        isError = OnboardingError.NAME_REQUIRED in errors,
        supportingText = errorText(errors, OnboardingError.NAME_REQUIRED),
        modifier = Modifier.fillMaxWidth(),
    )

    Text("الطور التعليمي", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EducationLevel.entries.forEach { level ->
            FilterChip(
                selected = draft.level == level, onClick = { vm.setLevel(level) },
                label = { Text(level.arabicLabel) },
            )
        }
    }
    if (OnboardingError.LEVEL_REQUIRED in errors) ErrorLine(OnboardingError.LEVEL_REQUIRED.arabicMessage)

    Text("اختياري", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    OutlinedTextField(
        value = draft.institutionName, onValueChange = vm::setInstitution,
        label = { Text("المؤسسة") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = draft.email, onValueChange = vm::setEmail,
        label = { Text("البريد الإلكتروني") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        isError = OnboardingError.INVALID_EMAIL in errors,
        supportingText = errorText(errors, OnboardingError.INVALID_EMAIL),
        modifier = Modifier.fillMaxWidth(),
    )
    Text("الجنس", style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Gender.entries.forEach { g ->
            FilterChip(
                selected = draft.gender == g, onClick = { vm.setGender(if (draft.gender == g) null else g) },
                label = { Text(g.arabicLabel) },
            )
        }
    }
    OutlinedTextField(
        value = draft.age, onValueChange = vm::setAge,
        label = { Text("العمر") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = OnboardingError.INVALID_AGE in errors,
        supportingText = errorText(errors, OnboardingError.INVALID_AGE),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- الخطوة 2: المواد

@Composable
private fun SubjectsStep(state: OnboardingUiState, errors: List<OnboardingError>, vm: OnboardingViewModel) {
    val level = state.draft.level
    StepTitle("موادك", "اختر مادة أو أكثر. تظهر هنا مواد ${level?.arabicLabel ?: ""} فقط.")

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(
            "القائمة مسودة غير موثّقة رسمياً (تعذّر التحقق من نصوص الوزارة أثناء البناء). أضف أي مادة تنقصك بزر «إضافة مادة».",
            modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
        )
    }

    if (state.subjects.isEmpty()) {
        Text("جارٍ تحميل المواد…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    state.subjects.forEach { s ->
        val checked = s.id in state.draft.subjectIds
        Row(
            Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Checkbox, onValueChange = { vm.toggleSubject(s.id) })
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = checked, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(s.nameAr + if (s.isCustom) " (أضفتها)" else "", style = MaterialTheme.typography.bodyLarge)
                s.nameFr?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    var showAdd by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Icon(Icons.Filled.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("إضافة مادة")
    }
    if (OnboardingError.SUBJECTS_REQUIRED in errors) ErrorLine(OnboardingError.SUBJECTS_REQUIRED.arabicMessage)
    if (showAdd) {
        AddSubjectDialog(onDismiss = { showAdd = false }, onConfirm = { vm.addCustomSubject(it); showAdd = false })
    }
}

@Composable
private fun AddSubjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة مادة") },
        text = {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("اسم المادة") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name) }) { Text("إضافة") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}

// ---------------------------------------------------------------- الخطوة 3: نظام العمل

@Composable
private fun WorkStep(draft: OnboardingDraft, errors: List<OnboardingError>, vm: OnboardingViewModel) {
    StepTitle("نظام العمل", "حدّد مدة الحصة وأيام العمل. لكل يوم إعداده المستقل، وكله قابل للتعديل لاحقاً.")

    Text("مدة الحصة (دقائق)", style = MaterialTheme.typography.titleSmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(listOf(45, 50, 55, 60)) { m ->
            FilterChip(selected = draft.lessonMinutes == m, onClick = { vm.setLessonMinutes(m) }, label = { Text("$m") })
        }
    }
    NumberField("أو أدخل مدة أخرى", draft.lessonMinutes, vm::setLessonMinutes)
    NumberField("مدة الاستراحة بين الحصص (دقائق)", draft.breakMinutes, vm::setBreakMinutes)
    if (!WorkSystem.durationValid(draft.lessonMinutes, draft.breakMinutes)) {
        ErrorLine("${WorkSystem.MIN_LESSON}–${WorkSystem.MAX_LESSON} دقيقة للحصة، و0–${WorkSystem.MAX_BREAK} للاستراحة")
    }

    draft.week.forEach { cfg -> DayCard(cfg, draft.lessonMinutes, draft.breakMinutes, vm) }

    if (OnboardingError.NO_WORK_DAYS in errors) ErrorLine(OnboardingError.NO_WORK_DAYS.arabicMessage)
    if (OnboardingError.WORK_CONFIG_INVALID in errors) ErrorLine(OnboardingError.WORK_CONFIG_INVALID.arabicMessage)
}

@Composable
private fun NumberField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onChange(it.filter { c -> c.isDigit() }.take(3).toIntOrNull() ?: 0) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DayCard(cfg: DayWorkConfig, lesson: Int, brk: Int, vm: OnboardingViewModel) {
    val issues = WorkSystem.issues(cfg, lesson, brk)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ArabicDates.dayName(cfg.day), style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(WorkMode.entries.toList()) { mode ->
                    FilterChip(
                        selected = cfg.mode == mode, onClick = { vm.setDayMode(cfg.day, mode) },
                        label = { Text(mode.arabicLabel) },
                    )
                }
            }
            if (cfg.mode.hasMorning) {
                SessionEditor(
                    title = "الفترة الصباحية", start = cfg.morningStart, periods = cfg.morningPeriods,
                    lesson = lesson, brk = brk,
                    onStart = { s -> vm.updateDay(cfg.day) { it.copy(morningStart = s) } },
                    onPeriods = { p -> vm.updateDay(cfg.day) { it.copy(morningPeriods = p) } },
                )
            }
            if (cfg.mode.hasEvening) {
                SessionEditor(
                    title = "الفترة المسائية", start = cfg.eveningStart, periods = cfg.eveningPeriods,
                    lesson = lesson, brk = brk,
                    onStart = { s -> vm.updateDay(cfg.day) { it.copy(eveningStart = s) } },
                    onPeriods = { p -> vm.updateDay(cfg.day) { it.copy(eveningPeriods = p) } },
                )
            }
            issues.forEach { ErrorLine(it.arabicMessage) }
        }
    }
}

@Composable
private fun SessionEditor(
    title: String, start: Int, periods: Int, lesson: Int, brk: Int,
    onStart: (Int) -> Unit, onPeriods: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(TimeFormat.minutes(start)) }
    val parsed = TimeFormat.parseMinutes(text)
    val list = WorkSystem.periods(start, periods, lesson, brk)

    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { t ->
                text = t
                TimeFormat.parseMinutes(t)?.let(onStart)
            },
            label = { Text("البداية HH:mm") }, singleLine = true, isError = parsed == null,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { onPeriods((periods - 1).coerceAtLeast(0)) }) {
            Icon(Icons.Filled.Remove, contentDescription = "إنقاص عدد الحصص")
        }
        Text("$periods", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = { onPeriods((periods + 1).coerceAtMost(WorkSystem.MAX_PERIODS)) }) {
            Icon(Icons.Filled.Add, contentDescription = "زيادة عدد الحصص")
        }
    }
    if (parsed == null) ErrorLine("صيغة الوقت: ساعة:دقيقة، مثل 08:00")
    if (list.isNotEmpty()) {
        Text(
            "${TimeFormat.minutes(list.first().startMinutes)} – ${TimeFormat.minutes(list.last().endMinutes % (24 * 60))}  ($periods حصص)",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------- الخطوة 4: الملخص

@Composable
private fun SummaryStep(state: OnboardingUiState, vm: OnboardingViewModel) {
    val d = state.draft
    StepTitle("ملخص", "راجع اختياراتك ثم أنشئ جدولك.")

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryLine("الاسم", d.name.trim())
            SummaryLine("الطور", d.level?.arabicLabel ?: "")
            if (d.institutionName.isNotBlank()) SummaryLine("المؤسسة", d.institutionName.trim())
            val names = state.subjects.filter { it.id in d.subjectIds }.joinToString("، ") { it.nameAr }
            SummaryLine("المواد", names)
            SummaryLine("الحصة", "${d.lessonMinutes} د  •  استراحة ${d.breakMinutes} د")
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            d.week.forEach { c ->
                val m = WorkSystem.morningPeriods(c, d.lessonMinutes, d.breakMinutes)
                val e = WorkSystem.eveningPeriods(c, d.lessonMinutes, d.breakMinutes)
                val desc = when (c.mode) {
                    WorkMode.OFF -> "راحة"
                    else -> listOfNotNull(
                        m.takeIf { it.isNotEmpty() }?.let { "صباحاً ${it.size} حصص" },
                        e.takeIf { it.isNotEmpty() }?.let { "مساءً ${it.size} حصص" },
                    ).joinToString(" + ")
                }
                SummaryLine(ArabicDates.dayName(c.day), desc)
            }
        }
    }
    Text(
        "سيُنشأ جدول فارغ بلا حصص: تُضيف حصصك بنفسك لاحقاً. ضبط التنبيهات يأتي في مرحلة لاحقة من تطوير التطبيق.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    state.flags.saveError?.let { ErrorLine("تعذّر الحفظ: $it") }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$label:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- مساعدات

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorLine(message: String) {
    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

private fun errorText(errors: List<OnboardingError>, e: OnboardingError): (@Composable () -> Unit)? =
    if (e in errors) ({ Text(e.arabicMessage) }) else null
