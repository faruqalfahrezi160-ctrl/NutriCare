package com.example.loginapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

@Composable fun RecipesPage(favorites:Set<String>,detail:(Int)->Unit,favorite:(String)->Unit,ai:()->Unit){
    var search by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf("Semua Resep")};var fridge by remember{mutableStateOf(false)};var ingredients by rememberSaveable{mutableStateOf("")}
    val matching=recipes.withIndex().filter{(_,r)->r.name.contains(search,true)&&(category=="Semua Resep"||category==r.category||category=="Favorit"&&r.name in favorites)}
    Scrolling{
        T("KATALOG NUTRISI NUSANTARA",9,true,Green)
        TitleRow("Resep Sehat Lokal","Cek Kulkas"){fridge=true}
        Field("Cari resep",search,{search=it},"Cari pepes ikan, sayur bening, tumis labu…",icon="s1398_5.svg")
        ChoiceRow(listOf("Semua Resep","Rendah Natrium","Ramah Diabetes","Favorit"),category,{category=it})
        Panel(Green){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Svg("s1398_6.svg",26);Column{T("REKOMENDASI PINTAR",9,true,PaleMint);T("AI Nutritionist",18,true,Color.White)}};T("Rempah aromatik seperti serai, daun salam, lengkuas, dan kunyit memberi cita rasa nusantara. Jelajahi contoh resep lokal di bawah.",13,color=Color.White);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Pill("Bebas MSG");Pill("Rempah Alami");Pill("Panduan Lengkap")};TextButton(onClick=ai){T("Tanya asisten",11,true,Color.White)}}
        TitleRow("Pilihan Menu Hari Ini","${matching.size} Rekomendasi")
        if(matching.isEmpty())Panel{T("Belum ada resep yang cocok.",14,true);T("Coba kata kunci atau filter lain.",12,color=Muted)}
        matching.forEach{(index,r)->Surface(shape=RoundedCornerShape(16.dp),color=Color.White){Column{
            Box(Modifier.fillMaxWidth().height(200.dp).clickable{detail(index)}){
                Asset(r.photo,Modifier.fillMaxSize(),r.name,true)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xAA111C2D)))))
                Column(Modifier.align(Alignment.TopStart).padding(12.dp)){Pill(r.category)}
                Surface(Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clickable{favorite(r.name)},shape=CircleShape,color=if(r.name in favorites)PaleMint else Color.White){Box(contentAlignment=Alignment.Center){if(r.name in favorites)T("✓",20,true,Green)else Asset("s1398_9.svg",Modifier.size(17.dp),"Simpan resep favorit")}}
                Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){T("◷ ${r.minutes} Menit",11,true,Color.White);T("Tingkat: Mudah",11,true,Color.White)}
            }
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){T(r.name,16,true);T(r.description,12,color=Muted);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Metric("Energi",r.calories.toString(),"kkal",Modifier.weight(1f));Metric("Natrium",r.sodium.toString(),"mg",Modifier.weight(1f));Metric(if(index==1)"Serat" else "Protein",r.protein,"gram",Modifier.weight(1f))};Action("Lihat Bahan & Masak",Green,"s1398_11.svg"){detail(index)}}
        }}}
        TitleRow("Tips Dapur Sehat NutriCare")
        Panel(Lavender){T("Pengganti Alami Micin / MSG",15,true);T("Coba jamur, bawang putih sangrai, dan rempah untuk menambah rasa pada masakan.",12,color=Muted);Divider();T("Teknik Masak Bebas Deep Frying",15,true);T("Kukus atau rebus bahan makanan. Sesuaikan bumbu dan teknik memasak dengan kebutuhanmu.",12,color=Muted)}
        Action("Bingung Mau Masak Apa? Cocokkan Bahan"){fridge=true}
    }
    if(fridge)AlertDialog(onDismissRequest={fridge=false},title={T("Cek Kulkas",20,true)},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){Field("Bahan yang tersedia",ingredients,{ingredients=it},"Contoh: ikan, kemangi, tempe");val tokens=ingredients.lowercase().split(","," ").filter{it.length>2};val found=recipes.filter{r->tokens.any{token->r.name.contains(token,true)||r.ingredients.any{it.first.contains(token,true)}}};if(ingredients.isNotBlank())if(found.isEmpty())T("Belum ada resep lokal yang cocok dengan bahan ini.",12,color=Muted)else found.forEach{r->TextButton(onClick={fridge=false;detail(recipes.indexOf(r))}){T(r.name,12,true,Green)}}}},confirmButton={TextButton(onClick={fridge=false}){T("Tutup",14,true,Green)}})
}
@Composable fun Metric(label:String,value:String,unit:String,modifier:Modifier=Modifier){Surface(modifier,shape=RoundedCornerShape(10.dp),color=Lavender){Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally){T(label,10,color=Muted);T(value,20,true,Green);T(unit,9,color=Muted)}}}
@Composable fun RecipeDetail(recipe:Recipe,saved:Boolean,favorite:()->Unit,ai:()->Unit){
    var tab by rememberSaveable(recipe.name){mutableStateOf("Daftar Bahan")};var portions by rememberSaveable(recipe.name){mutableStateOf("2×")};var checked by rememberSaveable(recipe.name){mutableStateOf(listOf<Int>())}
    Scrolling{
        // The Figma detail hero contains a gradient rather than a dish bitmap.
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(14.dp)).background(Brush.verticalGradient(listOf(Color(0xFFDEE8FF),Color(0xFF566174))))){
            Pill("Rekomendasi AI");Surface(Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clickable(onClick=favorite),shape=CircleShape,color=if(saved)PaleMint else Color.White){Box(contentAlignment=Alignment.Center){if(saved)T("✓",20,true,Green)else Asset("s1992_2.svg",Modifier.size(17.dp),"Simpan favorit")}}
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Pill(recipe.category);T(recipe.name,20,true,Color.White);T("◷ ${recipe.minutes} Menit   ↗ Mudah   2 Porsi",11,color=Color.White)}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Metric("Kalori",recipe.calories.toString(),"kkal",Modifier.weight(1f));Metric("Natrium",recipe.sodium.toString(),"mg",Modifier.weight(1f));Metric("Protein",recipe.protein,"gram",Modifier.weight(1f));Metric("Lemak Baik","8","gram",Modifier.weight(1f))}
        Panel(Lavender){Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Svg("s1992_6.svg",26);Column{T("Tips Gizi NutriCare AI",16,true);T("Ilustrasi rekomendasi nutrisi",10,color=Muted)}};T(if(recipe==recipes[0])"Bumbu kunyit bakar, daun salam, serai geprek, dan asam kandis memberi umami alami. Gunakan garam secukupnya sesuai kebutuhanmu." else recipe.description,13,color=Muted);TextButton(onClick=ai){T("Tanya AI: Ganti Bahan atau Bumbu",12,true,Green)}}
        ChoiceRow(listOf("Daftar Bahan","Langkah Memasak"),tab,{tab=it})
        if(tab=="Daftar Bahan"){
            Panel{T("Sesuaikan Porsi",13,true);T("Takaran di bawah untuk 1 porsi, kalikan sesuai pilihan.",11,color=Muted);ChoiceRow(listOf("1×","2×","4×"),portions,{portions=it})}
            recipe.ingredients.forEachIndexed{i,(name,amount)->Surface(Modifier.fillMaxWidth().clickable{checked=if(i in checked)checked-i else checked+i},shape=RoundedCornerShape(12.dp),color=Color.White){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.Top,horizontalArrangement=Arrangement.spacedBy(8.dp)){Checkbox(i in checked,{checked=if(it)checked+i else checked-i},Modifier.size(28.dp));Column(Modifier.weight(1f)){T(name,14,true);T("${portions.removeSuffix("×")} × $amount",12,color=Muted)};Svg("s1992_${8+i}.svg",18)}}}
        }else recipe.steps.forEachIndexed{i,step->Panel{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Pill("${i+1}");T(step,13,modifier=Modifier.weight(1f))}}}
        T("Resep dan nilai gizi untuk demonstrasi aplikasi.",10,color=Muted)
    }
}

