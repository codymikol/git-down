package com.codymikol.repositories

import com.codymikol.beans.ObjectMapperBean
import com.codymikol.data.recent.RecentProject
import com.codymikol.data.recent.RecentProjects
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Single
class RecentProjectRepository(
    private val objectMapperBean: ObjectMapperBean,
    private val userDirectoryRepository: UserDirectoryRepository,
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(RecentProjectRepository::class.java)
    }

    private var cachedRecentProjects: RecentProjects? = null
    private val defaultEmptyProjects: RecentProjects by lazy { RecentProjects(emptyList()) }
    private val recentFilename: String = "recentProjects.json"

    fun getRecentProjects() : RecentProjects {
        return cachedRecentProjects ?: initializeRecentProjects()
    }

    private fun setRecentProjects(newRecentProjects: RecentProjects) {
        try {
            Files.createDirectories(Paths.get(userDirectoryRepository.getUserDataDir()));
            val file = getFile()
            if(!file.exists()) file.createNewFile()
            objectMapperBean.value.writeValue(getFile(), newRecentProjects)
            cachedRecentProjects = newRecentProjects
        } catch (e: IOException) {
            logger.error("Failed to add new recent project.")
        }
    }

    fun addRecentProject(newProject: RecentProject) : Unit {
        val currentRecentProjects = getRecentProjects()
        val newProjects = (currentRecentProjects.projects + newProject).distinctBy { it.location }
        val newRecentProjects = RecentProjects(newProjects)
        setRecentProjects(newRecentProjects)
    }

    private fun getFile() = File("${requireNotNull(userDirectoryRepository.getUserDataDir())}/$recentFilename")

    private fun initializeRecentProjects(): RecentProjects {
        val file = getFile()
        return when(file.exists()) {
            true -> getRecentProjectsFromFile(file)
            false -> defaultEmptyProjects
        }
    }

    private fun getRecentProjectsFromFile(file: File): RecentProjects = try {
        objectMapperBean.value.readValue(file, RecentProjects::class.java)
    } catch (e: IOException) {
        logger.error("There was an error reading the recent projects, clearing and starting over")
        defaultEmptyProjects
    }

}