package org.teamtators.vision.config

import org.opencv.core.Scalar
import org.opencv.core.Size

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
    var cameraIndex: Int = 0
    var maxFPS: Int = 30

    var upsideDown: Boolean = false
    var fieldOfView: Size = Size(62.2, 48.8)
    var verticalCameraAngle: Double = 60.0
    var goalHeight: Double = (8 * 12) - 24.0
    var horizontalAngleOffset: Double = 0.0

    var lowerThreshold: Scalar = Scalar(60.0, 150.0, 20.0)
    var upperThreshold: Scalar = Scalar(100.0, 255.0, 255.0)
    var minArea: Int = 1000
    var maxArea: Int = 100000
    var arcLengthPercentage: Double = 0.01

    var debug: Boolean = false
    var display: VisionDisplay = VisionDisplay.CONTOURS

    var inputRes: Size = Size()
    var streamRes: Size = Size()

    var startVisionScript: String = "true"
}

class ProfilerConfig {
    var logToConsole = true
}

class Config {
    var tables: TablesConfig = TablesConfig()
    var server: ServerConfig = ServerConfig()
    var vision: VisionConfig = VisionConfig()
    var profiler: ProfilerConfig = ProfilerConfig()
    var display = false
}