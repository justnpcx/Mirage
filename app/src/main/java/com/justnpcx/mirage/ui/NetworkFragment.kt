package com.justnpcx.mirage.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.justnpcx.mirage.R
import com.justnpcx.mirage.databinding.LayoutNetworkBinding
import com.justnpcx.mirage.ktx.app

class NetworkFragment : NamedFragment(R.layout.layout_network) {

    override fun name0() = app.getString(R.string.tools_network)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutNetworkBinding.bind(view)
        binding.stunTest.setOnClickListener {
            startActivity(Intent(requireContext(), StunActivity::class.java))
        }
    }

}