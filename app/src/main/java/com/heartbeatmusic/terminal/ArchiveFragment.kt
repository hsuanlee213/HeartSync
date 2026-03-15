package com.heartbeatmusic.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.heartbeatmusic.R
import com.heartbeatmusic.heartsync.HeartSyncViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ArchiveFragment : Fragment() {

    private val viewModel: ArchiveViewModel by activityViewModels()
    private val heartSyncViewModel: HeartSyncViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onResume() {
        super.onResume()
        heartSyncViewModel.autoSaveSession()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById<ComposeView>(R.id.archive_compose))?.setContent {
            ArchiveScreen(viewModel = viewModel)
        }
    }
}
