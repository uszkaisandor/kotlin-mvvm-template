package com.uszkaisandor.mvvmnewsapp.features.searchnews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.uszkaisandor.mvvmnewsapp.data.NewsArticle
import com.uszkaisandor.mvvmnewsapp.data.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchNewsViewModel
@Inject
constructor(
    private val repository: NewsRepository,
    state: SavedStateHandle
) : ViewModel() {

    private val currentQuery = state.getLiveData<String?>("currentQuery", null)

    val hasCurrentQuery = currentQuery.asFlow().map { it != null }

    var refreshOnInit = false

    var refreshInProgress = false
    var pendingScrollToTopAfterRefresh = false
    var pendingScrollToTopAfterNewQuery = false
    var newQueryInProgress = false

    val searchResults = currentQuery.asFlow().flatMapLatest { query ->
        query?.let {
            repository.getSearchResultsPaged(it, refreshOnInit)
        } ?: emptyFlow()
    }.cachedIn(viewModelScope)

    fun onSearchQuerySubmit(query: String) {
        refreshOnInit = true
        currentQuery.value = query
        newQueryInProgress = true
        pendingScrollToTopAfterNewQuery = true
    }

    fun onBookmarkClick(article: NewsArticle) {
        val currentlyBookmarked = article.isBookmarked
        val updatedArticle = article.copy(isBookmarked = !currentlyBookmarked)
        viewModelScope.launch {
            repository.updateArticle(updatedArticle)
        }
    }
}