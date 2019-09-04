package com.francescozoccheddu.tdmclient.ui.components.us

import android.animation.LayoutTransition
import android.content.Context
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.francescozoccheddu.tdmclient.R
import com.francescozoccheddu.tdmclient.data.UserController
import com.francescozoccheddu.tdmclient.data.UserStats
import com.francescozoccheddu.tdmclient.ui.MainService
import com.francescozoccheddu.tdmclient.utils.android.getStyledString
import com.francescozoccheddu.tdmclient.utils.android.visible
import com.robinhood.ticker.TickerView
import com.squareup.picasso.Picasso
import me.everything.android.ui.overscroll.VerticalOverScrollBounceEffectDecorator
import me.everything.android.ui.overscroll.adapters.ScrollViewOverScrollDecorAdapter

class UserStatsProfilePage(parent: View) {

    private val nextLevelRoot: View

    init {
        parent.findViewById<ViewGroup>(R.id.uss_profile_root).apply {
            layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
            layoutTransition = layoutTransition
        }
        parent.findViewById<View>(R.id.uss_profile_score_root)
            .setOnClickListener { toggleHelp(scoreHelp) }
        parent.findViewById<View>(R.id.uss_profile_level_root)
            .setOnClickListener { toggleHelp(levelHelp) }
        parent.findViewById<View>(R.id.uss_profile_multiplier_root)
            .setOnClickListener { toggleHelp(multiplierHelp) }
        nextLevelRoot = parent.findViewById<View>(R.id.uss_profile_next_level_root).apply {
            setOnClickListener { toggleHelp(nextLevelHelp) }
        }
        VerticalOverScrollBounceEffectDecorator(
            ScrollViewOverScrollDecorAdapter(parent.findViewById(R.id.uss_profile_scroll)),
            VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_FWD * 2f,
            VerticalOverScrollBounceEffectDecorator.DEFAULT_TOUCH_DRAG_MOVE_RATIO_BCK * 2f,
            VerticalOverScrollBounceEffectDecorator.DEFAULT_DECELERATE_FACTOR
        )
    }

    private val scoreHelp = parent.findViewById<TextView>(R.id.uss_profile_score_help)
    private val levelHelp = parent.findViewById<TextView>(R.id.uss_profile_level_help)
    private val multiplierHelp = parent.findViewById<TextView>(R.id.uss_profile_multiplier_help)
    private val nextLevelHelp = parent.findViewById<TextView>(R.id.uss_profile_next_level_help)


    private fun toggleHelp(view: View) {
        showHelp(if (view.visible) null else view)
    }

    private fun showHelp(view: View?) {
        scoreHelp.visible = scoreHelp == view
        levelHelp.visible = levelHelp == view
        multiplierHelp.visible = multiplierHelp == view
        nextLevelHelp.visible = nextLevelHelp == view
    }

    fun abortAll() {
        showHelp(null)
        if (editNameState == NameEditState.EDITING)
            editNameState = NameEditState.IDLE
    }

    private val scoreText = parent.findViewById<TickerView>(R.id.uss_profile_score_tv)
    private val levelText = parent.findViewById<TickerView>(R.id.uss_profile_level_tv)
    private val multiplierText = parent.findViewById<TickerView>(R.id.uss_profile_multiplier_tv)
    private val nextLevelText = parent.findViewById<TickerView>(R.id.uss_profile_next_level_tv)
    private val nameText = parent.findViewById<TextView>(R.id.uss_profile_name)
    private val avatarImage = parent.findViewById<ImageView>(R.id.uss_profile_avatar)
    private val titleText = parent.findViewById<TextView>(R.id.uss_profile_title)
    private val nameView = parent.findViewById<View>(R.id.uss_profile_name_view)
    private val infoGroup = parent.findViewById<ViewGroup>(R.id.uss_profile_info)
    private val nameEditText = parent.findViewById<EditText>(R.id.uss_profile_name_edit).apply {
        setOnFocusChangeListener { view, focused ->
            if (!focused) {
                if (editNameState == NameEditState.EDITING)
                    editNameState = NameEditState.IDLE
                val service =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service.hideSoftInputFromWindow(windowToken, 0)
            }
        }
    }
    private val nameEditButton = parent.findViewById<ImageButton>(R.id.uss_profile_edit).apply {
        setOnClickListener {
            if (editNameState == NameEditState.IDLE) {
                nameEditText.setText(nameText.text)
                editNameState = NameEditState.EDITING
            }
            else if (editNameState == NameEditState.EDITING) {
                val service = MainService.instance
                if (service != null) {
                    editNameState = NameEditState.SAVING
                    service.userController.setName(nameEditText.text.toString()) {
                        when (it) {
                            UserController.EditNameResult.SUCCESS -> editNameState =
                                NameEditState.IDLE
                            UserController.EditNameResult.PROFANITY -> {
                                editNameState = NameEditState.EDITING
                                nameEditText.error =
                                    resources.getString(R.string.uss_profile_name_profanity)
                            }
                            UserController.EditNameResult.REQUEST_FAILURE -> {
                                editNameState = NameEditState.EDITING
                                Toast.makeText(
                                    context,
                                    R.string.uss_profile_name_failure,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private enum class NameEditState {
        IDLE, EDITING, SAVING
    }

    private var editNameState = NameEditState.IDLE
        set(value) {
            if (value != field) {
                field = value
                TransitionManager.beginDelayedTransition(infoGroup)
                nameView.visible = value == NameEditState.IDLE
                nameEditText.visible = value != NameEditState.IDLE
                nameEditText.isEnabled = value == NameEditState.EDITING
                nameEditButton.isEnabled = value != NameEditState.SAVING
            }
        }

    var stats = UserStats(0, 0, 1f, null, 0, "", "", "")
        set(value) {
            if (value != field) {
                field = value
                nameText.text = value.name
                titleText.text = value.title
                Picasso.get().load(value.avatarUrl).into(avatarImage)
                scoreText.text = value.score.toString()
                levelText.text = (value.level + 1).toString()
                multiplierText.text = value.multiplier.toString()
                val nextLevelScore = value.nextLevelScore
                nextLevelRoot.visible = nextLevelScore != null
                if (nextLevelScore != null) {
                    nextLevelText.text = nextLevelScore.toString()
                    nextLevelHelp.text = nextLevelHelp.resources.getStyledString(
                        R.string.uss_next_level_help,
                        nextLevelScore - value.score,
                        value.level + 2
                    )
                }
                else
                    nextLevelHelp.visible = false
            }
        }

}