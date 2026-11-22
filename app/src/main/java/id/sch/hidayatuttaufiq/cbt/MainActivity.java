package id.sch.hidayatuttaufiq.cbt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_CAMERA = 101;
    private static final String PREF_NAME = "cbt_security_prefs";
    private static final String KEY_CHEAT_LOCKED = "cheat_locked";

    private WebView web;
    private View splash;
    private View cheatLockOverlay;
    private EditText etUnlockPin;

    private String activeExamUrl = Config.EXAM_URL;
    private String activeAllowedHost = Config.ALLOWED_HOST;
    private boolean initialPageLoaded = false;

    private PermissionRequest pendingWebPermission;
    private boolean waitingPermission = false;   // dialog izin sedang tampil -> jangan hitung sbg pindah app
    private boolean leftApp = false;
    private boolean wasPinned = false;
    private boolean isExitingIntentionally = false;
    private boolean cheatLocked = false;
    private boolean exitDialogShowing = false;

    private final Handler pinMonitorHandler = new Handler(Looper.getMainLooper());
    private final Runnable pinMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            checkPinState();
            pinMonitorHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Config.BLOCK_SCREENSHOT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 28) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        super.onCreate(savedInstanceState);
        if (Config.BLOCK_SCREENSHOT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
        setContentView(R.layout.activity_main);

        applyUrlFromIntent(getIntent());

        web = findViewById(R.id.webview);
        splash = findViewById(R.id.splash);
        cheatLockOverlay = findViewById(R.id.cheat_lock_overlay);
        etUnlockPin = findViewById(R.id.etUnlockPin);
        Button btnUnlockCheat = findViewById(R.id.btnUnlockCheat);

        btnUnlockCheat.setOnClickListener(v -> verifyUnlockPin());

        // Cek apakah sebelumnya terkunci karena unpin/keluar aplikasi
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_CHEAT_LOCKED, false)) {
            triggerCheatLock();
        }

        setupWebView();
        hideSystemUi();

        // Izin kamera diminta di awal supaya webcam ujian langsung siap
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            waitingPermission = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(activeExamUrl);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyUrlFromIntent(intent);
        if (web != null) {
            initialPageLoaded = false;
            web.loadUrl(activeExamUrl);
        }
    }

    private void applyUrlFromIntent(Intent intent) {
        if (intent == null) return;
        String scannedUrl = intent.getStringExtra("URL_UJIAN");
        if (scannedUrl != null && !scannedUrl.trim().isEmpty()) {
            activeExamUrl = scannedUrl.trim();
            updateAllowedHost(activeExamUrl);
        }
    }

    private void updateAllowedHost(String url) {
        try {
            Uri parsed = Uri.parse(url);
            if (parsed.getHost() != null && !parsed.getHost().isEmpty()) {
                String h = parsed.getHost().toLowerCase();
                if (h.startsWith("www.")) {
                    h = h.substring(4);
                }
                activeAllowedHost = h;
            }
        } catch (Exception ignored) { }
    }

    private boolean isHostAllowed(String host) {
        if (host == null || activeAllowedHost == null) return false;
        String h = host.toLowerCase();
        String allowed = activeAllowedHost.toLowerCase();
        return h.equals(allowed) || h.endsWith("." + allowed);
    }

    private boolean isCurrentlyPinned() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        return am.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
    }

    private void checkPinState() {
        if (isExitingIntentionally || waitingPermission || cheatLocked) return;
        boolean pinnedNow = isCurrentlyPinned();
        if (pinnedNow) {
            wasPinned = true;
        } else if (wasPinned) {
            // Siswa melakukan unpin (tahan swipe atas / tombol back+recent)
            wasPinned = false;
            triggerCheatLock();
        }
    }

    private void triggerCheatLock() {
        cheatLocked = true;
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_CHEAT_LOCKED, true)
                .apply();
        if (cheatLockOverlay != null) {
            cheatLockOverlay.setVisibility(View.VISIBLE);
            cheatLockOverlay.bringToFront();
        }
        if (etUnlockPin != null) {
            etUnlockPin.setText("");
        }
    }

    private void verifyUnlockPin() {
        String input = etUnlockPin.getText().toString().trim();
        if (Config.UNLOCK_PIN.equals(input)) {
            cheatLocked = false;
            wasPinned = false;
            leftApp = false;
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_CHEAT_LOCKED, false)
                    .apply();

            // Sembunyikan keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(etUnlockPin.getWindowToken(), 0);
            }

            etUnlockPin.setText("");
            cheatLockOverlay.setVisibility(View.GONE);
            Toast.makeText(this, "Kunci dibuka. Silakan lanjutkan ujian.", Toast.LENGTH_SHORT).show();
            hideSystemUi();
            startPinning();
        } else {
            etUnlockPin.setText("");
            Toast.makeText(this, "PIN khusus salah!", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);              // localStorage dipakai sesi login CBT
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + Config.UA_SUFFIX);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        // Matikan seleksi/salin lewat tekan lama
        web.setLongClickable(false);
        web.setOnLongClickListener(v -> true);
        web.setHapticFeedbackEnabled(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                String host = u.getHost();
                boolean validScheme = "https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme);
                if (!validScheme || host == null) {
                    return true;
                }

                // Izinkan iframe di dalam halaman ujian tanpa memunculkan peringatan blokir
                if (!req.isForMainFrame()) {
                    return false;
                }

                // Izinkan redirect awal dari shortlink / QR generator / forms.gle
                // lalu kunci domain tujuan akhirnya sebagai domain ujian aktif
                if (!initialPageLoaded || req.isRedirect()) {
                    updateAllowedHost(u.toString());
                    activeExamUrl = u.toString();
                    return false;
                }

                // Izinkan domain login Google jika ujian memakai Google Form / akun Google
                String h = host.toLowerCase();
                if (h.equals("accounts.google.com") || h.endsWith(".accounts.google.com")) {
                    return false;
                }

                boolean ok = isHostAllowed(host);
                if (!ok) {
                    Toast.makeText(MainActivity.this,
                            "Tautan di luar sistem ujian diblokir", Toast.LENGTH_SHORT).show();
                }
                return !ok;   // true = diblokir
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (splash.getVisibility() != View.GONE && !url.startsWith("data:")) {
                    splash.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    if (!initialPageLoaded) {
                        updateAllowedHost(url);
                        initialPageLoaded = true;
                    }
                }
                splash.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) showErrorPage(view);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                web.loadUrl(activeExamUrl);
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }
        });
    }

    // ---------- Kamera ----------
    private void handleWebPermission(PermissionRequest request) {
        boolean wantsCamera = false;
        for (String r : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) wantsCamera = true;
        }
        if (!wantsCamera) { request.deny(); return; }

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else {
            pendingWebPermission = request;
            waitingPermission = true;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQ_CAMERA) return;
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        if (pendingWebPermission != null) {
            if (granted) pendingWebPermission.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
            else pendingWebPermission.deny();
            pendingWebPermission = null;
        }
        if (!granted) {
            new AlertDialog.Builder(this)
                    .setTitle("Izin kamera diperlukan")
                    .setMessage("Ujian ini memakai kamera untuk pemantauan. Aktifkan izin Kamera di pengaturan aplikasi, lalu buka ulang halaman.")
                    .setPositiveButton("Buka Pengaturan", (d, w) -> {
                        waitingPermission = true;
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName())));
                    })
                    .setNegativeButton("Nanti", null)
                    .show();
        }
        hideSystemUi();
    }

    // ---------- Halaman error (offline) ----------
    private void showErrorPage(WebView view) {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{font-family:sans-serif;background:#f0fdf4;color:#14532d;text-align:center;padding:12vh 24px}"
                + "a{display:inline-block;margin-top:24px;padding:14px 28px;background:#16a34a;color:#fff;"
                + "border-radius:12px;text-decoration:none;font-weight:bold}</style></head><body>"
                + "<h2>Tidak dapat terhubung</h2>"
                + "<p>Periksa koneksi internet / Wi-Fi sekolah, lalu coba lagi.</p>"
                + "<a href='" + activeExamUrl + "'>Coba Lagi</a></body></html>";
        view.loadDataWithBaseURL(activeExamUrl, html, "text/html", "UTF-8", null);
    }

    // ---------- Mode layar penuh ----------
    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    private void startPinning() {
        if (!Config.USE_SCREEN_PINNING) return;
        try { startLockTask(); } catch (Exception ignored) { }
    }

    private void stopPinning() {
        try { stopLockTask(); } catch (Exception ignored) { }
    }

    // ---------- Siklus hidup & deteksi unpin / pindah aplikasi ----------
    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        hideSystemUi();

        if (leftApp) {
            leftApp = false;
            triggerCheatLock();
        } else {
            checkPinState();
        }

        if (!cheatLocked) {
            startPinning();
        }

        pinMonitorHandler.removeCallbacks(pinMonitorRunnable);
        pinMonitorHandler.postDelayed(pinMonitorRunnable, 1000);
        waitingPermission = false;
    }

    @Override
    protected void onPause() {
        pinMonitorHandler.removeCallbacks(pinMonitorRunnable);
        if (!waitingPermission && !isFinishing() && !isExitingIntentionally) {
            leftApp = true;
            // Langsung tandai terkunci agar jika aplikasi ditutup paksa lalu dibuka lagi, blackscreen tetap muncul
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_CHEAT_LOCKED, true)
                    .apply();
        }
        web.onPause();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
            checkPinState();
        }
    }

    @Override
    public void onMultiWindowModeChanged(boolean inMultiWindow) {
        super.onMultiWindowModeChanged(inMultiWindow);
        if (inMultiWindow) {
            triggerCheatLock();
        }
    }

    // ---------- Tombol Back & keluar ----------
    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (cheatLocked) {
            Toast.makeText(this, "Masukkan PIN khusus untuk melanjutkan ujian", Toast.LENGTH_SHORT).show();
            return;
        }
        // Back tidak pernah mundur halaman ujian; hanya menampilkan konfirmasi keluar.
        showExitDialog();
    }

    private void showExitDialog() {
        if (exitDialogShowing) return;
        exitDialogShowing = true;

        final boolean usePin = Config.EXIT_PIN != null && !Config.EXIT_PIN.isEmpty();
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Keluar dari ujian?")
                .setCancelable(false);

        final EditText pin = new EditText(this);
        if (usePin) {
            pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            pin.setHint("PIN");
            b.setMessage("Masukkan PIN untuk keluar. Jawaban yang sudah tersimpan di server tidak hilang.");
            b.setView(pin);
        } else {
            b.setMessage("Apakah Anda yakin ingin keluar dari aplikasi ujian?");
        }

        b.setPositiveButton("Keluar", (d, w) -> {
            exitDialogShowing = false;
            if (usePin && !Config.EXIT_PIN.equals(pin.getText().toString().trim())) {
                Toast.makeText(this, "PIN salah", Toast.LENGTH_SHORT).show();
                hideSystemUi();
                return;
            }
            isExitingIntentionally = true;
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_CHEAT_LOCKED, false)
                    .apply();
            stopPinning();
            finishAndRemoveTask();
        });
        b.setNegativeButton("Lanjut Ujian", (d, w) -> { exitDialogShowing = false; hideSystemUi(); });
        AlertDialog dialog = b.create();
        if (Config.BLOCK_SCREENSHOT && dialog.getWindow() != null) {
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE);
        }
        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        pinMonitorHandler.removeCallbacks(pinMonitorRunnable);
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
