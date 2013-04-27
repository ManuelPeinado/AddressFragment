package com.manuelpeinado.addressfragment.demo.apiclients.directions;

import java.util.List;

public class GoogleDirectionsResponse {
    public List<Route> routes;

    public String getDuration() {
        return routes.get(0).legs.get(0).duration.text;
    }
}
