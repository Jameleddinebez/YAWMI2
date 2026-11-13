package com.yawmi.sequence

/**
 * قوالب افتراضية. هي نقاط انطلاق فقط: يستطيع الأستاذ تعديل الأسماء والعدد والترتيب.
 * المعرّفات ثابتة كي لا تتكرر القوالب عند المزامنة بين الأجهزة.
 */
object SequenceTemplates {
    val LEARNING = StageDef("stage_learning", "وضعية تعلمية")
    val SIMPLE = StageDef("stage_simple", "وضعية بسيطة")
    val APPLIED = StageDef("stage_applied", "وضعية تطبيقية")
    val INTEGRATION = StageDef("stage_integration", "وضعية إدماجية")
    val EVALUATION = StageDef("stage_evaluation", "وضعية تقويمية")

    fun threeStages(): List<StageDef> = listOf(LEARNING, INTEGRATION, EVALUATION)
    fun fourStages(): List<StageDef> = listOf(LEARNING, SIMPLE, INTEGRATION, EVALUATION)
    fun fiveStages(): List<StageDef> = listOf(LEARNING, SIMPLE, APPLIED, INTEGRATION, EVALUATION)
}
