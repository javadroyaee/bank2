package com.jrweb.banksmspopup

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.provider.Telephony
import java.text.DecimalFormat
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val fullBody = StringBuilder()
            var sender = ""

            for (sms in messages) {
                sender = sms.originatingAddress ?: ""
                fullBody.append(sms.messageBody)
            }

            parseSms(context, sender, fullBody.toString())
        }
    }

    private fun parseSms(context: Context, sender: String, body: String) {
        val englishBody = toEnglishDigits(body)
        val bankName = detectBankName(sender, body)

        // ۱. بررسی اینکه آیا پیامک حاوی رمز دوم، رمز پویا یا کد فعال‌سازی است
        val otpKeywords = listOf(
            "رمز", "کد", "كد", "تایید", "تاييد", "فعال سازی", "فعالسازی",
            "فعال سازي", "پویا", "پويا", "otp", "code", "password", "pass", "verification"
        )
        val isOtpMessage = otpKeywords.any { englishBody.lowercase().contains(it) }

        if (isOtpMessage) {
            val otpCode = extractOtpCode(englishBody)
            if (otpCode != null) {
                // ذخیره خودکار کد در کلیپ‌بورد گوشی
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("OTP Code", otpCode)
                clipboard.setPrimaryClip(clip)

                val title = bankName ?: "کد فعال‌سازی / رمز دوم"
                launchPopup(context, "OTP", title, otpCode)
                return
            }
        }

        // ۲. بررسی پیامک تراکنش مالی بانکی (واریز یا برداشت)
        if (bankName == null) return

        val isDeposit = when {
            body.contains("واريز") || body.contains("واریز") || body.contains("انتقال به") || body.contains("+") -> true
            body.contains("برداشت") || body.contains("خريد") || body.contains("خرید") || body.contains("انتقال از") || body.contains("-") -> false
            else -> return
        }

        val amountRaw = extractAmount(englishBody) ?: return
        val amountToman = if (body.contains("تومان")) amountRaw else amountRaw / 10
        val formattedAmount = DecimalFormat("#,###").format(amountToman)

        val popupType = if (isDeposit) "DEPOSIT" else "WITHDRAW"
        launchPopup(context, popupType, bankName, formattedAmount)
    }

    private fun launchPopup(context: Context, type: String, title: String, mainText: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
            val serviceIntent = Intent(context, PopupService::class.java).apply {
                putExtra("POPUP_TYPE", type)
                putExtra("TITLE", title)
                putExtra("MAIN_TEXT", mainText)
            }
            context.startService(serviceIntent)
        }
    }

    private fun toEnglishDigits(text: String): String {
        return text
            .replace("۰", "0").replace("۱", "1").replace("۲", "2")
            .replace("۳", "3").replace("۴", "4").replace("۵", "5")
            .replace("۶", "6").replace("۷", "7").replace("۸", "8")
            .replace("۹", "9")
            .replace("٠", "0").replace("١", "1").replace("٢", "2")
            .replace("٣", "3").replace("٤", "4").replace("٥", "5")
            .replace("٦", "6").replace("٧", "7").replace("٨", "8")
            .replace("٩", "9")
    }

    private fun extractOtpCode(englishBody: String): String? {
        // حذف مبالغ دارای کاما یا پسوند ریال/تومان تا با رمز اشتباه نشوند
        val cleaned = englishBody.replace(Regex("\\d{1,3}(,\\d{3})+"), "")
        val matcher = Pattern.compile("\\b(\\d{4,8})\\b").matcher(cleaned)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun detectBankName(sender: String, body: String): String? {
        val text = "$sender $body"
        val banks = mapOf(
            "ملت" to "بانک ملت",
            "ملي" to "بانک ملی",
            "ملی" to "بانک ملی",
            "صادرات" to "بانک صادرات",
            "تجارت" to "بانک تجارت",
            "سپه" to "بانک سپه",
            "پاسارگاد" to "بانک پاسارگاد",
            "سامان" to "بانک سامان",
            "پارسيان" to "بانک پارسیان",
            "پارسیان" to "بانک پارسیان",
            "کشاورزي" to "بانک کشاورزی",
            "کشاورزی" to "بانک کشاورزی",
            "مسکن" to "بانک مسکن",
            "رفاه" to "بانک رفاه",
            "شهر" to "بانک شهر",
            "بلو" to "بلوبانک",
            "آينده" to "بانک آینده",
            "آینده" to "بانک آینده",
            "رسالت" to "بانک رسالت",
            "مهر" to "بانک مهر ایران",
            "اقتصاد نوين" to "بانک اقتصاد نوین",
            "اقتصاد نوین" to "بانک اقتصاد نوین",
            "سينـا" to "بانک سینا",
            "سینا" to "بانک سینا"
        )
        for ((key, value) in banks) {
            if (text.contains(key)) return value
        }
        return null
    }

    private fun extractAmount(englishBody: String): Long? {
        val matcher = Pattern.compile("(\\d{1,3}(,\\d{3})+|\\d{4,})").matcher(englishBody)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "") ?: continue
            val value = numStr.toLongOrNull() ?: continue
            if (numStr.length in 4..12) {
                return value
            }
        }
        return null
    }
}