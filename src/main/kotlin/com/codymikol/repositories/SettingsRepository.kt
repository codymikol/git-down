package com.codymikol.repositories

import com.codymikol.beans.ObjectMapperBean
import com.codymikol.data.settings.AppSettings
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

@Single
class SettingsRepository(
    private val objectMapperBean: ObjectMapperBean,
    private val userDirectoryRepository: UserDirectoryRepository,
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SettingsRepository::class.java)
    }

    private var cachedSettings: AppSettings? = null
    private val settingsFilename: String = "appSettings.json"

    fun getSettings(): AppSettings {
        return cachedSettings ?: initializeSettings()
    }

    fun setSettings(newSettings: AppSettings) {
        try {
            Files.createDirectories(Paths.get(userDirectoryRepository.getUserDataDir()))
            val file = getFile()
            if (!file.exists()) file.createNewFile()
            objectMapperBean.value.writeValue(getFile(), newSettings)
            cachedSettings = newSettings
        } catch (e: IOException) {
            logger.error("Failed to save app settings.")
        }
    }

    private fun getFile() = File("${requireNotNull(userDirectoryRepository.getUserDataDir())}/$settingsFilename")

    private fun initializeSettings(): AppSettings {
        val file = getFile()
        return when (file.exists()) {
            true -> getSettingsFromFile(file)
            false -> AppSettings()
        }
    }

    private fun getSettingsFromFile(file: File): AppSettings = try {
        objectMapperBean.value.readValue(file, AppSettings::class.java)
    } catch (e: IOException) {
        logger.error("There was an error reading app settings, using defaults")
        AppSettings()
    }

}
