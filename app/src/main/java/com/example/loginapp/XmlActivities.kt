package com.example.loginapp

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginapp.databinding.*
import com.example.loginapp.ui.theme.LoginAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared session and bottom navigation for the XML Activities. */
abstract class XmlActivity : ComponentActivity() {
    protected val store by lazy { LocalStore(this) }
    protected val email by lazy { intent.getStringExtra("email") ?: store.user }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = android.graphics.Color.WHITE
        window.navigationBarColor = android.graphics.Color.WHITE
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
    }
    protected fun open(target: Class<out ComponentActivity>, id: String? = null, extra: String? = null) {
        startActivity(Intent(this, target).putExtra("email", email).putExtra("meal_id", id).putExtra("screen", extra))
    }
    protected fun loginRequired(): Boolean {
        if (email.isNotBlank()) return false
        startActivity(Intent(this, LoginActivity::class.java)); finish(); return true
    }
    protected fun navigation() {
        findViewById<View>(R.id.navHome)?.setOnClickListener {
            if (this !is HomeActivity) { open(HomeActivity::class.java); finish() }
        }
        findViewById<View>(R.id.navJournal)?.setOnClickListener {
            if (this !is DataListActivity) { open(DataListActivity::class.java); finish() }
        }
        findViewById<View>(R.id.navRecipes)?.setOnClickListener { open(ExtraScreensActivity::class.java, extra = "recipes") }
        findViewById<View>(R.id.navProfile)?.setOnClickListener { open(ExtraScreensActivity::class.java, extra = "profile") }
    }
    protected fun logout() {
        AiSettings(this,email).clearMemory()
        store.user = ""
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }
    protected fun error(view: TextView, message: String) { view.text = message; view.visibility = View.VISIBLE; view.announceForAccessibility(message) }
}

/** Launcher: activity_splash.xml is shown for two seconds, then routes to login/home. */
class SplashActivity : XmlActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val proceed = Runnable {
        val target = if (store.user.isEmpty()) LoginActivity::class.java else HomeActivity::class.java
        startActivity(Intent(this, target).putExtra("email", store.user)); finish()
    }
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContentView(ActivitySplashBinding.inflate(layoutInflater).root); findViewById<View>(R.id.btnStart).setOnClickListener { startActivity(Intent(this,RegisterActivity::class.java)); finish() }; findViewById<View>(R.id.btnSplashLogin).setOnClickListener { startActivity(Intent(this,LoginActivity::class.java)); finish() } }
    override fun onStart() { super.onStart(); handler.postDelayed(proceed, 2000) }
    override fun onStop() { handler.removeCallbacks(proceed); super.onStop() }
}

class LoginActivity : XmlActivity() {
    private lateinit var binding: ActivityLoginBinding
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityLoginBinding.inflate(layoutInflater); setContentView(binding.root); setupRestoredAppearance(this, email, store)
        // Passwords are deliberately excluded from Android's saved view state.
        binding.etPassword.isSaveEnabled = false
        binding.cbShowPassword.setOnCheckedChangeListener { _, visible ->
            binding.etPassword.transformationMethod = if (visible) null else PasswordTransformationMethod.getInstance()
            binding.etPassword.setSelection(binding.etPassword.length())
        }
        binding.btnDemo.setOnClickListener { binding.etEmail.setText("demo@nutricare.id"); binding.etPassword.setText("NutriCare123"); binding.tvError.visibility = View.GONE }
        binding.btnRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
        binding.btnAdminPortal.setOnClickListener { startActivity(Intent(this, AdminLoginActivity::class.java)) }
        binding.btnLogin.setOnClickListener { submit() }
        binding.etPassword.setOnEditorActionListener { _, action, _ -> if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { submit(); true } else false }
        binding.etPassword.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
    }
    private fun submit() {
        val address = binding.etEmail.text.toString().trim().lowercase(Locale.ROOT)
        val password = binding.etPassword.text.toString()
        if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) { binding.etEmail.error = "Masukkan email yang valid."; return }
        if (password.length < 8) { binding.etPassword.error = "Kata sandi minimal 8 karakter."; return }
        binding.btnLogin.isEnabled = false
        lifecycleScope.launch {
            val valid = withContext(Dispatchers.Default) { store.login(address, password) }
            binding.btnLogin.isEnabled = true
            if (valid) {
                store.user = if (binding.cbRemember.isChecked) address else ""
                binding.etPassword.text.clear()
                startActivity(Intent(this@LoginActivity, HomeActivity::class.java).putExtra("email", address).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            } else error(binding.tvError, "Email atau kata sandi tidak cocok.")
        }
    }
}

