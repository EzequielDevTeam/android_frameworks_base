package com.android.systemui.mrdynamicbar

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.android.systemui.CoreStartable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import javax.inject.Inject

/**
 * Ponto de entrada da Dynamic Bar (fase 1: mídia). Ligado via Dagger
 * (SystemUICoreStartableModule). Monta o grafo estático lido pelo binder
 * da status bar. Nada aqui desenha: o binder decide mostrar a pílula.
 */
@SysUISingleton
class MrDynamicBarManager
@Inject
constructor(
    @Application private val context: Context,
) : CoreStartable {

    override fun start() {
        try {
            val handler = Handler(Looper.getMainLooper())
            val resolver = context.contentResolver
            val settings =
                MrDynamicBarSettings(resolver, handler) {
                    refresh()
                }
            val monitor = MrDynamicBarMediaMonitor(context)
            MrDynamicBarGraph.settings = settings
            MrDynamicBarGraph.monitor = monitor
            settings.start()
            refresh()
        } catch (_: Exception) {
            // Ilha desativada em caso de falha: a status bar segue normal.
            MrDynamicBarGraph.settings = null
            MrDynamicBarGraph.monitor = null
        }
    }

    private fun refresh() {
        val settings = MrDynamicBarGraph.settings
        val monitor = MrDynamicBarGraph.monitor
        if (settings == null || monitor == null) return
        try {
            if (settings.isMediaEnabled()) {
                monitor.start()
            } else {
                monitor.stop()
            }
        } catch (_: Exception) {
        }
    }
}

/** Holder estático lido pelo binder (UI sem Dagger = gancho mínimo). */
object MrDynamicBarGraph {
    @Volatile var settings: MrDynamicBarSettings? = null
    @Volatile var monitor: MrDynamicBarMediaMonitor? = null
}
