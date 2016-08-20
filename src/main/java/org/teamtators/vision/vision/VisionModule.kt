package org.teamtators.vision.vision

import com.google.common.eventbus.AsyncEventBus
import com.google.common.eventbus.EventBus
import com.google.common.util.concurrent.MoreExecutors
import com.google.inject.Provides
import com.google.inject.Singleton
import org.teamtators.vision.guiceKt.AbstractKotlinModule
import java.util.concurrent.*

class VisionModule : AbstractKotlinModule() {
    override fun configure() {
        bind<OpenCVCapturer>().asEagerSingleton()
        bind<FrameProcessor>().asEagerSingleton()
        bind<ImageResizer>().asEagerSingleton()
//        bind<ExecutorShutdown>().asEagerSingleton()
    }

//    @Provides @Singleton
//    fun providesThreadPoolExecutor() = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>(),
//            ThreadPoolExecutor.DiscardPolicy())

    @Provides @Singleton
    fun providesExecutor(): Executor = MoreExecutors.directExecutor()

    @Provides @Singleton
    fun providesEventBus(executor: Executor): EventBus = AsyncEventBus("TatorVision", executor)
}
