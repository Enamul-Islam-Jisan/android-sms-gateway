package me.capcom.smsgateway.ui.settings

import android.os.Bundle
import me.capcom.smsgateway.R

class IncomingMessagesSettingsFragment : BasePreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.incoming_messages_preferences, rootKey)
    }
}
