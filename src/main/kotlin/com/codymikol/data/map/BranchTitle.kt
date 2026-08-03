package com.codymikol.data.map

/**
 * A branch title pinned above its lane's tip node in the map (see issue #307). Sits in
 * [lane] - the same lane the branch's tip commit occupies below - and carries the
 * name(s) of every branch pointing at that tip, so a shared tip shows all of them.
 */
data class BranchTitle(val lane: Int, val branchNames: List<String>)
