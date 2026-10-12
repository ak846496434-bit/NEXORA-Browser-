package com.nexora.browser

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

private const val PREFS = "nexora_browser"
private const val PINK = 0xFFE45B70.toInt()
private const val PALE = 0xFFFFF1F3.toInt()
private const val INK = 0xFF29232A.toInt()

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var address: EditText
    private lateinit var pageHost: FrameLayout
    private lateinit var tabLabel: TextView
    private lateinit var prefs: android.content.SharedPreferences
    private val tabs = mutableListOf<WebView>()
    private var current = 0
    private var desktop = false
    private var mousePanel: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        buildUi()
        addTab(prefs.getString("last_url", "https://www.google.com") ?: "https://www.google.com")
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(12, 10, 12, 8)
        }
        val brandRow = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val brand = TextView(this).apply {
            text = "◉  NEXORA"
            textSize = 21f
            setTextColor(PINK)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tabLabel = TextView(this).apply {
            text = "1 tab"
            textSize = 12f
            setTextColor(INK)
            setPadding(10, 7, 10, 7)
            setBackgroundColor(PALE)
        }
        brandRow.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(tabLabel)
        root.addView(brandRow)

        val addrRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 10, 0, 8)
        }
        address = EditText(this).apply {
            hint = "Search or enter website"
            singleLine = true
            textSize = 15f
            setTextColor(INK)
            setHintTextColor(0xFF8C7C80.toInt())
            setPadding(16, 5, 12, 5)
            setBackgroundColor(PALE)
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { _, action, _ ->
                if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                    navigateFromAddress()
                    true
                } else false
            }
        }
        addrRow.addView(address, LinearLayout.LayoutParams(0, 48, 1f))
        val go = smallButton("GO") { navigateFromAddress() }
        addrRow.addView(go)
        root.addView(addrRow)

        pageHost = FrameLayout(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(pageHost, LinearLayout.LayoutParams(-1, 0, 1f))

        val row1 = horizontalRow()
        row1.addView(smallButton("←") { active()?.let { if (it.canGoBack()) it.goBack() } })
        row1.addView(smallButton("→") { active()?.let { if (it.canGoForward()) it.goForward() } })
        row1.addView(smallButton("↻") { active()?.reload() })
        row1.addView(smallButton("＋ Tab") { addTab("https://www.google.com") })
        row1.addView(smallButton("Tabs") { showTabs() })
        root.addView(row1)

        val row2 = horizontalRow()
        row2.addView(smallButton(if (desktop) "Desktop ✓" else "Desktop") {
            desktop = !desktop
            active()?.let { applyDesktopMode(it); it.reload() }
            Toast.makeText(this, if (desktop) "Desktop mode on" else "Desktop mode off", Toast.LENGTH_SHORT).show()
        })
        row2.addView(smallButton("Mouse") { toggleMousePanel() })
        row2.addView(smallButton("☆ Save") { saveBookmark() })
        row2.addView(smallButton("★ Marks") { showBookmarks() })
        row2.addView(smallButton("History") { showHistory() })
        root.addView(row2)

        val row3 = horizontalRow()
        row3.addView(smallButton("Downloads") { showDownloadsInfo() })
        row3.addView(smallButton("Search") { showSearchEngines() })
        row3.addView(smallButton("Incognito") {
            startActivity(Intent(this, IncognitoActivity::class.java))
        })
        root.addView(row3)
        setContentView(root)
    }

    private fun horizontalRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(0, 4, 0, 2)
    }

    private fun smallButton(label: String, action: () -> Unit) = TextView(this).apply {
        text = label
        textSize = 12f
        gravity = Gravity.CENTER
        setTextColor(PINK)
        setPadding(9, 11, 9, 11)
        setBackgroundColor(PALE)
        isClickable = true
        setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(0, -2, 1f)
        lp.setMargins(2, 1, 2, 1)
        layoutParams = lp
    }

    private fun active(): WebView? = tabs.getOrNull(current)

    private fun addTab(url: String) {
        val web = WebView(this)
        web.setBackgroundColor(Color.WHITE)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.javaScriptCanOpenWindowsAutomatically = false
        web.settings.setSupportMultipleWindows(false)
        web.settings.builtInZoomControls = true
        web.settings.displayZoomControls = false
        web.settings.loadsImagesAutomatically = true
        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                address.setText(view?.url ?: "")
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                // Tab title is read from WebView when the tab switcher opens.
            }
        }
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
            override fun onPageFinished(view: WebView?, url: String?) {
                if (view === active()) {
                    address.setText(url ?: "")
                    prefs.edit().putString("last_url", url ?: "https://www.google.com").apply()
                    if (!url.isNullOrBlank()) addHistory(url)
                }
            }
        }
        web.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val request = DownloadManager.Request(Uri.parse(url))
                    .setMimeType(mimeType)
                    .addRequestHeader("User-Agent", userAgent)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeFileName(contentDisposition, url))
                val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        })
        tabs.add(web)
        current = tabs.lastIndex
        pageHost.removeAllViews()
        pageHost.addView(web, FrameLayout.LayoutParams(-1, -1))
        tabLabel.text = "${tabs.size} tab${if (tabs.size == 1) "" else "s"}"
        navigate(url)
    }

    private fun navigateFromAddress() {
        val raw = address.text.toString().trim()
        if (raw.isEmpty()) return
        val searchBase = prefs.getString("search_url", "https://www.google.com/search?q=")
            ?: "https://www.google.com/search?q="
        val url = if (raw.contains(" ") || (!raw.contains(".") && !raw.startsWith("http"))) {
            searchBase + URLEncoder.encode(raw, "UTF-8")
        } else if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        navigate(url)
    }

    private fun navigate(url: String) {
        val web = active() ?: return
        applyDesktopMode(web)
        web.loadUrl(url)
    }

    private fun applyDesktopMode(web: WebView) {
        web.settings.userAgentString = if (desktop) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        } else null
    }

    private fun showTabs() {
        if (tabs.isEmpty()) return
        val names = tabs.mapIndexed { i, w -> "${if (i == current) "● " else ""}${w.title ?: "New tab"}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Open tabs")
            .setItems(names) { _, which ->
                current = which
                pageHost.removeAllViews()
                pageHost.addView(tabs[current], FrameLayout.LayoutParams(-1, -1))
                address.setText(tabs[current].url ?: "")
                tabLabel.text = "${tabs.size} tabs"
            }
            .setNeutralButton("Close current") { _, _ -> closeCurrentTab() }
            .setPositiveButton("New tab") { _, _ -> addTab("https://www.google.com") }
            .show()
    }

    private fun closeCurrentTab() {
        if (tabs.size <= 1) {
            active()?.loadUrl("https://www.google.com")
            return
        }
        tabs.removeAt(current).apply { stopLoading(); destroy() }
        current = (current - 1).coerceAtLeast(0)
        pageHost.removeAllViews()
        pageHost.addView(tabs[current], FrameLayout.LayoutParams(-1, -1))
        address.setText(tabs[current].url ?: "")
        tabLabel.text = "${tabs.size} tabs"
    }

    private fun saveBookmark() {
        val web = active() ?: return
        val url = web.url ?: return
        val arr = JSONArray(prefs.getString("bookmarks", "[]"))
        val item = JSONObject().put("title", web.title ?: url).put("url", url)
        if ((0 until arr.length()).none { arr.optJSONObject(it)?.optString("url") == url }) arr.put(item)
        prefs.edit().putString("bookmarks", arr.toString()).apply()
        Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        val arr = JSONArray(prefs.getString("bookmarks", "[]"))
        val labels = (0 until arr.length()).map { arr.optJSONObject(it)?.optString("title", "Saved site") ?: "Saved site" }.toTypedArray()
        if (labels.isEmpty()) { Toast.makeText(this, "No bookmarks yet", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("Bookmarks").setItems(labels) { _, which ->
            navigate(arr.optJSONObject(which)?.optString("url") ?: "https://www.google.com")
        }.setNegativeButton("Close", null).show()
    }

    private fun addHistory(url: String) {
        if (url.startsWith("about:")) return
        val arr = JSONArray(prefs.getString("history", "[]"))
        arr.put(JSONObject().put("url", url).put("time", System.currentTimeMillis()))
        while (arr.length() > 200) arr.remove(0)
        prefs.edit().putString("history", arr.toString()).apply()
    }

    private fun showHistory() {
        val arr = JSONArray(prefs.getString("history", "[]"))
        val unique = linkedMapOf<String, String>()
        for (i in arr.length() - 1 downTo 0) {
            val url = arr.optJSONObject(i)?.optString("url") ?: continue
            unique.putIfAbsent(url, url)
        }
        val urls = unique.keys.toList()
        if (urls.isEmpty()) { Toast.makeText(this, "No history yet", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this).setTitle("Recent history")
            .setItems(urls.take(50).toTypedArray()) { _, which -> navigate(urls[which]) }
            .setNeutralButton("Clear history") { _, _ -> prefs.edit().remove("history").apply(); Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show() }
            .setNegativeButton("Close", null).show()
    }

    private fun showDownloadsInfo() {
        AlertDialog.Builder(this).setTitle("Downloads")
            .setMessage("Downloads are handled by Android Download Manager. Check your phone's Downloads app or notification shade for progress and completed files.")
            .setPositiveButton("OK", null).show()
    }

    private fun showSearchEngines() {
        val engines = arrayOf("Google", "Bing", "DuckDuckGo", "Brave Search")
        AlertDialog.Builder(this).setTitle("Default search engine").setItems(engines) { _, which ->
            val urls = arrayOf("https://www.google.com/search?q=", "https://www.bing.com/search?q=", "https://duckduckgo.com/?q=", "https://search.brave.com/search?q=")
            prefs.edit().putString("search_url", urls[which]).apply()
            Toast.makeText(this, "${engines[which]} selected", Toast.LENGTH_SHORT).show()
        }.show()
    }

    private fun toggleMousePanel() {
        mousePanel?.let { root.removeView(it); mousePanel = null; return }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 10, 12, 10)
            setBackgroundColor(PALE)
        }
        panel.addView(TextView(this).apply {
            text = "TOUCHPAD  •  Drag to move cursor"
            textSize = 12f
            setTextColor(PINK)
            gravity = Gravity.CENTER
        })
        val pad = FrameLayout(this).apply {
            setBackgroundColor(0xFFFFDDE3.toInt())
        }
        val cursor = TextView(this).apply {
            text = "➤"
            textSize = 22f
            setTextColor(PINK)
        }
        pad.addView(cursor)
        var lastX = 0f
        var lastY = 0f
        pad.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastX; val dy = event.y - lastY
                    cursor.x = (cursor.x + dx).coerceIn(0f, (pad.width - cursor.width).coerceAtLeast(0).toFloat())
                    cursor.y = (cursor.y + dy).coerceIn(0f, (pad.height - cursor.height).coerceAtLeast(0).toFloat())
                    lastX = event.x; lastY = event.y; true
                }
                MotionEvent.ACTION_UP -> {
                    if (event.eventTime - event.downTime < 250) {
                        val jsX = (event.x / pad.width.coerceAtLeast(1) * 100).toInt()
                        val jsY = (event.y / pad.height.coerceAtLeast(1) * 100).toInt()
                        active()?.evaluateJavascript("(function(){var e=document.elementFromPoint(window.innerWidth*$jsX/100,window.innerHeight*$jsY/100);if(e)e.click();})()", null)
                    }
                    true
                }
                else -> true
            }
        }
        panel.addView(pad, LinearLayout.LayoutParams(-1, 110))
        panel.addView(TextView(this).apply {
            text = "Note: cursor/tap controls are best-effort; some sites block scripted clicks."
            textSize = 10f
            setTextColor(INK)
            setPadding(2, 6, 2, 0)
        })
        root.addView(panel, root.indexOfChild(pageHost) + 1, LinearLayout.LayoutParams(-1, -2))
        mousePanel = panel
    }

    private fun safeFileName(disposition: String?, url: String): String {
        val fromHeader = disposition?.substringAfter("filename=", "")?.trim('"', ' ', ';')
        val fromUrl = Uri.parse(url).lastPathSegment
        return (fromHeader?.takeIf { it.isNotBlank() } ?: fromUrl?.takeIf { it.isNotBlank() } ?: "nexora_download")
            .replace(Regex("[^A-Za-z0-9._-]"), "_").take(100)
    }

    override fun onBackPressed() {
        val web = active()
        if (web?.canGoBack() == true) web.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        tabs.forEach { it.stopLoading(); it.destroy() }
        tabs.clear()
        super.onDestroy()
    }
}
