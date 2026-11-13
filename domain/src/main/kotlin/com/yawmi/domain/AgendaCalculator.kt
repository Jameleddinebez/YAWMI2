package com.yawmi.domain

/**
 * حسابات أجندة اليوم. دوال نقية تعتمد على «الآن» كمُدخل (بالثواني منذ منتصف الليل)،
 * فتكون قابلة للاختبار دون ساعة النظام.
 */
object AgendaCalculator {

    fun sortedAgenda(items: List<AgendaItem>): List<AgendaItem> =
        items.sortedWith(
            compareBy<AgendaItem>(
                { it.startMinutes == null },
                { it.startMinutes ?: 0 },
                { it.endMinutes ?: 0 },
                { it.title },
            ),
        )

    /** مجموع دقائق العمل المجدولة بدمج الفترات المتداخلة كي لا تُحتسب مرتين. */
    fun scheduledWorkMinutes(items: List<AgendaItem>): Int {
        val intervals = items
            .filter { it.countsAsWork && it.isTimed }
            .map { it.startMinutes!! to it.endMinutes!! }
            .filter { it.second > it.first }
            .sortedBy { it.first }
        var total = 0
        var curStart = -1
        var curEnd = -1
        for ((s, e) in intervals) {
            if (curEnd < 0) {
                curStart = s; curEnd = e
            } else if (s <= curEnd) {
                if (e > curEnd) curEnd = e
            } else {
                total += curEnd - curStart
                curStart = s; curEnd = e
            }
        }
        if (curEnd >= 0) total += curEnd - curStart
        return total
    }

    /** الحصص التي لم تنتهِ بعد (لم تبدأ أو جارية). */
    fun remainingLessons(items: List<AgendaItem>, nowSeconds: Int): Int =
        items.count { it.kind == AgendaKind.LESSON && it.isTimed && it.endMinutes!! * 60 > nowSeconds }

    fun totalLessons(items: List<AgendaItem>): Int = items.count { it.kind == AgendaKind.LESSON }

    /** النشاط الجاري الآن (غير الاستراحة)، الأبكر بداية إن وُجد أكثر من واحد. */
    fun currentActivity(items: List<AgendaItem>, nowSeconds: Int): AgendaItem? =
        items.filter { it.isTimed && it.kind != AgendaKind.BREAK }
            .filter { it.startMinutes!! * 60 <= nowSeconds && nowSeconds < it.endMinutes!! * 60 }
            .minByOrNull { it.startMinutes!! }

    /** النشاط القادم: أقرب نشاط (غير استراحة) يبدأ بعد الآن. */
    fun nextActivity(items: List<AgendaItem>, nowSeconds: Int): AgendaItem? =
        items.filter { it.isTimed && it.kind != AgendaKind.BREAK && it.startMinutes!! * 60 > nowSeconds }
            .minByOrNull { it.startMinutes!! }

    fun secondsUntilStart(item: AgendaItem, nowSeconds: Int): Int =
        (item.startMinutes ?: 0) * 60 - nowSeconds

    fun secondsUntilEnd(item: AgendaItem, nowSeconds: Int): Int =
        (item.endMinutes ?: 0) * 60 - nowSeconds
}
