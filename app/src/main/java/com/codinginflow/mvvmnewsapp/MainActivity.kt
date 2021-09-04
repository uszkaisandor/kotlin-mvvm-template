package com.codinginflow.mvvmnewsapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.codinginflow.mvvmnewsapp.data.AccountProperties
import com.codinginflow.mvvmnewsapp.data.NewsArticleDatabase
import com.codinginflow.mvvmnewsapp.databinding.ActivityMainBinding
import com.codinginflow.mvvmnewsapp.features.bookmarks.BookmarksFragment
import com.codinginflow.mvvmnewsapp.features.breakingnews.BreakingNewsFragment
import com.codinginflow.mvvmnewsapp.features.searchnews.SearchNewsFragment
import com.codinginflow.mvvmnewsapp.shared.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var database: NewsArticleDatabase

    @Inject
    lateinit var sessionManager: SessionManager

    private lateinit var breakingNewsFragment: BreakingNewsFragment
    private lateinit var searchNewsFragment: SearchNewsFragment
    private lateinit var bookmarksFragment: BookmarksFragment

    private val fragments: Array<Fragment>
        get() = arrayOf(
            breakingNewsFragment,
            searchNewsFragment,
            bookmarksFragment
        )

    private var selectedIndex = 0

    private val selectedFragment get() = fragments[selectedIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simulate insert user and get account properties from cache
        lifecycleScope.launch(IO) {
            val accountPropertiesDao = database.accountPropertiesDao()

            accountPropertiesDao.insertAccountProperties(
                accountProperties = AccountProperties(token = BuildConfig.NEWS_API_ACCESS_KEY)
            )

            val accountProperties = accountPropertiesDao.getAccountProperties()
            accountProperties?.let { safeAccountProperties ->
                // Update SessionManager
                sessionManager.setAccountProperties(safeAccountProperties)
            }
        }

        if (savedInstanceState == null) {
            breakingNewsFragment = BreakingNewsFragment()
            searchNewsFragment = SearchNewsFragment()
            bookmarksFragment = BookmarksFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, breakingNewsFragment, TAG_BREAKING_NEWS_FRAGMENT)
                .add(R.id.fragment_container, searchNewsFragment, TAG_SEARCH_NEWS_FRAGMENT)
                .add(R.id.fragment_container, bookmarksFragment, TAG_BOOKMARKS_FRAGMENT)
                .commit()
        } else {
            breakingNewsFragment = supportFragmentManager.findFragmentByTag(
                TAG_BREAKING_NEWS_FRAGMENT
            ) as BreakingNewsFragment
            searchNewsFragment = supportFragmentManager.findFragmentByTag(
                TAG_SEARCH_NEWS_FRAGMENT
            ) as SearchNewsFragment
            bookmarksFragment = supportFragmentManager.findFragmentByTag(
                TAG_BOOKMARKS_FRAGMENT
            ) as BookmarksFragment

            selectedIndex = savedInstanceState.getInt(KEY_SELECTED_INDEX, 0)
        }

        selectFragment(selectedFragment)

        binding.bottomNav.setOnNavigationItemSelectedListener() { item ->
            val fragment = when (item.itemId) {
                R.id.nav_breaking -> breakingNewsFragment
                R.id.nav_search -> searchNewsFragment
                R.id.nav_bookmarks -> bookmarksFragment
                else -> throw IllegalArgumentException("Unexpected itemId")
            }

            if (selectedFragment === fragment) {
                if (fragment is OnBottomNavigationFragmentReselectListener) {
                    fragment.onBottomNavigationFragmentReselected()
                }
            } else {
                selectFragment(fragment)
            }

            true
        }
    }

    private fun selectFragment(selectedFragment: Fragment) {
        var transaction = supportFragmentManager.beginTransaction()
        fragments.forEachIndexed { index, fragment ->
            if (selectedFragment == fragment) {
                transaction = transaction.attach(fragment)
                selectedIndex = index
            } else {
                transaction = transaction.detach(fragment)
            }
        }
        transaction.commit()

        title = when (selectedFragment) {
            is BreakingNewsFragment -> getString(R.string.title_breaking_news)
            is SearchNewsFragment -> getString(R.string.title_search_news)
            is BookmarksFragment -> getString(R.string.title_bookmarks)
            else -> ""
        }
    }

    override fun onBackPressed() {
        if (selectedIndex != 0) {
            binding.bottomNav.selectedItemId = R.id.nav_breaking
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_INDEX, selectedIndex)
    }
}

interface OnBottomNavigationFragmentReselectListener {
    fun onBottomNavigationFragmentReselected()
}

const val TAG_BREAKING_NEWS_FRAGMENT = "tag_breaking_news_fragment"
const val TAG_SEARCH_NEWS_FRAGMENT = "tag_search_news_fragment"
const val TAG_BOOKMARKS_FRAGMENT = "tag_bookmarks_news_fragment"
const val KEY_SELECTED_INDEX = "key_selected_index"