package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.IIdentityProvider

/**
 * User identity provider for the Sample App
 */
class SampleIdentityProvider : IIdentityProvider {

    override suspend fun getUserId(context: Context): String {
        return "sample app user"
    }

}