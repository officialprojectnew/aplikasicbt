# CBT SDIT Hidayatut Taufiq — Aplikasi Android (WebView)

Aplikasi pembungkus (exam browser) untuk sistem CBT di
`https://www.officialproject.my.id/cbt-online/index.html`.
Sistem ujian (siswa.html, script.js, api.php) tidak diubah sama sekali.

## Cara membuat APK
1. Install **Android Studio** (versi terbaru), pilih **Open** lalu pilih folder ini. Tunggu Gradle sync
   selesai (otomatis mengunduh Gradle 8.7 & SDK 34; butuh internet).
2. Uji: colok HP (USB debugging aktif) lalu tekan **Run**.
3. APK untuk dibagikan: **Build > Build Bundle(s)/APK(s) > Build APK(s)**
   (hasil: `app/build/outputs/apk/debug/app-debug.apk`).
4. Untuk rilis: **Build > Generate Signed Bundle / APK**, buat keystore
   (SIMPAN keystore & passwordnya baik-baik).

## Pengaturan (file `Config.java`)
| Konstanta | Fungsi |
|---|---|
| `EXAM_URL` | Alamat halaman ujian |
| `ALLOWED_HOST` | Domain yang boleh dibuka (tautan lain diblokir) |
| `EXIT_PIN` | PIN pengawas untuk keluar (`""` = hanya konfirmasi Ya/Tidak). **Ganti PIN default 2026!** |
| `USE_SCREEN_PINNING` | Pin layar agar Home/Recent sulit dipakai |
| `BLOCK_SCREENSHOT` | Blokir screenshot & rekam layar (FLAG_SECURE) |

## Mengganti logo
Klik kanan `app/src/main/res` > **New > Image Asset**, pilih `logo.png` sekolah.
Logo di layar pembuka (`drawable/ic_book.xml`) bisa diganti dengan gambar sendiri.

## Fitur
- Fullscreen immersive, layar tetap menyala, portrait & landscape
  (rotasi tidak me-reload halaman, jadi timer/kamera tidak terganggu).
- Anti-screenshot (FLAG_SECURE): layar hitam di Recent Apps & rekaman layar.
- Tombol Back tidak mundur halaman; hanya menampilkan dialog keluar (dengan PIN pengawas).
- Deteksi pindah aplikasi / split-screen: peringatan + penghitung pelanggaran.
- Izin kamera diminta saat aplikasi dibuka; diteruskan ke getUserMedia di halaman ujian.
- Hanya domain sekolah yang bisa dibuka; halaman "Coba Lagi" saat offline.
- User-Agent ditambah `CBTApp/1.0 SDITHT`.

## Batasan
- **Screen pinning**: Android menampilkan dialog "Sematkan aplikasi"; siswa bisa menolak.
  Kunci penuh (kiosk) butuh HP dikelola sebagai *device owner* / MDM.
- Pelanggaran hanya ditampilkan di HP. Agar masuk ke server/pengawas, siswa.html perlu dimodifikasi.
- Aplikasi bisa **diwajibkan** di server: tolak login jika User-Agent tidak mengandung `CBTApp/`.
