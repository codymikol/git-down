package com.codymikol// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.application
import com.codymikol.config.BeansModule
import com.codymikol.config.RepositoriesModule
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

@Suppress("UNUSED_PARAMETER")
fun main(args: Array<String>) = application {
    startKoin { modules(
        RepositoriesModule().module,
        BeansModule().module,
    ) }
    App(this)
}
