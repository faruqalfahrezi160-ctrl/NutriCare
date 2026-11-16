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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

fun today()=SimpleDateFormat("dd MMM yyyy",Locale("id","ID")).format(Date())
fun Meal.isToday():Boolean=SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(Date(stamp))==SimpleDateFormat("yyyyMMdd",Locale.ROOT).format(Date())
@Composable fun HomePage(name:String,condition:String,meals:List<Meal>,go:(String)->Unit,detail:(Meal)->Unit,ai:()->Unit){
    val daily=meals.filter{it.isToday()};val cal=daily.sumOf{it.calories*it.portions};val sugar=daily.sumOf{it.sugar*it.portions};val sodium=daily.sumOf{it.sodium*it.portions}
    Scrolling{
        Surface(color=PaleMint,shape=RoundedCornerShape(12.dp)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Surface(shape=CircleShape,color=Green){Box(Modifier.size(40.dp),contentAlignment=Alignment.Center){Svg("s1136_4.svg",20)}};Column(Modifier.weight(1f)){T("AI NUTRITION ASSISTANT",9,true,Green);T("Tanya Menu & Analisis Gizi",12,true);T("Mode lokal • belum terhubung",10,color=Muted)};Button(onClick=ai,shape=RoundedCornerShape(8.dp),contentPadding=PaddingValues(8.dp)){T("Chat AI",10,true,Color.White)}}}
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)){T("Halo, $name! 👋",23,true);T("Mari pertahankan pola makan seimbang hari ini.",12,color=Muted);Pill("Kondisi: $condition")}
        Panel{
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){Svg("s1136_6.svg",17);T("Target Nutrisi Hari Ini",14,true,modifier=Modifier.weight(1f));T(today(),9,color=Muted)}
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                listOf(Triple("Kalori",cal,1800),Triple("Gula",sugar,25),Triple("Natrium",sodium,1500)).forEachIndexed{i,(label,value,max)->Surface(Modifier.weight(1f),shape=RoundedCornerShape(12.dp),color=if(value>max)Color(0xFFFFF0F0)else Lavender){Column(Modifier.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally){Box(Modifier.size(58.dp),contentAlignment=Alignment.Center){CircularProgressIndicator(progress=(value.toFloat()/max).coerceIn(0f,1f),modifier=Modifier.size(54.dp),strokeWidth=5.dp,color=if(value>max)Coral else if(i==0)Mint else Green,trackColor=Color(0xFFD8E3FB));T("${value*100/max}%",12,true,if(value>max)Red else Ink)};Spacer(Modifier.height(8.dp));T(label,12,true);T("$value / $max",10,color=Muted);T(if(i==0)"kkal" else if(i==1)"gram" else "mg",9,color=Green)}}}
            }
            if(sodium>1500)Panel(Amber,10){T("Waspada: Batas Garam Terlampaui!",12,true);T("Total natrium hari ini $sodium mg. Tinjau catatan dan porsi makananmu.",11,color=Muted)}
        }
        TitleRow("Fitur Utama Diet Lokal","Sesuai Profil")
        listOf(Triple("Cari Menu Sehat","Rekomendasi masakan nusantara dan inspirasi rempah lokal.","recipes"),Triple("Kalkulator Makanan Lokal","Hitung estimasi kalori, gula, dan natrium sesuai porsi.","add"),Triple("Buku Harian Makanan","Catat sarapan, makan siang, dan camilan harianmu.","journal")).forEachIndexed{i,(title,desc,target)->Surface(Modifier.fillMaxWidth().clickable{go(target)},shape=RoundedCornerShape(12.dp),color=Color.White){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){Surface(shape=RoundedCornerShape(12.dp),color=if(i==1)Amber else Lavender){Box(Modifier.size(56.dp),contentAlignment=Alignment.Center){if(i==0)Asset("s1136_0.png",Modifier.fillMaxSize(),crop=true)else Svg(if(i==1)"s1136_16.svg" else "s1136_18.svg",24)}};Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){T(title,15,true);T(desc,11,color=Muted);T(listOf("Jelajahi Resep →","Hitung Porsi Makanan →","Catat Menu Terakhir →")[i],11,true,Green)}}}}
        TitleRow("Catatan Makanan Hari Ini","Lihat Semua"){go("journal")}
        if(daily.isEmpty())Panel{T("Belum ada catatan hari ini.",13,color=Muted)}
        daily.take(2).forEach{MealCard(it,true){detail(it)}}
        Action("Tambah Catatan Makan Sekarang",Green,"s1136_20.svg"){go("add")}
        T("Mode lokal • Target dan estimasi gizi adalah data contoh.",10,color=Muted)
    }
}

