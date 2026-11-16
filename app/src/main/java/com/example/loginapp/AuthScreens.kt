package com.example.loginapp

import android.util.Patterns
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable fun WelcomeScreen(register:()->Unit,login:()->Unit){
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PaleMint,Page,Page,PaleMint.copy(alpha=.4f)))).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Pill("Lokal Sehat • Berkelanjutan");T("● IDN v2.4",12,true,Muted)}
        Box(Modifier.padding(top=16.dp).size(220.dp),contentAlignment=Alignment.Center){
            Surface(Modifier.size(208.dp),shape=CircleShape,color=PaleMint,shadowElevation=2.dp){}
            Surface(Modifier.size(176.dp),shape=CircleShape,color=Color.White,shadowElevation=12.dp){}
            Surface(Modifier.align(Alignment.TopEnd).size(44.dp),shape=CircleShape,color=Coral){Box(contentAlignment=Alignment.Center){Svg("s853_3.svg",16)}}
            Surface(Modifier.align(Alignment.CenterStart).size(36.dp),shape=CircleShape,color=Color(0xFF6FFBBE)){Box(contentAlignment=Alignment.Center){Svg("s853_5.svg",15)}}
            Surface(Modifier.align(Alignment.BottomStart).size(40.dp),shape=CircleShape,color=Color.White,shadowElevation=4.dp){Box(contentAlignment=Alignment.Center){Svg("s853_4.svg",17)}}
        }
        Row{T("Nutri",32,true);T("Care",32,true,Mint)}
        T("Smart Nutrition for Your Daily Health",15,color=Muted)
        Surface(shape=RoundedCornerShape(50),color=Color(0xFFE7EEFF)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Svg("s853_2.svg",18);T("PILIHAN MAKANAN LOKAL SEHAT & PANTAUAN NUTRISI",10,true,Muted)}}
        Panel{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Asset("s853_0.png",Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),crop=true);Column(Modifier.weight(1f)){T("Kearifan Pangan Nusantara",16,true);T("Tempe, Alpukat Mentega, & Pepaya Segar",12,color=Muted)};Pill("100% Organik")}}
        Panel{TitleRow("Target Glikemik & Kardiak Terpantau");Asset("s853_7.svg",Modifier.fillMaxWidth().height(36.dp));Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Pill("Gula Darah Aman",Lavender);Pill("Kalori: 1.420 kkal",Lavender);Pill("Serat Tinggi",Lavender)}}
        Action("Mulai Sekarang (Get Started)",Coral,"s853_8.svg",register)
        T("Data ilustrasi • penyimpanan lokal",11,color=Muted)
        TextButton(onClick=login){T("Sudah punya akun? Masuk",14,true,Green)}
    }
}

