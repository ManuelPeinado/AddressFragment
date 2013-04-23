package com.manuelpeinado.addressfragment;

public class Place {

    private final String mDescription;

    public Place(String description) {
        this.mDescription = description;
    }

    public String getDescription() {
        return mDescription;
    }
    
    @Override
    public String toString() {
        return mDescription;
    }
}