@Composable fun MealCard(meal:Meal,compact:Boolean=false,click:()->Unit){
    Surface(Modifier.fillMaxWidth().clickable(onClick=click),shape=RoundedCornerShape(16.dp),color=Color.White){Column(Modifier.padding(if(compact)10.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){if(meal.photo.isNotBlank())Asset(meal.photo,Modifier.size(if(compact)48.dp else 80.dp).clip(RoundedCornerShape(10.dp)),meal.name,true);Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){T(meal.time.uppercase()+" • "+SimpleDateFormat("HH:mm",Locale.ROOT).format(Date(meal.stamp)),9,true,Green);T(meal.name,if(compact)12 else 14,true);T("Takaran: ${meal.portions} porsi",10,color=Muted)}}
        if(!compact){Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Pill("${meal.calories*meal.portions} kkal");Pill("Gula ${meal.sugar*meal.portions}g",Lavender);Pill("Na ${meal.sodium*meal.portions}mg",if(meal.sodium*meal.portions>600)Color(0xFFFFDAD6)else Lavender,if(meal.sodium*meal.portions>600)Red else Green)};T(if(meal.sodium*meal.portions>600)"Perhatikan porsi dan kandungan garam  ›" else "Catatan nutrisi tersimpan  ›",11,true,if(meal.sodium*meal.portions>600)Color(0xFF855300) else Green)}else T("Na: ${meal.sodium*meal.portions} mg • ${meal.calories*meal.portions} kkal",10,color=if(meal.sodium>600)Red else Muted)
    }}
}
@Composable fun JournalPage(meals:List<Meal>,detail:(Meal)->Unit,add:()->Unit){
    var search by rememberSaveable{mutableStateOf("")};var filter by rememberSaveable{mutableStateOf("Semua")};var daily by rememberSaveable{mutableStateOf(false)}
    val source=if(daily)meals.filter{it.isToday()}else meals
    val shown=source.filter{it.name.contains(search,true)&&(filter=="Semua"||it.time==filter||filter=="Perlu Review"&&it.sodium*it.portions>600)}
    Box(Modifier.fillMaxSize()){
        Scrolling{
            TitleRow("Jurnal Makanan Harian",if(daily)"Hari Ini" else "Semua Hari"){daily=!daily};T("Riwayat catatan makanan & estimasi nutrisi harian",11,color=Muted)
            Panel(if(source.sumOf{it.sodium*it.portions}>1500)Color(0xFFFFF0E6)else PaleMint){TitleRow("RINGKASAN ${if(daily)"HARI INI" else "CATATAN"}");T("${source.sumOf{it.calories*it.portions}} kkal",22,true);LinearProgressIndicator((source.sumOf{it.sodium*it.portions}/1500f).coerceIn(0f,1f),Modifier.fillMaxWidth(),color=Coral);T("Natrium: ${source.sumOf{it.sodium*it.portions}} mg • ${source.size} catatan",11,color=Muted)}
            Field("Cari makanan",search,{search=it},"Cari pecel, soto, sayur bening…",icon="s2_8.svg")
            ChoiceRow(listOf("Semua","Sarapan","Siang","Malam","Camilan","Perlu Review"),filter,{filter=it})
            T("Ketuk kartu untuk melihat detail • ${shown.size} item",10,color=Muted)
            if(shown.isEmpty())Panel{T("Belum ada makanan yang cocok.",14,true);T("Ubah pencarian atau tambahkan catatan baru.",12,color=Muted)}
            shown.forEach{MealCard(it){detail(it)}}
            Spacer(Modifier.height(70.dp))
        }
        FloatingActionButton(onClick=add,modifier=Modifier.align(Alignment.BottomEnd).padding(20.dp),containerColor=Mint,contentColor=Color.White,shape=CircleShape){Asset("s2_19.svg",Modifier.size(24.dp),"Tambah catatan makanan")}
    }
}

