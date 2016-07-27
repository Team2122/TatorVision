package org.teamtators.vision

import com.google.inject.AbstractModule
import com.google.inject.binder.AnnotatedBindingBuilder

abstract class AbstractKotlinModule : AbstractModule() {
    inline fun <reified T : Any> bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)
}