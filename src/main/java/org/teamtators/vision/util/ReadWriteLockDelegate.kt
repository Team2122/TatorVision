package org.teamtators.vision.util

import java.util.concurrent.locks.ReadWriteLock
import kotlin.concurrent.withLock
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ReadWriteLockDelegate<T : Any>(val readWriteLock: ReadWriteLock, initial : T) : ReadWriteProperty<Any?, T> {
    var value: T = initial

    operator override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
            readWriteLock.writeLock().withLock {
                this.value = value
            }


    operator override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            readWriteLock.readLock().withLock {
                this.value
            }

}

fun <T : Any> readWriteLocked(readWriteLock: ReadWriteLock, initial : T): ReadWriteProperty<Any?, T> =
        ReadWriteLockDelegate(readWriteLock, initial)