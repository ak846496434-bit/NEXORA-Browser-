package com.nexora.browser

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import java.net.URLEncoder

class IncognitoActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private var closed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // This Activity runs in :incognito; set a separate WebView data directory
        // before super.onCreate or any WebView APIs are touched in this process.
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            WebView.setDataDirectorySuffix("nexora_incognito")
        }
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 8)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text = "◉ NEXORA  •  INCOGNITO"
            textSize = 19f
            setTextColor(0xFFE45B70.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        address = EditText(this).apply {
            hint = "Search privately"
            singleLine = true
            imeOptions = EditorInfo.IME_ACTION_GO
            setBackgroundColor(0xFFFFF1F3.toInt())
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) { go(); true } else false
            }
        }
        row.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        row.addView(TextView(this).apply {
            text = "GO"; setTextColor(0xFFE45B70.toInt()); gravity = Gravity.CENTER
            setPadding(12, 12, 12, 12); setOnClickListener { go() }
        })
        root.addView(row)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) { address.setText(url ?: "") }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextView(this).apply {
            text = "Private session: this app does not save these pages to normal browser history. Downloads and files saved outside this session may remain."
            textSize = 11f; setTextColor(0xFF6D6064.toInt()); setPadding(4, 8, 4, 8)
        })
        root.addView(TextView(this).apply {
            text = "Close private session"; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setBackgroundColor(0xFFE45B70.toInt()); setPadding(12, 12, 12, 12)
            setOnClickListener { closePrivateSession() }
        })
        setContentView(root)
        web.loadUrl("https://www.google.com")
    }

    private fun go() {
        val raw = address.text.toString().trim()
        if (raw.isBlank()) return
        val url = if (raw.contains(" ") || (!raw.contains(".") && !raw.startsWith("http"))) {
            "https://www.google.com/search?q=" + URLEncoder.encode(raw, "UTF-8")
        } else if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        web.loadUrl(url)
    }

    private fun closePrivateSession() {
        if (closed) return
        closed = true
        web.stopLoading()
        web.loadUrl("about:blank")
        web.clearHistory()
        web.clearCache(true)
        web.clearFormData()
        web.destroy()
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            finish()
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else closePrivateSession()
    }

    override fun onDestroy() {
        if (!closed && ::web.isInitialized) {
            web.stopLoading()
            web.clearHistory()
            web.clearCache(true)
            web.destroy()
        }
        super.onDestroy()
    }
}
