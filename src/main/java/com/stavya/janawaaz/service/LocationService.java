package com.stavya.janawaaz.service;

import com.stavya.janawaaz.entity.LocationStatus;
import com.stavya.janawaaz.entity.Ward;
import org.springframework.stereotype.Service;

/**
 * Decides whether a GPS reading was taken inside the ward it was filed under.
 * Wards are modelled as a centre plus radius for the demo; real ward boundary
 * polygons (PostGIS point-in-polygon) can replace this without changing callers.
 */
@Service
public class LocationService {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    public record Check(LocationStatus status, Integer distanceMeters) {
    }

    public Check check(Ward ward, Double lat, Double lng) {
        if (lat == null || lng == null) {
            return new Check(LocationStatus.NOT_CAPTURED, null);
        }
        int distance = (int) Math.round(distanceMeters(ward.getCenterLat(), ward.getCenterLng(), lat, lng));
        LocationStatus status = distance <= ward.getRadiusMeters()
                ? LocationStatus.INSIDE_WARD
                : LocationStatus.OUTSIDE_WARD;
        return new Check(status, distance);
    }

    /** Haversine great-circle distance. */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
