package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.MapConnector

/**
 * Computes the parent/child connector lines to draw over the map's lanes (#271):
 * one connector per edge whose parent has already been loaded (and lane-assigned).
 * A parent that pagination hasn't reached yet is simply skipped - the connector
 * appears on its own once loadMore() reaches it, since compute() is re-run off the
 * same commits/lanesBySha it reads from.
 */
object MapConnectors {

    fun compute(commits: List<CommitGraphNode>, lanesBySha: Map<String, Int>): List<MapConnector> {
        val indexBySha = commits.withIndex().associate { (index, commit) -> commit.sha to index }

        return commits.flatMapIndexed { childIndex, commit ->
            val childLane = lanesBySha[commit.sha] ?: return@flatMapIndexed emptyList()

            commit.parentShas.mapNotNull { parentSha ->
                val parentIndex = indexBySha[parentSha] ?: return@mapNotNull null
                val parentLane = lanesBySha[parentSha] ?: return@mapNotNull null
                MapConnector(childIndex, childLane, parentIndex, parentLane)
            }
        }
    }
}
