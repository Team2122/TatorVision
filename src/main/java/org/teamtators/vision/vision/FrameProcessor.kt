package org.teamtators.vision.vision

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.google.inject.Inject
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.slf4j.Logger
import org.teamtators.vision.config.Config
import org.teamtators.vision.config.VisionConfig
import org.teamtators.vision.config.VisionDisplay
import org.teamtators.vision.events.CapturedMatEvent
import org.teamtators.vision.events.ProcessedFrameEvent
import org.teamtators.vision.loggerFactory
import java.util.*

private val overlayColor = Scalar(255.0, 255.0, 255.0)

class FrameProcessor @Inject constructor(
        val _config: Config,
        private val eventBus: EventBus
) {
    private val config: VisionConfig = _config.vision

    private val fpsCounter: FpsCounter = FpsCounter()
    private var fps: Long = 0

    companion object {
        private val logger: Logger by loggerFactory()
        private val fontFace = Core.FONT_HERSHEY_SIMPLEX
    }

    class ProcessResult(val frame: Mat, val target: Point?, val distance: Double?, val offsetAngle: Double?,
                        val newAngle: Double?)

    init {
        logger.debug("Registering FrameProcessor")
        eventBus.register(this)
    }

    private val erodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, Size(2.0, 2.0))
    private val dilateKernel = Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, Size(2.0, 2.0))

    @Subscribe
    fun processCapturedMat(event: CapturedMatEvent) {
        val result: ProcessResult
        try {
            result = process(event.matCapture)
        } catch(e: Exception) {
            logger.error("Exception thrown during processing of frame", e)
            return
        }
        eventBus.post(ProcessedFrameEvent(result))
    }

    private class ContourInfo(
            val contour: MatOfPoint,
            val contour2f: MatOfPoint2f,
            val area: Double,
            val minAreaRect: RotatedRect,
            val center: Point,
            val rectArea: Double,
            val solidity: Double
    )

    private fun getContourInfo(contour: MatOfPoint): ContourInfo {
        val area = Imgproc.contourArea(contour)
        val contour2f = MatOfPoint2f()
        contour.convertTo(contour2f, CvType.CV_32FC2)
        val epsilon = Imgproc.arcLength(contour2f, true) * config.arcLengthPercentage
        Imgproc.approxPolyDP(contour2f, contour2f, epsilon, true)
        val minAreaRect = Imgproc.minAreaRect(contour2f)
        val rectArea = minAreaRect.size.area()
        val center = minAreaRect.center
//        val moments = Imgproc.moments(contour)
//        val center = moments.center
        val solidity = area / rectArea
        return ContourInfo(contour, contour2f, area, minAreaRect, center, rectArea, solidity)
    }

    var displayMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val hsvMat = Mat.zeros(config.inputRes, CvType.CV_8UC3)
    val thresholdMat = Mat.zeros(config.inputRes, CvType.CV_8UC1)

    private val scalar: Scalar
        get() {
            val crosshairColor = overlayColor
            return crosshairColor
        }

    fun process(captureData: MatCaptureData): ProcessResult {
        val inputMat = captureData.frame

        if (config.display == VisionDisplay.INPUT || config.display == VisionDisplay.CONTOURS)
            displayMat = inputMat

        val thresholdStart = System.nanoTime()
        Imgproc.cvtColor(inputMat, hsvMat, Imgproc.COLOR_BGR2HSV)
        Core.inRange(hsvMat, config.lowerThreshold, config.upperThreshold, thresholdMat)

        val erodeStart = System.nanoTime()
//        Imgproc.erode(thresholdMat, thresholdMat, erodeKernel, Point(), 3);
//        Imgproc.dilate(thresholdMat, thresholdMat, dilateKernel, Point(), 2);
        if (config.display == VisionDisplay.THRESHOLD)
            displayMat = thresholdMat.clone()

        val contoursStart = System.nanoTime()
        val hierarchy = Mat()
        val rawContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(thresholdMat, rawContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS)

        val filterContoursStart = System.nanoTime()
        // Get information on contours and sort them
        val contours = rawContours
                .map { getContourInfo(it) }
                .sortedBy { it.center.x }

        // Filter by area range
        val filteredContours = contours
                .filter {
                    it.rectArea >= config.minArea
                            && it.rectArea <= config.maxArea
                            && it.solidity >= config.minSolidity
                            && it.solidity <= config.maxSolidity
                }



        // Find largest contour by area
        val trackingContour = filteredContours.maxBy { it.area }

        val drawStart = System.nanoTime()
        // Draw all contours, with the largest one in a larger thickness
        if (config.display == VisionDisplay.CONTOURS) {
            if (config.debug)
                contours.forEach { contour ->
                    drawContour(Scalar(0.0, 0.0, 255.0), contour, 1)
                }
            filteredContours.forEach { contour ->
                val thickness = if (contour == trackingContour) 3 else 1;
                drawContour(Scalar(0.0, 255.0, 0.0), contour, thickness)
            }
        }

        // Draw crosshair
        drawCrosshair(displayMat)

        val fps = fpsCounter.getFps()
        if (fps != null) {
            this.fps = fps
            if (config.debug)
                logger.trace("Process FPS: {}", fps)
        }

        displayMat.drawText("${this.fps}", Point(5.0, 30.0), fontFace, 1.0,
                overlayColor)

        val calculateStart = System.nanoTime()
        // Calculate and draw largest contour position
        var target: Point? = null
        var distance: Double? = null
        var offsetAngle: Double? = null
        var newAngle: Double? = null
        if (trackingContour != null) {
            val center = trackingContour.center
            val height = inputMat.height()
            val width = inputMat.width()
            target = center

            distance = config.distancePoly.calculate(center.y / height.toDouble())

//            val widthInches = distance * Math.tan(config.fieldOfView.width.toRadians() / 2) * 2
//            val offsetInches = (center.x / width - .5) * widthInches
//            offsetAngle = Math.atan2(offsetInches, distance).toDegrees() + config.horizontalAngleOffset
            offsetAngle = (center.x / width - 0.5) * config.fieldOfView.width
            newAngle = captureData.turretAngle + offsetAngle
        }
        val calculateEnd = System.nanoTime()

        if (_config.profile) {
            val scale = 1
            val thresholdTime = (erodeStart - thresholdStart) / scale
            val erodeTime = (contoursStart - erodeStart) / scale
            val contoursTime = (filterContoursStart - contoursStart) / scale
            val filterContoursTime = (drawStart - filterContoursStart) / scale
            val drawTime = (calculateStart - drawStart) / scale
            val calculateTime = (calculateEnd - calculateStart) / scale
            val totalTime = (calculateEnd - erodeStart) / scale
            logger.trace("threshold: {}, erode: {}, contours: {}, filter: {}, draw: {}, calculate: {}, total: {}",
                    thresholdTime, erodeTime, contoursTime, filterContoursTime, drawTime, calculateTime, totalTime)
        }


        return ProcessResult(displayMat, target, distance, offsetAngle, newAngle)
    }

    private fun drawContour(color: Scalar, contour: ContourInfo, thickness: Int) {
        if (config.debug)
            displayMat.drawContour(contour.contour, color, thickness)
        displayMat.drawRotatedRect(contour.minAreaRect, color = color, thickness = thickness)
        displayMat.drawCircle(contour.center, 2, color)

        val fontScale = 0.3

        val str1 = String.format("area: %.0f", contour.rectArea);
        val str1Size = Imgproc.getTextSize(str1, fontFace, fontScale, 1, null)
        displayMat.drawText(str1, contour.center + Point(-str1Size.width / 2, -str1Size.height / 2),
                fontFace = fontFace, fontScale = fontScale, color = color)

        val str2 = String.format("solidity: %.4f", contour.solidity);
        val str2Size = Imgproc.getTextSize(str2, fontFace, fontScale, 1, null)
        displayMat.drawText(str2, contour.center + Point(-str1Size.width / 2, str2Size.height / 2),
                fontFace = fontFace, fontScale = fontScale, color = color)
    }

    private fun drawCrosshair(mat: Mat) {
        val center = mat.size().toPoint() / 2.0
//        val width = mat.size().width
        val height = mat.size().height
        mat.drawCenterRect(center, 25, 25, overlayColor)   //to be replaced with corrected targeting values
//        mat.drawLine(Point(0.0, center.y), Point(width, center.y), overlayColor)
        mat.drawLine(Point(center.x, 0.0), Point(center.x, height), overlayColor)
    }

    private fun sortLeftToRight(contours: List<ContourInfo>) {
        Collections.sort(contours, {info1: ContourInfo, info2: ContourInfo -> info1.center.x.compareTo(info2.center.x)});
    }

    private fun sortRightToLeft(contours: List<ContourInfo>) {
        Collections.sort(contours, {info1: ContourInfo, info2: ContourInfo -> - info1.center.x.compareTo(info2.center.x)});
    }

    private fun sortTopToBottom(contours: List<ContourInfo>) {
        Collections.sort(contours, {info1: ContourInfo, info2: ContourInfo -> - info1.center.y.compareTo(info2.center.x)});
    }

    private fun sortBottomToTop(contours: List<ContourInfo>) {
        Collections.sort(contours, {info1: ContourInfo, info2: ContourInfo -> info1.center.y.compareTo(info2.center.x)});
    }
}