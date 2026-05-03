package me.capcom.smsgateway.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import me.capcom.smsgateway.R
import me.capcom.smsgateway.databinding.FragmentSettingsHolderBinding

class SettingsHolderFragment : Fragment(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var _binding: FragmentSettingsHolderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsHolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.addOnBackStackChangedListener {
            updateBreadcrumbs()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(R.id.settingsRootLayout, SettingsFragment.newInstance())
            }
        }
        
        updateBreadcrumbs()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = childFragmentManager.fragmentFactory.instantiate(
            requireActivity().classLoader,
            pref.fragment!!
        ).apply {
            arguments = pref.extras
        }
        
        childFragmentManager.commit {
            replace(R.id.settingsRootLayout, fragment)
            addToBackStack(pref.title?.toString())
        }
        
        return true
    }

    private fun updateBreadcrumbs() {
        binding.breadcrumbsContainer.removeAllViews()
        
        val rootTitle = getString(R.string.tab_text_settings)
        val stackCount = childFragmentManager.backStackEntryCount
        
        // Root "Settings"
        addBreadcrumb(rootTitle, stackCount > 0, -1)
        
        for (i in 0 until stackCount) {
            addSeparator()
            val entry = childFragmentManager.getBackStackEntryAt(i)
            val isClickable = i < stackCount - 1
            addBreadcrumb(entry.name ?: "...", isClickable, i)
        }
    }

    private fun addBreadcrumb(title: String, isClickable: Boolean, backStackIndex: Int) {
        val textView = TextView(context).apply {
            text = title.trimEnd('.', '…', ' ')
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 4)
            
            if (backStackIndex == -1) {
                // Add gear icon for the first breadcrumb
                val gearIcon = ContextCompat.getDrawable(context, R.drawable.ic_advanced)?.apply {
                    setBounds(0, 0, 40, 40)
                    setTint(ContextCompat.getColor(context, if (isClickable) R.color.primary else R.color.slate_500))
                }
                setCompoundDrawables(gearIcon, null, null, null)
                compoundDrawablePadding = 8
            }

            if (isClickable) {
                setTextColor(ContextCompat.getColor(context, R.color.primary))
                setBackgroundResource(R.drawable.rounded_background)
                backgroundTintList = ContextCompat.getColorStateList(context, R.color.slate_100)
                setOnClickListener {
                    if (backStackIndex == -1) {
                        childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    } else {
                        val entry = childFragmentManager.getBackStackEntryAt(backStackIndex)
                        childFragmentManager.popBackStack(entry.id, 0)
                    }
                }
            } else {
                setTextColor(ContextCompat.getColor(context, R.color.slate_500))
            }
        }
        binding.breadcrumbsContainer.addView(textView)
    }

    private fun addSeparator() {
        val separator = TextView(context).apply {
            text = " / "
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.slate_200))
            setPadding(4, 0, 4, 0)
        }
        binding.breadcrumbsContainer.addView(separator)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsHolderFragment()
    }
}
