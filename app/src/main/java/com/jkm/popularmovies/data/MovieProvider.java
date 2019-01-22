package com.jkm.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;

import com.jkm.popularmovies.R;

public class MovieProvider extends ContentProvider {
    public static final int CODE_MOVIE_FAVORITES = 100;
    public static final int CODE_MOVIE_WITH_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private MovieDbHelper mOpenHelper;

    private Context context;

    public static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, MovieContract.PATH_MOVIE, CODE_MOVIE_FAVORITES);
        matcher.addURI(authority, MovieContract.PATH_MOVIE + "/#", CODE_MOVIE_WITH_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        context = getContext();
        return true;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE_FAVORITES:
                db.beginTransaction();
                int rowsInserted = 0;
                try {
                    for (ContentValues value : values) {
                        long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, value);
                        if (_id != -1) {
                            rowsInserted++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }

                if (rowsInserted > 0) {
                    context.getContentResolver().notifyChange(uri, null);
                }
                return rowsInserted;
            default:
                return super.bulkInsert(uri, values);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE_FAVORITES:
                Uri resultUri = null;
                long id = mOpenHelper.getWritableDatabase().insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                if (id != -1) {
                    resultUri = MovieContract.MovieEntry.buildMovieUriWithMovieId(values.getAsInteger(MovieContract.MovieEntry.COLUMN_MOVIE_ID));
                }
                if (resultUri != null) {
                    context.getContentResolver().notifyChange(resultUri, null);
                }
                return resultUri;
            default:
                throw new UnsupportedOperationException(context.getString(R.string.content_provider_unknown_uri) + uri);
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE_WITH_ID: {
                String movieId= uri.getLastPathSegment();
                String[] selectionArguments = new String[]{movieId};

                cursor = mOpenHelper.getReadableDatabase().query(MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ? ",
                        selectionArguments,
                        null,
                        null,
                        sortOrder);
                break;
            }
            case CODE_MOVIE_FAVORITES: {
                cursor = mOpenHelper.getReadableDatabase().query(MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException(context.getString(R.string.content_provider_unknown_uri) + uri);
        }

        cursor.setNotificationUri(context.getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int numRowsDeleted;
        if (null == selection) selection = "1";

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIE_FAVORITES:
                numRowsDeleted = mOpenHelper.getWritableDatabase().delete(MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException(context.getString(R.string.content_provider_unknown_uri) + uri);
        }

        if (numRowsDeleted != 0) {
            context.getContentResolver().notifyChange(uri, null);
        }
        return numRowsDeleted;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new RuntimeException(context.getString(R.string.content_provider_not_implemented));
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new RuntimeException(context.getString(R.string.content_provider_not_implemented));
    }
}