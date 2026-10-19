package com.hololingo.core.renderer

import com.hololingo.core.model.HologramScene

interface HologramOutput {
    suspend fun connect(): OutputResult
    suspend fun display(scene: HologramScene): OutputResult
    suspend fun stop()
}

sealed interface OutputResult {
    data object Success : OutputResult
    data class Failure(val message: String) : OutputResult
}
