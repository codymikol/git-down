package com.codymikol.highlight

import java.util.concurrent.CopyOnWriteArrayList

object GrammarRegistry {

    private val grammars: MutableList<Grammar> = CopyOnWriteArrayList(Grammars.ALL)

    fun register(grammar: Grammar) {
        grammars.removeIf { it.id == grammar.id }
        grammars.add(grammar)
    }

    fun forPath(path: String): Grammar {
        val extension = path.substringAfterLast('.', "").lowercase()
        return grammars.lastOrNull { extension in it.extensions } ?: Grammars.PLAIN
    }

}