class RegisterActivity : XmlActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val b = ActivityRegisterBinding.inflate(layoutInflater); setContentView(b.root); setupRestoredAppearance(this, email, store)
        b.etPassword.isSaveEnabled = false; b.etConfirm.isSaveEnabled = false
        b.btnBack.setOnClickListener { finish() }; b.btnLogin.setOnClickListener { finish() }
        b.btnRegister.setOnClickListener {
            val name = b.etName.text.toString().trim(); val address = b.etEmail.text.toString().trim().lowercase(Locale.ROOT)
            val password = b.etPassword.text.toString()
            val message = when {
                name.isEmpty() -> "Nama lengkap wajib diisi."
                !Patterns.EMAIL_ADDRESS.matcher(address).matches() -> "Masukkan email yang valid."
                password.length < 8 -> "Kata sandi minimal 8 karakter."
                password != b.etConfirm.text.toString() -> "Konfirmasi kata sandi berbeda."
                !b.cbAgree.isChecked -> "Setujui penyimpanan data lokal terlebih dahulu."
                else -> ""
            }
            if (message.isNotEmpty()) { error(b.tvError, message); return@setOnClickListener }
            b.btnRegister.isEnabled = false
            val condition = b.spCondition.selectedItem.toString()
            lifecycleScope.launch {
                val created = withContext(Dispatchers.Default) { store.register(name, address, password, condition) }
                b.btnRegister.isEnabled = true
                if (created) {
                    b.etPassword.text.clear(); b.etConfirm.text.clear()
                    Toast.makeText(this@RegisterActivity, "Akun berhasil dibuat. Silakan Login.", Toast.LENGTH_LONG).show(); finish()
                } else error(b.tvError, "Email sudah terdaftar. Silakan Login.")
            }
        }
    }
}

class HomeActivity : XmlActivity() {
    private lateinit var binding: ActivityHomeBinding
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); if (loginRequired()) return
        binding = ActivityHomeBinding.inflate(layoutInflater); setContentView(binding.root); setupRestoredAppearance(this, email, store); navigation()
        binding.btnAdd.setOnClickListener { open(FormInputActivity::class.java) }
        binding.btnJournal.setOnClickListener { open(DataListActivity::class.java) }
        binding.btnRecipes.setOnClickListener { open(ExtraScreensActivity::class.java, extra = "recipes") }
        binding.btnCalculator.setOnClickListener { open(FoodCatalogActivity::class.java) }
        binding.btnAllMeals.setOnClickListener { open(DataListActivity::class.java) }
        binding.btnCatalog.setOnClickListener { open(FoodCatalogActivity::class.java) }
    }
    override fun onResume() {
        super.onResume(); if (!::binding.isInitialized) return
        binding.tvGreeting.text = "Halo, ${store.name(email)}! 👋"
        refreshRestoredHome(this, store, email)
        val meals = store.meals(email); val daily = meals.filter { it.isToday() }
        binding.tvSummary.text = "${daily.sumOf { it.calories * it.portions }} kkal\nGula ${daily.sumOf { it.sugar * it.portions }} g • Natrium ${daily.sumOf { it.sodium * it.portions }} mg"
        binding.tvRecent.text = if (meals.isEmpty()) "Belum ada catatan. Ketuk Tambah Catatan Makan." else meals.takeLast(3).reversed().joinToString("\n\n") { "${it.time} • ${it.name}\n${it.portions} porsi • ${it.calories * it.portions} kkal" }
    }
}

