package com.ke.hud_bluetooth

import android.Manifest
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.tbruyelle.rxpermissions2.RxPermissions

class RequestPermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_permissions)

        RxPermissions(this)
            .request(Manifest.permission.ACCESS_COARSE_LOCATION)
            .subscribe {
                if (it) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
                finish()
            }
    }
}
