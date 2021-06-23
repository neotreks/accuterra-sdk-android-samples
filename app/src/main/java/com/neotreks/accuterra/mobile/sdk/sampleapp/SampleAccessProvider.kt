package com.neotreks.accuterra.mobile.sdk.sampleapp

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.IAccessProvider
import com.neotreks.accuterra.mobile.sdk.security.model.ClientCredentials

/**
 * Class for managing access to AccuTerra services
 */
class SampleAccessProvider : IAccessProvider {

    override suspend fun getClientCredentials(context: Context): ClientCredentials {
        return ClientCredentials(
            clientId = BuildConfig.ACCUTERRA_CLIENT_ID,
            clientSecret = BuildConfig.ACCUTERRA_CLIENT_SECRET
        )
    }

}