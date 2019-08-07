package com.francescozoccheddu.tdmclient.utils.commons

import kotlin.reflect.KProperty

class LateInit<Type> {

    private data class Nullable<Type>(var value: Type)

    private lateinit var _value: Nullable<Type>

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Type) {
        this.value = value
    }

    var value: Type
        get() = _value.value
        set(value) {
            _value = Nullable(value)
        }

    val isInitialized get() = this::_value.isInitialized

}

