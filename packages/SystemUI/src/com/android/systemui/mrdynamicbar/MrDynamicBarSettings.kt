/*
 * SPDX-FileCopyrightText: EzequielDevTeam (MrEzequielOS)
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.mrdynamicbar

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import org.json.JSONArray

/**
 * Leitura das chaves da Dynamic Bar. Usa apenas APIs estáveis
 * (Settings.Secure + ContentObserver), sem wrappers internos versionados.
 */
class MrDynamicBarSettings(
    private val resolver: ContentResolver,
    handler: Handler,
    private val onChanged: () -> Unit = {},
) {
    private val observer =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                onChanged()
            }
        }

    private var watching = false

    fun start() {
        if (watching) return
        watching = true
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(MrDynamicBarKeys.ENABLED), false, observer
        )
        resolver.registerContentObserver(
            Settings.Secure.getUriFor(MrDynamicBarKeys.EVENTS), false, observer
        )
        onChanged()
    }

    fun stop() {
        if (!watching) return
        watching = false
        resolver.unregisterContentObserver(observer)
    }

    fun isEnabled(): Boolean =
        Settings.Secure.getIntForUser(
            resolver, MrDynamicBarKeys.ENABLED, 0, UserHandle.USER_CURRENT
        ) == 1

    fun isMediaEnabled(): Boolean {
        if (!isEnabled()) return false
        val json =
            Settings.Secure.getStringForUser(
                resolver, MrDynamicBarKeys.EVENTS, UserHandle.USER_CURRENT
            ) ?: return true
        if (json.isBlank()) return true
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).none { arr.optString(it) == MrDynamicBarKeys.EVENT_MEDIA }
        } catch (_: Exception) {
            true
        }
    }
}
