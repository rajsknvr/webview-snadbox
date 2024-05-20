package com.example.webview_sandbox

import android.content.res.Configuration
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var originalWebViewLayoutParams: ViewGroup.LayoutParams? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webview)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true


        // Enable cookies
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = WebViewClient()
        originalWebViewLayoutParams = webView.layoutParams

        webView.addJavascriptInterface(WebAppInterface(), "AndroidWebAppInterface")
        webView.loadUrl("https://frontend-classroom-git-test-video-orientation-lyearn.vercel.app/")
    }

    private fun enterFullscreen() {
        webView.rotation = 90f
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val params = webView.layoutParams

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            params.height = screenWidth
            params.width = screenHeight
        } else {
            params.height = screenHeight
            params.width = screenWidth
        }

        // Calculate margins to center the WebView
        val marginLeft = (screenWidth - params.width) / 2
        val marginTop = (screenHeight - params.height) / 2

        // Apply margins
        (params as? ViewGroup.MarginLayoutParams)?.setMargins(marginLeft, marginTop, 0, 0)
        webView.layoutParams = params
    }

    private fun exitFullscreen() {
        webView.rotation = 0f
        val params = webView.layoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT

        // Reset margins
        (params as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
        webView.layoutParams = params
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun enterFullscreen() {
            runOnUiThread { this@MainActivity.enterFullscreen() }
        }

        @JavascriptInterface
        fun exitFullscreen() {
            runOnUiThread { this@MainActivity.exitFullscreen() }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}