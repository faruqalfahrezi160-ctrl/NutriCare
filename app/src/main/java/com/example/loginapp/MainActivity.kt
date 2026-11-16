package com.example.loginapp

import android.os.Bundle
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.example.loginapp.ui.theme.LoginAppTheme

val Green=Color(0xFF006C49)
val Mint=Color(0xFF10B981)
val Coral=Color(0xFFFF6B6B)
val Ink=Color(0xFF111C2D)
val Muted=Color(0xFF53645B)
val Page=Color(0xFFF9F9FF)
val Lavender=Color(0xFFF0F3FF)
val PaleMint=Color(0xFFD0FCEE)
val Amber=Color(0xFFFFDDB8)
val Red=Color(0xFFB32635)
val Jakarta=FontFamily(Font(R.font.plus_jakarta))

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.WHITE;window.navigationBarColor=android.graphics.Color.WHITE;androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true;setContent{LoginAppTheme{NutriApp()}}}}

@Composable
fun Asset(file:String,modifier:Modifier=Modifier,description:String?=null,crop:Boolean=false){
    val context=LocalContext.current
    val bitmap=remember(file){runCatching{if(file.startsWith("content:")) context.contentResolver.openInputStream(Uri.parse(file)).use{BitmapFactory.decodeStream(it)} else context.assets.open("figma/"+if(file.endsWith(".svg"))file+".png" else file).use{BitmapFactory.decodeStream(it)}}.getOrNull()?.asImageBitmap()}
    if(bitmap!=null)Image(bitmap,description,modifier,contentScale=if(crop)ContentScale.Crop else ContentScale.Fit)
}
@Composable fun Svg(file:String,size:Int=20){Asset(file,Modifier.size(size.dp))}
@Composable fun T(text:String,size:Int=14,bold:Boolean=false,color:Color=Ink,modifier:Modifier=Modifier){Text(text,modifier,color=color,fontSize=size.sp,lineHeight=(size*1.45).sp,fontWeight=if(bold)FontWeight.Bold else FontWeight.Normal)}
@Composable fun Pill(text:String,bg:Color=PaleMint,color:Color=Green){Surface(shape=RoundedCornerShape(50),color=bg){T(text,10,true,color,Modifier.padding(horizontal=9.dp,vertical=4.dp))}}
@Composable fun Panel(bg:Color=Color.White,padding:Int=16,content:@Composable ColumnScope.()->Unit){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=bg){Column(Modifier.padding(padding.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}}
@Composable fun Action(text:String,color:Color=Green,icon:String?=null,onClick:()->Unit){Button(onClick,Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=color)){if(icon!=null&&icon!="s853_8.svg"){Svg(icon,17);Spacer(Modifier.width(8.dp))};T(text,14,true,Color.White);if(icon=="s853_8.svg"){Spacer(Modifier.width(8.dp));Svg(icon,17)}}}
@Composable fun TitleRow(title:String,small:String="",action:(()->Unit)?=null){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){T(title,16,true,modifier=Modifier.weight(1f));if(small.isNotEmpty())TextButton(onClick={action?.invoke()},enabled=action!=null){T(small,11,true,Green)}}}
@Composable fun Scrolling(content:@Composable ColumnScope.()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)}
@Composable fun Field(label:String,value:String,onChange:(String)->Unit,placeholder:String="",secret:Boolean=false,icon:String?=null,number:Boolean=false,multiline:Boolean=false,error:String?=null,onDone:(()->Unit)?=null){
    var visible by remember{mutableStateOf(false)}
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
        T(label,13,true)
        TextField(value,onChange,Modifier.fillMaxWidth(),textStyle=androidx.compose.ui.text.TextStyle(fontFamily=Jakarta,fontSize=14.sp,color=Ink),placeholder={T(placeholder,13,color=Muted)},singleLine=!multiline,minLines=if(multiline)3 else 1,isError=error!=null,
            visualTransformation=if(secret&&!visible)PasswordVisualTransformation() else VisualTransformation.None,
            leadingIcon=if(icon!=null)({Svg(icon,20)})else null,
            trailingIcon=if(secret)({IconButton(onClick={visible=!visible}){Asset("s290_3.svg",Modifier.size(21.dp),if(visible)"Sembunyikan kata sandi" else "Tampilkan kata sandi")}})else null,
            keyboardOptions=KeyboardOptions(keyboardType=if(secret)KeyboardType.Password else if(number)KeyboardType.Number else if(label.contains("Email"))KeyboardType.Email else KeyboardType.Text,imeAction=if(onDone!=null)ImeAction.Done else ImeAction.Next),
            keyboardActions=KeyboardActions(onDone={onDone?.invoke()}),shape=RoundedCornerShape(12.dp),
            colors=TextFieldDefaults.colors(focusedContainerColor=Lavender,unfocusedContainerColor=Lavender,errorContainerColor=Color(0xFFFFEFEF),focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
        if(error!=null)T(error,11,color=Red)
    }
}
@Composable fun ChoiceRow(options:List<String>,selected:String,onSelect:(String)->Unit){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lavender).horizontalScroll(rememberScrollState()).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){options.forEach{v->Surface(Modifier.clickable{onSelect(v)},shape=RoundedCornerShape(9.dp),color=if(v==selected)Mint else Color.Transparent){T(v,11,v==selected,if(v==selected)Color.White else Muted,Modifier.padding(horizontal=14.dp,vertical=10.dp))}}}}
@Composable fun InfoDialog(title:String,text:String,onClose:()->Unit){AlertDialog(onDismissRequest=onClose,title={T(title,20,true)},text={T(text)},confirmButton={TextButton(onClick=onClose){T("Mengerti",14,true,Green)}})}