/** The real RecyclerView inflates item_row.xml for every journal entry. */
class MealXmlAdapter(private val selected: (Meal) -> Unit) : ListAdapter<Meal, MealXmlAdapter.Holder>(object : DiffUtil.ItemCallback<Meal>() {
    override fun areItemsTheSame(old: Meal, new: Meal) = old.id == new.id
    override fun areContentsTheSame(old: Meal, new: Meal) = old == new
}) {
    class Holder(val binding: ItemRowBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val meal = getItem(position); val b = holder.binding
        b.tvMealName.text = meal.name; b.tvTime.text = "${meal.time.uppercase()} • ${SimpleDateFormat("HH:mm", Locale.ROOT).format(Date(meal.stamp))}"
        b.tvPortion.text = "Takaran: ${meal.portions} porsi";
        b.tvNutritionCalories.text=meal.energyLabel(); b.tvNutritionSugar.text="Gula ${meal.sugarLabel()}"; b.tvNutritionSodium.text="Natrium ${meal.sodiumLabel()}"; b.tvVerification.text=if(!meal.sodiumKnown||!meal.sugarKnown||!meal.caloriesKnown)"Data gizi belum lengkap" else if(meal.sodium*meal.portions>600)"Perhatikan porsi kuah dan bumbu" else "Catatan nutrisi tersimpan"; b.tvNutrition.text = "${meal.calories * meal.portions} kkal • Gula ${meal.sugar * meal.portions} g • Natrium ${meal.sodium * meal.portions} mg"
        b.ivMeal.contentDescription = meal.name; showMealPhoto(b.ivMeal, meal.photo)
        b.ivVerification.setImageResource(when(meal.id){"lunch"->R.drawable.fg_s2_16;"snack"->R.drawable.fg_s2_17;"dinner"->R.drawable.fg_s2_18;else->R.drawable.fg_s2_12})
        b.btnRowOptions.setOnClickListener { selected(meal) }
        b.root.setOnClickListener { selected(meal) }; b.root.contentDescription = null
    }
}
class DataListActivity : XmlActivity() {
    private lateinit var binding: ActivityListDataBinding
    private val adapter = MealXmlAdapter { open(DetailActivity::class.java, it.id) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); if (loginRequired()) return
        binding = ActivityListDataBinding.inflate(layoutInflater); setContentView(binding.root); setupRestoredAppearance(this, email, store); navigation()
        binding.rvMeals.layoutManager = LinearLayoutManager(this); binding.rvMeals.adapter = adapter
        binding.btnAdd.setOnClickListener { open(FormInputActivity::class.java) }
        binding.etSearch.afterChange { refresh() }
        listOf(R.id.filter0,R.id.filter1,R.id.filter2,R.id.filter3,R.id.filter4).forEachIndexed { i,id -> findViewById<View>(id).setOnClickListener { binding.spFilter.setSelection(i);refresh() } }
        binding.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = refresh()
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }
    override fun onResume() { super.onResume(); if (::binding.isInitialized) refresh() }
    private fun refresh() {
        val query = binding.etSearch.text.toString(); val filter = binding.spFilter.selectedItem?.toString() ?: "Semua"
        val shown = store.meals(email).filter { it.name.contains(query, true) && (filter == "Semua" || filter == it.time) }
        adapter.submitList(shown); binding.tvCount.text = "${shown.size} catatan"
        binding.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        val daily=store.meals(email).filter{it.isToday()};val salt=daily.sumOf{it.sodium*it.portions}
        binding.tvJournalCalories.text="${daily.sumOf{it.calories*it.portions}} kkal";binding.tvJournalSalt.text="Natrium ${salt*100/1500}%";binding.journalProgress.progress=salt;binding.tvJournalSummary.text="Natrium tercatat $salt mg"+if(daily.nutritionIncomplete())" • data belum lengkap" else ""
        listOf(R.id.filter0,R.id.filter1,R.id.filter2,R.id.filter3,R.id.filter4).forEachIndexed { i,id -> findViewById<TextView>(id).let { it.isSelected=i==binding.spFilter.selectedItemPosition;it.setTextColor(android.graphics.Color.parseColor(if(it.isSelected)"#FFFFFF" else "#3C4A42")) } }
    }
}
fun EditText.afterChange(changed: () -> Unit) { addTextChangedListener(object : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: Editable?) = Unit
}) }
fun showMealPhoto(view: ImageView, photo: String) {
    view.setImageResource(R.drawable.nc_food)
    if (photo.isBlank()) {
        if(view.context is FormInputActivity){view.setImageResource(R.drawable.fg_s951_2);view.scaleType=ImageView.ScaleType.CENTER_INSIDE;val padding=(60*view.resources.displayMetrics.density).toInt();view.setPadding(padding,padding,padding,padding)}
        return
    }
    view.setPadding(0,0,0,0);view.scaleType=ImageView.ScaleType.CENTER_CROP
    runCatching {
        val bitmap = if (photo.startsWith("content:")) view.context.contentResolver.openInputStream(Uri.parse(photo)).use { BitmapFactory.decodeStream(it) }
        else view.context.assets.open("figma/"+if(photo=="s2_1.png"&&view.context is FormUpdateActivity)"s515_0.png" else if(photo=="s2_1.png"&&view.context is DetailActivity)"s384_0.png" else photo).use { BitmapFactory.decodeStream(it) }
        if (bitmap != null) view.setImageBitmap(bitmap)
    }
}

