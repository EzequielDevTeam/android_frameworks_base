/*
 * SPDX-FileCopyrightText: EzequielDevTeam (MrEzequielOS)
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.systemui.mrdynamicbar

import com.android.systemui.CoreStartable
import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

/**
 * Módulo Dagger próprio da Dynamic Bar (o SystemUICoreStartableModule
 * está deprecated para novas adições). Instalado em SysUIComponent e
 * ReferenceSysUIComponent.
 */
@Module
abstract class MrDynamicBarModule {
    @Binds
    @IntoMap
    @ClassKey(MrDynamicBarManager::class)
    abstract fun bindMrDynamicBarManager(impl: MrDynamicBarManager): CoreStartable
}
