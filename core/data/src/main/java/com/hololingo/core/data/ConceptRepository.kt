package com.hololingo.core.data

import com.hololingo.core.model.Concept
import com.hololingo.core.model.ConceptId
import com.hololingo.core.model.HologramScene
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.SceneId

interface ConceptRepository {
    fun getConcepts(): List<Concept>
    fun getConcept(id: ConceptId): Concept?
    fun getScene(id: SceneId): HologramScene?
    fun getSupportedLanguages(): List<LearningLanguage>
}
