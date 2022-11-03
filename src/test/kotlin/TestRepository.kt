import com.codymikol.extensions.commitAll
import com.codymikol.extensions.scanForChanges
import com.codymikol.extensions.stageAll
import org.eclipse.jgit.api.Git
import com.codymikol.state.GitDownState
import java.io.File
import java.nio.file.Path

data class TestRepository(
    val dir: Path,
    val git: Git,
) {


    fun addFile(filename: String, content: String) = this.also {
        val path = this.dir.toString() + "/" +  filename
        File(path).also { file -> file.parentFile.mkdirs() }.writeText(content)
    }

    fun replaceFile(filename: String, content: String) {

    }

    fun appendToFile(filename: String, content: String) = this.also {
        val path = this.dir.toString() + "/" + filename
        File(path).appendText(content)
    }

    fun deleteFile(filename: String) = this.also {
        val path = this.dir.toString() + "/" + filename
        File(path).delete()
    }

    fun close() = this.also {
        this.git.close()
    }

    suspend fun stageAll() = this.also {
        this.git.stageAll()
    }

    suspend fun commitAll(message: String) = this.also {
        this.git.commitAll(message)
    }

    fun transferIntoGitDownState() = this.also {
        this.close()
        GitDownState.gitDirectory.value = this.dir.toString() + "/.git"
        GitDownState.git.value.scanForChanges()
    }

    fun closeGitDownState() = this.also {
        GitDownState.git.value.close()
    }

    companion object {
        fun createTestRepository() =
            GitDownState.git.value.close()
                .let{ kotlin.io.path.createTempDirectory("git-down-state-test-") }
                .let { TestRepository(it, Git.init().setDirectory(it.toFile()).call()) }
    }

}