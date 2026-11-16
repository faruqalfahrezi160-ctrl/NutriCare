package com.example.loginapp

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Live data chart: layout and palette follow the Figma target cards. */
class NutritionRingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var value = 0
    private var target = 100
    private var tint = Color.rgb(16,185,129)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun bind(value: Int, target: Int, color: Int) { this.value=value; this.target=target; this.tint=color; contentDescription="${value*100/target}%"; invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val stroke=5*resources.displayMetrics.density
        val circle=RectF(stroke,stroke,width-stroke,height-stroke)
        paint.style=Paint.Style.STROKE;paint.strokeWidth=stroke;paint.strokeCap=Paint.Cap.ROUND
        paint.color=Color.rgb(216,227,251);canvas.drawArc(circle,-90f,360f,false,paint)
        paint.color=tint;canvas.drawArc(circle,-90f,(value.toFloat()/target*360).coerceIn(0f,360f),false,paint)
        paint.style=Paint.Style.FILL;paint.color=if(value>target)Color.rgb(255,107,107) else Color.rgb(17,28,45)
        paint.textSize=13*resources.displayMetrics.scaledDensity;paint.textAlign=Paint.Align.CENTER
        paint.typeface=ResourcesCompat.getFont(context,R.font.plus_jakarta)
        canvas.drawText("${value*100/target}%",width/2f,height/2f-(paint.ascent()+paint.descent())/2,paint)
    }
}
private fun Context.notice(title: String, text: String) { android.app.AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("Mengerti",null).show() }
private fun ViewGroup.tintSelection(selected: Boolean, filled: Boolean = false) {
    isSelected=selected
    for(i in 0 until childCount){val child=getChildAt(i);if(child is TextView) child.setTextColor(Color.parseColor(if(selected&&filled)"#FFFFFF" else if(selected)"#006C49" else "#3C4A42"))}
}
fun setupRestoredAppearance(activity: XmlActivity, email: String, store: LocalStore) {
    fun <T:View> v(id:Int):T? = activity.findViewById(id)
    v<View>(R.id.headerSearch)?.setOnClickListener { activity.startActivity(Intent(activity,FoodCatalogActivity::class.java).putExtra("email",email)) }
    v<View>(R.id.headerProfile)?.setOnClickListener { activity.startActivity(Intent(activity,ExtraScreensActivity::class.java).putExtra("email",email).putExtra("screen","profile")) }
    v<View>(R.id.headerBell)?.setOnClickListener { activity.notice("Notifikasi","Belum ada notifikasi baru.") }
    listOf(R.id.btnAi,R.id.btnDetailAi).forEach { v<View>(it)?.setOnClickListener { activity.startActivity(Intent(activity,AiChatActivity::class.java).putExtra("email",email).putExtra("meal_id",activity.intent.getStringExtra("meal_id"))) } }
    listOf(R.id.btnGoogle,R.id.btnApple).forEach { id -> v<View>(id)?.setOnClickListener { activity.notice("Masuk dengan akun lain","Login Google/Apple belum terhubung. Gunakan akun lokal atau akun demo.") } }
    v<View>(R.id.btnForgot)?.setOnClickListener { activity.notice("Lupa Kata Sandi?","Pemulihan email belum tersedia. Untuk mencoba aplikasi, gunakan demo@nutricare.id dengan kata sandi NutriCare123.") }
    v<View>(R.id.btnEye)?.setOnClickListener { v<CheckBox>(R.id.cbShowPassword)?.let { it.isChecked=!it.isChecked } }
    v<View>(R.id.btnClearFood)?.setOnClickListener { v<EditText>(R.id.etFoodName)?.text?.clear() }
    v<View>(R.id.btnRegisterEye)?.setOnClickListener { v<EditText>(R.id.etPassword)?.let { input -> input.transformationMethod=if(input.transformationMethod==null)android.text.method.PasswordTransformationMethod.getInstance() else null;input.setSelection(input.length()) } }
    val strength=v<ProgressBar>(R.id.passwordStrength)
    if(strength!=null)v<EditText>(R.id.etPassword)?.afterChange { strength.progress=(v<EditText>(R.id.etPassword)?.length()?:0).coerceAtMost(12) }
    val cond=v<Spinner>(R.id.spCondition)
    if(cond!=null){
        val ids=listOf(R.id.condition0,R.id.condition1,R.id.condition2,R.id.condition3,R.id.condition4,R.id.condition5)
        fun sync(){ids.forEachIndexed{i,id->v<ViewGroup>(id)?.tintSelection(i==cond.selectedItemPosition,true)} }
        ids.forEachIndexed { i,id -> v<View>(id)?.setOnClickListener { cond.setSelection(i);sync() } }
        sync()
    }
    val time=v<Spinner>(R.id.spTime)
    if(time!=null){
        val ids=listOf(R.id.timeChoice0,R.id.timeChoice1,R.id.timeChoice2,R.id.timeChoice3)
        fun sync(){ids.forEachIndexed{i,id->v<TextView>(id)?.let{it.isSelected=i==time.selectedItemPosition;it.setTextColor(Color.parseColor(if(it.isSelected)"#FFFFFF" else "#3C4A42"))}}}
        ids.forEachIndexed{i,id->v<View>(id)?.setOnClickListener{time.setSelection(i);sync()}}
        time.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long)=sync();override fun onNothingSelected(parent:AdapterView<*>?)=Unit};sync()
    }
    v<View>(R.id.btnLess)?.setOnClickListener { v<EditText>(R.id.etPortions)?.let { it.setText(((it.text.toString().toIntOrNull()?:1)-1).coerceAtLeast(1).toString()) } }
    v<View>(R.id.btnMore)?.setOnClickListener { v<EditText>(R.id.etPortions)?.let { it.setText(((it.text.toString().toIntOrNull()?:1)+1).coerceAtMost(100).toString()) } }
    listOf(R.id.slideretCalories to R.id.etCalories,R.id.slideretSugar to R.id.etSugar,R.id.slideretSodium to R.id.etSodium).forEach { (sliderId,inputId)->
        val slider=v<SeekBar>(sliderId); val input=v<EditText>(inputId)
        if(slider!=null&&input!=null){
            slider.progress=input.text.toString().toIntOrNull()?:0
            slider.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(bar:SeekBar?,progress:Int,fromUser:Boolean){if(fromUser)input.setText(progress.toString())}
                override fun onStartTrackingTouch(bar:SeekBar?)=Unit;override fun onStopTrackingTouch(bar:SeekBar?)=Unit
            })
            input.afterChange { val value=input.text.toString().toIntOrNull()?:0;if(slider.progress!=value)slider.progress=value }
        }
    }
    listOf(R.id.noteTag0,R.id.noteTag1,R.id.noteTag2).forEachIndexed{i,id->v<View>(id)?.setOnClickListener { v<EditText>(R.id.etNotes)?.let { input->val tag=listOf("Tanpa MSG","Kurang Garam","Kuah/Saus Pisah")[i];if(!input.text.contains(tag))input.setText(input.text.toString()+if(input.text.isEmpty())tag else ", $tag") } }}
    val active=when(activity){is HomeActivity->R.id.navHome;is DataListActivity->R.id.navJournal;is ExtraScreensActivity->if(activity.intent.getStringExtra("screen")=="profile")R.id.navProfile else R.id.navRecipes;else->0}
    listOf(R.id.navHome,R.id.navJournal,R.id.navRecipes,R.id.navProfile).forEach { id->v<ViewGroup>(id)?.tintSelection(active==id) }
}
fun refreshRestoredHome(activity:HomeActivity,store:LocalStore,email:String){
    val meals=store.meals(email).filter{it.isToday()}
    val calories=meals.sumOf{it.calories*it.portions};val sugar=meals.sumOf{it.sugar*it.portions};val sodium=meals.sumOf{it.sodium*it.portions}
    activity.findViewById<NutritionRingView>(R.id.ringCalories).bind(calories,1800,Color.parseColor("#10B981"))
    activity.findViewById<NutritionRingView>(R.id.ringSugar).bind(sugar,25,Color.parseColor("#006C49"))
    activity.findViewById<NutritionRingView>(R.id.ringSodium).bind(sodium,1500,Color.parseColor(if(sodium>1500)"#FF6B6B" else "#006C49"))
    activity.findViewById<TextView>(R.id.tvCalValue).text="$calories / 1.800\nkkal"
    activity.findViewById<TextView>(R.id.tvSugarValue).text="$sugar / 25\ngram"
    activity.findViewById<TextView>(R.id.tvSodiumValue).text="$sodium / 1.500\nmg"
    activity.findViewById<TextView>(R.id.tvCondition).text="Kondisi: ${store.condition(email)}"
    activity.findViewById<TextView>(R.id.tvNutritionCompleteness).text=if(meals.nutritionIncomplete())"Total hanya mencakup nilai yang diketahui; ada data gizi belum lengkap." else "Ringkasan dari catatan hari ini."
    activity.findViewById<TextView>(R.id.tvDate).text=SimpleDateFormat("dd MMM yyyy",Locale("id","ID")).format(Date())
    activity.findViewById<View>(R.id.saltWarning).visibility=if(sodium>1500)View.VISIBLE else View.GONE
    val rows=listOf(R.id.recent1,R.id.recent2);val names=listOf(R.id.tvRecentName1,R.id.tvRecentName2);val times=listOf(R.id.tvRecentTime1,R.id.tvRecentTime2);val infos=listOf(R.id.tvRecentInfo1,R.id.tvRecentInfo2);val photos=listOf(R.id.ivRecent1,R.id.ivRecent2)
    rows.forEachIndexed{i,id->val meal=meals.getOrNull(i);activity.findViewById<View>(id).visibility=if(meal==null)View.GONE else View.VISIBLE;if(meal!=null){activity.findViewById<TextView>(names[i]).text=meal.name;activity.findViewById<TextView>(times[i]).text=meal.time.uppercase();activity.findViewById<TextView>(infos[i]).text="Na: ${meal.sodiumLabel()} • ${meal.energyLabel()}";showMealPhoto(activity.findViewById(photos[i]),meal.photo);activity.findViewById<View>(id).setOnClickListener{activity.startActivity(Intent(activity,DetailActivity::class.java).putExtra("email",email).putExtra("meal_id",meal.id))}}}
}
