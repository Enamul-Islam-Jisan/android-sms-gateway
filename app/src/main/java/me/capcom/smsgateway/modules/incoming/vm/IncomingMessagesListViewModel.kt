package me.capcom.smsgateway.modules.incoming.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import me.capcom.smsgateway.modules.incoming.IncomingMessagesSettings
import me.capcom.smsgateway.modules.incoming.db.IncomingMessage
import me.capcom.smsgateway.modules.incoming.db.IncomingMessageTotals
import me.capcom.smsgateway.modules.incoming.db.IncomingMessageType
import me.capcom.smsgateway.modules.incoming.repositories.IncomingMessagesRepository

class IncomingMessagesListViewModel(
    private val repository: IncomingMessagesRepository,
    private val settings: IncomingMessagesSettings,
) : ViewModel() {
    enum class Tab {
        All,
        SMS,
        DataSMS,
        MMS
    }

    val totals: LiveData<IncomingMessageTotals> = repository.totals

    private val _currentTab = MutableStateFlow(Tab.All)
    val currentTab = _currentTab

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: LiveData<List<IncomingMessage>> = _currentTab.flatMapLatest { tab ->
        val type = when (tab) {
            Tab.All -> null
            Tab.SMS -> IncomingMessageType.SMS
            Tab.DataSMS -> IncomingMessageType.DATA_SMS
            Tab.MMS -> IncomingMessageType.MMS
        }
        repository.select(type, settings.maxHistorySize)
    }.asLiveData()

    fun setTab(tab: Tab) {
        _currentTab.value = tab
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
