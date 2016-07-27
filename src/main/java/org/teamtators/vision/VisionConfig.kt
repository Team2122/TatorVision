package org.teamtators.vision

import org.opencv.core.Scalar
import org.opencv.core.Size

class VisionConfig {
    var networkTablesHost = "roboRIO-2122-FRC.local"
    var networkTableName = "TatorVision"

    var port = 8080
    var frameDelay = 30

    var cameraIndex = 0
    var fieldOfView = Size(62.2, 48.8)
    var correction = Size(0.0, 0.0)

    var lowerThreshold = Scalar(60.0, 150.0, 20.0)
    var upperThreshold = Scalar(100.0, 255.0, 255.0)
    var minArea = 1000
    var maxArea = 100000
    var arcLengthPercentage = 0.01f

    var flipX = false

    var display = false
    var server = true
    var tables = true;
    var debug = false

    var inputRes = Size()
    var streamRes = Size()
}
