package com.evcharging.mobile.api;

import com.evcharging.mobile.models.User;
import com.evcharging.mobile.models.ChargingStation;
import com.evcharging.mobile.models.Booking;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // EV Owners Endpoints (server uses capitalized controller name)
    @GET("api/EVOwners/{nic}")
    Call<User> getEVOwner(@Path("nic") String nic);

    @POST("api/EVOwners")
    Call<User> createEVOwner(@Body User user);

    @PUT("api/EVOwners/{nic}")
    Call<User> updateEVOwner(@Path("nic") String nic, @Body User user);

    @GET("api/EVOwners")
    Call<List<User>> getAllEVOwners(); // Added for testing

    @PUT("api/EVOwners/{nic}/deactivate")
    Call<User> deactivateEVOwner(@Path("nic") String nic);

    @PUT("api/EVOwners/{nic}/reactivate")
    Call<User> reactivateEVOwner(@Path("nic") String nic, @Body ReactivationRequest request);

    // Charging Stations Endpoints
    @GET("api/ChargingStations/active")
    Call<List<ChargingStation>> getActiveStations();

    @GET("api/ChargingStations")
    Call<List<ChargingStation>> getAllStations();

    @GET("api/ChargingStations/nearby")
    Call<List<ChargingStation>> getNearbyStations(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("radiusKm") double radiusKm);

    @GET("api/ChargingStations/{id}")
    Call<ChargingStation> getStationById(@Path("id") String id);

    // Bookings Endpoints (server uses capitalized controller name)
    @GET("api/Bookings")
    Call<List<Booking>> getAllBookings();

    @GET("api/Bookings/{id}")
    Call<Booking> getBookingById(@Path("id") String id);

    @GET("api/Bookings/upcoming/{nic}")
    Call<List<Booking>> getUpcomingBookings(@Path("nic") String nic);

    @POST("api/Bookings")
    Call<Booking> createBooking(@Body Booking booking);

    @PUT("api/Bookings/{id}")
    Call<Booking> updateBooking(@Path("id") String id, @Body Booking booking);

    @DELETE("api/Bookings/{id}")
    Call<Void> cancelBooking(@Path("id") String id);

    @GET("api/Bookings/{id}/can-modify")
    Call<Boolean> canModifyBooking(@Path("id") String id);

    @GET("api/Bookings/history/{nic}")
    Call<List<Booking>> getBookingHistory(@Path("nic") String nic);

    @GET("api/Bookings/pending/count/{nic}")
    Call<Integer> getPendingBookingsCount(@Path("nic") String nic);

    @GET("api/Bookings/approved/count/{nic}")
    Call<Integer> getApprovedBookingsCount(@Path("nic") String nic);

    @PUT("api/Bookings/{id}/approve")
    Call<Booking> approveBooking(@Path("id") String id);

    @POST("api/Bookings/{id}/generate-qr")
    Call<QRCodeResponse> generateQRCode(@Path("id") String id);

    @POST("api/Bookings/{id}/verify")
    Call<BookingVerificationResponse> verifyBooking(@Path("id") String id, @Body QRVerificationRequest request);

    @PUT("api/Bookings/{id}/complete")
    Call<Booking> completeBooking(@Path("id") String id);

    // Authentication Endpoints (for Station Operators)
    @POST("api/users/authenticate")
    Call<AuthResponse> authenticateUser(@Body AuthRequest request);
}