package me.capcom.smsgateway.modules.incoming

import me.capcom.smsgateway.modules.settings.KeyValueStorage
import me.capcom.smsgateway.modules.settings.get

class IncomingMessagesSettings(
    private val storage: KeyValueStorage,
) {
    val maxHistorySize: Int
        get() = storage.get<Int>(MAX_HISTORY_SIZE) ?: 50

    companion object {
        const val MAX_HISTORY_SIZE = "incoming_max_history_size"
    }
}
