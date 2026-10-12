package com.nexora.browser

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Bitmap
import java.net.URLEncoder

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var status: TextView
    private var desktopMode = false

    private val red = Color.rgb(235, 65, 85)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = white
        window.navigationBarColor = white
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(white)
            setPadding(8, 6, 8, 6)
        }

        val brand = TextView(this).apply {
            text = "NEXORA Browser"
            textSize = 23f
            setTextColor(red)
            setPadding(8, 8, 8, 12)
        }
        root.addView(brand)

        val addressRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        address = EditText(this).apply {
            hint = "Search or enter website"
            singleLine = true
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(12, 4, 8, 4)
            setBackgroundColor(Color.rgb(248, 240, 242))
            imeOptions = EditorInfo.IME_ACTION_GO
        }

        addressRow.addView(
            address,
            LinearLayout.LayoutParams(0, 48, 1f)
        )

        val go = Button(this).apply {
            text = "GO"
            setTextColor(white)
            setBackgroundColor(red)
            setOnClickListener { openAddress() }
        }
        addressRow.addView(go)
        root.addView(addressRow)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        addControl(controls, "Back") {
            if (web.canGoBack()) web.goBack()
        }
        addControl(controls, "Next") {
            if (web.canGoForward()) web.goForward()
        }
        addControl(controls, "Reload") {
            web.reload()
        }
        addControl(controls, "Home") {
            loadHome()
        }
        addControl(controls, "Desktop") {
            toggleDesktop()
        }

        root.addView(controls)

        status = TextView(this).apply {
            text = "Ready"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(8, 4, 8, 4)
        }
        root.addView(status)

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false

        web.webViewClient = object : WebViewClient() {
            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: Bitmap?
            ) {
                status.text = "Loading page..."
                address.setText(url ?: "")
            }

            override fun onPageFinished(
                view: WebView?,
                url: String?
            ) {
                status.text = "Page loaded"
                address.setText(url ?: "")
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int
            ) {
                status.text = "Loading $newProgress%"
            }
        }

        root.addView(
            web,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)

        address.setOnEditorActionListener { _, action, event ->
            if (
                action == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                openAddress()
                true
            } else {
                false
            }
        }

        loadHome()
    }

    private fun addControl(
        row: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            textSize = 10f
            setTextColor(red)
            setBackgroundColor(Color.rgb(255, 238, 241))
            setOnClickListener { action() }
        }

        row.addView(
            button,
            LinearLayout.LayoutParams(0, 44, 1f)
        )
    }

    private fun openAddress() {
        val text = address.text.toString().trim()
        if (text.isBlank()) return

        val url = when {
            text.startsWith("https://") ||
            text.startsWith("http://") -> text

            text.contains(" ") || !text.contains(".") ->
                "https://www.google.com/search?q=" +
                    URLEncoder.encode(text, "UTF-8")

            else -> "https://$text"
        }

        web.loadUrl(url)
    }

    private fun loadHome() {
        address.setText("https://www.google.com")
        web.loadUrl("https://www.google.com")
    }

    private fun toggleDesktop() {
        desktopMode = !desktopMode

        web.settings.userAgentString = if (desktopMode) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        } else {
            WebView(this).settings.userAgentString
        }

        web.settings.useWideViewPort = desktopMode
        web.settings.loadWithOverviewMode = desktopMode
        web.reload()

        Toast.makeText(
            this,
            if (desktopMode) "Desktop mode enabled"
            else "Mobile mode enabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) {
            web.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
