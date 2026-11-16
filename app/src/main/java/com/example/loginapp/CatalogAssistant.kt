package com.example.loginapp

import android.content.Context

/** Deterministic offline catalog helper. It is deliberately not labelled as a generative model. */
object CatalogAssistant {
    fun reply(context:Context,question:String,previousQuestion:String?=null):String{
        val q=normalizeFood(question)
        if(q.isBlank())return "Ketik nama makanan atau pertanyaan tentang katalog."
        if(listOf("obat","diagnosis","sembuh","dosis").any{q.contains(it)})return "Aku dapat membantu membaca katalog dan catatan makanan, tetapi tidak menentukan diagnosis, dosis, atau perubahan obat. Untuk keputusan tersebut, hubungi tenaga kesehatan."
        if(q.contains("rendah gula")||q.contains("diabetes"))return "Kolom gula pada data TKPI yang digunakan belum tersedia. Aku tidak bisa menyebut makanan ini rendah gula dari angka karbohidratnya. Periksa label gula atau isi catatan berdasarkan informasi yang kamu miliki; diskusikan kebutuhan pribadi dengan tenaga kesehatan."
        if(q.contains("hipertensi")||q.contains("rendah garam")||q.contains("rendah natrium")){
            val foods=FoodCatalog.all(context).filter{it.readyToEat&&it.sodium!=null&&it.sodium>0}.sortedBy{it.sodium}.take(5)
            return "Ini contoh entri siap santap dengan angka natrium tercatat relatif kecil per 100 g, bukan jaminan aman atau anjuran klinis pribadi:\n\n"+foods.joinToString("\n"){"• ${it.name}: ${it.sodium!!.toInt()} mg / 100 g"}+"\n\nPorsi, garam, kuah, dan saus mengubah totalnya. Kebutuhan pribadimu perlu disesuaikan bersama tenaga kesehatan."
        }
        if(q.contains("jajanan")||q.contains("cemilan")||q.contains("camilan")){
            val names=FoodCatalog.all(context).filter{it.popular&&it.category=="Jajanan & kue"}.take(12)
            return "Kamu bisa mencari jajanan ini di katalog:\n\n"+names.joinToString("\n"){"• ${it.name}"}+"\n\nGunakan pencarian untuk seblak, cilok, cimol, martabak, klepon, dan lainnya. Nilai yang belum tersedia akan ditandai, bukan dianggap nol."
        }
        if(q.contains("rendah kalori")){
            val foods=FoodCatalog.all(context).filter{it.readyToEat&&it.calories!=null&&it.calories in 1.0..100.0}.sortedBy{it.calories}.distinctBy{it.name}.take(6)
            return "Berikut contoh hidangan olahan dengan energi hingga 100 kkal per 100 gram yang tercatat di TKPI:\n\n"+foods.joinToString("\n"){"• ${it.name}: ${it.calories!!.toInt()} kkal / 100 g"}+"\n\nIni perbandingan energi, bukan penilaian kelengkapan gizi. Ukuran porsi, minyak, dan bumbu memengaruhi total. Buka katalog untuk memilih berat dan melihat sumber."
        }
        val hasWeight=Regex("\\d{1,4}\\s*(?:gram|gr|g)\\b").containsMatchIn(q)
        val contextualQuestion=if(hasWeight&&FoodCatalog.contextMatches(context,question).isEmpty()&&!previousQuestion.isNullOrBlank())previousQuestion+" "+question else question
        val matches=FoodCatalog.contextMatches(context,contextualQuestion)
        if(matches.isNotEmpty()){
            val grams=Regex("(\\d{1,4})\\s*(?:gram|gr|g)\\b").find(q)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1,5000)?:100
            return matches.take(if(q.contains("banding"))4 else 3).joinToString("\n\n"){food->"${food.name} — $grams gram\n${food.facts(grams)}\nSumber: ${food.source}"+(food.sourcePage?.let{" (PDF hal. $it)"}?:"") }+"\n\nData TKPI memakai basis 100 g BDD, bukan otomatis 1 porsi warung. Untuk berat lain, tulis misalnya ‘nasi putih 200 gram’."
        }
        if(hasWeight)return "Makanan apa yang ingin dihitung? Tulis nama beserta berat, misalnya nasi putih 200 gram."
        if(q.contains("minuman"))return "Katalog memuat es teh, kopi susu, boba, es cendol, wedang jahe, jus, dan banyak minuman lain. Cari nama minuman lewat tombol Katalog. Gula yang tidak tercatat perlu diisi dari label atau takaran, bukan diperkirakan dari karbohidrat."
        return "Aku asisten katalog offline. Aku bisa mencari ${FoodCatalog.all(context).size} entri pangan, menampilkan gizi TKPI per berat, dan membandingkan makanan yang tercatat.\n\nCoba: ‘kalori nasi putih 200 gram’, ‘bandingkan gado-gado dan soto betawi’, atau ‘jajanan populer’.\n\nUntuk percakapan AI bebas, aktifkan Gemini melalui Pengaturan dan isi kunci API milikmu."
    }
}


