package org.teamtators.vision.vision

class Polynomial {
    var a : Double = 0.0
    var b : Double = 0.0
    var c : Double = 0.0

    fun calculate(x : Double) = a * x * x + b * x + c
}