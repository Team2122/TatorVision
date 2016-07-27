package org.teamtators.vision.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.opencv.core.Scalar

import java.io.IOException

class ScalarDeserializer : JsonDeserializer<Scalar>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Scalar? {
        val array = p.readValueAs(DoubleArray::class.java)
        return Scalar(array)
    }
}
