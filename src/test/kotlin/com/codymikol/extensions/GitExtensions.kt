package com.codymikol.extensions

import com.codymikol.repository.TestRepository.Companion.createTestRepository
import io.kotest.core.spec.style.DescribeSpec

class GitExtensions : DescribeSpec({

    // Creates a file with the letters A-Z separated by newlines.
    fun a2z() = (65 until 90).map { Char(it) }.joinToString { "\n" }

    describe("stageSelectedLines") {

        describe("Staging a few lines between two hunks") {

            val repository = createTestRepository()

            repository.addFile("foo.txt", a2z())
            repository.addFile("bar.txt", a2z())
            repository.stageAll()
            repository.commitAll("Added two files with A-Z, what an accomplishment!")
            repository.replaceFile("foo.txt", a2z().replace("A", "$").replace("Z", "$"))
//            repository.stageSelectedLines()

        }

    }

    describe("unstageSelectedLines") {

        describe("Unstaging a few lines between two hunks") {



        }

    }

})
