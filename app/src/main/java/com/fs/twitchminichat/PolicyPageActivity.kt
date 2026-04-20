package com.fs.twitchminichat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

class PolicyPageActivity : AppCompatActivity(R.layout.activity_policy_page) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val assetPath = intent.getStringExtra(EXTRA_ASSET).orEmpty()
        val webUrl = intent.getStringExtra(EXTRA_WEB_URL).orEmpty()

        findViewById<TextView>(R.id.textTitle).text = title

        val webView = findViewById<WebView>(R.id.webPolicy)
        webView.settings.javaScriptEnabled = false
        webView.settings.domStorageEnabled = false
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/policies/$assetPath")

        val btnOpenWeb = findViewById<Button>(R.id.btnOpenWeb)
        btnOpenWeb.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, webUrl.toUri()))
        }
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_ASSET = "asset"
        private const val EXTRA_WEB_URL = "webUrl"

        fun open(context: Context, title: String, asset: String, webUrl: String) {
            context.startActivity(
                Intent(context, PolicyPageActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_ASSET, asset)
                    .putExtra(EXTRA_WEB_URL, webUrl)
            )
        }
    }
}