package com.heartbeatmusic.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.heartbeatmusic.R
import com.heartbeatmusic.data.remote.ArchiveRepository

class ArchiveFragment : Fragment() {

    private val archiveRepository = ArchiveRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById<ComposeView>(R.id.archive_compose))?.setContent {
            ArchiveScreen(archiveRepository = archiveRepository)
        }
    }
}
