package com.heartbeatmusic.terminal

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.heartbeatmusic.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncEngineFragment : Fragment() {

    private val hexCodes = listOf(
        "0x7F3A", "0xB2C1", "0xE9F0", "0x4D5A", "0xA1B2", "0xC3D4",
        "0x1E2F", "0x5A6B", "0x8C9D", "0xD0E1", "0x2F3A", "0x4B5C"
    )

    private val handler = Handler(Looper.getMainLooper())
    private var hexUpdateRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.grid_overlay).background = GridOverlayDrawable()

        hexUpdateRunnable = object : Runnable {
            override fun run() {
                if (view.parent != null) {
                    updateHexCodes(view)
                    handler.postDelayed(this, 2500)
                }
            }
        }
        handler.post(hexUpdateRunnable!!)
    }

    override fun onDestroyView() {
        hexUpdateRunnable?.let { handler.removeCallbacks(it) }
        super.onDestroyView()
    }

    private fun updateHexCodes(view: View) {
        listOf(
            R.id.tv_hex_corner_tl,
            R.id.tv_hex_corner_tr,
            R.id.tv_hex_corner_bl,
            R.id.tv_hex_corner_br
        ).forEach { id ->
            view.findViewById<TextView>(id)?.text = hexCodes.random()
        }
    }
}
