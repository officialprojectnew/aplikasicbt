package id.sch.hidayatuttaufiq.cbt;

/** Pengaturan utama aplikasi. Ubah di sini saja. */
public final class Config {
    private Config() {}

    /** Alamat halaman ujian. */
    public static final String EXAM_URL = "https://www.officialproject.my.id/cbt-online/index.html";

    /** Hanya domain ini yang boleh dibuka di dalam aplikasi. */
    public static final String ALLOWED_HOST = "officialproject.my.id";

    /** PIN pengawas untuk keluar aplikasi. Kosongkan ("") jika hanya ingin konfirmasi Ya/Tidak. */
    public static final String EXIT_PIN = "123456789";

    /** PIN khusus rahasia dari guru untuk membuka kunci layar hitam jika siswa unpin/keluar aplikasi. */
    public static final String UNLOCK_PIN = "1476";

    /** Aktifkan pin layar (screen pinning) agar tombol Home/Recent sulit dipakai. */
    public static final boolean USE_SCREEN_PINNING = true;

    /** Blokir screenshot & perekaman layar. */
    public static final boolean BLOCK_SCREENSHOT = true;

    /** Tambahan pada User-Agent, bisa dipakai server untuk mengenali aplikasi ini. */
    public static final String UA_SUFFIX = " CBTApp/1.0 SDITHT";
}
