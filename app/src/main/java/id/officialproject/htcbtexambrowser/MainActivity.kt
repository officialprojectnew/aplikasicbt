package id.officialproject.htcbtexambrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var examIsActive = false
    private var pendingWebPermission: PermissionRequest? = null

    companion object {
        private const val START_URL =
            "https://www.officialproject.my.id/cbt-online/index.html"
        private const val CAMERA_PERMISSION_CODE = 401
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mencegah screenshot/rekaman layar dari Android pada perangkat yang mendukungnya.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enterImmersiveMode()

        webView = WebView(this)
        setContentView(webView)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = false
            allowContentAccess = true
            setSupportMultipleWindows(false)
            userAgentString = userAgentString + " HTCBTExamBrowser/1.0"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri: Uri = request?.url ?: return false
                // Tetap di dalam aplikasi hanya untuk domain CBT sekolah.
                return uri.host != "www.officialproject.my.id" &&
                       uri.host != "officialproject.my.id"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url?.contains("officialproject.my.id/cbt-online") == true) {
                    bukaLoginSiswaOtomatis()
                    periksaStatusUjian()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val wantsVideo = request.resources.contains(
                    PermissionRequest.RESOURCE_VIDEO_CAPTURE
                )
                if (wantsVideo && hasCameraPermission()) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                } else if (wantsVideo) {
                    pendingWebPermission = request
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_CODE
                    )
                } else {
                    request.deny()
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) periksaStatusUjian()
            }
        }

        webView.loadUrl(START_URL)
    }

    private fun bukaLoginSiswaOtomatis() {
        // Halaman index CBT menampilkan beberapa jenis akun. Script ini memilih
        // tombol "Masuk sebagai Siswa" secara otomatis tanpa mengubah server.
        val js = """
            (function() {
              let tries = 0;
              const timer = setInterval(function() {
                tries++;
                const candidates = Array.from(document.querySelectorAll(
                  'button, a, [role="button"], input[type="button"], input[type="submit"], div, section'
                ));
                const target = candidates.find(function(el) {
                  const t = ((el.innerText || el.value || el.textContent || '')
                    .replace(/\s+/g, ' ').trim().toLowerCase());
                  return t === 'masuk sebagai siswa' ||
                         t === 'masuk sebagai siswa ›' ||
                         t === 'masuk sebagai siswa →';
                });
                if (target) {
                  clearInterval(timer);
                  target.click();
                  return;
                }
                if (tries >= 25) clearInterval(timer);
              }, 400);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun periksaStatusUjian() {
        val js = """
            (function() {
              const el = document.getElementById('view-ujian');
              if (!el) return false;
              const style = window.getComputedStyle(el);
              return !el.classList.contains('hidden') &&
                     style.display !== 'none' &&
                     style.visibility !== 'hidden';
            })();
        """.trimIndent()
        webView.evaluateJavascript(js) { value ->
            examIsActive = value == "true"
            if (examIsActive) enterImmersiveMode()
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            val request = pendingWebPermission
            pendingWebPermission = null
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            } else {
                request?.deny()
                Toast.makeText(
                    this,
                    "Izin kamera diperlukan jika ujian mewajibkan pemantauan kamera.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onBackPressed() {
        periksaStatusUjian()
        if (examIsActive) {
            Toast.makeText(
                this,
                "Tombol kembali dinonaktifkan selama ujian berlangsung.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Keluar dari HT CBT?")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ -> finish() }
            .show()
    }

    private fun enterImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onDestroy() {
        pendingWebPermission?.deny()
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }
}
