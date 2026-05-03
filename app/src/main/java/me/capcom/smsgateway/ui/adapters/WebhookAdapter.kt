package me.capcom.smsgateway.ui.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.capcom.smsgateway.R
import me.capcom.smsgateway.databinding.ItemWebhookBinding
import me.capcom.smsgateway.modules.webhooks.domain.WebHookDTO

class WebhookAdapter(
    private val onDelete: (WebHookDTO) -> Unit
) : ListAdapter<WebHookDTO, WebhookAdapter.ViewHolder>(WebhookDiffCallback()) {
    class ViewHolder(private val binding: ItemWebhookBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(webhook: WebHookDTO, onDelete: (WebHookDTO) -> Unit) {
            binding.apply {
                idText.text = binding.root.context.getString(R.string.webhook_id_format, webhook.id)
                urlText.text = webhook.url
                eventText.text = webhook.event.value
                sourceText.text = when (webhook.source) {
                    me.capcom.smsgateway.domain.EntitySource.Local -> binding.root.context.getString(
                        R.string.local
                    )

                    me.capcom.smsgateway.domain.EntitySource.Gateway,
                    me.capcom.smsgateway.domain.EntitySource.Cloud -> binding.root.context.getString(
                        R.string.cloud
                    )
                }

                otpFilterText.isVisible = webhook.ignoreOtp
                whitelistText.isVisible = !webhook.filterSenders.isNullOrBlank()
                whitelistText.text = "Whitelist: ${webhook.filterSenders}"

                btnDelete.isVisible = webhook.source == me.capcom.smsgateway.domain.EntitySource.Local
                btnDelete.setOnClickListener { onDelete(webhook) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWebhookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = ViewHolder(binding)

        binding.root.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val webhook = getItem(position)
                val context = binding.root.context
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Webhook ID", webhook.id)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.id_copied, Toast.LENGTH_SHORT).show()
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onDelete)
    }

    class WebhookDiffCallback : DiffUtil.ItemCallback<WebHookDTO>() {
        override fun areItemsTheSame(oldItem: WebHookDTO, newItem: WebHookDTO): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WebHookDTO, newItem: WebHookDTO): Boolean {
            return oldItem == newItem
        }
    }
}
