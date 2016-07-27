package org.teamtators.vision

import com.google.inject.Injector
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder

inline fun <reified T : Any> AnnotatedBindingBuilder<in T>.to(): ScopedBindingBuilder = this.to(T::class.java)

inline fun <reified T : Any> Injector.getInstance(): T = this.getInstance(T::class.java)