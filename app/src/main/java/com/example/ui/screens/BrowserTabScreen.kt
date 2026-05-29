package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTabScreen() {
    var urlInput by remember { mutableStateOf("https://www.google.com") }
    var loadUrl by remember { mutableStateOf("https://www.google.com") }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {

        // Address bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webView?.goBack() }, enabled = webView?.canGoBack() == true) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.LightGray)
            }
            IconButton(onClick = { webView?.goForward() }, enabled = webView?.canGoForward() == true) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color.LightGray)
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                placeholder = { Text("Search or type URL", color = Color.Gray, fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    // Fix URL formatting
                    val cleanUrl = urlInput.trim()
                    loadUrl = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                        cleanUrl
                    } else if (cleanUrl.contains(" ") || !cleanUrl.contains(".")) {
                        "https://www.google.com/search?q=${Uri.encode(cleanUrl)}"
                    } else {
                        "https://$cleanUrl"
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF334155),
                    unfocusedContainerColor = Color(0xFF334155),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(25.dp)
            )

            IconButton(onClick = { webView?.reload() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.LightGray)
            }
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    this.webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            urlInput = url ?: ""
                        }
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val urlString = request?.url?.toString() ?: ""
                            if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                                return false
                            }
                            return true
                        }
                    }
                    this.webChromeClient = WebChromeClient()
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        databaseEnabled = true
                        // Enable incognito traits
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        allowFileAccess = false
                        allowContentAccess = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    }
                    
                    // Clear initial cache and history to act as incognito
                    clearCache(true)
                    clearHistory()
                    clearFormData()
                    
                    webView = this
                }
            },
            update = { view ->
                view.loadUrl(loadUrl)
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleWebScreen(url: String) {
    var webView: WebView? by remember { mutableStateOf(null) }
    
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                    android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    
                    // Standard Mobile User Agent to ensure mobile video formats (MP4) are loaded instead of unsupported desktop formats
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.evaluateJavascript(
                                "(function() {" +
                                "var style = document.createElement('style');" +
                                "style.innerHTML = 'ytm-consent-bump-renderer, ytm-app-promo-renderer, ytm-bottom-sheet-renderer, .bottom-sheet-container, ytm-promoted-video-renderer, " +
                                ".tiktok-cookie-banner, .unlogin-roaming-app-container, .unlogin-bottom-bar, .tiktok-auth-modal, " +
                                "[id*=\"branch-banner\"], [class*=\"app-banner\"], [class*=\"download-app\"], [class*=\"upsell\"], .tux-modal " +
                                "{ display: none !important; opacity: 0 !important; pointer-events: none !important; z-index: -9999 !important; } " +
                                "body { overflow: auto !important; }';" +
                                "document.head.appendChild(style);" +
                                "setInterval(function() {" +
                                "  var vids = document.getElementsByTagName('video');" +
                                "  for(var i=0; i<vids.length; i++) { if(vids[i].paused && window.location.hostname.includes(\"tiktok\")) vids[i].play(); }" +
                                "}, 1500);" +
                                "})();", null
                            )
                        }
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val urlString = request?.url?.toString() ?: ""
                            if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                                return false
                            }
                            return true
                        }
                    }
                    webChromeClient = WebChromeClient()
                    
                    webView = this
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.onPause()
            webView?.destroy()
            webView = null
        }
    }
}
