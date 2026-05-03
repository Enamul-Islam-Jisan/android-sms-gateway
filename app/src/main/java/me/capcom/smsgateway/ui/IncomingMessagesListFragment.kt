package me.capcom.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import me.capcom.smsgateway.R
import me.capcom.smsgateway.databinding.FragmentIncomingMessagesListBinding
import me.capcom.smsgateway.modules.incoming.vm.IncomingMessagesListViewModel
import me.capcom.smsgateway.ui.adapters.IncomingMessagesAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class IncomingMessagesListFragment : Fragment() {
    private val viewModel: IncomingMessagesListViewModel by viewModel()
    private val adapter = IncomingMessagesAdapter()

    private var _binding: FragmentIncomingMessagesListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncomingMessagesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupTabs()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@IncomingMessagesListFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                when (tab.position) {
                    0 -> viewModel.setTab(IncomingMessagesListViewModel.Tab.All)
                    1 -> viewModel.setTab(IncomingMessagesListViewModel.Tab.SMS)
                    2 -> viewModel.setTab(IncomingMessagesListViewModel.Tab.DataSMS)
                    3 -> viewModel.setTab(IncomingMessagesListViewModel.Tab.MMS)
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })
    }

    private fun setupButtons() {
        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) {
            val shouldScrollToTop = binding.recyclerView.computeVerticalScrollOffset() == 0
            adapter.submitList(it) {
                if (shouldScrollToTop) binding.recyclerView.scrollToPosition(0)
            }
        }

        viewModel.totals.observe(viewLifecycleOwner) { stats ->
            updateStats(stats, viewModel.currentTab.value)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.currentTab.collect { tab ->
                    viewModel.totals.value?.let { stats ->
                        updateStats(stats, tab)
                    }
                }
            }
        }
    }

    private fun updateStats(
        stats: me.capcom.smsgateway.modules.incoming.db.IncomingMessageTotals,
        tab: IncomingMessagesListViewModel.Tab
    ) {
        val (label, count) = when (tab) {
            IncomingMessagesListViewModel.Tab.All -> "All" to stats.total
            IncomingMessagesListViewModel.Tab.SMS -> getString(R.string.incoming_type_sms) to stats.sms
            IncomingMessagesListViewModel.Tab.DataSMS -> getString(R.string.incoming_type_data_sms) to stats.dataSms
            IncomingMessagesListViewModel.Tab.MMS -> getString(R.string.incoming_type_mms) to stats.mms
        }

        binding.tabStatsText.text = "$label: $count"
        binding.btnClearHistory.visibility =
            if (count > 0) View.VISIBLE else View.GONE
    }

    companion object {
        fun newInstance() = IncomingMessagesListFragment()
    }
}
