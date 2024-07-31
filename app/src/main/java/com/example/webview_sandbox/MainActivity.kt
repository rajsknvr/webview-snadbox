package com.example.webview_sandbox
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.webkit.JavascriptInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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

        
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = MyChrome(this)


        webView.addJavascriptInterface(JSInterface(this), "Android")


        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (url.startsWith("blob:")) {
                webView.evaluateJavascript(
                    "(function() {" +
                            "var xhr = new XMLHttpRequest();" +
                            "xhr.open('GET', '$url', true);" +
                            "xhr.responseType = 'blob';" +
                            "xhr.onload = function() {" +
                            "if (xhr.status === 200) {" +
                            "var blob = xhr.response;" +
                            "var reader = new FileReader();" +
                            "reader.onloadend = function() {" +
                            "var base64Data = reader.result.split(',')[1];" +
                            "var guessedMimeType = '${URLUtil.guessFileName(url, contentDisposition, mimeType).split('.').last()}';" +
                            "var guessedExtension = guessedMimeType == 'mp4' ? 'mp4' : 'bin';" +
                            "Android.downloadBlob(base64Data, 'video/mp4', '${URLUtil.guessFileName(url, contentDisposition,"video/mp4" )}');" +
                            "};" +
                            "reader.readAsDataURL(blob);" +
                            "}" +
                            "};" +
                            "xhr.send();" +
                            "})()", null
                )
            } else {

                val mimeTypeToUse = mimeType ?: getMimeTypeFromUrl(url)
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeTypeToUse)
                val cookies = CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("cookie", cookies)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeTypeToUse))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeTypeToUse))

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
            }
        }

        originalWebViewLayoutParams = webView.layoutParams
        webView.loadUrl("https://meet-qa.lyearn.com")
    }

    private fun getMimeTypeFromUrl(url: String): String {
        return when {
            url.endsWith(".mp4") -> "video/mp4"
            url.endsWith(".webm") -> "video/webm"
            url.endsWith(".avi") -> "video/x-msvideo"
            url.endsWith(".mov") -> "video/quicktime"
            else -> "application/octet-stream" // Default MIME type
        }
    }

    private class MyChrome(private val activity: Activity) : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation: Int = 0
        private var mOriginalSystemUiVisibility: Int = 0

        override fun onHideCustomView() {
            (activity.window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            activity.window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            activity.requestedOrientation = mOriginalOrientation
            mCustomViewCallback?.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(paramView: View, paramCustomViewCallback: CustomViewCallback) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = activity.window.decorView.systemUiVisibility
            mOriginalOrientation = activity.requestedOrientation
            mCustomViewCallback = paramCustomViewCallback

            (activity.window.decorView as FrameLayout).addView(
                mCustomView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            activity.window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private class JSInterface(private val context: Context) {
        @JavascriptInterface
        fun downloadBlob(base64Data: String, mimeType: String, fileName: String) {
            try {
                val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(decodedBytes)
                }

                Toast.makeText(context, "File downloaded: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

}


