package com.jkm.popularmovies;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private Context mContext;
    private ArrayList<MovieModel> mMovieModels;
    private ItemClickListener itemClickListener;

    MovieAdapter(Context context, ArrayList<MovieModel> movieModels, ItemClickListener itemClickListener) {
        mContext = context;
        mMovieModels = movieModels;
        this.itemClickListener = itemClickListener;
    }

    @Override
    public MovieViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.poster_card, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MovieViewHolder holder, int position) {
        MovieModel movieModel = mMovieModels.get(position);

        holder.mNameTextView.setText(movieModel.getOriginalTitle());

        String year = movieModel.getReleaseDate().substring(0, 4);
        holder.mYearTextView.setText(year);

        String rating = String.valueOf(movieModel.getVoteAverage()) + mContext.getString(R.string.max_rating);
        holder.mRatingTextView.setText(rating);

        String posterPath = BuildConfig.MOVIE_DB_POSTER_URL + movieModel.getPosterPath();
        Picasso.with(mContext).load(posterPath).into(holder.mPosterImageView);
    }

    @Override
    public int getItemCount() {
        if (mMovieModels == null) return 0;
        else return mMovieModels.size();
    }

    interface ItemClickListener {
        void setOnItemClickListener(View view, int position);
    }

    class MovieViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView mPosterImageView;
        private TextView mNameTextView, mYearTextView, mRatingTextView;

        MovieViewHolder(View itemView) {
            super(itemView);
            mPosterImageView = (ImageView) itemView.findViewById(R.id.iv_movie_poster);
            mNameTextView = (TextView) itemView.findViewById(R.id.tv_movie_name);
            mYearTextView = (TextView) itemView.findViewById(R.id.tv_movie_year);
            mRatingTextView = (TextView) itemView.findViewById(R.id.tv_movie_rating);
            itemView.setTag(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemClickListener != null) {
                itemClickListener.setOnItemClickListener(view, getAdapterPosition());
            }
        }
    }
}