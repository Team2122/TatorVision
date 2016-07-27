package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.teamtators.vision.events.StopEvent
import java.util.concurrent.ThreadPoolExecutor

class ExecutorShutdown @Inject constructor(
        val executor: ThreadPoolExecutor,
        val eventBus: EventBus
) {
    init {
        eventBus.register(this)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        executor.shutdown()
    }
}