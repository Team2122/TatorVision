package org.teamtators.vision.display

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.DisplayImageEvent
import org.teamtators.vision.events.StartEvent
import org.teamtators.vision.loggerFor
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.WindowConstants

class VisionDisplay @Inject constructor(
        _config: Config,
        val eventBus: EventBus
) : JFrame() {
    companion object {
        val logger = loggerFor<VisionDisplay>()
    }

    val config = _config.vision
    val imageDisplay: OpenCVDisplay

    init {
        imageDisplay = OpenCVDisplay()
        this.add(imageDisplay)
        eventBus.register(this)
    }

    @Subscribe
    private fun onStart(ignored: StartEvent) {
        this.start()
    }

    fun start() {
        logger.info("Starting image display")
        val frame = BufferedImage(config.streamRes.width.toInt(),
                config.streamRes.height.toInt(),
                BufferedImage.TYPE_3BYTE_BGR)
        imageDisplay.updateImage(frame)
        this.pack()
        this.isVisible = true
        this.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        eventBus.register(this)
    }

    @Subscribe
    fun displayImage(event: DisplayImageEvent) {
        imageDisplay.updateImage(event.image)
    }
}