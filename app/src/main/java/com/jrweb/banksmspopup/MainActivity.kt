package com.jrweb.banksmspopup

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#4A148C"))
            setPadding(60, 60, 60, 60)
        }

        val titleText = TextView(this).apply {
            text = "پاپ‌آپ هوشمند پیامک و کد\nJRweb.ir"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 50)
        }

        val btnSmsPerm = Button(this).apply {
            text = "۱. تایید مجوز خواندن پیامک"
            setOnClickListener { requestSmsPermission() }
        }

        val btnOverlayPerm = Button(this).apply {
            text = "۲. تایید مجوز نمایش روی صفحه"
            setOnClickListener { requestOverlayPermission() }
        }

        val btnTestBank = Button(this).apply {
            text = "تست پاپ‌آپ واریز بانکی"
            setOnClickListener {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, PopupService::class.java).apply {
                        putExtra("POPUP_TYPE", "DEPOSIT")
                        putExtra("TITLE", "بانک ملت")
                        putExtra("MAIN_TEXT", "۲,۵۰۰,۰۰۰")
                    }
                    startService(intent)
                } else {
                    Toast.makeText(this@MainActivity, "ابتدا مجوز نمایش روی صفحه را فعال کنید", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnTestOtp = Button(this).apply {
            text = "تست پاپ‌آپ کد فعال‌سازی"
            setOnClickListener {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(this@MainActivity, PopupService::class.java).apply {
                        putExtra("POPUP_TYPE", "OTP")
                        putExtra("TITLE", "کد فعال‌سازی / رمز دوم")
                        putExtra("MAIN_TEXT", "849201")
                    }
                    startService(intent)
                } else {
                    Toast.makeText(this@MainActivity, "ابتدا مجوز نمایش روی صفحه را فعال کنید", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(titleText)
        layout.addView(btnSmsPerm)
        layout.addView(btnOverlayPerm)
        layout.addView(btnTestBank)
        layout.addView(btnTestOtp)
        setContentView(layout)
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 101)
        } else {
            Toast.makeText(this, "مجوز پیامک فعال است", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 102)
        } else {
            Toast.makeText(this, "مجوز نمایش روی صفحه فعال است", Toast.LENGTH_SHORT).show()
        }
    }
}