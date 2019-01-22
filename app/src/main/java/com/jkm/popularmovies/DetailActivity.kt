package com.jkm.popularmovies

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.jkm.popularmovies.data.MovieContract
import com.jkm.popularmovies.model.*
import com.jkm.popularmovies.util.EndlessRecyclerViewScrollListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_detail.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {

    private var mReviewAdapter: ReviewAdapter? = null
    private var mMovieModel: MovieModel? = null
    private var mReviewResultModels: ArrayList<ReviewResultModel>? = ArrayList()
    private var mTrailerResultModels: ArrayList<TrailerResultModel>? = ArrayList()

    private var mDetailApiInterface: DetailApiInterface? = null

    private var reviewPage = 2
    private var isFavorite = false

    private var mScrollListener: EndlessRecyclerViewScrollListener? = null

    private val isFavoriteMovie: Boolean
        get() {
            val detailContentResolver = contentResolver
            val cursor = detailContentResolver.query(
                    MovieContract.MovieEntry.buildMovieUriWithMovieId(mMovieModel!!.id), null, null, null,
                    MovieContract.MovieEntry._ID + " ASC")

            if (cursor != null) {
                cursor.close()
                return cursor.count > 0
            } else {
                return false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val orientation = resources.configuration.orientation
        setToolbarViewParams(orientation)
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLinearLayoutWeight(moviePartDetailLayout!!, 2f)
        } else {
            setLinearLayoutWeight(moviePartDetailLayout!!, 1f)
        }

        val retrofit = Retrofit.Builder().baseUrl(BuildConfig.MOVIE_DB_API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build()
        mDetailApiInterface = retrofit.create(DetailApiInterface::class.java)

        val intent = intent
        if (intent.hasExtra(getString(R.string.detail_key))) {
            mMovieModel = intent.getParcelableExtra(getString(R.string.detail_key))

            isFavorite = isFavoriteMovie
            renderView(mMovieModel!!)
            configureRecyclerView(reviewRecyclerView!!)

            if (savedInstanceState != null) {
                mTrailerResultModels = savedInstanceState.getParcelableArrayList(getString(R.string.trailer_key))
                configureViewPagerWithTabLayout(mTrailerResultModels)

                mReviewResultModels = savedInstanceState.getParcelableArrayList(getString(R.string.review_key))
                reviewPage = savedInstanceState.getInt(getString(R.string.review_page_key))
            } else {
                callTrailers(mMovieModel!!.id)
                callReviews(mMovieModel!!.id, 1, true)
            }

            mReviewAdapter = ReviewAdapter(this@DetailActivity, mReviewResultModels)
            reviewRecyclerView!!.adapter = mReviewAdapter
        } else {
            Toast.makeText(this, getString(R.string.failed_show_detail), Toast.LENGTH_LONG).show()
        }

        addFavoriteButton!!.setOnClickListener {
            if (isFavorite) {
                deleteFromDatabase()
                Toast.makeText(this@DetailActivity, getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show()
                addFavoriteButton!!.text = getString(R.string.add_favorite)
            } else {
                insertToDatabase()
                Toast.makeText(this@DetailActivity, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show()
                addFavoriteButton!!.text = getString(R.string.remove_favorite)
            }
        }
    }

    private fun insertToDatabase() {
        val detailContentResolver = contentResolver

        val movieValues = ContentValues()
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, mMovieModel!!.id)
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_NAME, mMovieModel!!.originalTitle)
        movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, mMovieModel!!.posterPath)
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, mMovieModel!!.overview)
        movieValues.put(MovieContract.MovieEntry.COLUMN_USER_RATING, mMovieModel!!.voteAverage)
        movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, mMovieModel!!.releaseDate)

        detailContentResolver.insert(MovieContract.MovieEntry.CONTENT_URI, movieValues)
    }

    private fun deleteFromDatabase() {
        val detailContentResolver = contentResolver
        val selectionArguments = arrayOf(mMovieModel!!.id.toString())
        detailContentResolver.delete(MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ? ", selectionArguments)
    }

    private fun renderView(movieModel: MovieModel) {
        collapsingToolbar!!.title = movieModel.originalTitle
        collapsingToolbar!!.setCollapsedTitleTextColor(Color.WHITE)
        collapsingToolbar!!.setExpandedTitleColor(Color.TRANSPARENT)

        nameTextView!!.text = movieModel.originalTitle

        val posterPath = BuildConfig.MOVIE_DB_POSTER_URL + movieModel.posterPath
        Picasso.with(this)
                .load(posterPath)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(posterImageView)

        val releaseDate = getFormattedDate(movieModel.releaseDate, getString(R.string.movie_db_date_format),
                getString(R.string.date_format))
        releaseDateTextView!!.text = releaseDate

        val rating = movieModel.voteAverage.toString() + getString(R.string.max_rating)
        ratingTextView!!.text = rating

        if (isFavorite)
            addFavoriteButton!!.text = getString(R.string.remove_favorite)
        else
            addFavoriteButton!!.text = getString(R.string.add_favorite)

        overviewTextView!!.text = movieModel.overview
    }

    private fun configureRecyclerView(recyclerView: RecyclerView) {
        val layoutManager = LinearLayoutManager(this@DetailActivity)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false

        mScrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                if (reviewPage <= totalReviewPages) {
                    callReviews(mMovieModel!!.id, reviewPage, false)
                }
            }
        }
        recyclerView.addOnScrollListener(mScrollListener!!)
    }

    private fun configureViewPagerWithTabLayout(results: ArrayList<TrailerResultModel>?) {
        trailerViewPager!!.adapter = TrailerViewPagerAdapter(supportFragmentManager, results)
        trailerTabLayout!!.setupWithViewPager(trailerViewPager, true)
    }

    private fun callTrailers(id: Int) {
        showTrailerLoading()

        val trailers = mDetailApiInterface!!.getTrailers(id, BuildConfig.MOVIE_DB_API_KEY)
        trailers.enqueue(object : Callback<TrailerModel> {
            override fun onResponse(call: Call<TrailerModel>, response: Response<TrailerModel>) {
                if (response.body() != null) {
                    val results = response.body()!!.results
                    if (results != null && !results.isEmpty()) {
                        mTrailerResultModels!!.clear()

                        for (result in results) {
                            if (result.site == getString(R.string.youtube)) {
                                mTrailerResultModels!!.add(result)
                            }
                        }

                        configureViewPagerWithTabLayout(mTrailerResultModels)
                        hideTrailerLoading()
                    } else {
                        hideAllTrailerLayout()
                    }
                } else {
                    Toast.makeText(this@DetailActivity, R.string.failed_fetch_trailers, Toast.LENGTH_SHORT).show()
                    hideAllTrailerLayout()
                }
            }

            override fun onFailure(call: Call<TrailerModel>, t: Throwable) {
                Log.e(TAG, getString(R.string.failed_fetch_trailers), t.cause)
                Toast.makeText(this@DetailActivity, R.string.failed_fetch_trailers, Toast.LENGTH_SHORT).show()
                hideAllTrailerLayout()
            }
        })
    }

    private fun callReviews(id: Int, page: Int, clearList: Boolean) {
        if (clearList) showReviewLoading()

        val reviews = mDetailApiInterface!!.getReviews(id, BuildConfig.MOVIE_DB_API_KEY, page)
        reviews.enqueue(object : Callback<ReviewModel> {
            override fun onResponse(call: Call<ReviewModel>, response: Response<ReviewModel>) {
                if (response.body() != null) {
                    totalReviewPages = response.body()!!.totalPages
                    val results = response.body()!!.results
                    if (results != null && !results.isEmpty()) {
                        if (clearList)
                            mReviewResultModels!!.clear()
                        else
                            reviewPage++

                        mReviewResultModels!!.addAll(results)
                        mReviewAdapter!!.notifyDataSetChanged()

                        mScrollListener!!.resetState()
                        hideReviewLoading()
                    } else {
                        showReviewEmptyMessage()
                    }
                } else {
                    Toast.makeText(this@DetailActivity, R.string.failed_fetch_reviews, Toast.LENGTH_SHORT).show()
                    showReviewEmptyMessage()
                }
            }

            override fun onFailure(call: Call<ReviewModel>, t: Throwable) {
                Log.e(TAG, getString(R.string.failed_fetch_reviews), t.cause)
                Toast.makeText(this@DetailActivity, R.string.failed_fetch_reviews, Toast.LENGTH_SHORT).show()
                hideReviewLoading()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(getString(R.string.trailer_key), mTrailerResultModels)
        outState.putParcelableArrayList(getString(R.string.review_key), mReviewResultModels)
        outState.putInt(getString(R.string.review_page_key), reviewPage)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getFormattedDate(date: String, currentFormat: String, targetFormat: String): String? {
        val simpleDateFormat = SimpleDateFormat(currentFormat, Locale.ENGLISH)
        try {
            val tempDate = simpleDateFormat.parse(date)
            val dateFormat = SimpleDateFormat(targetFormat, Locale.ENGLISH)
            return dateFormat.format(tempDate)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return null
    }

    private fun showTrailerLoading() {
        trailerProgressBar!!.visibility = View.VISIBLE
        trailerViewPager!!.visibility = View.INVISIBLE
        trailerTabLayout!!.visibility = View.INVISIBLE
    }

    private fun hideTrailerLoading() {
        trailerProgressBar!!.visibility = View.GONE
        trailerViewPager!!.visibility = View.VISIBLE
        trailerTabLayout!!.visibility = View.VISIBLE
    }

    private fun hideAllTrailerLayout() {
        trailerProgressBar!!.visibility = View.GONE
        trailerViewPager!!.visibility = View.INVISIBLE
        trailerTabLayout!!.visibility = View.INVISIBLE
    }

    private fun showReviewLoading() {
        reviewRecyclerView!!.visibility = View.INVISIBLE
        reviewProgressBar!!.visibility = View.VISIBLE
        reviewEmptyTextView!!.visibility = View.GONE
    }

    private fun hideReviewLoading() {
        reviewRecyclerView!!.visibility = View.VISIBLE
        reviewProgressBar!!.visibility = View.GONE
        reviewEmptyTextView!!.visibility = View.GONE
    }

    private fun showReviewEmptyMessage() {
        reviewRecyclerView!!.visibility = View.INVISIBLE
        reviewProgressBar!!.visibility = View.GONE
        reviewEmptyTextView!!.visibility = View.VISIBLE
    }

    private fun setLinearLayoutWeight(linearLayout: LinearLayout, weight: Float) {
        val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.weight = weight
        linearLayout.layoutParams = layoutParams
    }

    private fun setToolbarViewParams(orientation: Int) {
        val displaymetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displaymetrics)
        val width = displaymetrics.widthPixels
        val height = displaymetrics.heightPixels

        val viewPagerHeight: Int
        val tabLayoutMargin: Int
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewPagerHeight = width * 4 / 9
        } else {
            viewPagerHeight = height * 4 / 9
        }
        tabLayoutMargin = width / 3

        val layoutParamsForViewPager = CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, viewPagerHeight)
        trailerViewPager!!.layoutParams = layoutParamsForViewPager

        val layoutParamsForTabLayout = CollapsingToolbarLayout.LayoutParams(CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT, CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT)
        layoutParamsForTabLayout.rightMargin = tabLayoutMargin
        layoutParamsForTabLayout.leftMargin = tabLayoutMargin
        layoutParamsForTabLayout.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        trailerTabLayout!!.layoutParams = layoutParamsForTabLayout
    }

    internal interface DetailApiInterface {
        @GET("movie/{id}/reviews")
        fun getReviews(@Path("id") id: Int, @Query("api_key") apiKey: String, @Query("page") page: Int): Call<ReviewModel>

        @GET("movie/{id}/videos")
        fun getTrailers(@Path("id") id: Int, @Query("api_key") apiKey: String): Call<TrailerModel>
    }

    private inner class TrailerViewPagerAdapter internal constructor(manager: FragmentManager, private val results: ArrayList<TrailerResultModel>?) : FragmentPagerAdapter(manager) {

        override fun getItem(position: Int): Fragment {
            return TrailerFragment.newInstance(this@DetailActivity, results!![position].key)
        }

        override fun getCount(): Int {
            return results?.size ?: 0
        }
    }

    companion object {
        private val TAG = DetailActivity::class.java.simpleName
        private var totalReviewPages: Int = 0
    }
}