package com.jkm.popularmovies;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity implements MovieAdapter.ItemClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.rv_movie_list)
    RecyclerView mMovieRecyclerView;
    @BindView(R.id.pb_movie_list)
    ProgressBar mMovieProgressBar;

    private MovieAdapter mAdapter;
    private ArrayList<MovieModel> mMovieModels = new ArrayList<>();
    private ArrayList<MovieModel> mPopularMovieModels = new ArrayList<>();
    private ArrayList<MovieModel> mTopRatedMovieModels = new ArrayList<>();

    private MovieApiInterface mApiInterface;

    private boolean isSortPopular = true;
    private boolean disableMenu;
    private int popularPage = 2, topRatedPage = 2;
    private static int totalPopularPage, totalTopRatedPage;

    private EndlessRecyclerViewScrollListener mScrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configureRecyclerView(mMovieRecyclerView, 3);
        } else {
            configureRecyclerView(mMovieRecyclerView, 2);
        }

        Retrofit retrofit = new Retrofit.Builder().baseUrl(BuildConfig.MOVIE_DB_API_URL)
                .addConverterFactory(GsonConverterFactory.create()).build();
        mApiInterface = retrofit.create(MovieApiInterface.class);

        if (savedInstanceState != null) {
            isSortPopular = savedInstanceState.getBoolean(getString(R.string.sort_key));
            mPopularMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.popular_movies_key));
            mTopRatedMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.top_rated_movies_key));
            popularPage = savedInstanceState.getInt(getString(R.string.popular_page_key));
            topRatedPage = savedInstanceState.getInt(getString(R.string.top_rated_page_key));

            if (isSortPopular) {
                mMovieModels.addAll(mPopularMovieModels);
            } else {
                if (mTopRatedMovieModels != null) {
                    mMovieModels.addAll(mTopRatedMovieModels);
                }
            }
        } else {
            callPopularMovies(1, true);
        }

        mAdapter = new MovieAdapter(this, mMovieModels, this);
        mMovieRecyclerView.setAdapter(mAdapter);
    }

    private void callPopularMovies(int page, final boolean clearList) {
        if (clearList) showLoading();

        Call<MainModel> popularMovies = mApiInterface.getPopularMovies(BuildConfig.MOVIE_DB_API_KEY, page);
        popularMovies.enqueue(new Callback<MainModel>() {
            @Override
            public void onResponse(Call<MainModel> call, Response<MainModel> response) {
                if (response.body() != null) {
                    totalPopularPage = response.body().getTotalPages();
                    ArrayList<MovieModel> results = response.body().getResults();
                    if (results != null && !results.isEmpty()) {
                        if (clearList) mPopularMovieModels.clear();
                        else popularPage++;
                        mPopularMovieModels.addAll(results);

                        mMovieModels.clear();
                        mMovieModels.addAll(mPopularMovieModels);
                        mAdapter.notifyDataSetChanged();
                        mScrollListener.resetState();

                        isSortPopular = true;
                    } else {
                        Toast.makeText(MainActivity.this, R.string.no_results, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.failed_fetch_popular, Toast.LENGTH_SHORT).show();
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<MainModel> call, Throwable t) {
                Log.e(TAG, getString(R.string.failed_fetch_popular), t.getCause());
                Toast.makeText(MainActivity.this, R.string.failed_fetch_popular, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    private void callTopRatedMovies(int page, final boolean clearList) {
        if (clearList) showLoading();

        Call<MainModel> topRatedMovies = mApiInterface.getTopRatedMovies(BuildConfig.MOVIE_DB_API_KEY, page);
        topRatedMovies.enqueue(new Callback<MainModel>() {
            @Override
            public void onResponse(Call<MainModel> call, Response<MainModel> response) {
                if (response.body() != null) {
                    totalTopRatedPage = response.body().getTotalPages();
                    ArrayList<MovieModel> results = response.body().getResults();
                    if (results != null && !results.isEmpty()) {
                        if (clearList) mTopRatedMovieModels.clear();
                        else topRatedPage++;
                        mTopRatedMovieModels.addAll(results);

                        mMovieModels.clear();
                        mMovieModels.addAll(mTopRatedMovieModels);
                        mAdapter.notifyDataSetChanged();
                        mScrollListener.resetState();

                        isSortPopular = false;
                    } else {
                        Toast.makeText(MainActivity.this, R.string.no_results, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, R.string.failed_fetch_top_rated, Toast.LENGTH_SHORT).show();
                }
                hideLoading();
            }

            @Override
            public void onFailure(Call<MainModel> call, Throwable t) {
                Log.e(TAG, getString(R.string.failed_fetch_top_rated), t.getCause());
                Toast.makeText(MainActivity.this, R.string.failed_fetch_top_rated, Toast.LENGTH_SHORT).show();
                hideLoading();
            }
        });
    }

    @Override
    public void setOnItemClickListener(View view, int position) {
        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
        intent.putExtra(getString(R.string.detail_key), isSortPopular ?
                mPopularMovieModels.get(position) : mTopRatedMovieModels.get(position));
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        menu.findItem(R.id.action_sort).setEnabled(!disableMenu);
        if (isSortPopular) {
            menu.findItem(R.id.action_sort).setTitle(R.string.top_rated);
        } else {
            menu.findItem(R.id.action_sort).setTitle(R.string.popular);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            if (isSortPopular) {
                if (mTopRatedMovieModels.size() > 0) {
                    mMovieModels.clear();
                    mMovieModels.addAll(mTopRatedMovieModels);
                    mAdapter.notifyDataSetChanged();
                    mScrollListener.resetState();

                    isSortPopular = false;
                    invalidateOptionsMenu();
                } else {
                    callTopRatedMovies(1, true);
                }
            } else {
                if (mPopularMovieModels.size() > 0) {
                    mMovieModels.clear();
                    mMovieModels.addAll(mPopularMovieModels);
                    mAdapter.notifyDataSetChanged();
                    mScrollListener.resetState();

                    isSortPopular = true;
                    invalidateOptionsMenu();
                } else {
                    callPopularMovies(1, true);
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(getString(R.string.sort_key), isSortPopular);
        outState.putParcelableArrayList(getString(R.string.popular_movies_key), mPopularMovieModels);
        outState.putParcelableArrayList(getString(R.string.top_rated_movies_key), mTopRatedMovieModels);
        outState.putInt(getString(R.string.popular_page_key), popularPage);
        outState.putInt(getString(R.string.top_rated_page_key), topRatedPage);
    }

    private void configureRecyclerView(RecyclerView recyclerView, int spanCount) {
        GridLayoutManager layoutManager = new GridLayoutManager(MainActivity.this, spanCount);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(spanCount, 0, true));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mScrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (isSortPopular) {
                    if (popularPage <= totalPopularPage) {
                        callPopularMovies(popularPage, false);
                    }
                } else {
                    if (topRatedPage <= totalTopRatedPage) {
                        callTopRatedMovies(topRatedPage, false);
                    }
                }
            }
        };
        recyclerView.addOnScrollListener(mScrollListener);
    }

    private void showLoading() {
        mMovieRecyclerView.setVisibility(View.INVISIBLE);
        mMovieProgressBar.setVisibility(View.VISIBLE);

        disableMenu = true;
        invalidateOptionsMenu();
    }

    private void hideLoading() {
        mMovieRecyclerView.setVisibility(View.VISIBLE);
        mMovieProgressBar.setVisibility(View.GONE);

        disableMenu = false;
        invalidateOptionsMenu();
    }

    interface MovieApiInterface {
        @GET("movie/popular")
        Call<MainModel> getPopularMovies(@Query("api_key") String apiKey, @Query("page") int page);

        @GET("movie/top_rated")
        Call<MainModel> getTopRatedMovies(@Query("api_key") String apiKey, @Query("page") int page);
    }
}