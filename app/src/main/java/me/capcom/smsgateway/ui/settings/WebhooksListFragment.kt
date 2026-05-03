package me.capcom.smsgateway.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import me.capcom.smsgateway.databinding.FragmentWebhooksListBinding
import me.capcom.smsgateway.modules.webhooks.WebHooksService
import me.capcom.smsgateway.ui.adapters.WebhookAdapter
import org.koin.android.ext.android.inject

import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import me.capcom.smsgateway.databinding.DialogAddWebhookBinding
import me.capcom.smsgateway.domain.EntitySource
import me.capcom.smsgateway.modules.webhooks.domain.WebHookDTO
import me.capcom.smsgateway.modules.webhooks.domain.WebHookEvent

class WebhooksListFragment : Fragment() {
    private var _binding: FragmentWebhooksListBinding? = null
    private val binding get() = _binding!!

    private val webhookService: WebHooksService by inject()
    private val adapter: WebhookAdapter by lazy {
        WebhookAdapter(
            onDelete = { webhook -> showDeleteConfirmation(webhook) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWebhooksListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFab()
        loadWebhooks()
    }

    private fun setupRecyclerView() {
        binding.webhookList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WebhooksListFragment.adapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupFab() {
        binding.fabAddWebhook.setOnClickListener {
            showAddWebhookDialog()
        }
    }

    private fun loadWebhooks() {
        val webhooks = webhookService.select(null)
        adapter.submitList(webhooks)
        binding.emptyState.isVisible = webhooks.isEmpty()
        binding.webhookList.isVisible = webhooks.isNotEmpty()
    }

    private fun showAddWebhookDialog() {
        val dialogBinding = DialogAddWebhookBinding.inflate(layoutInflater)
        
        val events = WebHookEvent.values()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, events.map { it.value })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dialogBinding.spinnerEvent.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val url = dialogBinding.editUrl.text.toString()
                val event = events[dialogBinding.spinnerEvent.selectedItemPosition]
                val whitelist = dialogBinding.editWhitelist.text.toString().takeIf { it.isNotBlank() }
                val ignoreOtp = dialogBinding.switchIgnoreOtp.isChecked

                try {
                    webhookService.replace(
                        EntitySource.Local,
                        WebHookDTO(
                            id = null,
                            deviceId = null,
                            url = url,
                            event = event,
                            source = EntitySource.Local,
                            filterSenders = whitelist,
                            ignoreOtp = ignoreOtp
                        )
                    )
                    loadWebhooks()
                } catch (e: Exception) {
                    AlertDialog.Builder(requireContext())
                        .setMessage(e.message)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmation(webhook: WebHookDTO) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Webhook")
            .setMessage("Are you sure you want to delete this webhook?")
            .setPositiveButton("Delete") { _, _ ->
                webhookService.delete(webhook.source, webhook.id!!)
                loadWebhooks()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
