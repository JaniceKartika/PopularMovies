package com.jkm.popularmovies.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class TrailerModel {
    @SerializedName("id")
    private int id;
    @SerializedName("results")
    private ArrayList<TrailerResultModel> results;

    public TrailerModel() {
        // Constructor
    }

    public int getId() {
        return id;
    }

    public ArrayList<TrailerResultModel> getResults() {
        return results;
    }

    public void setResults(ArrayList<TrailerResultModel> results) {
        this.results = results;
    }
}