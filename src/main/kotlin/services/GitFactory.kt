package services

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import state.GitDownState
import java.io.File

object GitFactory {

    fun makeGit(repo: Repository): Git {
        return Git(repo)
    }

    fun makeRepository(dir: String): Repository = GitDownState.builder.setGitDir(File(dir))
        .readEnvironment()
        .findGitDir()
        .build()
}
