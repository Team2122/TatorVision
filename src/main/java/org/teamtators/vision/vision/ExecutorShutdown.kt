package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import java.util.concurrent.ThreadPoolExecutor

class ExecutorShutdown @Inject constructor(
        val executor: ThreadPoolExecutor,
        val eventBus: EventBus
) {
    init {
        eventBus.register(this)
    }

    @Subscribe
    private fun onStop() {
        executor.shutdown()
    }
}