package org.teamtators.vision.video

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.slf4j.LoggerFactory
import org.teamtators.vision.config.Config
import org.teamtators.vision.events.DisplayImageEvent
import org.teamtators.vision.vision.OpenCVCapturer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import javax.imageio.ImageIO

class VideoLogger @Inject constructor(
        _config: Config,
        val eventBus: EventBus
) {
    companion object {
        val logger = LoggerFactory.getLogger(OpenCVCapturer::class.java)
    }

    //private val visionConfig = _config.vision
    private val videoConfig = _config.video
    private val sep = File.separator

    @Subscribe
    fun logImage(event: DisplayImageEvent) {
        var dateString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(Date())
        ImageIO.write(event.image, "jpeg", File(videoConfig.outputPath + sep + dateString + ".jpeg"))
    }
}