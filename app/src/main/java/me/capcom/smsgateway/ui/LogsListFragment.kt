package me.capcom.smsgateway.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.capcom.smsgateway.databinding.FragmentLogsBinding
import me.capcom.smsgateway.modules.logs.LogsService
import me.capcom.smsgateway.modules.logs.vm.LogsViewModel
import me.capcom.smsgateway.ui.adapters.LogItemsAdapter
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A fragment representing a list of Items.
 */
class LogsListFragment : Fragment() {
    private val viewModel: LogsViewModel by viewModel()
    private val logsService: LogsService by inject()
    private val adapter = LogItemsAdapter()

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { exportLogsToUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.lastEntries.observe(this) {
            adapter.submitList(it) {
                _binding?.list?.scrollToPosition(0)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.list.adapter = adapter
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        binding.buttonExport.setOnClickListener {
            val fileName = "sms-gateway-logs-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())}.txt"
            createDocumentLauncher.launch(fileName)
        }
    }

    private fun exportLogsToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val logs = withContext(Dispatchers.IO) {
                    logsService.select().reversed() // Oldest first for chronological order
                }
                
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        val writer = outputStream.bufferedWriter()
                        logs.forEach { entry ->
                            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(entry.createdAt))
                            writer.write("[$timestamp] [${entry.priority}] [${entry.module}] ${entry.message}")
                            entry.context?.let { writer.write(" | Context: $it") }
                            writer.newLine()
                        }
                        writer.flush()
                    }
                }
                Toast.makeText(requireContext(), "Logs exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to export logs: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() =
            LogsListFragment()
    }
}