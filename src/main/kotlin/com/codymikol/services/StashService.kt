package com.codymikol.services

import com.codymikol.extensions.saveStash
import com.codymikol.state.GitDownState
import org.koin.core.annotation.Single

@Single
class StashService {

    suspend fun saveStash(message: String, includeUntrackedFiles: Boolean) {
        GitDownState.git.value.saveStash(message, includeUntrackedFiles)
    }

}