@Composable fun ProfilePage(store:LocalStore,email:String,explain:(String,String)->Unit,ai:()->Unit,logout:()->Unit){
    val adminContext=androidx.compose.ui.platform.LocalContext.current
    var reminder by rememberSaveable(email){mutableStateOf(store.reminder(email))};var confirm by remember{mutableStateOf(false)};var conditionDialog by remember{mutableStateOf(false)}
    Scrolling{
        Panel{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Asset("s1718_0.png",Modifier.size(56.dp).clip(CircleShape),"Avatar contoh",true);Column{T(store.name(email),20,true);Pill("NutriCare • Akun Lokal");T(email,11,color=Muted)}};Panel(Lavender){T("KONDISI KESEHATAN",9,true,Muted);T(store.condition(email),16,true);Surface(Modifier.fillMaxWidth().clickable(onClick=ai),shape=RoundedCornerShape(10.dp),color=Color.White){Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Svg("s1718_4.svg",20);Column{T("Asisten AI NutriCare",11,true);T("Buka asisten NutriCare",10,color=Muted)}}}}}
        Panel{TitleRow("Target Diet Harian");T("Target contoh untuk demonstrasi aplikasi",11,color=Muted);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Metric("NATRIUM","<1.500","mg / hari",Modifier.weight(1f));Metric("KALORI","1.800","kkal / hari",Modifier.weight(1f));Metric("AIR MINERAL","2.2","liter / hari",Modifier.weight(1f))};T("Sesuaikan kebutuhan gizi bersama tenaga kesehatan.",11,color=Muted)}
        T("PENGATURAN DIET & MEDIS",10,true,Muted)
        Panel{
            SettingRow("Kondisi Kesehatan & Alergi",store.condition(email),"s1718_11.svg"){conditionDialog=true}
            SettingRow("Preferensi Diet Lokal","Jawa/Sunda • Batasi Santan • Tanpa MSG","s1718_13.svg"){explain("Preferensi Diet Lokal","Resep lokal yang tersedia: pepes ikan, sayur asem, dan tahu tempe bacem. Personalisasi klinis belum terhubung.")}
            SettingRow("Riwayat Chat & Rekomendasi AI","Konsultasi dan panduan porsi harian","s1718_14.svg",ai)
            Row(verticalAlignment=Alignment.CenterVertically){Svg("s1718_15.svg",24);Column(Modifier.weight(1f).padding(start=12.dp)){T("Pengingat Makan & Jurnal",14,true);T(if(reminder)"Jadwal ditampilkan di aplikasi" else "Nonaktif",10,color=Muted)};Switch(reminder,{reminder=it;store.reminder(email,it)})}
            if(reminder)T("Pagi 07:00 • Siang 12:30 • Malam 19:00\nPengingat lokal ini belum mengirim notifikasi sistem.",11,color=Muted)
            SettingRow("Sinkronisasi Preferensi AI","Mode lokal • Tidak ada server","s1718_16.svg"){explain("Penyimpanan Lokal","Data belum tersinkron ke layanan online. Catatan tetap tersimpan di perangkat ini setelah aplikasi ditutup.")}
        }
        Panel{SettingRow("Portal Admin & Pengembang","Registrasi khusus dan dashboard pengelola","s1718_18.svg"){adminContext.startActivity(android.content.Intent(adminContext,AdminLoginActivity::class.java))}}
        T("AKUN & BANTUAN",10,true,Muted)
        Panel{SettingRow("Keamanan & Kata Sandi",email,"s1718_18.svg"){explain("Keamanan Akun","Kata sandi akun lokal disimpan sebagai hash dengan salt. Pemulihan melalui email belum tersedia. Gunakan akun demo jika hanya ingin mencoba: demo@nutricare.id / NutriCare123.")};SettingRow("Pusat Bantuan & Edukasi Gizi","Panduan penggunaan & FAQ","s1718_19.svg"){explain("Bantuan NutriCare","Gunakan Jurnal untuk menambah, mengubah, dan menghapus catatan makanan. Foto dapat dipilih dari galeri. Buka Resep Sehat untuk bahan, langkah memasak, dan favorit. Angka gizi adalah estimasi contoh, bukan analisis klinis.")}}
        Button(onClick={confirm=true},modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFDAD6),contentColor=Red)){Svg("s1718_20.svg",18);T(" Keluar Akun",14,true,Red)}
        T("NutriCare v1.3 • Prototipe lokal\nGemini opsional lewat Pengaturan AI. Jurnal tersimpan lokal.",10,color=Muted,modifier=Modifier.align(Alignment.CenterHorizontally))
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={T("Keluar dari akun?",20,true)},text={T("Catatan makanan tetap tersimpan di perangkat ini.")},confirmButton={TextButton(onClick={confirm=false;logout()}){T("Keluar Akun",14,true,Red)}},dismissButton={TextButton(onClick={confirm=false}){T("Batal",14,true,Green)}})
    if(conditionDialog)InfoDialog("Kondisi Kesehatan & Alergi","Kondisi profil: ${store.condition(email)}. Tidak ada diagnosis otomatis. Profil ini dipilih saat pendaftaran dan tersimpan di perangkat."){conditionDialog=false}
}
@Composable fun SettingRow(title:String,description:String,icon:String,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Surface(shape=RoundedCornerShape(12.dp),color=Lavender){Box(Modifier.size(40.dp),contentAlignment=Alignment.Center){Svg(icon,20)}};Column(Modifier.weight(1f)){T(title,14,true);T(description,11,color=Muted)};T("›",22,color=Muted)}}


