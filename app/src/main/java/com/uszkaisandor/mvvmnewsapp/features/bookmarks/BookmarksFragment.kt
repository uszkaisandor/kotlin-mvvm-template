package com.uszkaisandor.mvvmnewsapp.features.bookmarks

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.*
import com.uszkaisandor.mvvmnewsapp.MainActivity
import com.uszkaisandor.mvvmnewsapp.R
import com.uszkaisandor.mvvmnewsapp.databinding.FragmentBookmarksBinding
import com.uszkaisandor.mvvmnewsapp.shared.NewsArticleListAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class BookmarksFragment : Fragment(R.layout.fragment_bookmarks), MainActivity.OnBottomNavigationFragmentReselectedListener {

    private val viewModel: BookmarksViewModel by viewModels()

    private var currentBinding: FragmentBookmarksBinding? = null
    private val binding get() = currentBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentBinding = FragmentBookmarksBinding.bind(view)

        val bookmarksAdapter = NewsArticleListAdapter(
            onItemClick = { article ->
                val uri = Uri.parse(article.url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                requireActivity().startActivity(intent)
            },
            onBookmarkClick = { article ->
                viewModel.onBookmarkClick(article)
            }
        )

        bookmarksAdapter.stateRestorationPolicy = PREVENT_WHEN_EMPTY

        binding.apply {
            recyclerView.apply {
                adapter = bookmarksAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.bookmarks.collect { bookmarkedArticles ->
                    bookmarkedArticles?.let { safeBookmarkedArticles ->

                        bookmarksAdapter.submitList(safeBookmarkedArticles)
                        textViewNoBookmarks.isVisible = safeBookmarkedArticles.isEmpty()
                        recyclerView.isVisible = safeBookmarkedArticles.isNotEmpty()
                    }
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_bookmarks, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_delete_all_bookmarks -> {
                viewModel.onDeleteAllBookmarks()
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