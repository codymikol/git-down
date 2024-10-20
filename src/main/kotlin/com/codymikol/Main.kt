package com.codymikol// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.window.application
import com.codymikol.config.BeansModule
import com.codymikol.config.RepositoriesModule
import com.codymikol.state.GitDownState
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) = application {

            // todo(mikol): we should move this into a load directory service and make it more robust eventually...
            if(args.size == 1) {
                println("requested directory: " + args[0])
                GitDownState.gitDirectory.value = args[0] + "/.git"
            }

            startKoin { modules(
                RepositoriesModule().module,
                BeansModule().module,
            ) }
            App(this)
        }
    }
}

