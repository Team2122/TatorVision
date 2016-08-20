package org.teamtators.vision

import com.google.common.eventbus.EventBus
import org.opencv.core.Core
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.ConfigModule
import org.teamtators.vision.display.DisplayModule
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.guiceKt.childInjector
import org.teamtators.vision.guiceKt.getInstance
import org.teamtators.vision.guiceKt.injector
import org.teamtators.vision.http.ServerModule
import org.teamtators.vision.tables.TablesModule
import org.teamtators.vision.vision.VisionModule
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val TATORVISION_HEADER = "\n" +
        "┌─────────────────────────────┐\n" +
        "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
        "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
        "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
        "└─────────────────────────────┘\n"

fun onShutdown(task: () -> Unit) {
    Runtime.getRuntime()
            .addShutdownHook(Thread(task))
}

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val logger = LoggerFactory.getLogger("main")

    logger.info(TATORVISION_HEADER)

    val baseInjector = injector {
        install(ConfigModule())
    }
    val config = baseInjector.getInstance<Config>()

    logger.debug("Using OpenCV Version: {}", Core.VERSION)

    var opencvJavaDir: String? = System.getProperty("tatorvision.opencvjavadir")
    if (opencvJavaDir == null)
        opencvJavaDir = "/usr/local/share/OpenCV/java"
    val opencvLib = String.format("%s/lib%s.so", opencvJavaDir, Core.NATIVE_LIBRARY_NAME)

    logger.debug("Loading OpenCV native library: {}", opencvLib)
    System.load(opencvLib)

    val injector = baseInjector.childInjector {
        install(VisionModule())

        if (config.server.enabled)
            install(ServerModule())

        if (config.tables.enabled)
            install(TablesModule())

        if (config.display)
            install(DisplayModule())
    }

    val eventBus: EventBus = injector.getInstance()
//    val executor: ThreadPoolExecutor = injector.getInstance()

    eventBus.post(StartEvent())

    onShutdown {
        logger.info("Stopping...")

        eventBus.post(StopEvent())
//        executor.shutdown()
//        try {
//            executor.awaitTermination(5, TimeUnit.SECONDS)
//        } catch (ignored: InterruptedException) {
//        }
    }
}

