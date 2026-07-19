package com.codymikol.services

import com.codymikol.data.file.FileDelta
import com.codymikol.data.file.Status
import com.codymikol.state.GitDownState
import org.koin.core.annotation.Single
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

@Single
class DiffToolService {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(DiffToolService::class.java)
    }

    internal var launchProcess: (List<String>, File) -> Process = { command, workingDirectory ->
        ProcessBuilder(command).directory(workingDirectory).start()
    }

    fun getConfiguredDiffTool(): String? = GitDownState.repo.value.config.getString("diff", null, "tool")

    fun buildDiffToolCommand(fileDelta: FileDelta): List<String> {
        val command = mutableListOf("git", "difftool", "-y")
        getConfiguredDiffTool()?.let { command.addAll(listOf("-t", it)) }
        if (fileDelta.type == Status.INDEX) command.add("--cached")
        command.add("--")
        command.add(fileDelta.getPath())
        return command
    }

    fun launchDiffTool(fileDelta: FileDelta) {
        if (fileDelta.type == Status.STASH) {
            logger.warn("Launching an external diff tool for a stashed file is not currently supported")
            return
        }

        val command = buildDiffToolCommand(fileDelta)

        try {
            launchProcess(command, GitDownState.repo.value.workTree)
            logger.info("Launched diff tool for ${fileDelta.getPath()}: ${command.joinToString(" ")}")
        } catch (e: Exception) {
            logger.error("Failed to launch diff tool for ${fileDelta.getPath()}: ${e.message}")
        }
    }

}
