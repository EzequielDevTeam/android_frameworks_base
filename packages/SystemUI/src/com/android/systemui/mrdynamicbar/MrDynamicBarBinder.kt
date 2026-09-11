package com.android.systemui.mrdynamicbar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.android.systemui.statusbar.phone.PhoneStatusBarView
import com.android.systemui.statusbar.phone.PhoneStatusBarView

/**
 * Instala a pílula de mídia na status bar (fase 1).
 *
 * Gancho mínimo e defensivo: adiciona UMA view flutuante centralizada
 * (WRAP_CONTENT, só consome toque dentro da pílula) e qualquer exceção
 * aqui apenas desiste da ilha — a status bar segue normal.
 */
object MrDynamicBarBinder {
    private const val TAG = "MrDynamicBar"

    fun bind(root: PhoneStatusBarView) {
        try {
            bindInternal(root)
        } catch (t: Throwable) {
            Log.w(TAG, "ilha desativada (falha ao instalar)", t)
        }
    }

    private fun bindInternal(root: PhoneStatusBarView) {
        val context = root.context
        val monitor = MrDynamicBarGraph.monitor ?: return
        val settings = MrDynamicBarGraph.settings ?: return

        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBackground()
            val pad = dp(context, 10)
            setPadding(pad, dp(context, 5), pad, dp(context, 5))
            visibility = View.GONE
            isClickable = true
            isFocusable = false
            setOnClickListener {
                try {
                    monitor.togglePlayPause()
                } catch (_: Exception) {
                }
            }
        }
        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(context, 18), dp(context, 18))
        }
        val text = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.marginStart = dp(context, 6)
            layoutParams = lp
        }
        pill.addView(icon)
        pill.addView(text)

        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        root.addView(pill, params)

        val render: (MrMediaState) -> Unit = { state ->
            try {
                val show = settings.isMediaEnabled() &&
                    (state.playing || state.title.isNotEmpty())
                if (!show) {
                    pill.visibility = View.GONE
                } else {
                    val label =
                        if (state.artist.isNotEmpty()) {
                            "${state.title} — ${state.artist}"
                        } else {
                            state.title.ifEmpty { "♪" }
                        }
                    text.text = label
                    if (state.appIcon != null) {
                        icon.setImageDrawable(state.appIcon)
                        icon.visibility = View.VISIBLE
                    } else {
                        icon.visibility = View.GONE
                    }
                    pill.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                pill.visibility = View.GONE
            }
        }

        monitor.listener = render
        try {
            render(monitor.current())
        } catch (_: Exception) {
        }

        root.addOnAttachStateChangeListener(
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) = Unit

                override fun onViewDetachedFromWindow(v: View) {
                    try {
                        if (monitor.listener === render) {
                            monitor.listener = null
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        )
    }

    private fun pillBackground(): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.BLACK)
            cornerRadius = 999f
            alpha = 230
        }

    private fun dp(context: Context, v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}
