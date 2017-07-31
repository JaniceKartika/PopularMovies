package com.jkm.popularmovies;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jkm.popularmovies.data.MovieContract;
import com.jkm.popularmovies.model.MainModel;
import com.jkm.popularmovies.model.MovieModel;
import com.jkm.popularmovies.util.EndlessRecyclerViewScrollListener;
import com.jkm.popularmovies.util.GridSpacingItemDecoration;

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

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, MovieAdapter.ItemClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int SORT_FAVORITES = 1;
    private static final int SORT_POPULAR = 2;
    private static final int SORT_TOP_RATED = 3;

    public static final String[] MOVIE_PROJECTION = {
            MovieContract.MovieEntry.COLUMN_MOVIE_NAME,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
            MovieContract.MovieEntry.COLUMN_OVERVIEW,
            MovieContract.MovieEntry.COLUMN_USER_RATING,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE
    };
    public static final int INDEX_MOVIE_NAME = 0;
    public static final int INDEX_POSTER_PATH = 1;
    public static final int INDEX_OVERVIEW = 2;
    public static final int INDEX_USER_RATING = 3;
    public static final int INDEX_RELEASE_DATE = 4;

    private static final int ID_MOVIE_LOADER = 44;

    @BindView(R.id.rv_movie_list)
    RecyclerView mMovieRecyclerView;
    @BindView(R.id.tv_movie_empty)
    TextView mMovieEmptyTextView;
    @BindView(R.id.pb_movie_list)
    ProgressBar mMovieProgressBar;

    private MovieAdapter mAdapter;
    private ArrayList<MovieModel> mMovieModels = new ArrayList<>();
    private ArrayList<MovieModel> mFavoriteMovieModels = new ArrayList<>();
    private ArrayList<MovieModel> mPopularMovieModels = new ArrayList<>();
    private ArrayList<MovieModel> mTopRatedMovieModels = new ArrayList<>();

    private MovieApiInterface mApiInterface;

    private int sort = SORT_POPULAR;
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
            sort = savedInstanceState.getInt(getString(R.string.sort_key));
            mPopularMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.popular_movies_key));
            mTopRatedMovieModels = savedInstanceState.getParcelableArrayList(getString(R.string.top_rated_movies_key));
            popularPage = savedInstanceState.getInt(getString(R.string.popular_page_key));
            topRatedPage = savedInstanceState.getInt(getString(R.string.top_rated_page_key));

            if (sort == SORT_POPULAR) {
                mMovieModels.addAll(mPopularMovieModels);
            } else if (sort == SORT_TOP_RATED) {
                if (mTopRatedMovieModels != null) {
                    mMovieModels.addAll(mTopRatedMovieModels);
                }
            } else if (sort == SORT_FAVORITES) {
                getSupportLoaderManager().initLoader(ID_MOVIE_LOADER, null, this);
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

                        sort = SORT_POPULAR;
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

                        sort = SORT_TOP_RATED;
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
        if (sort == SORT_POPULAR) {
            intent.putExtra(getString(R.string.detail_key), mPopularMovieModels.get(position));
        } else if (sort == SORT_TOP_RATED) {
            intent.putExtra(getString(R.string.detail_key), mTopRatedMovieModels.get(position));
        } else if (sort == SORT_FAVORITES) {
            intent.putExtra(getString(R.string.detail_key), mFavoriteMovieModels.get(position));
        }
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        menu.findItem(R.id.action_favorites).setEnabled(!disableMenu);
        menu.findItem(R.id.action_popular).setEnabled(!disableMenu);
        menu.findItem(R.id.action_top_rated).setEnabled(!disableMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_favorites:
                if (sort != SORT_FAVORITES) {
                    getSupportLoaderManager().initLoader(ID_MOVIE_LOADER, null, this);
                }
                return true;
            case R.id.action_popular:
                if (mPopularMovieModels.size() > 0) {
                    if (sort != SORT_POPULAR) {
                        mMovieModels.clear();
                        mMovieModels.addAll(mPopularMovieModels);

                        mAdapter.notifyDataSetChanged();
                        mScrollListener.resetState();
                        sort = SORT_POPULAR;
                    }
                    hideLoading();
                } else {
                    callPopularMovies(1, true);
                }
                return true;
            case R.id.action_top_rated:
                if (mTopRatedMovieModels.size() > 0) {
                    if (sort != SORT_TOP_RATED) {
                        mMovieModels.clear();
                        mMovieModels.addAll(mTopRatedMovieModels);

                        mAdapter.notifyDataSetChanged();
                        mScrollListener.resetState();
                        sort = SORT_TOP_RATED;
                    }
                    hideLoading();
                } else {
                    callTopRatedMovies(1, true);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(getString(R.string.sort_key), sort);
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
                if (sort == SORT_POPULAR) {
                    if (popularPage <= totalPopularPage) {
                        callPopularMovies(popularPage, false);
                    }
                } else if (sort == SORT_TOP_RATED) {
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
        mMovieEmptyTextView.setVisibility(View.GONE);

        disableMenu = true;
        invalidateOptionsMenu();
    }

    private void hideLoading() {
        mMovieRecyclerView.setVisibility(View.VISIBLE);
        mMovieProgressBar.setVisibility(View.GONE);
        mMovieEmptyTextView.setVisibility(View.GONE);

        disableMenu = false;
        invalidateOptionsMenu();
    }

    private void showEmptyFavoritesMessage() {
        mMovieRecyclerView.setVisibility(View.INVISIBLE);
        mMovieProgressBar.setVisibility(View.GONE);
        mMovieEmptyTextView.setVisibility(View.VISIBLE);

        disableMenu = false;
        invalidateOptionsMenu();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        showLoading();
        switch (id) {
            case ID_MOVIE_LOADER:
                Uri movieQueryUri = MovieContract.MovieEntry.CONTENT_URI;
                String sortOrder = MovieContract.MovieEntry._ID + " ASC";

                return new CursorLoader(this, movieQueryUri, MOVIE_PROJECTION, null, null, sortOrder);
            default:
                throw new RuntimeException(getString(R.string.loader_not_implemented) + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.getCount() == 0) {
            showEmptyFavoritesMessage();
        } else {
            convertToMovieModel(data);
            mMovieModels.clear();
            mMovieModels.addAll(mFavoriteMovieModels);

            mAdapter.notifyDataSetChanged();
            hideLoading();
        }
        sort = SORT_FAVORITES;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMovieModels.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void convertToMovieModel(Cursor data) {
        mFavoriteMovieModels.clear();
        for (int i = 0; i < data.getCount(); i++) {
            data.moveToPosition(i);
            MovieModel model = new MovieModel();
            model.setOriginalTitle(data.getString(INDEX_MOVIE_NAME));
            model.setPosterPath(data.getString(INDEX_POSTER_PATH));
            model.setOverview(data.getString(INDEX_OVERVIEW));
            model.setVoteAverage(data.getDouble(INDEX_USER_RATING));
            model.setReleaseDate(data.getString(INDEX_RELEASE_DATE));
            mFavoriteMovieModels.add(model);
        }
    }

    interface MovieApiInterface {
        @GET("movie/popular")
        Call<MainModel> getPopularMovies(@Query("api_key") String apiKey, @Query("page") int page);

        @GET("movie/top_rated")
        Call<MainModel> getTopRatedMovies(@Query("api_key") String apiKey, @Query("page") int page);
    }
}