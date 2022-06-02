package schedules

import kotlin.concurrent.fixedRateTimer

interface ScheduledEvent {

    var task: Any

    val scheduleName: String

    val interval: Long

    fun task(): Unit

    fun stop() {

    }

    fun start() {
        task = fixedRateTimer(scheduleName, true, period = interval, action = { task() })
    }

}