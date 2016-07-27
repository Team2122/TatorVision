package org.teamtators.vision.json

import com.fasterxml.jackson.databind.module.SimpleModule
import org.opencv.core.Scalar
import org.opencv.core.Size

class OpenCVModule : SimpleModule {
    constructor() : super() {
        addDeserializer(Size::class.java, SizeDeserializer())
        addDeserializer(Scalar::class.java, ScalarDeserializer())
    }
}