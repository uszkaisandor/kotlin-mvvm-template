package com.codinginflow.mvvmnewsapp.data

import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.codinginflow.mvvmnewsapp.api.ClientPropertiesInterceptor
import com.codinginflow.mvvmnewsapp.api.NewsApi
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

private const val NEWS_STARTING_PAGE = 1

class SearchNewsRemoteMediator(
    private val searchQuery: String,
    private val newsApi: NewsApi,
    private val newsArticleDb: NewsArticleDatabase,
    private val refreshOnInit: Boolean,
    private val clientPropertiesInterceptor: ClientPropertiesInterceptor
) : RemoteMediator<Int, NewsArticle>() {

    private val newsArticleDao = newsArticleDb.newsArticleDao()
    private val searchQueryRemoteKeyDao = newsArticleDb.searchQueryRemoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsArticle>
    ): MediatorResult {
        val page = when(loadType) {
            LoadType.REFRESH -> NEWS_STARTING_PAGE
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> searchQueryRemoteKeyDao.getRemoteKey(searchQuery).nextPageKey
        }

        try {
            clientPropertiesInterceptor.addHeaderEntries((("place" to "searchNews")) to false)
            clientPropertiesInterceptor.addHeaderEntries((("language" to "hu")) to true)

            val response = newsApi.searchNews(
                query = searchQuery,
                page = page,
                pageSize = state.config.pageSize
            )

            val networkSearchResults = response.articles

            val bookmarkedArticles = newsArticleDao.getAllBookmarkedArticles().first()

            val searchResultArticles = networkSearchResults.map { networkNewsArticle ->
                val isBookmarked = bookmarkedArticles.any { cachedBookmarkedArticle ->
                    cachedBookmarkedArticle.url == networkNewsArticle.url
                }

                NewsArticle(
                    title = networkNewsArticle.title,
                    url = networkNewsArticle.url,
                    thumbnailUrl = networkNewsArticle.urlToImage,
                    isBookmarked = isBookmarked
                )
            }

            newsArticleDb.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    newsArticleDao.deleteSearchResultsForQuery(searchQuery)
                }

                val lastQueryPosition = newsArticleDao.getLastQueryPosition(searchQuery) ?: 0
                var queryPosition = lastQueryPosition.inc()

                val searchResults = searchResultArticles.map { article ->
                    SearchResult(
                        searchQuery = searchQuery,
                        articleUrl = article.url,
                        queryPosition = queryPosition.inc()
                    )
                }

                val nextPageKey = page.inc()

                newsArticleDao.insertArticles(searchResultArticles)
                newsArticleDao.insertSearchResults(searchResults)
                searchQueryRemoteKeyDao.insertRemoteKey(
                    SearchQueryRemoteKey(
                    searchQuery = searchQuery,
                        nextPageKey = nextPageKey
                ))
            }

            return MediatorResult.Success(endOfPaginationReached = networkSearchResults.isEmpty())
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    override suspend fun initialize(): InitializeAction {
        return if (refreshOnInit) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
}