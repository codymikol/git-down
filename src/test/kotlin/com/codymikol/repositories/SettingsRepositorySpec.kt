package com.codymikol.repositories

import com.codymikol.beans.ObjectMapperBean
import com.codymikol.data.settings.AppSettings
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempDirectory

private class SettingsTestUserDirectoryRepository(private val dir: String) : UserDirectoryRepository() {
    override fun getUserDataDir(): String = dir
}

class SettingsRepositorySpec : DescribeSpec({

    describe("SettingsRepository") {

        fun createRepository() = SettingsRepository(
            ObjectMapperBean(),
            SettingsTestUserDirectoryRepository(createTempDirectory("git-down-settings-test-").toString())
        )

        it("starts with the default settings") {
            createRepository().getSettings() shouldBe AppSettings()
        }

        it("returns the settings that were saved") {
            val repository = createRepository()

            repository.setSettings(AppSettings(headerTextSize = 24, bodyTextSize = 16))

            repository.getSettings() shouldBe AppSettings(headerTextSize = 24, bodyTextSize = 16)
        }

    }

})
