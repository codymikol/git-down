package com.codymikol.services

import com.codymikol.data.stash.StashListItem
import com.codymikol.extensions.applyStash
import com.codymikol.extensions.dropStash
import com.codymikol.extensions.saveStash
import com.codymikol.state.GitDownState
import org.koin.core.annotation.Single

@Single
class StashService {

    suspend fun saveStash(message: String, includeUntrackedFiles: Boolean) {
        GitDownState.git.value.saveStash(message, includeUntrackedFiles)
    }
    
    suspend fun applyStash(stash: StashListItem) {
        GitDownState.git.value.applyStash(stash)
    }

    suspend fun dropStash(stash: StashListItem) {
        GitDownState.git.value.dropStash(stash)

        if (GitDownState.selectedStash.value?.sha == stash.sha) {
            GitDownState.selectedStash.value = null
        }
    }

}
