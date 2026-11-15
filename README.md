# HT CBT Exam Browser

Aplikasi Android WebView untuk membuka portal CBT sekolah dan otomatis memilih login Siswa.

## Alamat ujian
https://www.officialproject.my.id/cbt-online/index.html

## Fitur
- Langsung memilih tombol "Masuk sebagai Siswa" pada halaman awal.
- WebView dengan JavaScript, DOM Storage, cookies, dan dukungan kamera.
- Mode layar penuh.
- FLAG_SECURE untuk memblokir screenshot pada Android yang mendukungnya.
- Tombol Back ditahan selama tampilan ujian (`#view-ujian`) aktif.
- Konfirmasi keluar saat tidak sedang mengerjakan ujian.

## Build APK otomatis melalui GitHub (tanpa Android Studio)

1. Buat repository baru di GitHub, lalu unggah seluruh isi folder `HT_CBT_Exam_Browser` ke repository tersebut.
2. Pastikan file workflow `.github/workflows/build-apk.yml` ikut terunggah.
3. Buka tab **Actions** di repository GitHub. Jika diminta, pilih **I understand my workflows, go ahead and enable them**.
4. Workflow **Build Android APK** akan berjalan otomatis saat ada push ke branch `main` atau `master`. Anda juga bisa menjalankannya lewat **Actions > Build Android APK > Run workflow**.
5. Setelah status workflow hijau, buka hasil run tersebut dan unduh artifact **HT-CBT-Exam-Browser-debug**. Di dalam ZIP artifact terdapat `app-debug.apk`.
6. Pindahkan APK ke HP Android, buka file tersebut, lalu izinkan pemasangan dari sumber ini jika Android memintanya.

Untuk membuat halaman rilis yang bisa diunduh, buat dan push tag versi, misalnya:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow akan membangun APK dan melampirkannya pada **GitHub Releases**. APK debug ini cocok untuk pengujian/internal; untuk distribusi publik jangka panjang, siapkan penandatanganan release dengan keystore yang disimpan sebagai GitHub Secrets.

## Catatan penting
- Proyek ini adalah source project Android Studio, bukan APK yang sudah ditandatangani.
- Website harus tetap online dan endpoint `api.php` harus aktif.
- `siswa.html` dan `script.js` tetap di hosting; aplikasi memuat website yang sudah ada.
- Script otomatis memilih tombol dengan teks "Masuk sebagai Siswa". Jika desain/teks tombol di website berubah, penyesuaian selector mungkin diperlukan.
- Izin kamera diminta melalui dialog izin Android ketika website meminta kamera.
- `FLAG_SECURE` dan pembatasan tombol Back bukan kiosk mode penuh. Untuk penguncian perangkat tingkat sekolah diperlukan Android Enterprise/Lock Task Mode yang dikonfigurasi oleh administrator perangkat.
- Uji terlebih dahulu di beberapa tipe HP Android, terutama alur login siswa, izin kamera, timer, autosave, dan pengiriman jawaban.
