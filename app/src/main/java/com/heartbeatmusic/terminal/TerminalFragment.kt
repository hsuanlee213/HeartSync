package com.heartbeatmusic.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.button.MaterialButtonToggleGroup
import com.heartbeatmusic.R
import com.heartbeatmusic.heartsync.HeartSyncViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TerminalFragment : Fragment() {

    private val viewModel: HeartSyncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terminal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupModeSwitcher(view)
        view.findViewById<ComposeView>(R.id.heart_animation_container)?.setContent {
            GeometricHeartContent(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCollection()
    }

    private fun setupModeSwitcher(view: View) {
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.mode_switcher_root)
        val checkedId = when (viewModel.currentMode.value) {
            TerminalMode.ZEN -> R.id.btn_mode_zen
            TerminalMode.SYNC -> R.id.btn_mode_sync
            TerminalMode.OVERDRIVE -> R.id.btn_mode_overdrive
        }
        toggleGroup.check(checkedId)
        toggleGroup.addOnButtonCheckedListener { _, id, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (id) {
                R.id.btn_mode_zen -> viewModel.setMode(TerminalMode.ZEN)
                R.id.btn_mode_sync -> viewModel.setMode(TerminalMode.SYNC)
                R.id.btn_mode_overdrive -> viewModel.setMode(TerminalMode.OVERDRIVE)
            }
        }
    }
}
