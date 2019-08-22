package com.francescozoccheddu.tdmclient.ui.utils.transitions

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.transition.Transition
import androidx.transition.TransitionValues


class AlphaTransition : Transition {

    constructor()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getTransitionProperties(): Array<String>? {
        return PROPERTIES
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        transitionValues.values[PROPERTY] = transitionValues.view.alpha
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val start = startValues.values[PROPERTY] as Float
        val end = endValues.values[PROPERTY] as Float
        val view = endValues.view
        view.alpha = start

        val animator = ValueAnimator.ofFloat(start, end)
        animator.addUpdateListener { animation ->
            view.alpha = animation.animatedValue as Float
        }

        return animator
    }

    private companion object {
        private val PROPERTY = "${this::class.java.canonicalName}:${View::getAlpha.name}"
        private val PROPERTIES = arrayOf(PROPERTY)
    }
}