package com.codymikol.data.stash

import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.text.SimpleDateFormat
import java.util.Date

private val WipStashMessage = Regex("^WIP on (.+?): .*$")
private val CustomStashMessage = Regex("^On (.+?): (.*)$")

data class StashListItem(
    val sha: String,
    val date: Date,
    val description: String,
    val branchOrSha: String,
    val commitMessage: String,
) {

    val title: String
        get() {
            val datePart = SimpleDateFormat("MM/dd/yy").format(date)
            val timePart = SimpleDateFormat("h:mm a").format(date)
            return "${sha.take(7)}    $datePart, $timePart"
        }

    val body: String
        get() = "$description on $branchOrSha: $commitMessage"

    companion object {
        fun make(repository: Repository, revCommit: RevCommit): StashListItem {
            val fullMessage = revCommit.fullMessage.trim()

            val (description, branchOrSha) = when {
                WipStashMessage.matches(fullMessage) ->
                    "WIP" to WipStashMessage.find(fullMessage)!!.groupValues[1]
                CustomStashMessage.matches(fullMessage) ->
                    CustomStashMessage.find(fullMessage)!!.groupValues[2] to CustomStashMessage.find(fullMessage)!!.groupValues[1]
                else -> "WIP" to (repository.branch ?: revCommit.name.take(7))
            }

            val commitMessage = when (revCommit.parentCount > 0) {
                true -> RevWalk(repository).use { walk ->
                    val parsedParent = walk.parseCommit(revCommit.getParent(0))
                    "${parsedParent.name.take(7)} ${parsedParent.shortMessage}"
                }
                false -> ""
            }

            return StashListItem(
                sha = revCommit.name,
                date = Date(revCommit.commitTime.toLong() * 1000),
                description = description,
                branchOrSha = branchOrSha,
                commitMessage = commitMessage,
            )
        }
    }
}
