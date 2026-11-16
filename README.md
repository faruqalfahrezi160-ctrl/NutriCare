# NutriCare — Layout XML sesuai ketentuan tugas

Semua halaman wajib kini memakai Android Views/XML saat dijalankan. Launcher berada di SplashActivity, bukan MainActivity Compose lama. Aset dan warna NutriCare tetap digunakan. Halaman resep/profil tambahan masih memakai komponen Compose di dalam activity_extra.xml.

| Ketentuan | File di app/src/main/res/layout | Penghubung Kotlin |
| --- | --- | --- |
| Splash beberapa detik | activity_splash.xml | SplashActivity, otomatis 2 detik ke Login/Home |
| Login email, sandi, tombol Login | activity_login.xml | LoginActivity, validasi dan autentikasi lokal |
| Register nama, email, sandi | activity_register.xml | RegisterActivity, konfirmasi sandi dan pendaftaran lokal |
| Home/Dashboard | activity_home.xml | HomeActivity, ringkasan jurnal hari ini dan navigasi |
| Daftar data RecyclerView | activity_list_data.xml | DataListActivity + MealXmlAdapter |
| Satu baris RecyclerView | item_row.xml | ItemRowBinding, di-inflate oleh adapter |
| Form tambah data | activity_form_input.xml | FormInputActivity |
| Form ubah data | activity_form_update.xml | FormUpdateActivity, terisi data yang dipilih |
| Detail dan hapus | activity_detail.xml | DetailActivity + AlertDialog konfirmasi |

## Cara menjalankan

Buka proyek C:\CODING\mk 4 psas\LoginApp di Android Studio. Gunakan Gradle JDK bawaan (JBR 17/21), SDK Android 34, lakukan Sync Gradle, lalu pilih modul app dan Run.

Splash muncul sekitar 2 detik. Jika sebelumnya memilih Ingat Saya, aplikasi langsung membuka Home. Jika belum, aplikasi membuka Login. Gunakan tombol Isi akun demo lokal, kemudian Login.

Demo: demo@nutricare.id / NutriCare123. Akun lama dari versi sebelumnya tetap tersimpan.

Untuk melihat XML, buka app/src/main/res/layout dan pilih tampilan Code, Split, atau Design. RecyclerView memiliki tools:listitem untuk pratinjau baris di editor.

## Memahami kode

- ViewBinding diaktifkan pada app/build.gradle untuk menghubungkan ID XML dengan Kotlin. Misalnya ActivityLoginBinding memberikan akses ke etEmail, etPassword, dan btnLogin.
- XmlActivities.kt berisi Activity halaman wajib. Setiap Activity meng-inflate XML yang sesuai atau memakai setContentView pada form bersama.
- MealXmlAdapter merupakan ListAdapter berbasis RecyclerView dengan DiffUtil. onCreateViewHolder meng-inflate item_row.xml; onBindViewHolder mengisi nama, waktu, porsi, foto, dan nutrisi.
- FoodFormActivity membagi logika validasi dan simpan antara FormInputActivity dan FormUpdateActivity. Keduanya menggunakan file layout XML terpisah.
- LocalStore.kt menyimpan akun dan jurnal per email. Sandi menggunakan hash PBKDF2 dengan salt; jurnal dan favorit disimpan di SharedPreferences.
- DetailActivity menampilkan data terbaru di onResume sehingga perubahan dari FormUpdateActivity langsung terlihat. Penghapusan hanya dilakukan setelah tombol Hapus pada AlertDialog ditekan; Batal tidak menghapus apa pun.
- Register mengembalikan pengguna ke Login setelah berhasil. Login memulai Home dan membersihkan back stack autentikasi.

## Batas fitur

Katalog berisi 1.579 entri dalam 14 kategori: 1.146 rujukan TKPI 2017 dan 433 nama populer. Nilai yang belum tersedia ditandai, termasuk gula total yang tidak tersedia dalam sumber TKPI. Form mendukung pencarian katalog dan berat gram. Asisten katalog offline dapat langsung digunakan. Gemini online memakai kunci pribadi yang dimasukkan melalui Pengaturan aplikasi; model chat dipilih otomatis dari daftar yang tersedia. Pengecekan langsung menunjukkan kunci yang tersimpan ditolak sebagai API_KEY_INVALID; ganti melalui Pengaturan. Bantuan katalog offline tetap tersedia dengan label terpisah. Lihat [panduan katalog dan AI](docs/Katalog-AI.md). Google/Apple OAuth, pemulihan sandi via email, koneksi klinis, dan SatuSehat belum tersedia. Foto opsional dipilih melalui galeri.

## Verifikasi build

assembleDebug dan lintAnalyzeDebug berhasil. Hasil analisis terakhir tidak mengandung error; masih ada peringatan gaya kode, aksesibilitas, dan kompatibilitas yang tidak menghambat build.

- Di emulator: splash beralih otomatis, daftar memakai RecyclerView, tambah/ubah data, batal/hapus lewat dialog, serta register akun baru sudah berhasil diuji.

## Pemulihan tampilan Figma

Tampilan XML kini kembali mengikuti acuan Figma: header gradien hijau dan gelombang pada login, tombol coral, kartu nutrisi dengan indikator lingkaran, kartu fitur bergambar, ikon navigasi, serta kartu jurnal dan form dengan pemilih waktu makan. Foto dan ikon memakai aset Figma lokal. RecyclerView dan sembilan layout XML wajib tetap aktif.



## Logo dan perbaikan asisten versi 1.2

Logo pengguna dipakai di login, splash, ikon launcher, dan header semua halaman fitur. Asisten menampilkan penyebab kegagalan, menyediakan tombol cek koneksi dan coba lagi, serta jawaban katalog offline ketika Gemini tidak dapat dipakai. Build dan lint berhasil; 43 pemeriksaan fitur lulus di emulator.

