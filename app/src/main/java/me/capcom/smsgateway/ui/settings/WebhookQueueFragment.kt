package me.capcom.smsgateway.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import me.capcom.smsgateway.R
import me.capcom.smsgateway.databinding.FragmentWebhookQueueBinding
import me.capcom.smsgateway.modules.webhooks.vm.WebhookQueueViewModel
import me.capcom.smsgateway.ui.adapters.WebhookQueueAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class WebhookQueueFragment : Fragment() {
    private var _binding: FragmentWebhookQueueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WebhookQueueViewModel by viewModel()
    private val adapter: WebhookQueueAdapter by lazy {
        WebhookQueueAdapter(
            onCancel = { id -> viewModel.cancelWebhook(id) },
            onRetry = { id -> viewModel.retryWebhook(id) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebhookQueueBinding.inflate(inflater, container, false)
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
            adapter = this@WebhookQueueFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                when (tab.position) {
                    0 -> viewModel.setTab(WebhookQueueViewModel.Tab.Pending)
                    1 -> viewModel.setTab(WebhookQueueViewModel.Tab.Failed)
                    2 -> viewModel.setTab(WebhookQueueViewModel.Tab.History)
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
        viewModel.queue.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            updateStats(stats, viewModel.currentTab.value)
        }

        lifecycle.coroutineScope.launchWhenStarted {
            viewModel.currentTab.collect { tab ->
                viewModel.statistics.value?.let { stats ->
                    updateStats(stats, tab)
                }
            }
        }
    }

    private fun updateStats(
        stats: me.capcom.smsgateway.modules.webhooks.db.WebhookQueueStatistics,
        tab: WebhookQueueViewModel.Tab
    ) {
        val (label, count) = when (tab) {
            WebhookQueueViewModel.Tab.Pending -> getString(R.string.pending) to (stats.pending + stats.processing)
            WebhookQueueViewModel.Tab.Failed -> getString(R.string.failed) to (stats.failed + stats.permanentlyFailed)
            WebhookQueueViewModel.Tab.History -> getString(R.string.history) to (stats.completed + stats.cancelled)
        }

        binding.tabStatsText.text = "$label: $count"
        binding.btnClearHistory.visibility =
            if (tab == WebhookQueueViewModel.Tab.History && count > 0) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = WebhookQueueFragment()
    }
}
