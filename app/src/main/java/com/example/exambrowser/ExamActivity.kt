package com.example.exambrowser

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ExamActivity : AppCompatActivity() {

    private lateinit var webViewExam: WebView
    private val examUrl = "https://ulc.unibabwi.ac.id" // URL PORTAL UJIAN
    private var isExamFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Mencegah Screenshot & Rekam Layar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_exam)

        webViewExam = findViewById(R.id.webViewExam)
        val btnExitExam = findViewById<Button>(R.id.btnExitExam)

        setupWebView()
        setupNetworkMonitoring()

        btnExitExam.setOnClickListener {
            showExitPasswordDialog()
        }
    }

    private fun setupWebView() {
        // Matikan Chrome DevTools Remote Debugging
        WebView.setWebContentsDebuggingEnabled(false)

        CookieManager.getInstance().removeAllCookies(null)
        webViewExam.clearCache(true)
        webViewExam.clearHistory()

        webViewExam.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            supportZoom()
            allowFileAccess = false
            allowContentAccess = false
        }

        webViewExam.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                // Batalkan koneksi jika SSL bermasalah demi keamanan
                handler?.cancel()
                Toast.makeText(applicationContext, "Koneksi Tidak Aman (SSL Error)!", Toast.LENGTH_SHORT).show()
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                webViewExam.loadUrl(examUrl)
                return true
            }
        }

        webViewExam.loadUrl(examUrl)
    }

    private fun setupNetworkMonitoring() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val builder = NetworkRequest.Builder()

        connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Koneksi Terputus! Periksa internet Anda.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onAvailable(network: Network) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Terhubung Kembali.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startLockTaskMode()
    }

    private fun startLockTaskMode() {
        try {
            startLockTask()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isExamFinished) {
            Toast.makeText(this, "PERINGATAN: Dilarang meninggalkan layar ujian!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        Toast.makeText(this, "Tombol kembali dinonaktifkan.", Toast.LENGTH_SHORT).show()
    }

    private fun showExitPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Keluar Sesi Ujian")
        builder.setMessage("Masukkan PIN Pengawas:")

        val input = EditText(this)
        input.hint = "PIN Pengawas"
        builder.setView(input)

        builder.setPositiveButton("Konfirmasi") { _, _ ->
            val password = input.text.toString()
            if (password == "admin123") {
                isExamFinished = true
                try {
                    stopLockTask()
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                Toast.makeText(this, "PIN Salah!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}
