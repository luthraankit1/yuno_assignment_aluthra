package com.yuno.assignment.provider;

import lombok.Value;

@Value
public class ProviderResponse {

    boolean success;
    String providerReference;
    String errorCode;
    String errorMessage;

    public static ProviderResponse success(String providerReference) {
        return new ProviderResponse(true, providerReference, null, null);
    }

    public static ProviderResponse failure(String errorCode, String errorMessage) {
        return new ProviderResponse(false, null, errorCode, errorMessage);
    }
}
