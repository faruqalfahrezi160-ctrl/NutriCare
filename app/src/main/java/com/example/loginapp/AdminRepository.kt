package com.example.loginapp

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class AdminAccount(val email:String,val name:String,val role:String,val status:String)
data class AdminPerson(val id:String,val name:String,val email:String,val condition:String,val active:Boolean,val journals:Int)
data class AdminCondition(val id:String,val name:String,val category:String,val notes:String,val active:Boolean=true)
data class AdminFood(val id:String,val name:String,val category:String,val calories:Double?,val sodium:Double?,val source:String,val active:Boolean=true)
data class AdminRecipe(val id:String,val name:String,val category:String,val description:String,val ingredients:String,val steps:String,val status:String="Draf",val image:Int=0)
data class AdminEvent(val title:String,val detail:String,val time:Long=System.currentTimeMillis())
data class AdminSnapshot(val account:AdminAccount,val people:List<AdminPerson>,val requests:List<AdminAccount>,val conditions:List<AdminCondition>,val foods:List<AdminFood>,val recipes:List<AdminRecipe>,val events:List<AdminEvent>,val dailyEntries:List<Int>)

/** Database team: replace this implementation at AdminServices; all authorization must be repeated on the server. */
interface AdminRepository {
    val modeLabel:String
    fun current():AdminAccount?
    fun login(email:String,password:String,remember:Boolean):AdminAccount
    fun register(name:String,email:String,password:String):AdminAccount
    fun logout()
    fun snapshot():AdminSnapshot
    fun decideAdmin(email:String,approve:Boolean)
    fun setPersonActive(id:String,active:Boolean)
    fun saveCondition(value:AdminCondition)
    fun saveFood(value:AdminFood)
    fun saveRecipe(value:AdminRecipe)
    fun deleteRecord(kind:String,id:String)
    fun notifyUsers(message:String)
}
object AdminServices {
    @Volatile private var cached:AdminRepository?=null
    fun repository(context:Context):AdminRepository=cached?:synchronized(this){cached?:DemoAdminRepository(context.applicationContext).also{cached=it}}
}

