package org.teamtators.vision.vision

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.EventBus
import com.google.inject.Provides
import com.google.inject.Singleton
import org.teamtators.vision.guiceKt.AbstractKotlinModule
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class VisionModule : AbstractKotlinModule() {
    override fun configure() {
        bind<OpenCVCapturer>().asEagerSingleton()
        bind<FrameProcessor>().asEagerSingleton()
        bind<ImageResizer>().asEagerSingleton()
        bind<ExecutorShutdown>().asEagerSingleton()
    }

    @Provides @Singleton
    fun providesThreadPoolExecutor() = ThreadPoolExecutor(2, 10, 10, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(),
            ThreadPoolExecutor.DiscardPolicy())

    @Provides @Singleton
    fun providesExecutor(executor: ThreadPoolExecutor): Executor = executor

    @Provides @Singleton
    fun providesEventBus(executor: Executor): EventBus = AsyncEventBus("TatorVision", executor)
}
