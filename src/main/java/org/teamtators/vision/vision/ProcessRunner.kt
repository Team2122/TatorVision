package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.events.StopEvent
import org.teamtators.vision.util.readWriteLocked
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

private val MAT_BUFFER_SIZE = 2
private val MAT_TYPE = CvType.CV_8UC3

class ProcessRunner @Inject constructor(
        _config: Config,
        val eventBus: EventBus,
        val executor: ExecutorService
) {
    companion object {
        val logger = LoggerFactory.getLogger(ProcessRunner::class.java)
    }

    private val rwLock: ReadWriteLock = ReentrantReadWriteLock()
    private val readLock = rwLock.readLock()
    private val writeLock = rwLock.writeLock()

    private val config = _config.vision
    private val frameBuffer: Array<Mat> = makeFrameBuffer()
    var currentFrame: Int by readWriteLocked(rwLock, 0)

    val nextWritableFrame: Int
        get() = if (currentFrame + 1 >= frameBuffer.size) 0 else currentFrame + 1

    fun getFrame(index : Int) = frameBuffer[index]

    private fun makeFrameBuffer() = Array(MAT_BUFFER_SIZE, { Mat.zeros(config.inputRes, CvType.CV_8UC3) })

    val running = AtomicBoolean(false)

    init {
        eventBus.register(this)
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.submit { run() }
    }

    @Suppress("UNUSED_PARAMETER")
    @Subscribe
    private fun onStop(ignored: StopEvent) {
        this.stop()
    }

    fun stop() = running.set(false)

    inline fun writeToFrame(f: (Mat) -> Unit) {
        val nextFrame = nextWritableFrame
        f(getFrame(nextFrame))
        currentFrame = nextFrame
    }

    private fun run() {
        logger.debug("Starting process runner")
        while (running.get()) {
            readLock.withLock {
                val frame = frameBuffer[currentFrame]
                eventBus.post(CapturedMatEvent(frame))
            }
        }
    }
}