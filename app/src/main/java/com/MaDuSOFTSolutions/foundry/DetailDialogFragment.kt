package com.MaDuSOFTSolutions.foundry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class DetailDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(combinedData: CombinedData): DetailDialogFragment {
            return DetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("combinedData", combinedData)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.detail_tab_container, container, false)

        val closeBtn = view.findViewById<TextView>(R.id.btnClose)
        closeBtn.setOnClickListener {
            dismiss()
        }
        val recyclerViewSummary = view.findViewById<RecyclerView>(R.id.recyclerViewSummary)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)

        val combinedData = arguments?.getSerializable("combinedData") as? CombinedData

        // Configure Summary RecyclerView
        combinedData?.mainData?.let { summaryData ->
            recyclerViewSummary.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = DetailItemAdapter(summaryData)
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        // Configure Tabbed Detailed Sections
        combinedData?.detailedSections?.let { sections ->
            viewPager.adapter = SectionsPagerAdapter(this, sections)
            // Inside onCreateView
            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (val title = sections[position].title) {
                    "Work Order" -> "Work Order"
                    "Pouring Details" -> "Pouring"
                    "Production" -> "Production"
                    "Subcontract" -> "Subcontract"
                    "Fettling" -> "Fettling"
                    else -> title
                }
            }.attach()
        }

        return view
    }

    private inner class SectionsPagerAdapter(
        fragment: Fragment,
        private val sections: List<DetailSection>
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = sections.size
        override fun createFragment(position: Int): Fragment {
            return DetailSectionFragment.newInstance(sections[position])
        }
    }

}