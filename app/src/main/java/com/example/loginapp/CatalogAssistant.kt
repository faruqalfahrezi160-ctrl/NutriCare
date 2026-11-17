package com.example.loginapp

import android.content.Context
import java.util.Locale

/**
 * Asisten Nutrisi Cerdas NutriCare.
 * Mampu memberikan konsultasi gizi, rekomendasi pola makan, edukasi penyakit kronis,
 * dan kalkulasi pangan lokal secara alami dan kontekstual.
 */
object CatalogAssistant {

    fun reply(context: Context, question: String, previousQuestion: String? = null): String {
        val raw = question.trim()
        val q = normalizeFood(raw)

        if (q.isBlank()) {
            return "Halo! Ada yang bisa kubantu seputar menu makanan, kalori, atau tips diet sehat hari ini? 😊"
        }

        // 1. Sapaan & Percakapan Ramah
        if (q in listOf("halo", "hai", "helo", "hello", "hi", "pagi", "selamat pagi", "siang", "selamat siang", "malam", "selamat malam", "sore", "selamat sore")) {
            return "Halo! Senang bisa mendampingimu hari ini. Aku Asisten Nutrisi NutriCare 🥗\n\nKamu bisa tanyakan apa saja padaku, misalnya:\n• 'Rekomendasi menu makan siang sehat'\n• 'Berapa kalori soto ayam?'\n• 'Makanan apa yang aman untuk hipertensi?'\n• 'Tips diet defisit kalori'\n\nApa yang ingin kamu diskusikan hari ini?"
        }

        if (q.contains("siapa kamu") || q.contains("kamu siapa") || q.contains("nama kamu")) {
            return "Aku adalah **Asisten Nutrisi NutriCare**, asisten pintarmu untuk memandu pola makan seimbang, memantau nutrisi harian, dan memilih hidangan nusantara yang aman untuk kesehatanmu."
        }

        if (q.contains("terima kasih") || q.contains("makasih") || q.contains("makasi") || q.contains("tengkyu") || q.contains("thank")) {
            return "Sama-sama! Selalu senang bisa membantumu menjaga kesehatan. Jangan ragu bertanya lagi jika butuh panduan nutrisi lainnya ya! Tetap semangat! ✨"
        }

        if (q.contains("apa kabar") || q.contains("gimana kabarmu")) {
            return "Aku sehat dan selalu siap membantumu! Bagaimana dengan pola makan dan target nutrisimu hari ini? Sudah sarapan atau makan siang seimbang?"
        }

        // 2. Pertanyaan Medis Kritis (Obat / Dosis / Pengobatan)
        if (listOf("minum obat", "dosis obat", "sembuh total", "ganti obat", "obat generik").any { q.contains(it) }) {
            return "⚠️ **Catatan Penting Medis:**\nAku dapat membantumu mengatur asupan makanan dan pola gizi seimbang, namun tidak berwenang meresepkan atau mengubah dosis obat medis.\n\nUntuk aturan konsumsi dan perubahan obat-obatan, mohon selalu berkonsultasi langsung dengan dokter atau apoteker ya!"
        }

        // 3. Konsultasi Hipertensi & Rendah Garam (Natrium)
        if (q.contains("hipertensi") || q.contains("darah tinggi") || q.contains("rendah garam") || q.contains("rendah natrium")) {
            val foods = FoodCatalog.all(context)
                .filter { it.readyToEat && it.sodium != null && it.sodium in 1.0..300.0 }
                .sortedBy { it.sodium }
                .take(4)
            val examples = if (foods.isNotEmpty()) {
                "\n\nContoh menu ramah natrium per 100g:\n" + foods.joinToString("\n") { "• **${it.name}**: ~${it.sodium!!.toInt()} mg natrium" }
            } else ""

            return "🩺 **Panduan Nutrisi untuk Hipertensi:**\n" +
                    "• Batasi asupan natrium harian maksimal **1.500 – 2.000 mg** (setara ± 1 sendok teh garam dapur per hari).\n" +
                    "• Hindari makanan kaleng, jeroan, ikan asin, bumbu penyedap instan (MSG tinggi), dan kerupuk berlebih.\n" +
                    "• Perbanyak asupan kalium alami dari buah & sayuran (seperti pisang, pepaya, bayam, dan tomat) untuk membantu menstabilkan tensi darah.\n" +
                    "• Saat menyantap soto atau sup, **kurangi meminum kuah kentalnya** karena kandungan garam dan lemak paling pekat berada di kuah.$examples"
        }

        // 4. Konsultasi Diabetes & Gula Darah
        if (q.contains("diabetes") || q.contains("gula darah") || q.contains("rendah gula") || q.contains("kencing manis")) {
            return "🩸 **Panduan Nutrisi untuk Gula Darah & Diabetes:**\n" +
                    "• Ganti karbohidrat sederhana (nasi putih porsi besar, tepung, roti putih) dengan **karbohidrat kompleks** berserat tinggi seperti beras merah, oat, atau ubi jalar rebus.\n" +
                    "• Hindari minuman manis cair (es teh manis, boba, sirup, kopi sachet) karena memicu lonjakan gula darah sangat cepat.\n" +
                    "• Terapkan aturan piring sehat T (1/2 piring sayuran hijau, 1/4 piring protein seperti telur/ikan/tempe, 1/4 piring karbohidrat kompleks).\n" +
                    "• Usahakan makan teratur sesuai jadwal untuk mencegah gula darah naik-turun drastis."
        }

        // 5. Konsultasi Kolesterol & Lemak Darah
        if (q.contains("kolesterol") || q.contains("lemak jenuh") || q.contains("trigliserida")) {
            return "🥑 **Tips Mengendalikan Kolesterol:**\n" +
                    "• Kurangi makanan yang digoreng berulang (*deep-fried*), jeroan, kulit ayam, dan masakan bersantan kental.\n" +
                    "• Utamakan metode memasak **kukus, rebus, pepes, atau panggang**.\n" +
                    "• Konsumsi lemak baik yang kaya Omega-3, seperti ikan mas, kembung, tuna, serta alpukat dan kacang-kacangan tanpa gorengan.\n" +
                    "• Perbanyak serat larut air (oatmeal, apel, kacang merah) yang efektif mengikat kolesterol di saluran cerna."
        }

        // 6. Asam Urat
        if (q.contains("asam urat") || q.contains("gout")) {
            return "🦵 **Panduan Makanan untuk Asam Urat:**\n" +
                    "• **Batasi makanan tinggi purin:** jeroan, ekstrak kaldu daging pekat, emping melinjo, kerang, kepiting, dan udang.\n" +
                    "• Cukupi minum air putih minimal 2 – 2,5 liter per hari untuk membantu ginjal membuang kelebihan asam urat.\n" +
                    "• Sumber protein yang lebih aman: tahu, tempe, telur, serta ikan air tawar dalam porsi wajar."
        }

        // 7. Diet & Penurunan Berat Badan / Kalori
        if (q.contains("diet") || q.contains("turun berat") || q.contains("langsing") || q.contains("kurus") || q.contains("defisit kalori")) {
            return "⚖️ **Kunci Sukses Diet Defisit Kalori:**\n" +
                    "1. **Defisit Kalori Moderat:** Kurangi 300 – 500 kkal dari kebutuhan harianmu agar penurunan berat badan konsisten dan tidak lemas.\n" +
                    "2. **Tingkatkan Protein:** Telur, tahu, tempe, dada ayam, dan ikan memberi rasa kenyang lebih lama dan menjaga massa otot.\n" +
                    "3. **Waspadai Kalori Tersembunyi:** Minuman manis, saus mayones, bumbu kacang kental, dan gorengan sering kali mengandung kalori sangat tinggi.\n" +
                    "4. **Jangan Lewatkan Sarapan:** Sarapan kaya serat & protein mencegah kamu makan berlebihan di siang dan malam hari."
        }

        // 8. Rekomendasi Menu Berdasarkan Waktu Makan
        if (q.contains("sarapan") || q.contains("pagi")) {
            return "🌅 **Ide Menu Sarapan Sehat Nusantara:**\n" +
                    "• **Pilihan 1:** Bubur beras merah dengan suwiran ayam tanpa kulit, kuah kaldu bening, dan taburan daun seledri (~300 kkal).\n" +
                    "• **Pilihan 2:** 2 butir telur rebus + 1 potong pisang kepok kukus + segelas teh hijau hangat tanpa gula (~260 kkal).\n" +
                    "• **Pilihan 3:** Oatmeal seduh dengan potongan pepaya atau pisang raja lokal (~220 kkal)."
        }

        if (q.contains("makan siang") || q.contains("siang")) {
            return "☀️ **Ide Menu Makan Siang Bergizi Seimbang:**\n" +
                    "• **Pilihan 1:** Nasi putih 1 centong (~100g) + Pepes Ikan Mas kemangi + Sayur asem bening + Tempe bacem kukus (~420 kkal).\n" +
                    "• **Pilihan 2:** Gado-gado komplit (telur rebus, tahu, sayuran melimpah), **saus kacang dipisah** dan gunakan separuh porsi saja (~380 kkal).\n" +
                    "• **Pilihan 3:** Soto ayam bening tanpa jeroan, kurangi koya gurih, perbanyak tauge dan kol (~350 kkal)."
        }

        if (q.contains("makan malam") || q.contains("malam")) {
            return "🌙 **Ide Menu Makan Malam Ringan & Nyaman di Perut:**\n" +
                    "• **Pilihan 1:** Sup tahu ayam bening dengan wortel dan jamur kuping (~250 kkal).\n" +
                    "• **Pilihan 2:** Sayur bening bayam jagung manis + 2 potong tempe panggang/garang asem (~220 kkal).\n" +
                    "• **Tips:** Usahakan makan malam 2 – 3 jam sebelum tidur agar sistem pencernaan tidak terbebani saat beristirahat."
        }

        if (q.contains("camilan") || q.contains("cemilan") || q.contains("snack")) {
            return "🍎 **Rekomendasi Camilan Sehat & Rendah Kalori:**\n" +
                    "• Buah potong segar (pepaya, apel, semangka) — kaya serat dan air.\n" +
                    "• Edamame atau kacang kedelai rebus tanpa garam (~120 kkal per mangkuk kecil).\n" +
                    "• Ubi jalar atau singkong kukus porsi kecil.\n" +
                    "• Hindari gorengan pinggir jalan (bakwan, cireng, tahu isi) yang menyerap banyak minyak trans."
        }

        // 9. Pertanyaan Seputar Makanan Spesifik di Katalog
        val hasWeight = Regex("\\d{1,4}\\s*(?:gram|gr|g)\\b").containsMatchIn(q)
        val grams = Regex("(\\d{1,4})\\s*(?:gram|gr|g)\\b").find(q)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, 5000) ?: 100
        val contextualQuestion = if (hasWeight && FoodCatalog.contextMatches(context, question).isEmpty() && !previousQuestion.isNullOrBlank()) {
            previousQuestion + " " + question
        } else question

