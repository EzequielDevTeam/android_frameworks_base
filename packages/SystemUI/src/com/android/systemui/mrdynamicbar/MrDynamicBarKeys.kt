/*
 * SPDX-FileCopyrightText: EzequielDevTeam (MrEzequielOS)
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.mrdynamicbar

/**
 * Contrato de Settings da Dynamic Bar do MrEzequielOS (fase 1: só mídia).
 * Tabela Settings.Secure. Ideia inspirada no Evolution X/crDroid;
 * chaves próprias (prefixo mrezequiel_) e implementação original.
 */
object MrDynamicBarKeys {
    const val ENABLED = "mrezequiel_dynamic_bar_enabled"
    const val EVENTS = "mrezequiel_dynamic_bar_events"

    /** Id do evento de mídia na lista de desligados. */
    const val EVENT_MEDIA = "media"
}
