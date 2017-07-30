package com.jkm.popularmovies;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    @BindView(R.id.tv_overview_detail)
    TextView overviewTextView;
    @BindView(R.id.layout_movie_part_detail)
    LinearLayout moviePartDetailLayout;

    @BindView(R.id.rv_review_detail)
    RecyclerView reviewRecyclerView;
    @BindView(R.id.tv_review_empty)
    TextView reviewEmptyTextView;
    @BindView(R.id.pb_review_detail)
    ProgressBar reviewProgressBar;

    private ReviewAdapter mReviewAdapter;
    private MovieModel mMovieModel;
    private ArrayList<ReviewResultModel> mReviewResultModels = new ArrayList<>();

    private DetailApiInterface mDetailApiInterface;

    private int reviewPage = 2;
    private static int totalReviewPages;

    private EndlessRecyclerViewScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        int orientation = getResources().getConfiguration().orientation;
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
            renderView(mMovieModel);
            configureRecyclerView(reviewRecyclerView);

            if (savedInstanceState != null) {
                mReviewResultModels = savedInstanceState.getParcelableArrayList(getString(R.string.review_key));
                reviewPage = savedInstanceState.getInt(getString(R.string.review_page_key));
            } else {
                callReviews(mMovieModel.getId(), 1, true);
            }

            mReviewAdapter = new ReviewAdapter(DetailActivity.this, mReviewResultModels);
            reviewRecyclerView.setAdapter(mReviewAdapter);
        } else {
            Toast.makeText(this, getString(R.string.failed_show_detail), Toast.LENGTH_LONG).show();
        }
    }

    private void renderView(MovieModel movieModel) {
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

                        mReviewResultModels.addAll(response.body().getResults());
                        mReviewAdapter.notifyDataSetChanged();

                        mScrollListener.resetState();
                        hideReviewLoading();
                    } else {
                        showReviewEmptyMessage();
                    }
                } else {
                    Toast.makeText(DetailActivity.this, R.string.failed_fetch_reviews, Toast.LENGTH_SHORT).show();
                    hideReviewLoading();
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
        outState.putParcelableArrayList(getString(R.string.review_key), mReviewResultModels);
        outState.putInt(getString(R.string.review_page_key), reviewPage);
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

    private void setLinearLayoutWeight(LinearLayout linearLayout, float weight) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = weight;
        linearLayout.setLayoutParams(layoutParams);
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

    interface DetailApiInterface {
        @GET("movie/{id}/reviews")
        Call<ReviewModel> getReviews(@Path("id") int id, @Query("api_key") String apiKey, @Query("page") int page);
    }
}