package com.example.loginapp

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class Meal(val id:String=UUID.randomUUID().toString(), val name:String, val time:String="Siang", val portions:Int=1, val calories:Int=420, val sugar:Int=6, val sodium:Int=380, val photo:String="", val notes:String="", val stamp:Long=System.currentTimeMillis(), val caloriesKnown:Boolean=true, val sugarKnown:Boolean=true, val sodiumKnown:Boolean=true, val foodId:String="", val grams:Int=0, val nutritionSource:String="")
data class Recipe(val name:String,val photo:String,val calories:Int,val sodium:Int,val protein:String,val minutes:Int,val description:String,val ingredients:List<Pair<String,String>>,val steps:List<String>,val category:String)
val recipes=listOf(
    Recipe("Pepes Ikan Mas Daun Kemangi","s1398_0.png",380,320,"34",35,"Dimasak kukus dengan bumbu kuning melimpah dan rempah daun kemangi segar untuk aroma sedap.",listOf("Ikan Mas segar" to "1 ekor (@ 300g), lumuri jeruk nipis","Daun kemangi segar" to "1 ikat, dipetik","Bumbu Rempah Halus Sehat" to "3 bawang merah, 2 bawang putih, kunyit dan kemiri","Garam Beriodium Terkontrol" to "¼ sdt","Aromatik & Daun Pembungkus" to "1 serai, ½ tomat, daun pisang dan lidi"),listOf("Bersihkan ikan dan lumuri jeruk nipis. Diamkan 10 menit.","Haluskan bawang, kunyit, dan kemiri. Balurkan pada ikan.","Letakkan ikan, kemangi, tomat dan serai di atas daun pisang. Bungkus dan sematkan lidi.","Kukus sekitar 30 menit hingga ikan matang sepenuhnya. Sajikan hangat."),"Rendah Natrium"),
    Recipe("Sayur Asem Bening Krai & Jagung Manis","s1398_1.png",145,180,"5.2",25,"Sensasi asam segar asam jawa alami dipadu jagung dan labu siam renyah.",listOf("Jagung manis" to "½ tongkol","Labu siam / krai" to "100 gram","Kacang panjang" to "3 batang","Asam jawa dan rempah" to "1 sdt asam, bawang merah, lengkuas","Air" to "400 ml"),listOf("Cuci dan potong semua sayuran.","Didihkan air bersama bawang, asam jawa dan lengkuas.","Masukkan jagung lalu labu dan kacang panjang. Masak sampai matang.","Cicipi dan sajikan hangat."),"Rendah Natrium"),
    Recipe("Tahu Tempe Bacem Kukus Rendah Gula","s1398_2.png",210,240,"18",30,"Dimatangkan perlahan dengan air kelapa muda dan sedikit gula kelapa.",listOf("Tahu" to "100 gram","Tempe" to "100 gram","Air kelapa" to "200 ml","Bawang dan ketumbar" to "2 siung, ½ sdt","Gula kelapa" to "½ sdt"),listOf("Potong tahu dan tempe. Haluskan bawang serta ketumbar.","Rebus air kelapa dengan bumbu dan gula.","Masukkan tahu serta tempe. Masak dengan api kecil sampai bumbu meresap.","Kukus 10 menit, kemudian sajikan."),"Ramah Diabetes")
)
class LocalStore(context:Context) {
    private val prefs=context.getSharedPreferences("nutricare_local_v2",Context.MODE_PRIVATE)
    var user:String
        get()=prefs.getString("session","")!!
        set(value){prefs.edit().putString("session",value).apply()}
    private fun account(email:String)=JSONObject(prefs.getString("account_${email.lowercase()}","{}")!!)
    fun name(email:String)=account(email).optString("name","Siti Rahmawati")
    fun condition(email:String)=account(email).optString("condition","Hipertensi")
    private fun hash(password:String,salt:ByteArray):String {
        val spec=PBEKeySpec(password.toCharArray(),salt,100000,256)
        return try {Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded,Base64.NO_WRAP)} finally {spec.clearPassword()}
    }
    fun register(name:String,email:String,password:String,condition:String):Boolean {
        if(prefs.contains("account_${email.lowercase()}"))return false
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
        val a=JSONObject().put("name",name).put("condition",condition).put("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).put("hash",hash(password,salt))
        prefs.edit().putString("account_${email.lowercase()}",a.toString()).apply();return true
    }
    fun login(email:String,password:String):Boolean {
        if(email.equals("demo@nutricare.id",true)&&password=="NutriCare123")return true
        val a=account(email);if(!a.has("salt"))return false
        return MessageDigest.isEqual(hash(password,Base64.decode(a.getString("salt"),Base64.NO_WRAP)).toByteArray(),a.getString("hash").toByteArray())
    }
    fun meals(email:String):List<Meal> {
        val raw=prefs.getString("meals_$email",null)?:run {
            if(!email.equals("demo@nutricare.id",true))return emptyList()
            val seeded=listOf(
            Meal(id="breakfast",name="Bubur Ayam Beras Merah & Kuah Kaldu Bening",time="Sarapan",calories=320,sugar=3,sodium=240,photo="s2_0.png"),
            Meal(id="lunch",name="Soto Ayam Lamongan Lengkap Koya & Kerupuk",calories=510,sugar=5,sodium=850,photo="s2_1.png",notes="Kuah kaldu diminum habis, koya 2 sendok makan penuh."),
            Meal(id="snack",name="Jus Pepaya Muda Jeruk Nipis Tanpa Gula",time="Camilan",calories=90,sugar=11,sodium=15,photo="s2_2.png"),
            Meal(id="dinner",name="Pepes Ikan Mas Daun Kemangi & Nasi Merah",time="Malam",calories=380,sugar=2,sodium=320,photo="s2_3.png"))
            save(email,seeded);return seeded
        }
        return runCatching {val a=JSONArray(raw);(0 until a.length()).map {val j=a.getJSONObject(it);Meal(j.getString("id"),j.getString("name"),j.getString("time"),j.getInt("portions"),j.getInt("calories"),j.getInt("sugar"),j.getInt("sodium"),j.optString("photo"),j.optString("notes"),j.optLong("stamp",System.currentTimeMillis()),j.optBoolean("caloriesKnown",true),j.optBoolean("sugarKnown",true),j.optBoolean("sodiumKnown",true),j.optString("foodId"),j.optInt("grams",0),j.optString("nutritionSource"))}}.getOrDefault(emptyList())
    }
    fun save(email:String,meals:List<Meal>) {val a=JSONArray();meals.forEach{a.put(JSONObject().put("id",it.id).put("name",it.name).put("time",it.time).put("portions",it.portions).put("calories",it.calories).put("sugar",it.sugar).put("sodium",it.sodium).put("photo",it.photo).put("notes",it.notes).put("stamp",it.stamp).put("caloriesKnown",it.caloriesKnown).put("sugarKnown",it.sugarKnown).put("sodiumKnown",it.sodiumKnown).put("foodId",it.foodId).put("grams",it.grams).put("nutritionSource",it.nutritionSource))};prefs.edit().putString("meals_$email",a.toString()).apply()}
    fun favorites(email:String)=prefs.getStringSet("favorites_$email",emptySet())!!.toSet()
    fun favorites(email:String,ids:Set<String>){prefs.edit().putStringSet("favorites_$email",ids).apply()}
    fun reminder(email:String)=prefs.getBoolean("reminder_$email",false)
    fun reminder(email:String,on:Boolean){prefs.edit().putBoolean("reminder_$email",on).apply()}
}

fun Meal.energyLabel():String=if(caloriesKnown)"${calories*portions} kkal" else "belum tersedia"
fun Meal.sugarLabel():String=if(sugarKnown)"${sugar*portions} g" else "belum tersedia"
fun Meal.sodiumLabel():String=if(sodiumKnown)"${sodium*portions} mg" else "belum tersedia"
fun List<Meal>.nutritionIncomplete()=any{!it.caloriesKnown||!it.sugarKnown||!it.sodiumKnown}
