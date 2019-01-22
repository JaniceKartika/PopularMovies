package com.jkm.popularmovies;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jkm.popularmovies.model.ReviewResultModel;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private static final int CHARS_LIMIT = 250;

    private Context mContext;
    private ArrayList<ReviewResultModel> mReviewResultModels;

    ReviewAdapter(Context context, ArrayList<ReviewResultModel> reviewResultModels) {
        mContext = context;
        mReviewResultModels = reviewResultModels;
    }

    @Override
    public ReviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReviewViewHolder holder, int position) {
        ReviewResultModel reviewResultModel = mReviewResultModels.get(position);
        holder.mAuthorTextView.setText(reviewResultModel.getAuthor());

        String review = reviewResultModel.getContent();
        if (review.length() > CHARS_LIMIT) {
            setSpannableString(holder.mReviewTextView, review, true);
        } else {
            holder.mReviewTextView.setText(review);
        }
    }

    @Override
    public int getItemCount() {
        if (mReviewResultModels == null) return 0;
        else return mReviewResultModels.size();
    }

    private void setSpannableString(final TextView textView, final String text, final boolean isMore) {
        SpannableString spannableString;
        if (isMore) {
            String subText = text.substring(0, CHARS_LIMIT) + "...";
            spannableString = new SpannableString(subText + " " + mContext.getString(R.string.read_more));
        } else {
            spannableString = new SpannableString(text + " " + mContext.getString(R.string.read_less));
        }

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                setSpannableString(textView, text, !isMore);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(ds.linkColor);
                ds.setUnderlineText(false);
            }
        };

        spannableString.setSpan(clickableSpan, spannableString.length() - 9, spannableString.length(), 0);
        textView.setText(spannableString);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.tv_author_review)
        TextView mAuthorTextView;
        @BindView(R2.id.tv_review)
        TextView mReviewTextView;

        ReviewViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setTag(itemView);
        }
    }
}