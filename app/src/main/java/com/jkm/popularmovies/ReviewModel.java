package com.jkm.popularmovies;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class ReviewModel {
    @SerializedName("id")
    private int id;
    @SerializedName("page")
    private int page;
    @SerializedName("results")
    private ArrayList<ReviewResultModel> results;
    @SerializedName("total_pages")
    private int totalPages;
    @SerializedName("total_results")
    private int totalResults;

    public ReviewModel() {
        // Constructor
    }

    public int getId() {
        return id;
    }

    public int getPage() {
        return page;
    }

    public ArrayList<ReviewResultModel> getResults() {
        return results;
    }

    public void setResults(ArrayList<ReviewResultModel> results) {
        this.results = results;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalResults() {
        return totalResults;
    }
}