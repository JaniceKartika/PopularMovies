package com.jkm.popularmovies;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class MainModel {
    @SerializedName("page")
    private int page;
    @SerializedName("total_results")
    private int totalResults;
    @SerializedName("total_pages")
    private int totalPages;
    @SerializedName("results")
    private ArrayList<MovieModel> mResults;

    public MainModel() {
        // Constructor
    }

    public int getPage() {
        return page;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public ArrayList<MovieModel> getResults() {
        return mResults;
    }

    public void setResults(ArrayList<MovieModel> results) {
        mResults = results;
    }
}