# NutriCare — Katalog Makanan Indonesia & Asisten Gemini

Fitur ditambahkan ke proyek `C:\CODING\mk 4 psas\LoginApp` dengan mempertahankan tampilan Figma, XML, dan RecyclerView.

## Katalog

- 1.579 entri dalam 14 kategori: 1.146 entri TKPI 2017 dan 433 tambahan nama makanan/minuman populer.
- Makanan pokok, lauk ayam/daging, seafood, sayuran, telur, buah, tahu/tempe, umbi, susu, bumbu, minyak, jajanan, dan minuman.
- Contoh jajanan: seblak, cilok, cimol, cireng, cilor, pentol, sempol, martabak, klepon, putu, risoles, bakpia, batagor, siomay, dimsum, dan banyak lainnya.
- Pencarian nama/alias, kategori, serta filter hidangan olahan.
- Buka dari tombol Katalog Makanan Indonesia di Home atau pencarian pada header.
- Pada form tambah/ubah, ketik nama untuk saran otomatis atau pilih Cari dari katalog.
- Pilih berat sajian dalam gram. Nilai rujukan per 100 g dikonversi ke berat itu; jumlah porsi mengalikannya lagi.

Katalog mencakup banyak makanan umum, bukan klaim seluruh variasi masakan Indonesia. Tambahan makanan populer yang tidak memiliki rujukan angka tetap dapat dicatat.

## Ketepatan data

Rujukan: [TKPI 2017, Kementerian Kesehatan](https://repository.kemkes.go.id/book/777), dengan salinan PDF publik pada [Perpustakaan Poltekkes Kemenkes Palembang](https://lib.poltekkespalembang.ac.id/index.php?bid=15003&fid=94&p=fstream-pdf).

Data TKPI berbasis **100 gram bagian yang dapat dimakan (BDD)**, bukan satu porsi warung. Kode TKPI dan nomor halaman PDF disimpan pada data katalog untuk penelusuran. PDF lengkap tidak dibundel ke APK.

TKPI yang digunakan tidak menyediakan kolom gula total. Karbohidrat tidak dipakai sebagai pengganti gula. Nilai yang tidak tersedia tetap kosong/null. Nilai nol yang benar-benar tercantum tetap dianggap nol. Form dapat menyimpan data yang belum lengkap; kartu jurnal/detail menandainya sebagai belum tersedia dan Home memberi catatan bahwa total hanya mencakup nilai yang diketahui.

Jumlah bahan, metode memasak, bumbu, porsi, dan merek dapat mengubah angka gizi. Jangan menganggap angka referensi sebagai analisis hidangan tertentu.

## Mengaktifkan Gemini

1. Buka Chat AI Sekarang / Asisten NutriCare.
2. Pilih **Pengaturan**.
3. Aktifkan **Gunakan Gemini online**.
4. Buat/salin kunci API milikmu dari [Google AI Studio](https://aistudio.google.com/apikey), lalu tempel sendiri pada kolom kunci di aplikasi. Jangan kirim kunci melalui chat publik.
5. Biarkan **Pilih model otomatis** aktif. Aplikasi mengambil daftar model chat yang tersedia untuk kuncimu melalui models.list, sehingga tidak bergantung pada nama model tetap. Pengaturan lama memakai model bawaan sebelumnya akan otomatis bermigrasi. Tombol **Cek koneksi Gemini** menguji daftar model dan satu jawaban singkat sebelum pengaturan disimpan.
6. Pilih opsi simpan kunci terenkripsi jika ingin bertahan setelah aplikasi ditutup; fitur ini menggunakan Android Keystore pada Android 6 ke atas. Tanpa opsi itu, kunci hanya berada dalam sesi memori.
7. Simpan, tulis pertanyaan, lalu tekan Kirim.

Pesan online dikirim ke Google dan memakai kuota akun/kunci milikmu. Profil dan ringkasan jurnal hanya disertakan jika kotaknya kamu centang. Kunci tidak ditanam dalam APK atau source code, tidak dicetak ke log, dan bisa dihapus melalui Pengaturan.

Riwayat chat disimpan lokal, terpisah per akun NutriCare. Menu Hapus chat menghapus riwayat perangkat; tidak mengklaim menghapus data pada penyedia Gemini.

Untuk aplikasi produksi yang didistribusikan dengan kunci pengembang bersama, gunakan backend proxy. Mode saat ini ditujukan untuk kunci pribadi yang dimasukkan pemiliknya. Rujukan integrasi: [Gemini generateContent](https://ai.google.dev/api/generate-content) dan [keamanan API key](https://ai.google.dev/gemini-api/docs/api-key).

## Tanpa kunci / tanpa internet

Mode **Asisten katalog offline** tetap dapat mencari makanan, membaca data TKPI, membandingkan entri, dan menghitung nilai sesuai gram. Ini bantuan deterministik berbasis katalog, bukan model AI generatif, dan labelnya dibedakan dari Gemini online.

Coba:
- Kalori nasi putih 200 gram
- Bandingkan gado-gado dan soto betawi per 100 gram
- Jajanan populer

Jika Gemini gagal karena kunci, model, kuota, atau internet, aplikasi menampilkan penyebab yang sesuai dan jawaban bantuan katalog dengan label offline terpisah. Model yang menghasilkan 404 dicari penggantinya dari daftar model yang tersedia. Kegagalan autentikasi dan kuota tidak diulang ke model lain. Tombol Coba lagi mengirim ulang pertanyaan terakhir. Aplikasi tidak menyamarkan respons offline sebagai respons Gemini.

## Validasi

Pengujian perangkat mencakup jumlah/keunikan data, pencarian alias, satuan 100 g, nilai kosong, migrasi jurnal lama, isolasi riwayat per akun, enkripsi/dekripsi Android Keystore, format request Gemini, dan parsing respons. Sebanyak 43 pemeriksaan fitur lulus tanpa panggilan Gemini nyata di runner pengujian. Pengecekan koneksi terpisah melalui tombol aplikasi memakai kunci yang sudah tersimpan, tanpa membaca/menyalin nilainya. Google menolaknya sebagai API_KEY_INVALID. Belum ada respons generatif Gemini yang berhasil diverifikasi; pengguna perlu mengganti kunci melalui Pengaturan. Bantuan katalog offline telah diuji langsung.


## Logo dan versi 1.2

Logo asli dari pengguna dipakai pada login, splash screen, header seluruh halaman fitur (termasuk resep/profil), serta ikon launcher Android. Palet dan susunan utama desain sebelumnya dipertahankan.

Dokumentasi pemilihan model: [Gemini Models](https://ai.google.dev/api/models). Tidak ada kunci nyata yang ditanam dalam source/APK. Bantuan katalog langsung tersedia tanpa kunci; percakapan Gemini membutuhkan kunci pengguna yang valid, akses model, internet, dan kuota.


