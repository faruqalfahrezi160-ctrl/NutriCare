package com.example.loginapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable fun MealForm(existing:Meal?,save:(Meal)->Unit,cancel:()->Unit){
    val edit=existing!=null;val context=LocalContext.current;val focus=LocalFocusManager.current
    var name by rememberSaveable(existing?.id){mutableStateOf(existing?.name?:"")};var time by rememberSaveable(existing?.id){mutableStateOf(existing?.time?:"Siang")};var portions by rememberSaveable(existing?.id){mutableStateOf(existing?.portions?:1)};var calories by rememberSaveable(existing?.id){mutableStateOf(existing?.calories?.toFloat()?:420f)};var sugar by rememberSaveable(existing?.id){mutableStateOf(existing?.sugar?.toFloat()?:6f)};var sodium by rememberSaveable(existing?.id){mutableStateOf(existing?.sodium?.toFloat()?:380f)};var photo by rememberSaveable(existing?.id){mutableStateOf(existing?.photo?:"")};var notes by rememberSaveable(existing?.id){mutableStateOf(existing?.notes?:"")};var error by remember{mutableStateOf("")}
    var showPhotoDialog by remember{mutableStateOf(false)}
    var tempCameraUri by rememberSaveable{mutableStateOf<String?>(null)}

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ success ->
        if (success && tempCameraUri != null) {
            runCatching {
                val finalUri = persistCameraPhoto(context, Uri.parse(tempCameraUri!!))
                photo = finalUri.toString()
            }.onFailure {
                photo = tempCameraUri!!
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
        if (granted) {
            runCatching {
                val uri = createTempCameraUri(context)
                tempCameraUri = uri.toString()
                cameraLauncher.launch(uri)
            }.onFailure { error = "Gagal membuka kamera: ${it.message}" }
        } else {
            error = "Izin kamera dibutuhkan untuk mengambil foto makanan."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){ uri ->
        if (uri != null) {
            runCatching {
                val finalUri = copyToInternalStorage(context, uri)
                photo = finalUri.toString()
            }.onFailure {
                error = "Gagal memproses foto dari galeri."
            }
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val uri = createTempCameraUri(context)
                tempCameraUri = uri.toString()
                cameraLauncher.launch(uri)
            }.onFailure { error = "Gagal membuka kamera: ${it.message}" }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scrolling{
        Panel(if(edit)Color(0xFFFFF0E6)else Color(0xFFE0F3F0)){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Svg(if(edit)"s515_2.svg" else "s951_1.svg",24);Column{T(if(edit)"MODE PERUBAHAN ENTRI" else "Catat Menu Makananmu",if(edit)10 else 17,true,if(edit)Color(0xFF855300)else Ink);T(if(edit)existing!!.name else "Data akurat membantu mencatat asupan dan kebiasaan makan harianmu.",12,color=Muted)}}}
        T("Foto Makanan",13,true)
        if(photo.isNotEmpty())Box(Modifier.fillMaxWidth().height(192.dp).clip(RoundedCornerShape(12.dp))){
            Asset(if(existing?.id=="lunch"&&photo==existing.photo)"s515_0.png" else photo,Modifier.fillMaxSize(),name,true)
            Button(onClick={showPhotoDialog=true},modifier=Modifier.align(Alignment.BottomEnd).padding(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Green)){
                Svg("s515_3.svg",16)
                T(" Ganti Foto",11,true,Green)
            }
        } else Surface(Modifier.fillMaxWidth().clickable{showPhotoDialog=true},shape=RoundedCornerShape(16.dp),color=Color.White){
            Column(Modifier.padding(24.dp).fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                Surface(shape=androidx.compose.foundation.shape.CircleShape,color=PaleMint){
                    Box(Modifier.size(56.dp),contentAlignment=Alignment.Center){Svg("s951_2.svg",26)}
                }
                T("Ambil Foto / Unggah Galeri",15,true)
                T("Kamera langsung atau galeri foto • Opsional",11,color=Muted)
            }
        }
        Panel(padding=16){
            Field("Nama Makanan Lokal",name,{name=it;error=""},"Contoh: Gado-gado Komplit",icon="s951_3.svg",error=if(error.isNotEmpty()&&name.isBlank())error else null)
            if(!edit){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Gado-gado","Soto Kudus","Pepes Ikan","Pecel Sayur").forEachIndexed{i,v->AssistChip(onClick={name=v;calories=listOf(420f,510f,380f,350f)[i];sodium=listOf(380f,850f,320f,420f)[i];sugar=listOf(6f,5f,2f,7f)[i]},label={T(v,10)})}}}
            T("Waktu Makan",13,true);ChoiceRow(listOf("Sarapan","Siang","Malam","Camilan"),time,{time=it})
            T("Porsi / Takaran Konsumsi",13,true)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lavender).padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(onClick={if(portions>1)portions--},enabled=portions>1){Asset("s951_5.svg",Modifier.size(16.dp),"Kurangi porsi")};Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){T("$portions Porsi ${if(edit)"" else "Sedang"}",17,true);T("Estimasi ${portions*(if(edit)450 else 350)} gram",11,color=Muted)};IconButton(onClick={if(portions<10)portions++},enabled=portions<10){Asset("s951_6.svg",Modifier.size(16.dp),"Tambah porsi")}}
            TitleRow(if(edit)"Rincian Komposisi Gizi" else "Estimasi Kandungan Nutrisi")
            T("Nilai per porsi dapat disesuaikan dengan racikan makanan. Total dikalikan jumlah porsi.",11,color=Muted)
            listOf(Triple("Kalori Total",calories,"kkal"),Triple("Gula Alami & Tambahan",sugar,"gram"),Triple("Natrium / Garam",sodium,"mg")).forEachIndexed{i,(label,value,unit)->Panel(Lavender,12){Row(verticalAlignment=Alignment.CenterVertically){Svg("s951_${8+i}.svg",16);T(label,11,modifier=Modifier.weight(1f).padding(start=5.dp));T("${value.toInt()*portions}",19,true,if(i==2&&value*portions>600)Color(0xFF855300)else Green);T(" $unit",10,color=Muted)};Slider(value,onValueChange={when(i){0->calories=it;1->sugar=it;2->sodium=it}},valueRange=when(i){0->0f..1500f;1->0f..80f;else->0f..2000f},colors=SliderDefaults.colors(thumbColor=Mint,activeTrackColor=Mint))}}
            if(sodium*portions>600)Panel(Amber,10){T("Perhatikan natrium pada porsi ini",12,true);T("Tinjau kembali jumlah kuah, garam, serta bumbu tambahan.",11,color=Muted)}else Pill("Estimasi natrium: ${(sodium*portions).toInt()} mg",Lavender)
            Field("Catatan Kesehatan & Resep Lokal",notes,{notes=it},"Contoh: Tanpa MSG, kurangi kerupuk, saus pisah…",multiline=true)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Tanpa MSG","Kurang Garam","Kuah/Saus Pisah").forEach{tag->AssistChip(onClick={if(!notes.contains(tag))notes=notes+if(notes.isEmpty())tag else ", $tag"},label={T("+ $tag",9)})}}
            if(error.isNotEmpty())T(error,12,color=Red)
            Action(if(edit)"Perbarui Data (Update)" else "Simpan Data Makanan",Mint,if(edit)"s515_15.svg" else "s951_13.svg"){
                focus.clearFocus();if(name.isBlank())error="Nama makanan wajib diisi." else save(Meal(existing?.id?:java.util.UUID.randomUUID().toString(),name.trim(),time,portions,calories.toInt(),sugar.toInt(),sodium.toInt(),photo,notes.trim(),existing?.stamp?:System.currentTimeMillis()))
            }
            TextButton(onClick=cancel,modifier=Modifier.fillMaxWidth()){T("Batal",13,true,Muted)}
        }
        if(edit)Panel(Lavender){Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Svg("s515_18.svg",24);Column{T("Tips Lokal NutriCare",15,true);T("Catat kuah dan bumbu tambahan agar estimasi harian lebih lengkap.",12,color=Muted)}}}
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { T("Pilih Sumber Foto", 18, true) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            openCamera()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("📷", fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
                            Spacer(Modifier.width(12.dp))
                            T("Ambil dengan Kamera", 14, true, Green)
                        }
                    }
                    TextButton(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("🖼️", fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
                            Spacer(Modifier.width(12.dp))
                            T("Pilih dari Galeri", 14, true, Ink)
                        }
                    }
                    if (photo.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                showPhotoDialog = false
                                photo = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("🗑️", fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp))
                                Spacer(Modifier.width(12.dp))
                                T("Hapus Foto", 14, true, Red)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoDialog = false }) {
                    T("Tutup", 13, true, Muted)
                }
            }
        )
    }
}

private fun createTempCameraUri(context: Context): Uri {
    val tempDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
    val file = File(tempDir, "temp_camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

private fun persistCameraPhoto(context: Context, sourceUri: Uri): Uri {
    val photosDir = File(context.filesDir, "meal_photos").apply { mkdirs() }
    val destFile = File(photosDir, "meal_${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
    return Uri.fromFile(destFile)
}

private fun copyToInternalStorage(context: Context, sourceUri: Uri): Uri {
    val photosDir = File(context.filesDir, "meal_photos").apply { mkdirs() }
    val destFile = File(photosDir, "meal_${UUID.randomUUID()}.jpg")
    context.contentResolver.openInputStream(sourceUri)?.use { input ->
        FileOutputStream(destFile).use { output ->
            input.copyTo(output)
        }
    }
    return Uri.fromFile(destFile)
}
