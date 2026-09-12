/*
 * SPDX-FileCopyrightText: EzequielDevTeam (MrEzequielOS)
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.mrdynamicbar

import android.content.Context
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper

/** Estado mínimo da pílula de mídia (fase 1). */
data class MrMediaState(
    val playing: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val packageName: String = "",
    val appIcon: Drawable? = null,
)

/**
 * Observa a sessão de mídia ativa via APIs públicas do framework
 * (MediaSessionManager/MediaController). Sem dependência de managers
 * internos do SystemUI (que mudam entre versões). Entrega via callback
 * simples na thread principal — sem coroutines na camada de UI.
 */
class MrDynamicBarMediaMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager =
        appContext.getSystemService(MediaSessionManager::class.java)

    /** Chamado na main thread a cada mudança. Nunca lança. */
    var listener: ((MrMediaState) -> Unit)? = null

    private var last = MrMediaState()

    /** Último estado conhecido (para render inicial). */
    fun current(): MrMediaState = last

    private var controller: MediaController? = null
    private var listening = false

    private val controllerCallback =
        object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                push()
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                push()
            }

            override fun onSessionDestroyed() {
                controller = null
                emit(MrMediaState())
            }
        }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bind(controllers)
        }

    fun start() {
        if (listening) return
        listening = true
        try {
            sessionManager.addOnActiveSessionsChangedListener(
                sessionsListener, null, handler
            )
        } catch (_: SecurityException) {
            listening = false
            return
        }
        try {
            bind(sessionManager.getActiveSessions(null))
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!listening) return
        listening = false
        try {
            sessionManager.removeOnActiveSessionsChangedListener(sessionsListener)
        } catch (_: Exception) {
        }
        try {
            controller?.unregisterCallback(controllerCallback)
        } catch (_: Exception) {
        }
        controller = null
        emit(MrMediaState())
    }

    /** Alterna play/pause da sessão atual. Retorna false se não há sessão. */
    fun togglePlayPause(): Boolean {
        val c = controller ?: return false
        return try {
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            if (playing) c.transportControls.pause() else c.transportControls.play()
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun bind(controllers: List<MediaController>?) {
        try {
            controller?.unregisterCallback(controllerCallback)
        } catch (_: Exception) {
        }
        controller = controllers?.firstOrNull()
        try {
            controller?.registerCallback(controllerCallback, handler)
        } catch (_: Exception) {
            controller = null
        }
        push()
    }

    private fun emit(state: MrMediaState) {
        last = state
        try {
            listener?.invoke(state)
        } catch (_: Exception) {
        }
    }

    private fun push() {
        val c = controller
        if (c == null) {
            emit(MrMediaState())
            return
        }
        try {
            val md = c.metadata
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            val title =
                md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
            val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            val pkg =
                try {
                    c.packageName ?: ""
                } catch (_: Exception) {
                    ""
                }
            val icon =
                try {
                    if (pkg.isNotEmpty()) appContext.packageManager.getApplicationIcon(pkg)
                    else null
                } catch (_: Exception) {
                    null
                }
            if (!playing && title.isEmpty()) {
                emit(MrMediaState())
            } else {
                emit(MrMediaState(playing, title, artist, pkg, icon))
            }
        } catch (_: Exception) {
            emit(MrMediaState())
        }
    }
}