/** Isolated development data. Does not send notifications, touch patient journals, or connect to a server. */
class DemoAdminRepository(private val context:Context):AdminRepository {
    override val modeLabel="MODE DEMO • database tim belum terhubung"
    private val prefs=context.getSharedPreferences("nutricare_admin_demo_v1",Context.MODE_PRIVATE)
    private var session:String=prefs.getString("session","").orEmpty()
    private fun normalizeEmail(value:String)=value.trim().lowercase(Locale.ROOT)
    private fun hash(value:String,salt:ByteArray):String {
        val spec=PBEKeySpec(value.toCharArray(),salt,100000,256)
        return try{Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded,Base64.NO_WRAP)}finally{spec.clearPassword()}
    }
    private fun credentials(name:String,address:String,password:String,role:String,status:String):JSONObject {
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
        return JSONObject().put("name",name).put("email",address).put("role",role).put("status",status).put("salt",Base64.encodeToString(salt,Base64.NO_WRAP)).put("hash",hash(password,salt))
    }
    private fun account(j:JSONObject)=AdminAccount(j.getString("email"),j.getString("name"),j.getString("role"),j.getString("status"))
    private fun accounts():List<JSONObject> = prefs.all.filterKeys{it.startsWith("account_")}.values.mapNotNull{runCatching{JSONObject(it as String)}.getOrNull()}
    private fun ensureOwner(){if(!prefs.contains("account_$DEMO_EMAIL"))prefs.edit().putString("account_$DEMO_EMAIL",credentials("Amanda • Pemilik Demo",DEMO_EMAIL,DEMO_PASSWORD,"owner","active").toString()).commit()}
    override fun current():AdminAccount? = accounts().firstOrNull{it.optString("email")==session&&it.optString("status")=="active"}?.let(::account)
    private fun authorized():AdminAccount=current()?:throw IllegalStateException("Sesi admin berakhir. Silakan masuk kembali.")
    @Synchronized override fun login(email:String,password:String,remember:Boolean):AdminAccount {
        ensureOwner();val address=normalizeEmail(email)
        val a=accounts().firstOrNull{it.optString("email")==address}?:throw IllegalArgumentException("Email atau kata sandi admin tidak cocok.")
        val actual=hash(password,Base64.decode(a.getString("salt"),Base64.NO_WRAP))
        require(MessageDigest.isEqual(actual.toByteArray(),a.getString("hash").toByteArray())){"Email atau kata sandi admin tidak cocok."}
        check(a.getString("status")=="active"){if(a.getString("status")=="pending")"Pendaftaranmu menunggu persetujuan pemilik admin." else "Permintaan admin ditolak atau akses dinonaktifkan."}
        session=address;prefs.edit().putString("session",if(remember)address else "").commit()
        ensureData();event("Admin masuk",a.getString("name"));return account(a)
    }
    @Synchronized override fun register(name:String,email:String,password:String):AdminAccount {
        ensureOwner();val address=normalizeEmail(email)
        require(name.trim().length in 2..80){"Nama harus 2–80 karakter."}
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()){"Email tidak valid."}
        require(password.length in 8..128){"Kata sandi harus 8–128 karakter."}
        require(!prefs.contains("account_$address")){"Email admin sudah terdaftar."}
        val a=credentials(name.trim(),address,password,"admin","active")
        prefs.edit().putString("account_$address",a.toString()).commit();event("Registrasi admin baru","${name.trim()} siap masuk ke dashboard.")
        return account(a)
    }
    override fun logout(){session="";prefs.edit().remove("session").commit()}
    private fun list(key:String)=JSONArray(prefs.getString(key,"[]"))
    private fun save(key:String,value:JSONArray){prefs.edit().putString(key,value.toString()).commit()}
    private fun rows(a:JSONArray)=(0 until a.length()).map{a.getJSONObject(it)}
    private fun event(title:String,detail:String){val events=list("events");events.put(JSONObject().put("title",title).put("detail",detail).put("time",System.currentTimeMillis()));save("events",JSONArray(rows(events).takeLast(60)))}
    private fun upsert(key:String,id:String,row:JSONObject){val items=rows(list(key)).toMutableList();val index=items.indexOfFirst{it.optString("id")==id};if(index<0)items.add(row) else items[index]=row;save(key,JSONArray(items))}
    private fun number(j:JSONObject,k:String)=if(j.isNull(k))null else j.optDouble(k).takeIf{it.isFinite()}
    private fun ensureData(){
        if(prefs.getBoolean("seeded",false))return
        val people=listOf("Budi Santoso" to "Hipertensi","Siti Aminah" to "Umum / Sehat","Dewi Lestari" to "Diabetes","Rizky Pratama" to "Umum / Sehat","Agus Wijaya" to "Asam Urat","Nadia Putri" to "Kolesterol")
        save("people",JSONArray(people.mapIndexed{i,p->JSONObject().put("id","NC-${1001+i}").put("name",p.first).put("email","pengguna${i+1}@demo.invalid").put("condition",p.second).put("active",i!=4).put("journals",listOf(12,8,15,4,0,7)[i])}))
        save("conditions",JSONArray(listOf("Hipertensi" to "Kardiovaskular","Diabetes" to "Endokrin","Asam Urat" to "Metabolik","Kolesterol" to "Metabolik").mapIndexed{i,p->JSONObject().put("id","condition-$i").put("name",p.first).put("category",p.second).put("notes","Konten contoh. Parameter klinis perlu divalidasi tim kesehatan sebelum dipublikasikan.").put("active",true)}))
        save("foods",JSONArray(FoodCatalog.all(context).map{JSONObject().put("id",it.id).put("name",it.name).put("category",it.category).put("calories",it.calories?:JSONObject.NULL).put("sodium",it.sodium?:JSONObject.NULL).put("source",it.source).put("active",true)}))
        save("recipes",JSONArray(com.example.loginapp.recipes.mapIndexed{i,r->JSONObject().put("id","recipe-$i").put("name",r.name).put("category",r.category).put("description",r.description).put("ingredients",r.ingredients.joinToString("\n"){it.first+" — "+it.second}).put("steps",r.steps.joinToString("\n")).put("status",if(i==2)"Menunggu review" else "Terbit").put("image",i+1)}))
        prefs.edit().putBoolean("seeded",true).commit()
        event("Ruang demo disiapkan","6 akun contoh dan katalog lokal. Tidak ada data pengguna lintas perangkat.")
    }
    override fun snapshot():AdminSnapshot {
        val a=authorized();ensureData()
        return AdminSnapshot(a,
            rows(list("people")).map{AdminPerson(it.getString("id"),it.getString("name"),it.getString("email"),it.getString("condition"),it.getBoolean("active"),it.getInt("journals"))},
            accounts().map(::account).sortedBy{it.name},
            rows(list("conditions")).map{AdminCondition(it.getString("id"),it.getString("name"),it.getString("category"),it.getString("notes"),it.getBoolean("active"))},
            rows(list("foods")).map{AdminFood(it.getString("id"),it.getString("name"),it.getString("category"),number(it,"calories"),number(it,"sodium"),it.getString("source"),it.getBoolean("active"))},
            rows(list("recipes")).map{AdminRecipe(it.getString("id"),it.getString("name"),it.getString("category"),it.getString("description"),it.getString("ingredients"),it.getString("steps"),it.getString("status"),it.optInt("image"))},
            rows(list("events")).map{AdminEvent(it.getString("title"),it.getString("detail"),it.getLong("time"))}.reversed(),listOf(3,5,4,8,7,9,10))
    }
    @Synchronized override fun decideAdmin(email:String,approve:Boolean){
        check(authorized().role=="owner"){"Hanya pemilik admin yang boleh menyetujui pendaftaran."}
        val address=normalizeEmail(email);val a=accounts().firstOrNull{it.optString("email")==address}?:error("Permintaan tidak ditemukan.")
        check(a.getString("status")=="pending"){"Permintaan sudah diproses."}
        a.put("status",if(approve)"active" else "rejected");prefs.edit().putString("account_$address",a.toString()).commit();event(if(approve)"Admin disetujui" else "Permintaan admin ditolak",a.getString("name"))
    }
    @Synchronized override fun setPersonActive(id:String,active:Boolean){authorized();val row=rows(list("people")).firstOrNull{it.getString("id")==id}?:error("Pengguna tidak ditemukan.");row.put("active",active);upsert("people",id,row);event(if(active)"Akun contoh diaktifkan" else "Akun contoh dinonaktifkan",row.getString("name"))}
    @Synchronized override fun saveCondition(value:AdminCondition){authorized();require(value.name.isNotBlank()&&value.category.isNotBlank()){"Nama dan kategori wajib diisi."};upsert("conditions",value.id,JSONObject().put("id",value.id).put("name",value.name).put("category",value.category).put("notes",value.notes).put("active",value.active));event("Kondisi disimpan",value.name)}
    @Synchronized override fun saveFood(value:AdminFood){authorized();require(value.name.isNotBlank()&&value.category.isNotBlank()){"Nama dan kategori wajib diisi."};require(value.calories==null||(value.calories.isFinite()&&value.calories in 0.0..1000.0)){"Energi harus 0–1000 kkal per 100 g."};require(value.sodium==null||(value.sodium.isFinite()&&value.sodium in 0.0..100000.0)){"Natrium harus 0–100000 mg per 100 g."};upsert("foods",value.id,JSONObject().put("id",value.id).put("name",value.name).put("category",value.category).put("calories",value.calories?:JSONObject.NULL).put("sodium",value.sodium?:JSONObject.NULL).put("source",value.source).put("active",value.active));event("Bahan pangan disimpan",value.name)}
    @Synchronized override fun saveRecipe(value:AdminRecipe){authorized();require(value.name.isNotBlank()&&value.ingredients.isNotBlank()&&value.steps.isNotBlank()){"Nama, bahan, dan langkah wajib diisi."};require(value.status in listOf("Draf","Menunggu review","Terbit")){"Status tidak valid."};upsert("recipes",value.id,JSONObject().put("id",value.id).put("name",value.name).put("category",value.category).put("description",value.description).put("ingredients",value.ingredients).put("steps",value.steps).put("status",value.status).put("image",value.image));event("Resep disimpan",value.name+" • "+value.status)}
    @Synchronized override fun deleteRecord(kind:String,id:String){authorized();require(kind in listOf("conditions","foods","recipes"));val items=rows(list(kind));val found=items.firstOrNull{it.getString("id")==id}?:error("Data tidak ditemukan.");save(kind,JSONArray(items.filter{it.getString("id")!=id}));event("Data demo dihapus",found.getString("name"))}
    @Synchronized override fun notifyUsers(message:String){authorized();require(message.isNotBlank()&&message.length<=500){"Pesan harus 1–500 karakter."};event("Simulasi notifikasi",message+" (tersimpan lokal; belum dikirim ke HP pengguna)")}
    companion object {const val DEMO_EMAIL="admin.demo@nutricare.id";const val DEMO_PASSWORD="AdminDemo123!";fun newId()=UUID.randomUUID().toString()}
}

