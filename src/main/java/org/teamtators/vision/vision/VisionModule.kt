package org.teamtators.vision.vision

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.EventBus
import com.google.common.util.concurrent.MoreExecutors
import com.google.inject.Provides
import com.google.inject.Singleton
import org.teamtators.vision.guiceKt.AbstractKotlinModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class VisionModule(val useRpiCapturer: Boolean) : AbstractKotlinModule() {
    override fun configure() {
        if (useRpiCapturer)
            bind<RpiCapturer>().asEagerSingleton()
        else
            bind<OpenCVCapturer>().asEagerSingleton()

        bind<ProcessRunner>().asEagerSingleton()
        bind<FrameProcessor>().asEagerSingleton()
        bind<ImageResizer>().asEagerSingleton()
        bind<ExecutorShutdown>().asEagerSingleton()
    }

    @Provides @Singleton
    fun providesScheduledExecutorService() = Executors.newScheduledThreadPool(5)

    @Provides @Singleton
    fun providesExecutorService(scheduledExecutorService: ScheduledExecutorService): ExecutorService
            = scheduledExecutorService

    @Provides @Singleton
    fun providesEventBus(/*executor: Executor*/): EventBus = AsyncEventBus("TatorVision", MoreExecutors.directExecutor())
}
