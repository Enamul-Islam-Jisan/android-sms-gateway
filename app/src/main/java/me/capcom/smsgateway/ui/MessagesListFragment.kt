package me.capcom.smsgateway.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.capcom.smsgateway.R
import me.capcom.smsgateway.data.entities.Message
import me.capcom.smsgateway.databinding.FragmentMessagesListBinding
import me.capcom.smsgateway.modules.messages.vm.MessagesListViewModel
import me.capcom.smsgateway.ui.adapters.MessagesAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel


class MessagesListFragment : Fragment(), MessagesAdapter.OnItemClickListener<Message> {

    private val viewModel: MessagesListViewModel by viewModel()
    private val messagesAdapter = MessagesAdapter(this)
    private var _binding: FragmentMessagesListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentMessagesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = messagesAdapter
        binding.recyclerView.addOnScrollListener(scrollListener)
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        // Observe stats LiveData
        viewModel.totals.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.totalCount.text = it.total.toString()
                binding.pendingCount.text = it.pending.toString()
                binding.processingCount.text = it.processing.toString()
                binding.sentCount.text = it.sent.toString()
                binding.failedCount.text = it.failed.toString()
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) {
            val shouldScrollToTop = _binding?.recyclerView?.computeVerticalScrollOffset() == 0
            messagesAdapter.submitList(it) {
                if (shouldScrollToTop) _binding?.recyclerView?.scrollToPosition(0)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.messages_list_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cancel_all -> {
                showConfirmationDialog(
                    R.string.action_cancel_all_pending,
                    R.string.cancel_confirmation
                ) {
                    viewModel.cancelAllPending()
                }
                true
            }
            R.id.action_clear_history -> {
                showConfirmationDialog(
                    R.string.action_clear_history,
                    R.string.clear_history_confirmation
                ) {
                    viewModel.clearHistory()
                }
                true
            }
            R.id.action_delete_all -> {
                showConfirmationDialog(
                    R.string.action_delete_all,
                    R.string.delete_all_confirmation
                ) {
                    viewModel.deleteAll()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showConfirmationDialog(titleResId: Int, messageResId: Int, onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(titleResId)
            .setMessage(messageResId)
            .setPositiveButton(R.string.yes) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    override fun onItemClick(item: Message) {
        parentFragmentManager.commit {
            replace(R.id.rootLayout, MessageDetailsFragment.newInstance(item.id))
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        binding.recyclerView.removeOnScrollListener(scrollListener)
        super.onDestroyView()
        _binding = null
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val linearLayoutManager = recyclerView.layoutManager as? LinearLayoutManager
            linearLayoutManager?.findLastVisibleItemPosition()?.let {
                if (it == messagesAdapter.itemCount - 1) viewModel.loadMore(messagesAdapter.itemCount)
            }
        }
    }

    companion object {
        fun newInstance() =
            MessagesListFragment()
    }
}