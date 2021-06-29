package com.neotreks.accuterra.mobile.sdk.sampleapp.extensions

import java.util.*

/**
 * String related extensions
*/

/**
 * Converts UUID into [Long] ID
 */
fun String.uuidToLong(): Long {
    return UUID.fromString(this).mostSignificantBits and Long.MAX_VALUE
}