class DetailActivity : XmlActivity() {
    private lateinit var binding: ActivityDetailBinding
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); if (loginRequired()) return
        binding = ActivityDetailBinding.inflate(layoutInflater); setContentView(binding.root); setupRestoredAppearance(this, email, store)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnEdit.setOnClickListener { open(FormUpdateActivity::class.java, intent.getStringExtra("meal_id")) }
        binding.btnDelete.setOnClickListener {
            android.app.AlertDialog.Builder(this).setTitle("Hapus catatan makanan?")
                .setMessage("Catatan ini akan dihapus dari jurnal. Tindakan ini tidak dapat dibatalkan.")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                    store.save(email, store.meals(email).filter { it.id != intent.getStringExtra("meal_id") }); finish()
                }.show()
        }
    }
    override fun onResume() {
        super.onResume(); if (!::binding.isInitialized) return
        val meal = store.meals(email).find { it.id == intent.getStringExtra("meal_id") } ?: run { finish(); return }
        binding.tvMealName.text = meal.name; binding.tvTime.text = "${meal.time} • ${meal.portions} porsi • ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(meal.stamp))}"
        binding.tvNutrition.text = "${meal.energyLabel()}\nGula ${meal.sugarLabel()}\nNatrium ${meal.sodiumLabel()}"
        val reference=FoodCatalog.find(this,meal.foodId)
        fun macro(value:Double?)=if(value==null||meal.grams<=0)"—" else String.format(java.util.Locale.ROOT,"%.1fg",value*meal.grams/100.0*meal.portions)
        binding.tvMacroCarb.text=macro(reference?.carbs);binding.tvMacroProtein.text=macro(reference?.protein);binding.tvMacroFat.text=macro(reference?.fat)
        binding.tvDetailSodium.text="Natrium ${meal.sodiumLabel()}";binding.tvMealCategory.text=meal.time.uppercase()
        binding.tvNotes.text = meal.notes.ifBlank { "Tidak ada catatan tambahan." }+if(meal.nutritionSource.isBlank())"" else "\n\nSumber gizi: ${meal.nutritionSource}"; showMealPhoto(binding.ivPhoto, meal.photo)
    }
}

/** Optional existing recipe/profile screens remain available outside the required XML flow. */
class ExtraScreensActivity : XmlActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state); if (loginRequired()) return
        val b = ActivityExtraBinding.inflate(layoutInflater); setContentView(b.root); setupRestoredAppearance(this, email, store); navigation()
        b.tvExtraTitle.text = if (intent.getStringExtra("screen") == "profile") "Profil" else "Resep Sehat"
        var returnFromDetail: (() -> Boolean)? = null
        b.btnBack.setOnClickListener { if (returnFromDetail?.invoke() != true) finish() }
        onBackPressedDispatcher.addCallback(this) { if (returnFromDetail?.invoke() != true) finish() }
        b.composeContent.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        b.composeContent.setContent {
            LoginAppTheme {
                var recipe by rememberSaveable { mutableStateOf(-1) }
                var favorites by remember { mutableStateOf(store.favorites(email)) }
                var info by remember { mutableStateOf<Pair<String, String>?>(null) }
                returnFromDetail = { if (recipe >= 0) { recipe = -1; true } else false }
                fun favorite(name: String) { favorites = if (name in favorites) favorites - name else favorites + name; store.favorites(email, favorites) }
                fun explain(title: String, body: String) { info = title to body }
                fun ai() { open(AiChatActivity::class.java) }
                if (intent.getStringExtra("screen") == "profile") ProfilePage(store, email, ::explain, ::ai, ::logout)
                else if (recipe >= 0) RecipeDetail(recipes[recipe], recipes[recipe].name in favorites, { favorite(recipes[recipe].name) }, ::ai)
                else RecipesPage(favorites, { recipe = it }, ::favorite, ::ai)
                info?.let { InfoDialog(it.first, it.second) { info = null } }
            }
        }
    }
}


