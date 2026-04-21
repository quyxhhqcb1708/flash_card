package com.example.xq.flashcard.ui.progress

import android.content.Context
import com.example.xq.flashcard.R
import kotlin.math.roundToInt

object ProgressUiFormatter {

    fun formatPercent(rate: Float): String {
        return "${(rate.coerceIn(0f, 1f) * 100f).roundToInt()}%"
    }

    fun buildFocusMessage(
        context: Context,
        dashboard: LearningProgressDashboard
    ): String {
        return when {
            dashboard.totalSavedCount == 0 -> {
                context.getString(R.string.progress_focus_empty)
            }

            dashboard.dueTodayCount > 0 -> {
                context.getString(R.string.progress_focus_due, dashboard.dueTodayCount)
            }

            dashboard.distribution.learningCount > 0 -> {
                context.getString(
                    R.string.progress_focus_learning,
                    dashboard.distribution.learningCount
                )
            }

            dashboard.masteredCount >= dashboard.totalSavedCount && dashboard.totalSavedCount > 0 -> {
                context.getString(R.string.progress_focus_mastered)
            }

            else -> {
                context.getString(
                    R.string.progress_focus_steady,
                    dashboard.distribution.upcomingCount
                )
            }
        }
    }

    fun buildChartDescription(
        context: Context,
        dashboard: LearningProgressDashboard
    ): String {
        return context.getString(
            R.string.progress_chart_description,
            dashboard.distribution.dueCount,
            dashboard.distribution.learningCount,
            dashboard.distribution.upcomingCount,
            dashboard.distribution.stableCount
        )
    }
}
