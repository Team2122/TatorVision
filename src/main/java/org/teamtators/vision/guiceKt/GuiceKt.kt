package org.teamtators.vision.guiceKt

import com.google.inject.*
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaType

abstract class AbstractKotlinModule : com.google.inject.AbstractModule() {
    inline fun <reified T : Any> bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)
}

private class GuiceKtModule(val bindInit: Binder.() -> Unit) : AbstractModule() {
    override fun configure() {
        binder().bindInit()
    }
}

private class GuiceInjectedDelegate<T>(val onInjected : (() -> Unit)? = null) : ReadWriteProperty<Any?, T> {
    var value: T? = null

    operator override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        onInjected?.invoke()
    }

    operator override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val v = value;
        return if (v != null) v
        else {
            val propName = property.name
            val propClassName = property.getter.returnType.javaType.typeName
            val className = thisRef?.javaClass?.name
            throw IllegalStateException(
                    "Property $className$$propName : $propClassName not injected before being accessed." +
                            " Are you missing a call to Injector\$injectMembers?")
        }
    }
}

inline fun <reified T : Any> Binder.bind(): AnnotatedBindingBuilder<T> = bind(T::class.java)

inline fun <reified T : Any> LinkedBindingBuilder<in T>.to(): ScopedBindingBuilder = this.to(T::class.java)

inline fun <reified T : Annotation> AnnotatedBindingBuilder<in T>.to(): LinkedBindingBuilder<in T>
        = this.annotatedWith(T::class.java)

fun module(bindInit: Binder.() -> Unit): Module =
        GuiceKtModule(bindInit)

fun injector(bindInit: Binder.() -> Unit): Injector =
        Guice.createInjector(module(bindInit))

inline fun <reified T : Any> Injector.getInstance(): T = this.getInstance(T::class.java)

fun Injector.childInjector(bindInit: Binder.() -> Unit): Injector
        = this.createChildInjector(module(bindInit))

fun <T> injected(): ReadWriteProperty<Any?, T> {
    return GuiceInjectedDelegate<T>(null)
}

fun <T> injected(onInjected: (() -> Unit)): ReadWriteProperty<Any?, T> {
    return GuiceInjectedDelegate<T>(onInjected)
}