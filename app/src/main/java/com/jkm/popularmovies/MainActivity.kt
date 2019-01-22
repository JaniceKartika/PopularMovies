package com.jkm.popularmovies

import android.content.Intent
import android.content.res.Configuration
import android.database.Cursor
import android.os.Bundle
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.jkm.popularmovies.data.MovieContract
import com.jkm.popularmovies.model.MainModel
import com.jkm.popularmovies.model.MovieModel
import com.jkm.popularmovies.util.EndlessRecyclerViewScrollListener
import com.jkm.popularmovies.util.GridSpacingItemDecoration
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*

class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor>, MovieAdapter.ItemClickListener {

    private var mAdapter: MovieAdapter? = null
    private val mMovieModels = ArrayList<MovieModel>()
    private val mFavoriteMovieModels = ArrayList<MovieModel>()
    private var mPopularMovieModels: ArrayList<MovieModel>? = ArrayList()
    private var mTopRatedMovieModels: ArrayList<MovieModel>? = ArrayList()

    private var mApiInterface: MovieApiInterface? = null

    private var sort = SORT_POPULAR
    private var disableMenu: Boolean = false
    private var popularPage = 2
    private var topRatedPage = 2

    private var mScrollListener: EndlessRecyclerViewScrollListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configureRecyclerView(mMovieRecyclerView!!, 3)
        } else {
            configureRecyclerView(mMovieRecyclerView!!, 2)
        }

        val retrofit = Retrofit.Builder().baseUrl(BuildConfig.MOVIE_DB_API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()
        mApiInterface = retrofit.create(MovieApiInterface::class.java)

        if (savedInstanceState != null) {
            sort = savedInstanceState.getInt(getString(R.string.sort_key))
            mPopularMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.popular_movies_key))
            mTopRatedMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.top_rated_movies_key))
            popularPage = savedInstanceState.getInt(getString(R.string.popular_page_key))
            topRatedPage = savedInstanceState.getInt(getString(R.string.top_rated_page_key))

            if (sort == SORT_POPULAR) {
                mMovieModels.addAll(mPopularMovieModels!!)
            } else if (sort == SORT_TOP_RATED) {
                if (mTopRatedMovieModels != null) {
                    mMovieModels.addAll(mTopRatedMovieModels!!)
                }
            } else if (sort == SORT_FAVORITES) {
                supportLoaderManager.initLoader(ID_MOVIE_LOADER, null, this)
            }
        } else {
            callPopularMovies(1, true)
        }

        mAdapter = MovieAdapter(this, mMovieModels, this)
        mMovieRecyclerView!!.adapter = mAdapter
    }

    private fun callPopularMovies(page: Int, clearList: Boolean) {
        if (clearList) showLoading()

        val popularMovies = mApiInterface!!.getPopularMovies(BuildConfig.MOVIE_DB_API_KEY, page)
        popularMovies.enqueue(object : Callback<MainModel> {
            override fun onResponse(call: Call<MainModel>, response: Response<MainModel>) {
                if (response.body() != null) {
                    totalPopularPage = response.body()!!.totalPages
                    val results = response.body()!!.results
                    if (results != null && !results.isEmpty()) {
                        if (clearList)
                            mPopularMovieModels!!.clear()
                        else
                            popularPage++
                        mPopularMovieModels!!.addAll(results)

                        mMovieModels.clear()
                        mMovieModels.addAll(mPopularMovieModels!!)
                        mAdapter!!.notifyDataSetChanged()
                        mScrollListener!!.resetState()

                        sort = SORT_POPULAR
                    } else {
                        Toast.makeText(this@MainActivity, R.string.no_results, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.failed_fetch_popular, Toast.LENGTH_SHORT).show()
                }
                hideLoading()
            }

            override fun onFailure(call: Call<MainModel>, t: Throwable) {
                Log.e(TAG, getString(R.string.failed_fetch_popular), t.cause)
                Toast.makeText(this@MainActivity, R.string.failed_fetch_popular, Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        })
    }

    private fun callTopRatedMovies(page: Int, clearList: Boolean) {
        if (clearList) showLoading()

        val topRatedMovies = mApiInterface!!.getTopRatedMovies(BuildConfig.MOVIE_DB_API_KEY, page)
        topRatedMovies.enqueue(object : Callback<MainModel> {
            override fun onResponse(call: Call<MainModel>, response: Response<MainModel>) {
                if (response.body() != null) {
                    totalTopRatedPage = response.body()!!.totalPages
                    val results = response.body()!!.results
                    if (results != null && !results.isEmpty()) {
                        if (clearList)
                            mTopRatedMovieModels!!.clear()
                        else
                            topRatedPage++
                        mTopRatedMovieModels!!.addAll(results)

                        mMovieModels.clear()
                        mMovieModels.addAll(mTopRatedMovieModels!!)
                        mAdapter!!.notifyDataSetChanged()
                        mScrollListener!!.resetState()

                        sort = SORT_TOP_RATED
                    } else {
                        Toast.makeText(this@MainActivity, R.string.no_results, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, R.string.failed_fetch_top_rated, Toast.LENGTH_SHORT).show()
                }
                hideLoading()
            }

            override fun onFailure(call: Call<MainModel>, t: Throwable) {
                Log.e(TAG, getString(R.string.failed_fetch_top_rated), t.cause)
                Toast.makeText(this@MainActivity, R.string.failed_fetch_top_rated, Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        })
    }

    override fun setOnItemClickListener(view: View, position: Int) {
        val intent = Intent(this@MainActivity, DetailActivity::class.java)
        if (sort == SORT_POPULAR) {
            intent.putExtra(getString(R.string.detail_key), mPopularMovieModels!![position])
        } else if (sort == SORT_TOP_RATED) {
            intent.putExtra(getString(R.string.detail_key), mTopRatedMovieModels!![position])
        } else if (sort == SORT_FAVORITES) {
            intent.putExtra(getString(R.string.detail_key), mFavoriteMovieModels[position])
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)
        menu.findItem(R.id.action_favorites).isEnabled = !disableMenu
        menu.findItem(R.id.action_popular).isEnabled = !disableMenu
        menu.findItem(R.id.action_top_rated).isEnabled = !disableMenu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_favorites -> {
                if (sort != SORT_FAVORITES) {
                    supportLoaderManager.initLoader(ID_MOVIE_LOADER, null, this)
                }
                return true
            }
            R.id.action_popular -> {
                if (mPopularMovieModels!!.size > 0) {
                    if (sort != SORT_POPULAR) {
                        mMovieModels.clear()
                        mMovieModels.addAll(mPopularMovieModels!!)

                        mAdapter!!.notifyDataSetChanged()
                        mScrollListener!!.resetState()
                        sort = SORT_POPULAR
                    }
                    hideLoading()
                } else {
                    callPopularMovies(1, true)
                }
                return true
            }
            R.id.action_top_rated -> {
                if (mTopRatedMovieModels!!.size > 0) {
                    if (sort != SORT_TOP_RATED) {
                        mMovieModels.clear()
                        mMovieModels.addAll(mTopRatedMovieModels!!)

                        mAdapter!!.notifyDataSetChanged()
                        mScrollListener!!.resetState()
                        sort = SORT_TOP_RATED
                    }
                    hideLoading()
                } else {
                    callTopRatedMovies(1, true)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(getString(R.string.sort_key), sort)
        outState.putParcelableArrayList(getString(R.string.popular_movies_key), mPopularMovieModels)
        outState.putParcelableArrayList(getString(R.string.top_rated_movies_key), mTopRatedMovieModels)
        outState.putInt(getString(R.string.popular_page_key), popularPage)
        outState.putInt(getString(R.string.top_rated_page_key), topRatedPage)
    }

    private fun configureRecyclerView(recyclerView: RecyclerView, spanCount: Int) {
        val layoutManager = GridLayoutManager(this@MainActivity, spanCount)
        recyclerView.layoutManager = layoutManager
        recyclerView.addItemDecoration(GridSpacingItemDecoration(spanCount, 0, true))
        recyclerView.itemAnimator = DefaultItemAnimator()

        mScrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                if (sort == SORT_POPULAR) {
                    if (popularPage <= totalPopularPage) {
                        callPopularMovies(popularPage, false)
                    }
                } else if (sort == SORT_TOP_RATED) {
                    if (topRatedPage <= totalTopRatedPage) {
                        callTopRatedMovies(topRatedPage, false)
                    }
                }
            }
        }
        recyclerView.addOnScrollListener(mScrollListener!!)
    }

    private fun showLoading() {
        mMovieRecyclerView!!.visibility = View.INVISIBLE
        mMovieProgressBar!!.visibility = View.VISIBLE
        mMovieEmptyTextView!!.visibility = View.GONE

        disableMenu = true
        invalidateOptionsMenu()
    }

    private fun hideLoading() {
        mMovieRecyclerView!!.visibility = View.VISIBLE
        mMovieProgressBar!!.visibility = View.GONE
        mMovieEmptyTextView!!.visibility = View.GONE

        disableMenu = false
        invalidateOptionsMenu()
    }

    private fun showEmptyFavoritesMessage() {
        mMovieRecyclerView!!.visibility = View.INVISIBLE
        mMovieProgressBar!!.visibility = View.GONE
        mMovieEmptyTextView!!.visibility = View.VISIBLE

        disableMenu = false
        invalidateOptionsMenu()
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        showLoading()
        when (id) {
            ID_MOVIE_LOADER -> {
                val movieQueryUri = MovieContract.MovieEntry.CONTENT_URI
                val sortOrder = MovieContract.MovieEntry._ID + " DESC"

                return CursorLoader(this, movieQueryUri, MOVIE_PROJECTION, null, null, sortOrder)
            }
            else -> throw RuntimeException(getString(R.string.loader_not_implemented) + id)
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        if (data.count == 0) {
            showEmptyFavoritesMessage()
        } else {
            convertToMovieModel(data)
            mMovieModels.clear()
            mMovieModels.addAll(mFavoriteMovieModels)

            mAdapter!!.notifyDataSetChanged()
            hideLoading()
        }
        sort = SORT_FAVORITES
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        mMovieModels.clear()
        mAdapter!!.notifyDataSetChanged()
    }

    private fun convertToMovieModel(data: Cursor) {
        mFavoriteMovieModels.clear()
        for (i in 0 until data.count) {
            data.moveToPosition(i)
            val model = MovieModel()
            model.id = data.getInt(INDEX_MOVIE_ID)
            model.originalTitle = data.getString(INDEX_MOVIE_NAME)
            model.posterPath = data.getString(INDEX_POSTER_PATH)
            model.overview = data.getString(INDEX_OVERVIEW)
            model.voteAverage = data.getDouble(INDEX_USER_RATING)
            model.releaseDate = data.getString(INDEX_RELEASE_DATE)
            mFavoriteMovieModels.add(model)
        }
    }

    internal interface MovieApiInterface {
        @GET("movie/popular")
        fun getPopularMovies(@Query("api_key") apiKey: String, @Query("page") page: Int): Call<MainModel>

        @GET("movie/top_rated")
        fun getTopRatedMovies(@Query("api_key") apiKey: String, @Query("page") page: Int): Call<MainModel>
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val SORT_FAVORITES = 1
        private val SORT_POPULAR = 2
        private val SORT_TOP_RATED = 3

        val MOVIE_PROJECTION = arrayOf(MovieContract.MovieEntry.COLUMN_MOVIE_ID, MovieContract.MovieEntry.COLUMN_MOVIE_NAME, MovieContract.MovieEntry.COLUMN_POSTER_PATH, MovieContract.MovieEntry.COLUMN_OVERVIEW, MovieContract.MovieEntry.COLUMN_USER_RATING, MovieContract.MovieEntry.COLUMN_RELEASE_DATE)
        val INDEX_MOVIE_ID = 0
        val INDEX_MOVIE_NAME = 1
        val INDEX_POSTER_PATH = 2
        val INDEX_OVERVIEW = 3
        val INDEX_USER_RATING = 4
        val INDEX_RELEASE_DATE = 5

        private val ID_MOVIE_LOADER = 44
        private var totalPopularPage: Int = 0
        private var totalTopRatedPage: Int = 0
    }
}