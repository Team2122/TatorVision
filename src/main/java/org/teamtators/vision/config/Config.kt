package org.teamtators.vision.config

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

class VisionConfig {
    var cameraIndex = 0
    var maxFPS = 30

    var fieldOfView = Size(62.2, 48.8)
    var offset = Size(0.0, 0.0)

    var lowerThreshold = Scalar(60.0, 150.0, 20.0)
    var upperThreshold = Scalar(100.0, 255.0, 255.0)
    var minArea = 1000
    var maxArea = 100000
    var arcLengthPercentage = 0.01f

    var debug = false

    var inputRes = Size()
    var streamRes = Size()
}

class Config {
    var tables : TablesConfig = TablesConfig()
    var server : ServerConfig = ServerConfig()
    var vision : VisionConfig = VisionConfig()

    var display = false
}
