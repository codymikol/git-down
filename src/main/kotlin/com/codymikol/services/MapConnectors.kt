package com.codymikol.services

import com.codymikol.data.map.CommitGraphNode
import com.codymikol.data.map.MapConnector

/**
 * Computes the parent/child connector lines to draw over the map's lanes (#271):
 * one connector per edge whose parent has already been loaded (and lane-assigned).
 * A parent that pagination hasn't reached yet is simply skipped - the connector
 * appears on its own once loadMore() reaches it, since compute() is re-run off the
 * same commits/lanesBySha/rowsBySha it reads from.
 *
 * Endpoints are the commits' packed rows (see issue #305 / [LanePacker]), so each
 * connector reaches the node where it sits after its lane is packed tight to the
 * top - showing which node a commit forks off of when the parent lane runs longer.
 */
object MapConnectors {

    fun compute(
        commits: List<CommitGraphNode>,
        lanesBySha: Map<String, Int>,
        rowsBySha: Map<String, Int>,
    ): List<MapConnector> {
        return commits.flatMap { commit ->
            val childLane = lanesBySha[commit.sha] ?: return@flatMap emptyList()
            val childRow = rowsBySha[commit.sha] ?: return@flatMap emptyList()

            commit.parentShas.mapNotNull { parentSha ->
                val parentRow = rowsBySha[parentSha] ?: return@mapNotNull null
                val parentLane = lanesBySha[parentSha] ?: return@mapNotNull null
                MapConnector(childRow, childLane, parentRow, parentLane)
            }
        }
    }
}
