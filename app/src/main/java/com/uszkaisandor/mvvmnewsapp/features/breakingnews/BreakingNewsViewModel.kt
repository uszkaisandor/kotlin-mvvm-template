package com.uszkaisandor.mvvmnewsapp.features.breakingnews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uszkaisandor.mvvmnewsapp.data.NewsArticle
import com.uszkaisandor.mvvmnewsapp.data.NewsRepository
import com.uszkaisandor.mvvmnewsapp.features.breakingnews.BreakingNewsViewModel.Event.*
import com.uszkaisandor.mvvmnewsapp.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class BreakingNewsViewModel
@Inject
constructor(
    private val repository: NewsRepository
) : ViewModel() {
    private val _stateEvents = MutableStateFlow<Event>(None)
    val stateEvents = _stateEvents.asStateFlow()

    private val _breakingNews = MutableStateFlow<Resource<List<NewsArticle>>?>(null)
    val breakingNews = _breakingNews.asStateFlow()

    init {
        viewModelScope.launch {
            repository.deleteNonBookmarkedArticlesOlderThan(
                timestampInMillis = TimeUnit.DAYS.toMillis(7)
            )
        }
    }

    fun onStart() {
        doRefresh(false)
    }

    fun onManualRefresh() {
        doRefresh(true)
    }

    private fun doRefresh(forceRefresh: Boolean) {
        viewModelScope.launch {
            repository.getBreakingNews(
                forceRefresh,
                onFetchSuccess = {
                    _stateEvents.value = FetchedSuccessfully
                },
                onFetchFailed = {
                    _stateEvents.value = ShowErrorMessage(it)
                }
            ).collect { breakingNews ->
                _breakingNews.value = breakingNews
            }
        }
    }

    fun onBookmarkClick(article: NewsArticle) {
        val currentlyBookmarked = article.isBookmarked
        val updatedArticle = article.copy(isBookmarked = !currentlyBookmarked)
        viewModelScope.launch {
            repository.updateArticle(updatedArticle)
        }
    }

    sealed class Event {
        data class ShowErrorMessage(val error: Throwable) : Event()
        object FetchedSuccessfully : Event()
        object None: Event()
    }
}