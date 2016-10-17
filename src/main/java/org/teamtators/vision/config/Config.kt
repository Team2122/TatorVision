package org.teamtators.vision.config

import org.opencv.core.Scalar
import org.opencv.core.Size
import org.teamtators.vision.vision.Polynomial

class TablesConfig {
    var enabled = false
    var host = "roboRIO-2122-FRC.local"
    var tableName = "TatorVision"
}

class ServerConfig {
    var enabled = false
    var port = 8080
}

enum class VisionDisplay {
    NONE, INPUT, THRESHOLD, CONTOURS
}

class VisionConfig {
    var startVisionScript: String = "true"

    var cameraIndex: Int = 0
    var inputRes: Size = Size()
    var streamRes: Size = Size()
    var maxFPS: Int = 30
    var upsideDown: Boolean = false

    var lowerThreshold: Scalar = Scalar(60.0, 150.0, 20.0)
    var upperThreshold: Scalar = Scalar(100.0, 255.0, 255.0)
    var minArea: Int = 1000
    var maxArea: Int = 100000
    var minSolidity: Double = 0.0
    var maxSolidity: Double = 1.0
    var maxAreaDifference: Int = 50
    var arcLengthPercentage: Double = 0.01

    var fieldOfView: Size = Size(62.2, 48.8)
    var distancePoly: Polynomial = Polynomial()
    var horizontalAngleOffset: Double = 0.0

    var debug: Boolean = false
    var display: VisionDisplay = VisionDisplay.CONTOURS
}

class Config {
    var tables: TablesConfig = TablesConfig()
    var server: ServerConfig = ServerConfig()
    var vision: VisionConfig = VisionConfig()
    var profile = false
    var display = false
}