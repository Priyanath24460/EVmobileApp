package com.evcharging.mobile.interfaces;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}