@Composable fun LoginPage(store:LocalStore,onLogin:(String,Boolean)->Unit,register:()->Unit,explain:(String,String)->Unit){
    var email by rememberSaveable{mutableStateOf("")};var password by remember{mutableStateOf("")};var keep by rememberSaveable{mutableStateOf(false)};var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)}
    val scope=rememberCoroutineScope();val focus=LocalFocusManager.current
    fun submit(){if(busy)return;focus.clearFocus();if(!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()){error="Masukkan alamat email yang valid.";return};if(password.length<8){error="Kata sandi minimal 8 karakter.";return};busy=true;scope.launch{val valid=withContext(Dispatchers.Default){store.login(email.trim(),password)};busy=false;if(valid){password="";onLogin(email.trim().lowercase(),keep)}else error="Email atau kata sandi tidak cocok."}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.fillMaxWidth().height(232.dp).background(Brush.verticalGradient(listOf(Mint,Green)))){
            Column(Modifier.fillMaxWidth().padding(top=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)){
                Box(Modifier.size(92.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha=.25f)).padding(6.dp)){Box(Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Color.White));Box(Modifier.align(Alignment.TopEnd).size(16.dp).background(Coral,CircleShape))}
                T("Selamat Datang Kembali!",24,true,Color.White)
                T("Masuk untuk memantau asupan gizi harianmu",13,color=Color(0xFF6FFBBE))
            }
            Asset("s290_0.svg",Modifier.align(Alignment.BottomCenter).fillMaxWidth().aspectRatio(390f/32f))
        }
        Column(Modifier.offset(y=(-12).dp).padding(horizontal=16.dp).widthIn(max=500.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
            Surface(shape=RoundedCornerShape(24.dp),color=Color.White,shadowElevation=8.dp){Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
                Field("Email / No. Handphone",email,{email=it;error=""},"nama@email.com",icon="s290_1.svg")
                Field("Kata Sandi",password,{password=it;error=""},"••••••••",secret=true,icon="s290_2.svg",onDone={submit()})
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Row(Modifier.weight(1f).clickable{keep=!keep},verticalAlignment=Alignment.CenterVertically){Checkbox(keep,{keep=it},Modifier.size(28.dp));T("Ingat Saya",11,color=Muted)};TextButton(onClick={explain("Lupa Kata Sandi?","Akun tersimpan di perangkat ini. Pemulihan email belum tersedia. Gunakan akun demo: demo@nutricare.id dengan kata sandi NutriCare123.")}){T("Lupa Kata Sandi?",11,true,Green)}}
                if(error.isNotEmpty())T(error,12,color=Red)
                Button(onClick={submit()},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(16.dp),colors=ButtonDefaults.buttonColors(containerColor=Coral)){T(if(busy)"Memeriksa…" else "Masuk ke Akun",18,true,Color.White);Spacer(Modifier.width(8.dp));Svg("s290_4.svg",15)}
                Row(verticalAlignment=Alignment.CenterVertically){Divider(Modifier.weight(1f));T("atau masuk dengan",11,color=Muted,modifier=Modifier.padding(horizontal=10.dp));Divider(Modifier.weight(1f))}
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf("Google" to "s290_5.svg","Apple" to "s290_6.svg").forEach{(name,asset)->Surface(Modifier.weight(1f).clickable{explain("Masuk dengan $name","Login $name memerlukan konfigurasi layanan autentikasi. Untuk saat ini, daftar akun lokal atau gunakan akun demo.")},shape=RoundedCornerShape(12.dp),color=Lavender){Row(Modifier.padding(vertical=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp,Alignment.CenterHorizontally),verticalAlignment=Alignment.CenterVertically){Svg(asset,20);T(name,14,true)}}}}
            }}
            Surface(shape=RoundedCornerShape(28.dp),color=Color(0xFFE7EEFF)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Svg("s290_7.svg",17);T("Terhubung dengan 1.200+ warung makan sehat mitra NutriCare",11,color=Muted)}}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically){T("Belum punya akun?",13,color=Muted);TextButton(onClick=register){T("Daftar Sekarang",15,true,Mint)}}
            TextButton(onClick={email="demo@nutricare.id";password="NutriCare123";error=""},modifier=Modifier.align(Alignment.CenterHorizontally)){T("Isi akun demo lokal",12,true,Green)}
        }
    }
}

