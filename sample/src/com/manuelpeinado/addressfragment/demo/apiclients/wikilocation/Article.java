package com.manuelpeinado.addressfragment.demo.apiclients.wikilocation;

import com.google.gson.annotations.SerializedName;

public class Article {
    public String type;
    public String title;
    @SerializedName("mobileurl")
    public String mobileUrl;
    public String distance;
    
    @Override
    public String toString() {
        return title;
    }
}
