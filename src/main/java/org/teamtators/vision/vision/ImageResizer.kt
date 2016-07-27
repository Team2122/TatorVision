package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.imgproc.Imgproc
import org.teamtators.vision.config.Config
import org.teamtators.vision.display.matToBufferedImage
import org.teamtators.vision.events.DisplayImageEvent
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.loggerFor

class ImageResizer @Inject constructor(
        val config: Config,
        val eventBus: EventBus
) {
    companion object {
        val logger = loggerFor<ImageResizer>()
    }

    init {
        logger.debug("Registering ImageResizer")
        eventBus.register(this)
    }

    @Subscribe
    fun resizeImage(event: ProcessedFrameEvent) {
        val frame = event.result.frame
        val streamRes = config.vision.streamRes

        if (streamRes.width > 0 && streamRes.height > 0)
            Imgproc.resize(frame, frame, streamRes)

        eventBus.post(DisplayImageEvent(matToBufferedImage(frame)))
    }
}
