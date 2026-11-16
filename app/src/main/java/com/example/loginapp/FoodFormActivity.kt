package com.example.loginapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import java.util.UUID

/** Both required XML form layouts share catalog selection and nullable nutrition semantics. */
abstract class FoodFormActivity(private val layout:Int,private val editing:Boolean):XmlActivity(){
    private var existing:Meal?=null
    private var photo=""
    private var foodId=""
    private var grams=100
    private fun input(id:Int)=findViewById<EditText>(id)
    private val picker=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{
        val size=contentResolver.openAssetFileDescriptor(uri,"r").use{it?.length?:-1};require(size in 1..5*1024*1024){"Pilih foto maksimal 5 MB."}
        contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);photo=uri.toString();showMealPhoto(findViewById(R.id.ivPhoto),photo)
    }.onFailure{error(findViewById(R.id.tvError),it.message?:"Foto tidak dapat dibuka.")}}}
    private val catalogPicker=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        if(result.resultCode==RESULT_OK){val data=result.data;FoodCatalog.find(this,data?.getStringExtra("food_id"))?.let{applyFood(it,data?.getIntExtra("food_grams",100)?:100)}}
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state);if(loginRequired())return
        setContentView(layout)
        existing=store.meals(email).find{it.id==intent.getStringExtra("meal_id")}
        if(editing&&existing==null){Toast.makeText(this,"Catatan sudah tidak tersedia.",Toast.LENGTH_SHORT).show();finish();return}
        photo=state?.getString("photo")?:existing?.photo.orEmpty();foodId=state?.getString("foodId")?:existing?.foodId.orEmpty();grams=state?.getInt("grams")?:existing?.grams?.takeIf{it>0}?:100
        showMealPhoto(findViewById(R.id.ivPhoto),photo)
        if(state==null){
            val m=existing
            input(R.id.etFoodName).setText(m?.name.orEmpty());input(R.id.etPortions).setText((m?.portions?:1).toString())
            input(R.id.etCalories).setText(m?.takeIf{it.caloriesKnown}?.calories?.toString().orEmpty());input(R.id.etSugar).setText(m?.takeIf{it.sugarKnown}?.sugar?.toString().orEmpty());input(R.id.etSodium).setText(m?.takeIf{it.sodiumKnown}?.sodium?.toString().orEmpty());input(R.id.etNotes).setText(m?.notes.orEmpty())
            findViewById<Spinner>(R.id.spTime).setSelection(resources.getStringArray(R.array.nc_mealtimes).indexOf(m?.time?:"Siang").coerceAtLeast(0))
            if(!editing)FoodCatalog.find(this,intent.getStringExtra("food_id"))?.let{applyFood(it,intent.getIntExtra("food_grams",100))}
        }
        val entries=FoodCatalog.all(this);val labels=entries.map{"${it.name} • ${it.id}"}
        findViewById<AutoCompleteTextView>(R.id.etFoodName).apply{
            threshold=2;setAdapter(ArrayAdapter(this@FoodFormActivity,android.R.layout.simple_dropdown_item_1line,labels))
            setOnItemClickListener{parent,_,position,_->val selected=parent.getItemAtPosition(position).toString().substringAfterLast(" • ");FoodCatalog.find(this@FoodFormActivity,selected)?.let{applyFood(it,100)}}
        }
        findViewById<View>(R.id.btnFoodCatalog).setOnClickListener{catalogPicker.launch(Intent(this,FoodCatalogActivity::class.java).putExtra("email",email).putExtra("pick_food",true))}
        findViewById<View>(R.id.btnPhoto).setOnClickListener{picker.launch(arrayOf("image/jpeg","image/png"))}
        findViewById<View>(R.id.btnBack).setOnClickListener{finish()};findViewById<View>(R.id.btnCancel).setOnClickListener{finish()}
        listOf(R.id.etPortions,R.id.etCalories,R.id.etSugar,R.id.etSodium).forEach{input(it).afterChange{updateTotal()}}
        setupRestoredAppearance(this,email,store);updateTotal();showReference()
        findViewById<View>(R.id.btnSave).setOnClickListener{save()}
    }
    private fun applyFood(food:FoodEntry,weight:Int){
        grams=weight.coerceIn(1,5000);foodId=food.id
        findViewById<AutoCompleteTextView>(R.id.etFoodName).setText(food.name,false)
        input(R.id.etPortions).setText("1")
        input(R.id.etCalories).setText(food.scaled(food.calories,grams)?.toString().orEmpty());input(R.id.etSugar).setText(food.scaled(food.sugar,grams)?.toString().orEmpty());input(R.id.etSodium).setText(food.scaled(food.sodium,grams)?.toString().orEmpty())
        showReference();updateTotal()
    }
    private fun showReference(){findViewById<TextView>(R.id.tvCatalogSelection).text=FoodCatalog.find(this,foodId)?.let{"${it.source} • ${it.id}\n1 porsi pada form = $grams gram. Gula yang tidak tersedia tetap kosong."}?:"Nilai kosong berarti belum tersedia. Isi sesuai label atau racikan; bukan otomatis nol."}
    private fun number(id:Int)=input(id).text.toString().toIntOrNull()
    private fun updateTotal(){
        val portion=number(R.id.etPortions)?.coerceIn(0,100)?:0
        fun value(id:Int,unit:String)=number(id)?.let{"${it.toLong()*portion} $unit"}?:"belum tersedia"
        findViewById<TextView>(R.id.tvTotal).text="Total: ${value(R.id.etCalories,"kkal")} • Gula ${value(R.id.etSugar,"g")} • Natrium ${value(R.id.etSodium,"mg")}" 
    }
    private fun save(){
        val name=input(R.id.etFoodName).text.toString().trim()
        if(name.isBlank()){input(R.id.etFoodName).error="Nama makanan wajib diisi.";input(R.id.etFoodName).requestFocus();return}
        val portion=number(R.id.etPortions)
        if(portion==null||portion !in 1..100){error(findViewById(R.id.tvError),"Porsi harus 1–100.");return}
        val limits=listOf(R.id.etCalories to 50000,R.id.etSugar to 5000,R.id.etSodium to 500000)
        if(limits.any{(id,max)->input(id).text.isNotBlank()&&(number(id)==null||number(id)!! !in 0..max)}){error(findViewById(R.id.tvError),"Isi angka positif yang valid, atau kosongkan nilai yang belum tersedia.");return}
        val cal=number(R.id.etCalories);val sugar=number(R.id.etSugar);val sodium=number(R.id.etSodium)
        val food=FoodCatalog.find(this,foodId)?.takeIf{it.name==name}
        val exact=food!=null&&cal==food.scaled(food.calories,grams)&&sugar==food.scaled(food.sugar,grams)&&sodium==food.scaled(food.sodium,grams)
        val source=if(food==null)"Input manual; periksa label/racikan" else (if(exact)"" else "Diubah manual dari ")+"${food.source} • ${food.id} • $grams g per porsi"
        val meal=Meal(existing?.id?:UUID.randomUUID().toString(),name,findViewById<Spinner>(R.id.spTime).selectedItem.toString(),portion,cal?:0,sugar?:0,sodium?:0,photo,input(R.id.etNotes).text.toString().trim(),existing?.stamp?:System.currentTimeMillis(),cal!=null,sugar!=null,sodium!=null,food?.id.orEmpty(),if(food==null)0 else grams,source)
        val all=store.meals(email);store.save(email,if(editing)all.map{if(it.id==meal.id)meal else it}else all+meal)
        Toast.makeText(this,if(editing)"Data berhasil diperbarui." else "Data berhasil disimpan.",Toast.LENGTH_SHORT).show();finish()
    }
    override fun onSaveInstanceState(out:Bundle){out.putString("photo",photo);out.putString("foodId",foodId);out.putInt("grams",grams);super.onSaveInstanceState(out)}
}
class FormInputActivity:FoodFormActivity(R.layout.activity_form_input,false)
class FormUpdateActivity:FoodFormActivity(R.layout.activity_form_update,true)
