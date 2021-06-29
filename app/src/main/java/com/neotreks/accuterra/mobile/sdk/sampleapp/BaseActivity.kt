package com.neotreks.accuterra.mobile.sdk.sampleapp

import androidx.appcompat.app.AppCompatActivity

/**
 * A base Activity class with a common functionality
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    protected fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

}