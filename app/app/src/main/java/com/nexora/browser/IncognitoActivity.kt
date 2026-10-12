package com.nexora.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class IncognitoActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(Color.WHITE)
        }

        address = EditText(this).apply {
            hint = "Private browsing — enter website"
            singleLine = true
        }

        val go = Button(this).apply {
            text = "GO"
            setOnClickListener { openPage() }
        }

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            webViewClient = WebViewClient()
        }

        root.addView(address)
        root.addView(go)
        root.addView(
            web,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
        openPage("https://www.google.com")
    }

    private fun openPage(defaultUrl: String? = null) {
        val text = defaultUrl ?: address.text.toString().trim()
        if (text.isBlank()) return

        val url = if (text.startsWith("http://") ||
            text.startsWith("https://")) {
            text
        } else if (text.contains(".") && !text.contains(" ")) {
            "https://$text"
        } else {
            "https://www.google.com/search?q=" +
                java.net.URLEncoder.encode(text, "UTF-8")
        }

        address.setText(url)
        web.loadUrl(url)
    }

    override fun onDestroy() {
        if (::web.isInitialized) {
            web.clearHistory()
            web.clearCache(true)
            web.clearFormData()
            web.destroy()
        }
        super.onDestroy()
    }
}
