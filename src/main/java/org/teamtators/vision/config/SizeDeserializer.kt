package org.teamtators.vision.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.opencv.core.Size

import java.io.IOException

class SizeDeserializer : JsonDeserializer<Size>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Size {
        val array = p.readValueAs(DoubleArray::class.java)
        return Size(array)
    }
}
