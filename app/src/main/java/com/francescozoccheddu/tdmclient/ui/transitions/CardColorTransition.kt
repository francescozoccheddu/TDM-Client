package com.francescozoccheddu.tdmclient.ui.transitions

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.transition.Transition
import androidx.transition.TransitionValues


class CardColorTransition : Transition {

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
        if (view is CardView)
            transitionValues.values[PROPERTY] = view.cardBackgroundColor.defaultColor
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val start = startValues.values[PROPERTY] as Int
        val end = endValues.values[PROPERTY] as Int
        val view = endValues.view as CardView
        view.setCardBackgroundColor(start)

        val animator = ValueAnimator.ofArgb(start, end)
        animator.addUpdateListener { animation ->
            view.setCardBackgroundColor(animation.animatedValue as Int)
        }

        return animator
    }

    private companion object {
        private val PROPERTY = "${this::class.java.canonicalName}:${CardView::getCardBackgroundColor.name}"
        private val PROPERTIES = arrayOf(PROPERTY)
    }
}