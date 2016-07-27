package org.teamtators.vision

import com.google.common.eventbus.EventBus
import com.google.inject.Guice
import com.google.inject.Module
import org.opencv.core.Core
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import org.teamtators.vision.config.ConfigManager
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.modules.*
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private val TATORVISION_HEADER = "\n" +
        "┌─────────────────────────────┐\n" +
        "│╺┳╸┏━┓╺┳╸┏━┓┏━┓╻ ╻╻┏━┓╻┏━┓┏┓╻│\n" +
        "│ ┃ ┣━┫ ┃ ┃ ┃┣┳┛┃┏┛┃┗━┓┃┃ ┃┃┗┫│\n" +
        "│ ╹ ╹ ╹ ╹ ┗━┛╹┗╸┗┛ ╹┗━┛╹┗━┛╹ ╹│\n" +
        "└─────────────────────────────┘\n"

fun main(args: Array<String>) {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    val logger = LoggerFactory.getLogger("main")

    logger.info(TATORVISION_HEADER)

    val config = ConfigManager.loadVisionConfig()

    logger.debug("Using OpenCV Version: {}", Core.VERSION)

    var opencvJavaDir: String? = System.getProperty("tatorvision.opencvjavadir")
    if (opencvJavaDir == null)
        opencvJavaDir = "/usr/local/share/OpenCV/java"
    val opencvLib = String.format("%s/lib%s.so", opencvJavaDir, Core.NATIVE_LIBRARY_NAME)

    logger.debug("Loading OpenCV native library: {}", opencvLib)
    System.load(opencvLib)

    val modules = ArrayList<Module>()
    modules.add(ConfigModule(config))
    modules.add(VisionModule())

    if (config.server.enabled)
        modules.add(ServerModule())

    if (config.tables.enabled)
        modules.add(TablesModule())

    if (config.display)
        modules.add(DisplayModule())

    val injector = Guice.createInjector(modules)
    val eventBus : EventBus = injector.getInstance()
    val executor : ThreadPoolExecutor = injector.getInstance()

    eventBus.post(StartEvent())

    Runtime.getRuntime()
            .addShutdownHook(Thread(Runnable {
                logger.info("Stopping...")

                eventBus.post(StopEvent())
                executor.shutdownNow()
                try {
                    executor.awaitTermination(5, TimeUnit.SECONDS)
                } catch (ignored: InterruptedException) {
                }
            }, "Stop"))
}

