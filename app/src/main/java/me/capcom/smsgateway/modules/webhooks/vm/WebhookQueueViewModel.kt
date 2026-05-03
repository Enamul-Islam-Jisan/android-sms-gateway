package me.capcom.smsgateway.modules.webhooks.vm

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.capcom.smsgateway.modules.webhooks.WebhooksSettings
import me.capcom.smsgateway.modules.webhooks.db.WebhookQueueEntity
import me.capcom.smsgateway.modules.webhooks.db.WebhookQueueRepository
import me.capcom.smsgateway.modules.webhooks.db.WebhookQueueStatistics
import me.capcom.smsgateway.modules.webhooks.workers.WebhookQueueProcessorWorker

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class WebhookQueueViewModel(
    private val context: Context,
    private val repository: WebhookQueueRepository,
    private val settings: WebhooksSettings,
) : ViewModel() {
    enum class Tab {
        Pending,
        Failed,
        History
    }

    private val _currentTab = MutableStateFlow(Tab.Pending)
    val currentTab = _currentTab

    @OptIn(ExperimentalCoroutinesApi::class)
    val queue: LiveData<List<WebhookQueueEntity>> = _currentTab.flatMapLatest { tab ->
        when (tab) {
            Tab.Pending -> repository.selectPendingQueue()
            Tab.Failed -> repository.selectFailedQueue()
            Tab.History -> repository.selectHistoryQueue(settings.maxHistorySize)
        }
    }.asLiveData()

    val statistics: LiveData<WebhookQueueStatistics> = repository.getQueueStatistics().asLiveData()

    fun setTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun cancelWebhook(id: String) {
        viewModelScope.launch {
            repository.cancel(id)
        }
    }

    fun retryWebhook(id: String) {
        viewModelScope.launch {
            repository.retry(id)
            WebhookQueueProcessorWorker.start(context, settings.internetRequired)
        }
    }
}
