package com.jkm.popularmovies;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TrailerFragment extends Fragment {
    @BindView(R.id.iv_thumbnail_trailer)
    ImageView thumbnailImageView;
    @BindView(R.id.iv_play_trailer)
    ImageView playTrailerImageView;

    public TrailerFragment() {
        // Constructor
    }

    public static TrailerFragment newInstance(Context context, String key) {
        TrailerFragment fragment = new TrailerFragment();

        Bundle bundle = new Bundle();
        bundle.putString(context.getString(R.string.trailer_link_key), key);
        fragment.setArguments(bundle);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trailer, container, false);
        ButterKnife.bind(this, view);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            final String key = bundle.getString(getString(R.string.trailer_link_key));

            String thumbnailPath = BuildConfig.YOUTUBE_THUMBNAIL_URL + key + getString(R.string.youtube_thumbnail_suffix_link);
            Picasso.with(getContext())
                    .load(thumbnailPath)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(thumbnailImageView);

            playTrailerImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playYoutubeVideo(key);
                }
            });
        } else {
            thumbnailImageView.setVisibility(View.INVISIBLE);
            playTrailerImageView.setVisibility(View.INVISIBLE);
        }

        return view;
    }

    private void playYoutubeVideo(String key) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.youtube_app) + key));
        Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUTUBE_VIDEO_URL + key));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException e) {
            startActivity(webIntent);
        }
    }
}