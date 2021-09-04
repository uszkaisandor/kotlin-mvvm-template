package com.uszkaisandor.mvvmnewsapp.features.breakingnews

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.uszkaisandor.mvvmnewsapp.MainActivity
import com.uszkaisandor.mvvmnewsapp.R
import com.uszkaisandor.mvvmnewsapp.databinding.FragmentBreakingNewsBinding
import com.uszkaisandor.mvvmnewsapp.features.breakingnews.BreakingNewsViewModel.Event.*
import com.uszkaisandor.mvvmnewsapp.shared.NewsArticleListAdapter
import com.uszkaisandor.mvvmnewsapp.util.Resource
import com.uszkaisandor.mvvmnewsapp.util.exhaustive
import com.uszkaisandor.mvvmnewsapp.util.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news),
    MainActivity.OnBottomNavigationFragmentReselectedListener {

    private val viewModel: BreakingNewsViewModel by viewModels()

    private var currentBinding: FragmentBreakingNewsBinding? = null
    private val binding get() = currentBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBinding = FragmentBreakingNewsBinding.bind(view)

        val newsArticleAdapter = NewsArticleListAdapter(
            onItemClick = { article ->
                val uri = Uri.parse(article.url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                requireActivity().startActivity(intent)
            },
            onBookmarkClick = { article ->
                viewModel.onBookmarkClick(article)
            }
        )

        newsArticleAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY

        binding.apply {
            recyclerView.apply {
                adapter = newsArticleAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
                itemAnimator?.changeDuration = 0
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.breakingNews.collect {
                    val result = it ?: return@collect

                    swipeRefreshLayout.isRefreshing = result is Resource.Loading
                    recyclerView.isVisible = !result.data.isNullOrEmpty()
                    textViewError.isVisible = result.error != null && result.data.isNullOrEmpty()
                    buttonRetry.isVisible = result.error != null && result.data.isNullOrEmpty()
                    textViewError.text = getString(
                        R.string.could_not_refresh,
                        result.error?.localizedMessage ?: getString(R.string.unknown_error_occurred)
                    )

                    newsArticleAdapter.submitList(result.data)
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.stateEvents.collect { event ->
                    when (event) {
                        is ShowErrorMessage -> showSnackbar(
                            message = getString(R.string.could_not_refresh,
                                event.error.localizedMessage
                                    ?: getString(R.string.unknown_error_occurred)
                            )
                        )
                        is FetchedSuccessfully -> {
                            recyclerView.scrollToPosition(0)
                        }
                        else -> {
                            // Handle it
                        }
                    }.exhaustive
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.onManualRefresh()
            }

            buttonRetry.setOnClickListener {
                viewModel.onManualRefresh()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_breaking_news, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.onManualRefresh()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onBottomNavigationFragmentReselected() {
        binding.recyclerView.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        currentBinding = null
    }
}