package org.teamtators.vision.config

import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size

class TablesConfig {
    var enabled = false
    var host = "roboRIO-2122-FRC.local"
    var rootName = "TatorVision"
}

class ServerConfig {
    var enabled = false
    var port = 8080
}

enum class VisionDisplay {
    NONE, INPUT, THRESHOLD, CONTOURS
}

class VisionConfig {
    var cameraIndex : Int = 0
    var maxFPS : Int = 30

    var fieldOfView : Size = Size(62.2, 48.8)
    var offset : Point = Point(0.0, 0.0)

    var lowerThreshold : Scalar = Scalar(60.0, 150.0, 20.0)
    var upperThreshold : Scalar = Scalar(100.0, 255.0, 255.0)
    var minArea : Int = 1000
    var maxArea : Int = 100000
    var arcLengthPercentage : Double = 0.01

    var debug : Boolean = false
    var display : VisionDisplay = VisionDisplay.CONTOURS

    var inputRes : Size = Size()
    var streamRes : Size = Size()
}

class Config {
    var tables: TablesConfig = TablesConfig()
    var server: ServerConfig = ServerConfig()
    var vision: VisionConfig = VisionConfig()

    var display = false
}