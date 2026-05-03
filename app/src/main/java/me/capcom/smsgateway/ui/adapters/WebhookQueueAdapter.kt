package me.capcom.smsgateway.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import me.capcom.smsgateway.R
import me.capcom.smsgateway.databinding.ItemWebhookQueueBinding
import me.capcom.smsgateway.modules.webhooks.db.WebhookQueueEntity
import me.capcom.smsgateway.modules.webhooks.db.WebhookStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WebhookQueueAdapter(
    private val onCancel: (String) -> Unit,
    private val onRetry: (String) -> Unit
) : ListAdapter<WebhookQueueEntity, WebhookQueueAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemWebhookQueueBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemWebhookQueueBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WebhookQueueEntity) {
            binding.senderText.text = item.sender ?: "System"
            binding.messageText.text = item.message ?: item.url
            binding.statusText.text = item.status.name
            binding.idAndRetriesText.text = "${item.id} • RETRIES: ${item.retryCount}"
            
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            binding.createdAtText.text = dateFormat.format(Date(item.createdAt))
            
            binding.errorText.isVisible = !item.lastError.isNullOrEmpty()
            binding.errorText.text = item.lastError

            val statusColor = when (item.status) {
                WebhookStatus.PENDING -> R.color.primary
                WebhookStatus.PROCESSING -> R.color.secondary
                WebhookStatus.COMPLETED -> R.color.success
                WebhookStatus.FAILED -> R.color.warning
                WebhookStatus.PERMANENTLY_FAILED -> R.color.error
                WebhookStatus.CANCELLED -> R.color.slate_500
            }
            binding.statusText.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(binding.root.context, R.color.slate_100)
            )
            binding.statusText.setTextColor(
                ContextCompat.getColor(binding.root.context, statusColor)
            )

            binding.btnCancel.setOnClickListener { onCancel(item.id) }
            binding.btnRetry.setOnClickListener { onRetry(item.id) }
            
            binding.btnRetry.isVisible = item.status != WebhookStatus.COMPLETED && item.status != WebhookStatus.PROCESSING
            binding.btnCancel.isVisible = item.status == WebhookStatus.PENDING || item.status == WebhookStatus.FAILED
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WebhookQueueEntity>() {
        override fun areItemsTheSame(oldItem: WebhookQueueEntity, newItem: WebhookQueueEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WebhookQueueEntity, newItem: WebhookQueueEntity): Boolean {
            return oldItem == newItem
        }
    }
}
