package com.MaDuSOFTSolutions.foundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DetailSectionFragment : Fragment() {
    private lateinit var section: DetailSection

    companion object {
        private const val ARG_SECTION = "section"

        @JvmStatic
        fun newInstance(section: DetailSection): DetailSectionFragment {
            return DetailSectionFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_SECTION, section)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            section = it.getSerializable(ARG_SECTION) as DetailSection
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail_section, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val textContent = view.findViewById<TextView>(R.id.tvContent)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)

        // Show either raw text content or RecyclerView based on data availability
        if (section.content.isNotEmpty()) {
            textContent.text = section.content
            recyclerView.visibility = View.GONE
            textContent.visibility = View.VISIBLE
        } else {
            recyclerView.apply {
                visibility = View.VISIBLE
                textContent.visibility = View.GONE
                layoutManager = LinearLayoutManager(requireContext())
                adapter = DetailItemAdapter(section.fields)
            }
        }
    }
}