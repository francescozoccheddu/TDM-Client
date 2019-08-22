package com.francescozoccheddu.tdmclient.ui.utils.transitions


import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.transition.Transition
import androidx.transition.TransitionValues
import kotlin.math.roundToInt


class PaddingTransition : Transition {

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
        transitionValues.values[PROPERTY_BOTTOM] = transitionValues.view.paddingBottom
        transitionValues.values[PROPERTY_TOP] = transitionValues.view.paddingTop
        transitionValues.values[PROPERTY_LEFT] = transitionValues.view.paddingLeft
        transitionValues.values[PROPERTY_RIGHT] = transitionValues.view.paddingRight
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val startBottom = startValues.values[PROPERTY_BOTTOM] as Int
        val endBottom = endValues.values[PROPERTY_BOTTOM] as Int
        val startTop = startValues.values[PROPERTY_TOP] as Int
        val endTop = endValues.values[PROPERTY_TOP] as Int
        val startLeft = startValues.values[PROPERTY_LEFT] as Int
        val endLeft = endValues.values[PROPERTY_LEFT] as Int
        val startRight = startValues.values[PROPERTY_RIGHT] as Int
        val endRight = endValues.values[PROPERTY_RIGHT] as Int
        val view = endValues.view
        view.setPadding(startLeft, startTop, startRight, startBottom)

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { animation ->
            val a = animation.animatedValue as Float
            view.setPadding(
                lerp(startLeft, endLeft, a),
                lerp(startTop, endTop, a),
                lerp(startRight, endRight, a),
                lerp(startBottom, endBottom, a)
            )
        }

        return animator
    }

    private companion object {

        private fun lerp(from: Int, to: Int, progress: Float) = (from * (1f - progress) + to * progress).roundToInt()

        private val PROPERTY_BOTTOM = "${this::class.java.canonicalName}:${View::getPaddingBottom.name}"
        private val PROPERTY_TOP = "${this::class.java.canonicalName}:${View::getPaddingTop.name}"
        private val PROPERTY_LEFT = "${this::class.java.canonicalName}:${View::getPaddingLeft.name}"
        private val PROPERTY_RIGHT = "${this::class.java.canonicalName}:${View::getPaddingRight.name}"
        private val PROPERTIES = arrayOf(PROPERTY_BOTTOM, PROPERTY_TOP, PROPERTY_LEFT, PROPERTY_RIGHT)
    }
}