@Composable fun NutriApp(){
    val context=LocalContext.current
    val store=remember{LocalStore(context)}
    var email by rememberSaveable{mutableStateOf(store.user)}
    var route by rememberSaveable{mutableStateOf(if(email.isEmpty())"splash" else "home")}
    var selected by rememberSaveable{mutableStateOf("")}
    var recipeIndex by rememberSaveable{mutableStateOf(0)}
    var meals by remember(email){mutableStateOf(store.meals(email))}
    var favorites by remember(email){mutableStateOf(store.favorites(email))}
    var info by remember{mutableStateOf<Pair<String,String>?>(null)}
    val main=route in listOf("home","journal","recipes","profile")
    fun go(r:String){route=r}
    fun explain(title:String,body:String){info=title to body}
    fun offlineAI(){explain("Asisten nutrisi","Layanan AI belum dihubungkan. Angka gizi dan saran pada aplikasi ini adalah contoh, bukan hasil analisis makanan atau rekomendasi medis personal.")}
    fun back(){route=when(route){"register"->"login";"login"->"splash";"add","detail"->"journal";"edit"->"detail";"recipe"->"recipes";else->"home"}}
    BackHandler(route!="splash"&&route!="home"){back()}
    Surface(Modifier.fillMaxSize(),color=Page){
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()){
            if(route !in listOf("splash","login","register")){
                Surface(color=Color.White,shadowElevation=1.dp){Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){
                    if(!main)IconButton(onClick={back()}){Asset("s384_12.svg",Modifier.size(18.dp),"Kembali")}
                    Column(Modifier.weight(1f),horizontalAlignment=if(main)Alignment.CenterHorizontally else Alignment.Start){T(if(main)"NutriCare" else "Catat Makanan",18,true,if(main)Green else Ink);if(main)T(when(route){"home"->"BERANDA";"journal"->"JURNAL";else->"NUTRISI & KESEHATAN"},8,color=Muted)}
                    if(main){IconButton(onClick={go(if(route=="recipes")"recipes" else "journal")}){Asset("s2_20.svg",Modifier.size(18.dp),"Cari makanan")};IconButton(onClick={explain("Notifikasi","Belum ada notifikasi baru. Semua data pada perangkat ini tersimpan secara lokal.")}){Asset("s2_21.svg",Modifier.size(19.dp),"Notifikasi")}}
                    IconButton(onClick={go("profile")}){Asset("s2_4.png",Modifier.size(32.dp).clip(CircleShape),"Profil",true)}
                }}
            }
            Box(Modifier.weight(1f).fillMaxWidth()){
                when(route){
                    "splash"->WelcomeScreen({go("register")},{go("login")})
                    "login"->LoginPage(store,{e,keep->email=e;store.user=if(keep)e else "";go("home")},{go("register")},::explain)
                    "register"->RegisterPage(store,{go("login")},{e->email=e;go("home")})
                    "home"->HomePage(store.name(email),store.condition(email),meals,{go(it)},{selected=it.id;go("detail")},::offlineAI)
                    "journal"->JournalPage(meals,{selected=it.id;go("detail")},{go("add")})
                    "add","edit"->MealForm(if(route=="edit")meals.find{it.id==selected}else null,{meal->meals=if(route=="edit")meals.map{if(it.id==meal.id)meal else it}else meals+meal;store.save(email,meals);go("journal")},{back()})
                    "detail"->{val meal=meals.find{it.id==selected};if(meal!=null)MealDetail(meal,{go("edit")},{meals=meals.filter{it.id!=selected};store.save(email,meals);go("journal")},::offlineAI)}
                    "recipes"->RecipesPage(favorites,{i->recipeIndex=i;go("recipe")},{name->favorites=if(name in favorites)favorites-name else favorites+name;store.favorites(email,favorites)},::offlineAI)
                    "recipe"->RecipeDetail(recipes[recipeIndex],recipes[recipeIndex].name in favorites,{val name=recipes[recipeIndex].name;favorites=if(name in favorites)favorites-name else favorites+name;store.favorites(email,favorites)},::offlineAI)
                    "profile"->ProfilePage(store,email,::explain,::offlineAI,{store.user="";email="";go("login")})
                }
            }
            if(main){Surface(color=Color.White,shadowElevation=4.dp){Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal=8.dp),horizontalArrangement=Arrangement.SpaceAround,verticalAlignment=Alignment.CenterVertically){listOf("home" to "Beranda","journal" to "Jurnal","recipes" to "Resep Sehat","profile" to "Profil").forEachIndexed{i,(r,label)->Column(Modifier.clip(RoundedCornerShape(12.dp)).background(if(route==r)PaleMint else Color.Transparent).clickable{go(r)}.padding(horizontal=12.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally){Svg("s2_${22+i}.svg",20);T(label,9,route==r,if(route==r)Green else Muted)}}}}}
        }
    }
    info?.let{InfoDialog(it.first,it.second){info=null}}
}