@Composable fun MealDetail(meal:Meal,edit:()->Unit,delete:()->Unit,ai:()->Unit){var confirm by remember{mutableStateOf(false)}
    Scrolling{
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp)).background(Lavender)){
            if(meal.photo.isNotEmpty())Asset(if(meal.id=="lunch")"s384_0.png" else meal.photo,Modifier.fillMaxSize(),meal.name,true)
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent,Color(0xCC111C2D)))))
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Pill(meal.time.uppercase());T(meal.name,20,true,Color.White);T("${SimpleDateFormat("dd MMM yyyy, HH:mm",Locale("id","ID")).format(Date(meal.stamp))} • ${meal.portions} porsi",11,color=Color.White)}
        }
        Panel{
            TitleRow("Analisis Nutrisi","Sesuai Porsi")
            Panel(Lavender,12){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Svg("s384_4.svg",30);Column{T("${meal.calories*meal.portions} kkal",23,true);T("Energi Total Makanan",11,color=Muted)}}}
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("KARBOHIDRAT" to "48g","PROTEIN" to "32g","LEMAK" to "18g").forEachIndexed{i,(label,value)->Surface(Modifier.weight(1f),shape=RoundedCornerShape(10.dp),color=listOf(Color(0xFFFFF4E8),PaleMint,Color(0xFFFFEFEF))[i]){Column(Modifier.padding(10.dp),horizontalAlignment=Alignment.CenterHorizontally){T(label,9,true);T(value,18,true,Green);T("Ilustrasi",8,color=Muted)}}}}
            Panel(if(meal.sodium*meal.portions>600)Color(0xFFFFEFEF)else PaleMint,12){T("Natrium ${meal.sodium*meal.portions} mg",14,true,if(meal.sodium>600)Red else Green);T(if(meal.sodium>600)"Perhatikan kandungan garam pada porsi ini." else "Catat total asupan sesuai kebutuhanmu.",12,color=Muted)}
            T("Nilai nutrisi merupakan estimasi contoh.",10,color=Muted)
        }
        Panel(Lavender){Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){Svg("s384_6.svg",30);Column{T("AI NutriCare Assistant",17,true);T("Asisten Nutrisi Pintar",12,color=Green)}};Panel{T("SARAN & ANALISIS",12,true,Green);T(if(meal.sodium>600)"Kuah soto dan bumbu tambahan dapat meningkatkan kandungan garam. Tinjau porsi kuah serta bumbu yang kamu konsumsi." else "Gunakan bahan segar dan sesuaikan porsi dengan kebutuhanmu.",13)};Action("Tanya Chat AI tentang Menu Ini",onClick=ai)}
        if(meal.notes.isNotBlank())Panel{T("Catatan kesehatan",14,true);T(meal.notes,13,color=Muted)}
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Button(onClick=edit,modifier=Modifier.weight(1f).heightIn(min=52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFDEE8FF),contentColor=Ink)){Svg("s384_10.svg",18);T(" Ubah Data",13,true)};Button(onClick={confirm=true},modifier=Modifier.weight(1f).heightIn(min=52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFDAD6),contentColor=Red)){Svg("s384_11.svg",18);T(" Hapus Data",13,true,Red)}}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={T("Hapus catatan makanan?",20,true)},text={T("Catatan ${meal.name} akan dihapus dari jurnal. Tindakan ini tidak bisa dibatalkan.")},confirmButton={TextButton(onClick={confirm=false;delete()}){T("Hapus Data",14,true,Red)}},dismissButton={TextButton(onClick={confirm=false}){T("Batal",14,true,Green)}})
}
