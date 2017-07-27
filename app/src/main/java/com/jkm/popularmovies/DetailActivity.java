package com.jkm.popularmovies;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailActivity extends AppCompatActivity {

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

    private MovieModel mMovieModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        if (intent.hasExtra(getString(R.string.detail_key))) {
            mMovieModel = intent.getParcelableExtra(getString(R.string.detail_key));
            nameTextView.setText(mMovieModel.getOriginalTitle());

            String posterPath = BuildConfig.MOVIE_DB_POSTER_URL + mMovieModel.getPosterPath();
            Picasso.with(this)
                    .load(posterPath)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(posterImageView);

            String releaseDate = getFormattedDate(mMovieModel.getReleaseDate(), getString(R.string.movie_db_date_format),
                    getString(R.string.date_format));
            releaseDateTextView.setText(releaseDate);

            String rating = String.valueOf(mMovieModel.getVoteAverage()) + getString(R.string.max_rating);
            ratingTextView.setText(rating);

            overviewTextView.setText(mMovieModel.getOverview());
        } else {
            Toast.makeText(this, getString(R.string.failed_show_detail), Toast.LENGTH_LONG).show();
        }
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
}