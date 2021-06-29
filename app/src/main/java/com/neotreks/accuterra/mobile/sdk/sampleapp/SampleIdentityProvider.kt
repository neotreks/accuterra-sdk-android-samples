package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.IIdentityProvider

/**
 * User identity provider for the Sample App
 */
class SampleIdentityProvider : IIdentityProvider {

    /**
     * Change to your preferred user ID string.
     *
     * But don't forget that this setting affects what data will be returned
     * e.g. when fetching the list of user trips.
     */
    override suspend fun getUserId(context: Context): String {
        return "test driver uuid"
    }

}