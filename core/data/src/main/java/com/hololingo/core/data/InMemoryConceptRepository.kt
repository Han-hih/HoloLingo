package com.hololingo.core.data

import com.hololingo.core.model.Concept
import com.hololingo.core.model.ConceptId
import com.hololingo.core.model.HologramScene
import com.hololingo.core.model.LearningLanguage
import com.hololingo.core.model.Lexeme
import com.hololingo.core.model.SceneId

class InMemoryConceptRepository : ConceptRepository {
    private val mountainScene = HologramScene(
        id = SceneId("SCENE_MOUNTAIN"),
        modelAsset = "models/mountain.glb",
        appearAnimation = "appear",
        loopAnimation = "idle",
    )

    private val concepts = listOf(
        Concept(
            id = ConceptId("MOUNTAIN"),
            category = "NATURE",
            meaning = "땅이 높고 크게 솟아 있는 지형",
            sceneId = mountainScene.id,
            lexemes = listOf(
                Lexeme(LearningLanguage.KOREAN, "산", "산"),
                Lexeme(LearningLanguage.ENGLISH, "mountain", "mountain", alternatives = listOf("mountains")),
                Lexeme(LearningLanguage.JAPANESE, "山", "山", pronunciation = "やま"),
                Lexeme(LearningLanguage.CHINESE, "山", "山", pronunciation = "shān"),
            ),
        ),
    )

    override fun getConcepts(): List<Concept> = concepts

    override fun getConcept(id: ConceptId): Concept? = concepts.firstOrNull { it.id == id }

    override fun getScene(id: SceneId): HologramScene? =
        mountainScene.takeIf { it.id == id }

    override fun getSupportedLanguages(): List<LearningLanguage> = LearningLanguage.entries
}
