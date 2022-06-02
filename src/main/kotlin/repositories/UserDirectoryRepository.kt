package repositories

import net.harawata.appdirs.AppDirs
import net.harawata.appdirs.AppDirsFactory
import org.koin.core.annotation.Single

@Single
class UserDirectoryRepository {

    private val appDirs : AppDirs by lazy { AppDirsFactory.getInstance() }

    private val projectName = "gitdown"
    private val projectVersion = "0.0.0"
    private val author = "Cody Mikol"

    fun getUserDataDir(): String? = appDirs.getUserDataDir(projectName, projectVersion, author)

}