package com.jrweb.banksmspopup

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView

class PopupService : Service() {

    private var windowManager: WindowManager? = null
    private var popupView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val popupType = intent?.getStringExtra("POPUP_TYPE") ?: "DEPOSIT"
        val title = intent?.getStringExtra("TITLE") ?: "بانک"
        val mainText = intent?.getStringExtra("MAIN_TEXT") ?: "0"

        showPopup(popupType, title, mainText)
        return START_NOT_STICKY
    }

    private fun showPopup(popupType: String, title: String, mainText: String) {
        removePopup()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        popupView = LayoutInflater.from(this).inflate(R.layout.popup_layout, null)

        val tvBankName = popupView!!.findViewById<TextView>(R.id.tvBankName)
        val tvType = popupView!!.findViewById<TextView>(R.id.tvTransactionType)
        val tvAmount = popupView!!.findViewById<TextView>(R.id.tvAmount)
        val btnClose = popupView!!.findViewById<ImageButton>(R.id.btnClose)

        tvBankName.text = title

        when (popupType) {
            "DEPOSIT" -> {
                tvType.text = "واریز به حساب"
                tvAmount.text = "+ $mainText تومان"
                tvAmount.setTextColor(Color.parseColor("#2E7D32"))
            }
            "WITHDRAW" -> {
                tvType.text = "برداشت از حساب"
                tvAmount.text = "- $mainText تومان"
                tvAmount.setTextColor(Color.parseColor("#C62828"))
            }
            "OTP" -> {
                tvType.text = "در کلیپ‌بورد ذخیره شد"
                tvType.setTextColor(Color.parseColor("#2E7D32"))
                tvAmount.text = mainText
                tvAmount.setTextColor(Color.parseColor("#1565C0"))
            }
        }

        btnClose.setOnClickListener {
            removePopup()
            stopSelf()
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        windowManager?.addView(popupView, params)

        Handler(Looper.getMainLooper()).postDelayed({
            removePopup()
            stopSelf()
        }, 8000)
    }

    private fun removePopup() {
        if (popupView != null && windowManager != null) {
            try {
                windowManager?.removeView(popupView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removePopup()
    }
}