package com.jkm.popularmovies

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_trailer.*

class TrailerFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trailer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = this.arguments
        if (bundle != null) {
            val key = bundle.getString(getString(R.string.trailer_link_key))

            val thumbnailPath = BuildConfig.YOUTUBE_THUMBNAIL_URL + key + getString(R.string.youtube_thumbnail_suffix_link)
            Picasso.with(context)
                    .load(thumbnailPath)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .into(thumbnailImageView)

            playTrailerImageView!!.setOnClickListener { playYoutubeVideo(key) }
        } else {
            thumbnailImageView!!.visibility = View.INVISIBLE
            playTrailerImageView!!.visibility = View.INVISIBLE
        }
    }

    private fun playYoutubeVideo(key: String?) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.youtube_app) + key!!))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.YOUTUBE_VIDEO_URL + key))
        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            startActivity(webIntent)
        }

    }

    companion object {

        fun newInstance(context: Context, key: String): TrailerFragment {
            val fragment = TrailerFragment()

            val bundle = Bundle()
            bundle.putString(context.getString(R.string.trailer_link_key), key)
            fragment.arguments = bundle

            return fragment
        }
    }
}// Constructor