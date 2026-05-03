package me.capcom.smsgateway

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import me.capcom.smsgateway.databinding.ActivityMainBinding
import me.capcom.smsgateway.ui.HolderFragment
import me.capcom.smsgateway.ui.HomeFragment
import me.capcom.smsgateway.ui.SettingsFragment
import me.capcom.smsgateway.ui.SettingsHolderFragment
import me.capcom.smsgateway.helpers.LocaleHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val adapter = FragmentsAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // Disable swiping for bottom nav consistency

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    binding.viewPager.currentItem = TAB_INDEX_HOME
                    supportActionBar?.title = getString(R.string.tab_text_home)
                    true
                }
                R.id.navigation_messages -> {
                    binding.viewPager.currentItem = TAB_INDEX_MESSAGES
                    supportActionBar?.title = getString(R.string.tab_text_messages)
                    true
                }
                R.id.navigation_settings -> {
                    binding.viewPager.currentItem = TAB_INDEX_SETTINGS
                    supportActionBar?.title = getString(R.string.tab_text_settings)
                    true
                }
                else -> false
            }
        }

        processIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent) {
        val tabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, TAB_INDEX_HOME)

        binding.viewPager.currentItem = tabIndex
        binding.bottomNavigation.selectedItemId = when (tabIndex) {
            TAB_INDEX_HOME -> {
                supportActionBar?.title = getString(R.string.tab_text_home)
                R.id.navigation_home
            }
            TAB_INDEX_MESSAGES -> {
                supportActionBar?.title = getString(R.string.tab_text_messages)
                R.id.navigation_messages
            }
            TAB_INDEX_SETTINGS -> {
                supportActionBar?.title = getString(R.string.tab_text_settings)
                R.id.navigation_settings
            }
            else -> {
                supportActionBar?.title = getString(R.string.tab_text_home)
                R.id.navigation_home
            }
        }
    }

    class FragmentsAdapter(activity: AppCompatActivity) :
        androidx.viewpager2.adapter.FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment.newInstance()
                1 -> HolderFragment.newInstance()
                else -> SettingsHolderFragment.newInstance()
            }
        }

    }

    companion object {
        const val TAB_INDEX_HOME = 0
        const val TAB_INDEX_MESSAGES = 1
        const val TAB_INDEX_SETTINGS = 2

        private const val EXTRA_TAB_INDEX = "tabIndex"

        fun starter(context: Context, tabIndex: Int): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_TAB_INDEX, tabIndex)
            }
        }
    }
}