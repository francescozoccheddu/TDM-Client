package com.francescozoccheddu.tdmclient.ui.utils.transitions

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.transition.Transition
import androidx.transition.TransitionValues


class CardRadiusTransition : Transition {

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
        val view = transitionValues.view
        if (view is CardView) {
            transitionValues.values[PROPERTY] = view.radius
        }
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
        val view = endValues.view as CardView
        view.radius = start

        val animator = ValueAnimator.ofFloat(start, end)
        animator.addUpdateListener { animation ->
            view.radius = animation.animatedValue as Float
        }

        return animator
    }

    private companion object {
        private val PROPERTY = "${this::class.java.canonicalName}:${CardView::getRadius.name}"
        private val PROPERTIES = arrayOf(PROPERTY)
    }
}