# Alur admin NutriCare — 15 November 2026

Buka Portal Admin dari login pengguna atau profil. Akun contoh: admin.demo@nutricare.id / AdminDemo123! (tombol Isi akun pemilik demo).

Admin baru: tekan Daftar akun admin khusus, isi nama, Gmail/email yang disiapkan tim, dan kata sandi khusus aplikasi. Jangan gunakan kata sandi Gmail. Setelah registrasi berhasil, akun langsung aktif dan dashboard terbuka. Ini bukan Google Sign-In dan tidak membuat akun Gmail.

Dari dashboard, Daftarkan Admin Baru atau Pengguna > + Admin menambahkan anggota tanpa mengganti sesi admin yang sedang masuk. Akun baru bisa dipakai pada login admin di perangkat ini. Akun pending versi sebelumnya masih dapat disetujui pemilik melalui Pengguna > Admin.

Dashboard menyediakan ringkasan, statistik, daftar akun, makanan, kondisi, resep, pencarian, filter, tambah/edit/hapus data, review resep, ekspor PDF, serta simulasi notifikasi. Data contoh disimpan lokal terpisah dari data pengguna. Notifikasi belum dikirim ke HP lain.

## Sambungan database oleh tim
Implementasikan AdminRepository dan ganti provider AdminServices.repository. Backend harus memvalidasi akun yang disediakan tim, hak akses admin, sesi, serta seluruh operasi CRUD di server. Registrasi langsung aktif dalam DemoAdminRepository hanya untuk prototipe lokal; bukan bukti kepemilikan Gmail dan bukan otorisasi produksi. Gunakan undangan/daftar email yang diizinkan di backend sesuai kebijakan tim. Jangan membawa akun demo ke produksi.

## Desain dan pemeriksaan
Mempertahankan dashboard native yang telah dibuat berdasarkan referensi visual Figma sebelumnya. Kecocokan 1:1 belum dapat dikonfirmasi karena batas akses plugin Figma. Tes instrumentasi diperbarui untuk login langsung, akun duplikat, dan sesi admin yang tetap saat mendaftarkan rekan.
`nValidasi 15 November 2026: APK utama dan APK instrumentasi berhasil dibangun. Emulator emulator-5554: PASS 65 feature checks. Tes mencakup repositori/autentikasi/data; tidak membuktikan kecocokan visual Figma atau sambungan Gemini langsung.
