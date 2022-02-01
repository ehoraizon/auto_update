package com.egarcia.auto_update;

import java.util.HashMap;

public class GithubResults{
    private final String assetUrl;
    private final String body;
    private final String tag;

    public GithubResults(String assetUrl, String body, String tag){
        this.assetUrl = assetUrl;
        this.body = body;
        this.tag = tag;
    }

    public GithubResults(){
        this.assetUrl = "";
        this.body = "";
        this.tag = "";
    }

    static GithubResults upToDate(){
        return new GithubResults("up-to-date", "", "");
    }

    public HashMap<String, String> toMap(){
        HashMap<String, String> map = new HashMap<>();
        map.put("assetUrl", assetUrl);
        map.put("body", body);
        map.put("tag", tag);
        return map;
    }

    public String getAssetUrl() {
        return assetUrl;
    }

    public String getBody() {
        return body;
    }

    public String getTag() {
        return tag;
    }
}
