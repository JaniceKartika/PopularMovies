package com.jkm.popularmovies;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jkm.popularmovies.data.MovieContract;
import com.jkm.popularmovies.model.MovieModel;
import com.jkm.popularmovies.model.ReviewModel;
import com.jkm.popularmovies.model.ReviewResultModel;
import com.jkm.popularmovies.model.TrailerModel;
import com.jkm.popularmovies.model.TrailerResultModel;
import com.jkm.popularmovies.util.EndlessRecyclerViewScrollListener;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = DetailActivity.class.getSimpleName();

    @BindView(R.id.tv_name_detail)
    TextView nameTextView;
    @BindView(R.id.iv_poster_detail)
    ImageView posterImageView;
    @BindView(R.id.tv_release_date_detail)
    TextView releaseDateTextView;
    @BindView(R.id.tv_rating_detail)
    TextView ratingTextView;
    @BindView(R.id.bt_add_favorite)
    Button addFavoriteButton;
    @BindView(R.id.tv_overview_detail)
    TextView overviewTextView;
    @BindView(R.id.layout_movie_part_detail)
    LinearLayout moviePartDetailLayout;

    @BindView(R.id.collapsing_toolbar_detail)
    CollapsingToolbarLayout collapsingToolbar;
    @BindView(R.id.tab_trailer_detail)
    TabLayout trailerTabLayout;
    @BindView(R.id.view_pager_trailer_detail)
    ViewPager trailerViewPager;
    @BindView(R.id.pb_trailer_detail)
    ProgressBar trailerProgressBar;
    @BindView(R.id.toolbar_detail)
    Toolbar toolbar;

    @BindView(R.id.rv_review_detail)
    RecyclerView reviewRecyclerView;
    @BindView(R.id.tv_review_empty)
    TextView reviewEmptyTextView;
    @BindView(R.id.pb_review_detail)
    ProgressBar reviewProgressBar;

    private ReviewAdapter mReviewAdapter;
    private MovieModel mMovieModel;
    private ArrayList<ReviewResultModel> mReviewResultModels = new ArrayList<>();
    private ArrayList<TrailerResultModel> mTrailerResultModels = new ArrayList<>();

    private DetailApiInterface mDetailApiInterface;

    private int reviewPage = 2;
    private static int totalReviewPages;
    private boolean isFavorite = false;

    private EndlessRecyclerViewScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        int orientation = getResources().getConfiguration().orientation;
        setToolbarViewParams(orientation);
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLinearLayoutWeight(moviePartDetailLayout, 2);
        } else {
            setLinearLayoutWeight(moviePartDetailLayout, 1);
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl(BuildConfig.MOVIE_DB_API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
        mDetailApiInterface = retrofit.create(DetailApiInterface.class);

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.detail_key))) {
            mMovieModel = intent.getParcelableExtra(getString(R.string.detail_key));

            isFavorite = isFavoriteMovie();
            renderView(mMovieModel);
            configureRecyclerView(reviewRecyclerView);

            if (savedInstanceState != null) {
                mTrailerResultModels = savedInstanceState.getParcelableArrayList(getString(R.string.trailer_key));
                configureViewPagerWithTabLayout(mTrailerResultModels);

                mReviewResultModels = savedInstanceState.getParcelableArrayList(getString(R.string.review_key));
                reviewPage = savedInstanceState.getInt(getString(R.string.review_page_key));
            } else {
                callTrailers(mMovieModel.getId());
                callReviews(mMovieModel.getId(), 1, true);
            }

            mReviewAdapter = new ReviewAdapter(DetailActivity.this, mReviewResultModels);
            reviewRecyclerView.setAdapter(mReviewAdapter);
        } else {
            Toast.makeText(this, getString(R.string.failed_show_detail), Toast.LENGTH_LONG).show();
        }

        addFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFavorite) {
                    deleteFromDatabase();
                    Toast.makeText(DetailActivity.this, getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show();
                    addFavoriteButton.setText(getString(R.string.add_favorite));
                } else {
                    insertToDatabase();
                    Toast.makeText(DetailActivity.this, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show();
                    addFavoriteButton.setText(getString(R.string.remove_favorite));
                }
            }
        });
    }

    private boolean isFavoriteMovie() {
        ContentResolver detailContentResolver = getContentResolver();
        Cursor cursor = detailContentResolver.query(
                MovieContract.MovieEntry.buildMovieUriWithMovieId(mMovieModel.getId()),
                null,
                null,
                null,
                MovieContract.MovieEntry._ID + " ASC");

        if (cursor != null) {
            cursor.close();
            return cursor.getCount() > 0;
        } else {
            return false;
        }
    }

    private void insertToDatabase() {
        ContentResolver detailContentResolver = getContentResolver();

        ContentValues movieValues = new ContentValues();
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, mMovieModel.getId());
        movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_NAME, mMovieModel.getOriginalTitle());
        movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, mMovieModel.getPosterPath());
        movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, mMovieModel.getOverview());
        movieValues.put(MovieContract.MovieEntry.COLUMN_USER_RATING, mMovieModel.getVoteAverage());
        movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, mMovieModel.getReleaseDate());

        detailContentResolver.insert(MovieContract.MovieEntry.CONTENT_URI, movieValues);
    }

    private void deleteFromDatabase() {
        ContentResolver detailContentResolver = getContentResolver();
        String[] selectionArguments = new String[]{String.valueOf(mMovieModel.getId())};
        detailContentResolver.delete(MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ? ", selectionArguments);
    }

    private void renderView(MovieModel movieModel) {
        collapsingToolbar.setTitle(movieModel.getOriginalTitle());
        collapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
        collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT);

        nameTextView.setText(movieModel.getOriginalTitle());

        String posterPath = BuildConfig.MOVIE_DB_POSTER_URL + movieModel.getPosterPath();
        Picasso.with(this)
                .load(posterPath)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .into(posterImageView);

        String releaseDate = getFormattedDate(movieModel.getReleaseDate(), getString(R.string.movie_db_date_format),
                getString(R.string.date_format));
        releaseDateTextView.setText(releaseDate);

        String rating = String.valueOf(movieModel.getVoteAverage()) + getString(R.string.max_rating);
        ratingTextView.setText(rating);

        if (isFavorite) addFavoriteButton.setText(getString(R.string.remove_favorite));
        else addFavoriteButton.setText(getString(R.string.add_favorite));

        overviewTextView.setText(movieModel.getOverview());
    }

    private void configureRecyclerView(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = new LinearLayoutManager(DetailActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);

        mScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (reviewPage <= totalReviewPages) {
                    callReviews(mMovieModel.getId(), reviewPage, false);
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    private void configureViewPagerWithTabLayout(ArrayList<TrailerResultModel> results) {
        trailerViewPager.setAdapter(new TrailerViewPagerAdapter(getSupportFragmentManager(), results));
        trailerTabLayout.setupWithViewPager(trailerViewPager, true);
    }

    private void callTrailers(int id) {
        showTrailerLoading();

        Call<TrailerModel> trailers = mDetailApiInterface.getTrailers(id, BuildConfig.MOVIE_DB_API_KEY);
        trailers.enqueue(new Callback<TrailerModel>() {
            @Override
            public void onResponse(Call<TrailerModel> call, Response<TrailerModel> response) {
                if (response.body() != null) {
                    ArrayList<TrailerResultModel> results = response.body().getResults();
                    if (results != null && !results.isEmpty()) {
                        mTrailerResultModels.clear();

                        for (TrailerResultModel result : results) {
                            if (result.getSite().equals(getString(R.string.youtube))) {
                                mTrailerResultModels.add(result);
                            }
                        }

                        configureViewPagerWithTabLayout(mTrailerResultModels);
                        hideTrailerLoading();
                    } else {
                        hideAllTrailerLayout();
                    }
                } else {
                    Toast.makeText(DetailActivity.this, R.string.failed_fetch_trailers, Toast.LENGTH_SHORT).show();
                    hideAllTrailerLayout();
                }
            }

            @Override
            public void onFailure(Call<TrailerModel> call, Throwable t) {
                Log.e(TAG, getString(R.string.failed_fetch_trailers), t.getCause());
                Toast.makeText(DetailActivity.this, R.string.failed_fetch_trailers, Toast.LENGTH_SHORT).show();
                hideAllTrailerLayout();
            }
        });
    }

    private void callReviews(int id, int page, final boolean clearList) {
        if (clearList) showReviewLoading();

        Call<ReviewModel> reviews = mDetailApiInterface.getReviews(id, BuildConfig.MOVIE_DB_API_KEY, page);
        reviews.enqueue(new Callback<ReviewModel>() {
            @Override
            public void onResponse(Call<ReviewModel> call, Response<ReviewModel> response) {
                if (response.body() != null) {
                    totalReviewPages = response.body().getTotalPages();
                    ArrayList<ReviewResultModel> results = response.body().getResults();
                    if (results != null && !results.isEmpty()) {
                        if (clearList) mReviewResultModels.clear();
                        else reviewPage++;

                        mReviewResultModels.addAll(results);
                        mReviewAdapter.notifyDataSetChanged();

                        mScrollListener.resetState();
                        hideReviewLoading();
                    } else {
                        showReviewEmptyMessage();
                    }
                } else {
                    Toast.makeText(DetailActivity.this, R.string.failed_fetch_reviews, Toast.LENGTH_SHORT).show();
                    showReviewEmptyMessage();
                }
            }

            @Override
            public void onFailure(Call<ReviewModel> call, Throwable t) {
                Log.e(TAG, getString(R.string.failed_fetch_reviews), t.getCause());
                Toast.makeText(DetailActivity.this, R.string.failed_fetch_reviews, Toast.LENGTH_SHORT).show();
                hideReviewLoading();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(getString(R.string.trailer_key), mTrailerResultModels);
        outState.putParcelableArrayList(getString(R.string.review_key), mReviewResultModels);
        outState.putInt(getString(R.string.review_page_key), reviewPage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private String getFormattedDate(String date, String currentFormat, String targetFormat) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(currentFormat, Locale.ENGLISH);
        try {
            Date tempDate = simpleDateFormat.parse(date);
            SimpleDateFormat dateFormat = new SimpleDateFormat(targetFormat, Locale.ENGLISH);
            return dateFormat.format(tempDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showTrailerLoading() {
        trailerProgressBar.setVisibility(View.VISIBLE);
        trailerViewPager.setVisibility(View.INVISIBLE);
        trailerTabLayout.setVisibility(View.INVISIBLE);
    }

    private void hideTrailerLoading() {
        trailerProgressBar.setVisibility(View.GONE);
        trailerViewPager.setVisibility(View.VISIBLE);
        trailerTabLayout.setVisibility(View.VISIBLE);
    }

    private void hideAllTrailerLayout() {
        trailerProgressBar.setVisibility(View.GONE);
        trailerViewPager.setVisibility(View.INVISIBLE);
        trailerTabLayout.setVisibility(View.INVISIBLE);
    }

    private void showReviewLoading() {
        reviewRecyclerView.setVisibility(View.INVISIBLE);
        reviewProgressBar.setVisibility(View.VISIBLE);
        reviewEmptyTextView.setVisibility(View.GONE);
    }

    private void hideReviewLoading() {
        reviewRecyclerView.setVisibility(View.VISIBLE);
        reviewProgressBar.setVisibility(View.GONE);
        reviewEmptyTextView.setVisibility(View.GONE);
    }

    private void showReviewEmptyMessage() {
        reviewRecyclerView.setVisibility(View.INVISIBLE);
        reviewProgressBar.setVisibility(View.GONE);
        reviewEmptyTextView.setVisibility(View.VISIBLE);
    }

    private void setLinearLayoutWeight(LinearLayout linearLayout, float weight) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = weight;
        linearLayout.setLayoutParams(layoutParams);
    }

    private void setToolbarViewParams(int orientation) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int width = displaymetrics.widthPixels;
        int height = displaymetrics.heightPixels;

        int viewPagerHeight, tabLayoutMargin;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            viewPagerHeight = (width * 4) / 9;
        } else {
            viewPagerHeight = (height * 4 / 9);
        }
        tabLayoutMargin = width / 3;

        CollapsingToolbarLayout.LayoutParams layoutParamsForViewPager = new CollapsingToolbarLayout
                .LayoutParams(CollapsingToolbarLayout.LayoutParams.MATCH_PARENT, viewPagerHeight);
        trailerViewPager.setLayoutParams(layoutParamsForViewPager);

        CollapsingToolbarLayout.LayoutParams layoutParamsForTabLayout = new CollapsingToolbarLayout
                .LayoutParams(CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT, CollapsingToolbarLayout.LayoutParams.WRAP_CONTENT);
        layoutParamsForTabLayout.rightMargin = tabLayoutMargin;
        layoutParamsForTabLayout.leftMargin = tabLayoutMargin;
        layoutParamsForTabLayout.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        trailerTabLayout.setLayoutParams(layoutParamsForTabLayout);
    }

    interface DetailApiInterface {
        @GET("movie/{id}/reviews")
        Call<ReviewModel> getReviews(@Path("id") int id, @Query("api_key") String apiKey, @Query("page") int page);

        @GET("movie/{id}/videos")
        Call<TrailerModel> getTrailers(@Path("id") int id, @Query("api_key") String apiKey);
    }

    private class TrailerViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<TrailerResultModel> results;

        TrailerViewPagerAdapter(FragmentManager manager, ArrayList<TrailerResultModel> results) {
            super(manager);
            this.results = results;
        }

        @Override
        public Fragment getItem(int position) {
            return TrailerFragment.newInstance(DetailActivity.this, results.get(position).getKey());
        }

        @Override
        public int getCount() {
            if (results == null) return 0;
            else return results.size();
        }
    }
}