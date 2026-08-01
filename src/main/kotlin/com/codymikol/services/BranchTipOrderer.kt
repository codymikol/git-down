package com.codymikol.services

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.filter.RevFilter

/**
 * One branch tip as shown by a map pill (see issue #277): the tip commit's sha and
 * the name(s) of every branch pointing at it, so a shared tip renders a single pill
 * carrying all of its names.
 */
data class OrderedBranchTip(val sha: String, val branchNames: List<String>)

/**
 * Orders the branch tips whose pills sit along the top of the map (see issue #277).
 * The default branch's tip comes first - it is the mainline everything merges back
 * into - and every other tip follows ordered by how soon it rejoins that mainline:
 * the fewer commits the mainline has gained since the two histories last shared a
 * commit (their merge base), the nearer the tip sits to the mainline. Ties, and any
 * tip with no shared history, fall back to branch name so the order is stable.
 */
object BranchTipOrderer {

    fun order(git: Git, defaultBranchName: String?, branches: List<Ref>): List<OrderedBranchTip> {
        if (branches.isEmpty()) return emptyList()

        val tips = branches
            .groupBy({ it.objectId.name }, { it.name.removePrefix("refs/heads/") })
            .map { (sha, names) -> OrderedBranchTip(sha, names.sorted()) }

        val repo = git.repository
        val defaultSha = branches
            .firstOrNull { it.name.removePrefix("refs/heads/") == defaultBranchName }
            ?.objectId?.name
        val defaultId = defaultSha?.let { repo.resolve(it) }

        fun mainlineDistance(tip: OrderedBranchTip): Int {
            if (defaultId == null || tip.sha == defaultSha) return 0
            val tipId = repo.resolve(tip.sha) ?: return Int.MAX_VALUE
            val base = mergeBase(repo, defaultId, tipId) ?: return Int.MAX_VALUE
            return countAhead(repo, defaultId, base)
        }

        val distances = tips.associateWith(::mainlineDistance)

        return tips.sortedWith(
            compareByDescending<OrderedBranchTip> { it.sha == defaultSha }
                .thenBy { distances.getValue(it) }
                .thenBy { it.branchNames.firstOrNull() ?: "" }
        )
    }

    private fun mergeBase(repo: Repository, a: ObjectId, b: ObjectId): ObjectId? =
        RevWalk(repo).use { walk ->
            walk.revFilter = RevFilter.MERGE_BASE
            walk.markStart(walk.parseCommit(a))
            walk.markStart(walk.parseCommit(b))
            walk.next()?.id
        }

    // Commits reachable from [tip] but not from [base]: how far the mainline has
    // advanced since it last shared history with the branch.
    private fun countAhead(repo: Repository, tip: ObjectId, base: ObjectId): Int =
        RevWalk(repo).use { walk ->
            walk.markStart(walk.parseCommit(tip))
            walk.markUninteresting(walk.parseCommit(base))
            var count = 0
            while (walk.next() != null) count++
            count
        }
}
