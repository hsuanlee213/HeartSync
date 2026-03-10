package com.heartbeatmusic.terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TerminalMode {
    ZEN,
    SYNC,
    OVERDRIVE
}

/**
 * Holds the selected TerminalMode with StateFlow.
 * Shared across MainActivity and fragments.
 */
object TerminalModeHolder {
    private val _selectedMode = MutableStateFlow(TerminalMode.SYNC)
    val selectedMode: StateFlow<TerminalMode> = _selectedMode.asStateFlow()

    fun setMode(mode: TerminalMode) {
        _selectedMode.value = mode
    }

    /** For Java interop: get current mode value. */
    fun getCurrentMode(): TerminalMode = _selectedMode.value
}
