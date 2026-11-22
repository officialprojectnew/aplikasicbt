package id.sch.hidayatuttaufiq.cbt;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Button btnScanQr = findViewById(R.id.btnScanQr);
        btnScanQr.setOnClickListener(v -> mulaiScan());
    }

    private void mulaiScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Arahkan kamera ke Barcode / QR Code Ujian");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String urlUjian = result.getContents().trim();
                if (!urlUjian.startsWith("http://") && !urlUjian.startsWith("https://")) {
                    if (urlUjian.contains(".") && !urlUjian.contains(" ")) {
                        urlUjian = "https://" + urlUjian;
                    }
                }
                if (urlUjian.startsWith("http://") || urlUjian.startsWith("https://")) {
                    Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                    intent.putExtra("URL_UJIAN", urlUjian);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Barcode tidak berisi link website yang valid!", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
