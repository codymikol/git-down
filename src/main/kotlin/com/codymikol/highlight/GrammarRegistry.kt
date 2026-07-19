package com.codymikol.highlight

object GrammarRegistry {

    private val grammars = mutableListOf<Grammar>().apply { addAll(Grammars.ALL) }

    fun register(grammar: Grammar) {
        grammars.removeAll { it.id == grammar.id }
        grammars.add(grammar)
    }

    fun forPath(path: String): Grammar {
        val extension = path.substringAfterLast('.', "").lowercase()
        return grammars.firstOrNull { extension in it.extensions } ?: Grammars.PLAIN
    }

}
