package com.codymikol.repositories

import com.codymikol.beans.ObjectMapperBean
import com.codymikol.data.recent.RecentProject
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.io.path.createTempDirectory

private class TestUserDirectoryRepository(private val dir: String) : UserDirectoryRepository() {
    override fun getUserDataDir(): String = dir
}

class RecentProjectRepositorySpec : DescribeSpec({

    describe("RecentProjectRepository") {

        fun createRepository() = RecentProjectRepository(
            ObjectMapperBean(),
            TestUserDirectoryRepository(createTempDirectory("git-down-recent-projects-test-").toString())
        )

        it("starts with an empty list of recent projects") {
            createRepository().getRecentProjects().projects shouldBe emptyList()
        }

        it("adds a project to the front of the list") {
            val repository = createRepository()

            repository.addRecentProject(RecentProject(name = "foo", location = "/foo/.git"))

            repository.getRecentProjects().projects shouldBe listOf(RecentProject(name = "foo", location = "/foo/.git"))
        }

        it("moves an existing project to the front when re-added") {
            val repository = createRepository()

            repository.addRecentProject(RecentProject(name = "foo", location = "/foo/.git"))
            repository.addRecentProject(RecentProject(name = "bar", location = "/bar/.git"))
            repository.addRecentProject(RecentProject(name = "foo", location = "/foo/.git"))

            repository.getRecentProjects().projects.map { it.location } shouldBe listOf("/foo/.git", "/bar/.git")
        }

        describe("removeRecentProject") {

            it("removes the project matching the given location") {
                val repository = createRepository()

                repository.addRecentProject(RecentProject(name = "foo", location = "/foo/.git"))
                repository.addRecentProject(RecentProject(name = "bar", location = "/bar/.git"))

                repository.removeRecentProject("/foo/.git")

                repository.getRecentProjects().projects shouldBe listOf(RecentProject(name = "bar", location = "/bar/.git"))
            }

            it("does nothing when no project matches the given location") {
                val repository = createRepository()

                repository.addRecentProject(RecentProject(name = "foo", location = "/foo/.git"))

                repository.removeRecentProject("/does/not/exist/.git")

                repository.getRecentProjects().projects shouldBe listOf(RecentProject(name = "foo", location = "/foo/.git"))
            }

        }

    }

})
