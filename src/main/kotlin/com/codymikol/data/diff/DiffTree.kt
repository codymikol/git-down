package com.codymikol.data.diff

import com.codymikol.data.file.FileDelta

data class DiffTree(
    val fileDeltaNodes: List<FileDeltaNode>,
) {
    companion object {
        fun make(fileDeltas: List<FileDelta>): DiffTree {

            val tree = DiffTree(
                fileDeltaNodes = fileDeltas.map { fileDelta -> FileDeltaNode.make(fileDelta) }
            )

            // todo(mikol): Is there a better way to define parent relationships without drilling back down through the tree?
            tree.fileDeltaNodes.forEach {fileDeltaNode ->
               fileDeltaNode.parent = tree
               fileDeltaNode.hunks.forEach {hunkNode ->
                   hunkNode.parent = fileDeltaNode
               }
            }

            return tree


        }
    }
}