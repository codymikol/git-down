package com.codymikol// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.application
import com.codymikol.config.BeansModule
import com.codymikol.config.RepositoriesModule
import com.codymikol.config.ServicesModule
import com.codymikol.repositories.SettingsRepository
import com.codymikol.state.GitDownState
import com.codymikol.windows.handleDirectorySelection
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject
import org.koin.ksp.generated.module

fun main(args: Array<String>) = application {

    startKoin {
        modules(
            RepositoriesModule().module,
            BeansModule().module,
            ServicesModule().module,
        )
    }

    val settingsRepository: SettingsRepository by inject(SettingsRepository::class.java)
    val settings = settingsRepository.getSettings()
    GitDownState.headerTextSize.value = settings.headerTextSize
    GitDownState.bodyTextSize.value = settings.bodyTextSize

    // todo(mikol): we should move this into a load directory service and make it more robust eventually...
    if (args.size == 1) {
        handleDirectorySelection(args[0] + "/.git")
    }

    App(this)
}