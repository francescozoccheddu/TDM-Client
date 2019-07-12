package com.francescozoccheddu.tdmclient.utils

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

}

open class ProcEvent : WellSet<() -> Unit>() {

    operator fun invoke() {
        for (handler in this)
            handler()
    }

}

open class FuncEvent<ArgumentType> : WellSet<(ArgumentType) -> Unit>() {

    operator fun invoke(arg: ArgumentType) {
        for (handler in this)
            handler(arg)
    }

}