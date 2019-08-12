package com.francescozoccheddu.tdmclient.utils.commons

import kotlin.reflect.KProperty

abstract class WellSet<FunctionType> {

    private val handlers = mutableSetOf<FunctionType>()

    protected open fun add(handler: FunctionType) {
        handlers += handler
    }

    protected open fun remove(handler: FunctionType) {
        handlers -= handler
    }

    operator fun plusAssign(handler: FunctionType) {
        add(handler)
    }

    operator fun minusAssign(handler: FunctionType) {
        remove(handler)
    }

    operator fun contains(handler: FunctionType) = handler in handlers

    protected operator fun iterator() = handlers.iterator()

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

}

open class ProcEvent : WellSet<() -> Unit>() {

    operator fun invoke() {
        for (handler in this)
            handler()
    }

    operator fun plusAssign(handler: ProcEvent) {
        add(handler::invoke)
    }

    operator fun minusAssign(handler: ProcEvent) {
        remove(handler::invoke)
    }

}

open class FuncEvent<ArgumentType> : WellSet<(ArgumentType) -> Unit>() {

    operator fun invoke(arg: ArgumentType) {
        for (handler in this)
            handler(arg)
    }

    operator fun plusAssign(handler: FuncEvent<ArgumentType>) {
        add(handler::invoke)
    }

    operator fun minusAssign(handler: FuncEvent<ArgumentType>) {
        remove(handler::invoke)
    }

}

open class FuncEvent2<ArgumentType1, ArgumentType2> : WellSet<(ArgumentType1, ArgumentType2) -> Unit>() {

    operator fun invoke(arg1: ArgumentType1, arg2: ArgumentType2) {
        for (handler in this)
            handler(arg1, arg2)
    }

    operator fun plusAssign(handler: FuncEvent2<ArgumentType1, ArgumentType2>) {
        add(handler::invoke)
    }

    operator fun minusAssign(handler: FuncEvent2<ArgumentType1, ArgumentType2>) {
        remove(handler::invoke)
    }

}