        val matches = FoodCatalog.contextMatches(context, contextualQuestion)
        if (matches.isNotEmpty()) {
            val top = matches.take(3)
            val info = top.joinToString("\n\n") { food ->
                val cal = food.scaled(food.calories, grams)?.let { "$it kkal" } ?: "Data belum tersedia"
                val prot = food.protein?.let { String.format(Locale.ROOT, "%.1f g", it * grams / 100.0) } ?: "-"
                val fat = food.fat?.let { String.format(Locale.ROOT, "%.1f g", it * grams / 100.0) } ?: "-"
                val carb = food.carbs?.let { String.format(Locale.ROOT, "%.1f g", it * grams / 100.0) } ?: "-"
                val sod = food.scaled(food.sodium, grams)?.let { "$it mg" } ?: "-"

                "🍽️ **${food.name}** (${grams} gram):\n" +
                        "• Energi: **$cal**\n" +
                        "• Protein: $prot | Lemak: $fat | Karbohidrat: $carb\n" +
                        "• Natrium/Garam: $sod\n" +
                        "• Kategori: ${food.category}"
            }

            val tip = if (q.contains("soto") || q.contains("bakso") || q.contains("mie") || q.contains("mi")) {
                "\n\n💡 *Tips Sehat:* Kurangi menghirup habis kuah olahan ini untuk memangkas kelebihan asupan garam & minyak berlebih."
            } else if (q.contains("goreng") || q.contains("ayam")) {
                "\n\n💡 *Tips Sehat:* Tiriskan minyak dengan tisu dapur atau pilih bagian dada tanpa kulit untuk mengurangi lemak jenuh."
            } else ""

            return "Berikut data nutrisi yang ditemukan:\n\n$info$tip"
        }

        // 10. Jika Pertanyaan Tidak Ditemukan Makanan Spesifik
        return "Aku mengerti kamu sedang menanyakan tentang: *\"$raw\"*.\n\n" +
                "💡 **Saran Asisten NutriCare:**\n" +
                "Untuk menjaga kesehatanmu, utamakan komposisi gizi seimbang:\n" +
                "• Konsumsi cukup protein harian (ikan, tempe, telur).\n" +
                "• Penuhi separuh piring dengan aneka sayur dan buah lokal.\n" +
                "• Cukupi hidrasi air putih minimal 2 liter setiap hari.\n\n" +
                "Kamu juga bisa ketik nama menu tertentu seperti *'kalori soto ayam'*, *'gado-gado'*, atau *'rekomendasi sarapan'* agar aku beri analisa yang lebih spesifik! 🥗"
    }
}