@Composable fun RegisterPage(store:LocalStore,back:()->Unit,registered:(String)->Unit){
    var name by rememberSaveable{mutableStateOf("")};var email by rememberSaveable{mutableStateOf("")};var pass by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};var condition by rememberSaveable{mutableStateOf("Hipertensi")};var agree by rememberSaveable{mutableStateOf(false)};var error by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var terms by remember{mutableStateOf(false)};val scope=rememberCoroutineScope()
    Scrolling{
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Asset("s694_0.svg",Modifier.size(18.dp),"Kembali")};Spacer(Modifier.weight(1f));Pill("NutriCare",Lavender);Spacer(Modifier.weight(1f))}
        Pill("♡ LANGKAH 1 DARI 2");T("Buat Akun Baru",26,true);T("Lengkapi profil nutrisi & kondisi kesehatan personalmu untuk kurasi menu lokal yang aman.",13,color=Muted)
        Panel{
            Field("Nama Lengkap",name,{name=it},"Nama lengkap kamu",icon="s694_3.svg")
            TitleRow("Kondisi Kesehatan Khusus");T("Pilih status utama untuk personalisasi profil:",11,color=Muted)
            Surface(Modifier.fillMaxWidth().clickable{condition="Hipertensi"},shape=RoundedCornerShape(12.dp),color=if(condition=="Hipertensi")Green else Lavender){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Svg("s694_4.svg",20);Column{T("Hipertensi (Darah Tinggi)",13,true,if(condition=="Hipertensi")Color.White else Ink);T("Monitor asupan garam & MSG",10,color=if(condition=="Hipertensi")PaleMint else Muted)}}}
            val options=listOf("Umum / Sehat","Diabetes Melitus","Kolesterol Tinggi","Asam Urat","Ibu Hamil / Menyusui")
            options.chunked(2).forEachIndexed{r,row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEachIndexed{c,v->Surface(Modifier.weight(1f).clickable{condition=v},shape=RoundedCornerShape(12.dp),color=if(condition==v)PaleMint else Lavender){Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){Svg("s694_${6+r*2+c}.svg",15);T(v,10)}}}}}
            Panel(Amber,12){T(if(condition=="Hipertensi")"Mode Rendah Natrium Diaktifkan" else "Profil kesehatan: $condition",12,true);T("Profil ini tersimpan secara lokal. Target pada aplikasi merupakan data contoh dan dapat dibahas dengan ahli gizi.",11,color=Muted)}
            Field("Email Aktif",email,{email=it},"nama@email.com",icon="s694_12.svg")
            Field("Kata Sandi",pass,{pass=it},"Minimal 8 karakter",secret=true,icon="s694_14.svg")
            LinearProgressIndicator(progress=(pass.length/12f).coerceAtMost(1f),modifier=Modifier.fillMaxWidth().height(4.dp),color=Mint)
            Field("Konfirmasi Kata Sandi",confirm,{confirm=it},"Ulangi kata sandi",secret=true,icon="s694_16.svg")
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(agree,{agree=it});Column{T("Saya menyetujui ketentuan penggunaan lokal.",11);TextButton(onClick={terms=true}){T("Baca ketentuan & privasi",11,true,Green)}}}
            if(error.isNotEmpty())T(error,12,color=Red)
            Button(onClick={
                error=when{ name.isBlank()->"Nama wajib diisi.";!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()->"Email belum valid.";pass.length<8->"Kata sandi minimal 8 karakter.";pass!=confirm->"Konfirmasi kata sandi berbeda.";!agree->"Setujui ketentuan terlebih dahulu.";else->""}
                if(error.isEmpty()){busy=true;scope.launch{val ok=withContext(Dispatchers.Default){store.register(name.trim(),email.trim(),pass,condition)};busy=false;if(ok){pass="";confirm="";registered(email.trim().lowercase())}else error="Email sudah terdaftar. Silakan masuk."}}
            },enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Mint)){T(if(busy)"Menyimpan…" else "Daftar Sekarang (Register)",14,true,Color.White)}
        }
        Panel(Lavender){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Svg("s694_19.svg",24);Column{T("Data tersimpan di perangkat",14,true);T("Sandi dilindungi dengan hash dan salt. Akun ini belum tersinkron ke server.",11,color=Muted)}}}
        TextButton(onClick=back,modifier=Modifier.align(Alignment.CenterHorizontally)){T("Sudah punya akun? Masuk di sini",13,true,Green)}
    }
    if(terms)InfoDialog("Ketentuan & Privasi","Aplikasi menyimpan profil, jurnal, dan akun di perangkat ini. Hapus data aplikasi untuk menghapusnya. Nilai gizi merupakan estimasi contoh; fitur ini tidak menggantikan konsultasi tenaga kesehatan. Belum ada layanan AI, koneksi klinis, atau sinkronisasi online."){terms=false}
}
