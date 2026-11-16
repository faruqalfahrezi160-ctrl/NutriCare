package com.example.loginapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.loginapp.databinding.ActivityAdminPanelBinding
import com.example.loginapp.databinding.ItemAdminRecordBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class AdminCard(val title:String,val subtitle:String,val badge:String,val body:String,val primary:String,val secondary:String="",val image:Int=0,val action:()->Unit,val other:()->Unit={})
class AdminPanelActivity:AdminBaseActivity(){
    private lateinit var b:ActivityAdminPanelBinding
    private var snapshot:AdminSnapshot?=null
    private var section="dashboard"
    private var filter="Semua"
    private var busy=false
    private val cards=AdminCardsAdapter()
    private var exportSnapshot:AdminSnapshot?=null
    private val exportPdf=registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->
        val data=exportSnapshot;exportSnapshot=null
        if(uri!=null&&data!=null)lifecycleScope.launch{
            try{withContext(Dispatchers.IO){val pdf=PdfDocument();try{
                val page=pdf.startPage(PdfDocument.PageInfo.Builder(595,842,1).create());val paint=Paint(Paint.ANTI_ALIAS_FLAG);paint.color=Color.rgb(0,108,73);paint.textSize=23f
                page.canvas.drawText("NutriCare • Laporan Admin",40f,60f,paint);paint.textSize=11f;paint.color=Color.DKGRAY
                val lines=listOf("MODE DEMO — bukan laporan pengguna lintas perangkat", "Dibuat: "+SimpleDateFormat("dd MMM yyyy HH:mm",Locale("id","ID")).format(Date()),"Pengguna contoh: ${data.people.size}","Akun contoh aktif: ${data.people.count{it.active}}","Catatan contoh: ${data.people.sumOf{it.journals}}","Bahan pangan: ${data.foods.size}","Resep terbit: ${data.recipes.count{it.status=="Terbit"}} / ${data.recipes.size}","Kondisi aktif: ${data.conditions.count{it.active}}","Permintaan admin tertunda: ${data.requests.count{it.status=="pending"}}","","Koneksi database akan disediakan oleh tim backend.")
                lines.forEachIndexed{i,line->page.canvas.drawText(line,40f,100f+i*25f,paint)};pdf.finishPage(page)
                contentResolver.openOutputStream(uri)?.use{pdf.writeTo(it)}?:error("Berkas tidak dapat ditulis.")
            }finally{pdf.close()}};Toast.makeText(this@AdminPanelActivity,"Laporan PDF demo tersimpan.",Toast.LENGTH_LONG).show()}
            catch(e:Exception){showError("Ekspor gagal: ${e.message}")}
        }
    }
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        b=ActivityAdminPanelBinding.inflate(layoutInflater);setContentView(b.root)
        section=state?.getString("section")?:"dashboard"
        b.rvAdminItems.layoutManager=LinearLayoutManager(this);b.rvAdminItems.adapter=cards
        val navigation=listOf(b.adminNavDashboard to "dashboard",b.adminNavConditions to "conditions",b.adminNavFoods to "foods",b.adminNavRecipes to "recipes",b.adminNavUsers to "users")
        navigation.forEach{(view,key)->view.compoundDrawables.filterNotNull().forEach{it.setBounds(0,0,dp(21),dp(21))};val top=view.compoundDrawables[1];view.setCompoundDrawables(null,top,null,null);view.setOnClickListener{select(key)}}
        b.btnAdminRefresh.setOnClickListener{load()}
        b.btnAdminAccount.setOnClickListener{val a=repo.current()?:return@setOnClickListener;AlertDialog.Builder(this).setTitle(a.name).setMessage("${a.email}\nPeran: ${if(a.role=="owner")"Pemilik" else "Admin"}\n\n${repo.modeLabel}").setNegativeButton("Tutup",null).setPositiveButton("Keluar admin"){_,_->repo.logout();goLogin()}.show()}
        b.etAdminSearch.afterChange{renderList()}
        b.btnAdminPrimary.setOnClickListener{if(busy)return@setOnClickListener;when(section){"dashboard"->select("stats");"stats"->{exportSnapshot=snapshot;exportPdf.launch("NutriCare-Laporan-Demo.pdf")};"conditions"->conditionForm(null);"foods"->foodForm(null);"recipes"->recipeForm(null);"users"->startActivity(Intent(this,AdminRegisterActivity::class.java))}}
        onBackPressedDispatcher.addCallback(this){if(section!="dashboard")select("dashboard") else finish()}
    }
    override fun onResume(){super.onResume();if(repo.current()==null)goLogin() else load()}
    override fun onSaveInstanceState(out:Bundle){out.putString("section",section);super.onSaveInstanceState(out)}
    private fun goLogin(){startActivity(Intent(this,AdminLoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish()}
    private fun dp(value:Int)=(value*resources.displayMetrics.density).toInt()
    private fun showError(message:String){b.tvAdminError.text=message;b.tvAdminError.visibility=View.VISIBLE}
    private fun work(action:()->Unit={}){
        if(busy)return
        busy=true;b.adminLoading.visibility=View.VISIBLE;b.tvAdminError.visibility=View.GONE;b.btnAdminRefresh.isEnabled=false
        lifecycleScope.launch{
            try{snapshot=withContext(Dispatchers.IO){action();repo.snapshot()};render()}
            catch(e:CancellationException){throw e}
            catch(e:Exception){if(repo.current()==null)goLogin() else showError(e.message?:"Data belum dapat dimuat.")}
            finally{busy=false;b.adminLoading.visibility=View.GONE;b.btnAdminRefresh.isEnabled=true}
        }
    }
    private fun load()=work()
    private fun select(value:String){section=value;filter="Semua";b.etAdminSearch.text.clear();render();b.adminDashboardScroll.scrollTo(0,0);b.rvAdminItems.scrollToPosition(0)}
    private fun render(){
        val data=snapshot?:return
        b.tvAdminOperator.text="DEMO • ${data.account.name}";b.tvAdminMode.text=repo.modeLabel
        val titles=mapOf("dashboard" to "Dashboard Admin","conditions" to "Kondisi Kesehatan","foods" to "Database Bahan Makanan","recipes" to "Menu & Resep Sehat","users" to "Akun Pengguna","stats" to "Statistik & Dampak")
        b.tvAdminSectionTitle.text=titles[section]
        b.tvAdminSectionNote.text=when(section){"dashboard"->"Ringkasan data contoh dan aktivitas pengelola.";"foods"->"${data.foods.size} bahan • nilai gizi per 100 gram.";"users"->"${data.people.size} pengguna contoh • ${data.requests.count{it.status=="pending"}} permintaan admin.";"conditions"->"Kelola informasi kondisi; konten perlu validasi tim kesehatan.";"recipes"->"Kelola draf, review, dan publikasi resep demo.";else->"Analitik data demo • belum tersinkron dengan HP pengguna."}
        b.btnAdminPrimary.text=when(section){"dashboard"->"Statistik ↗";"stats"->"Unduh PDF";"users"->"+ Admin";else->"+ Tambah"}
        val dashboard=section in listOf("dashboard","stats")
        b.adminDashboardScroll.visibility=if(dashboard)View.VISIBLE else View.GONE;b.rvAdminItems.visibility=if(dashboard)View.GONE else View.VISIBLE
        b.etAdminSearch.visibility=if(dashboard)View.GONE else View.VISIBLE;b.adminFiltersScroll.visibility=if(dashboard)View.GONE else View.VISIBLE
        b.etAdminSearch.hint=when(section){"users"->"Cari nama, email, atau ID…";"foods"->"Cari bahan atau kategori…";else->"Cari nama atau kategori…"}
        listOf(b.adminNavDashboard to "dashboard",b.adminNavConditions to "conditions",b.adminNavFoods to "foods",b.adminNavRecipes to "recipes",b.adminNavUsers to "users").forEach{(v,s)->v.isSelected=section==s||(section=="stats"&&s=="dashboard");v.setTextColor(Color.parseColor(if(v.isSelected)"#006C49" else "#53645B"))}
        if(dashboard){b.tvAdminEmpty.visibility=View.GONE;renderDashboard(data,section=="stats")}else{renderFilters();renderList()}
    }
    private fun renderFilters(){
        b.adminFilters.removeAllViews()
        val values=when(section){"users"->listOf("Semua","Aktif","Nonaktif","Admin");"recipes"->listOf("Semua","Terbit","Draf","Menunggu review");else->listOf("Semua","Aktif","Nonaktif")}
        values.forEach{value->val chip=TextView(this).apply{text=value;textSize=12f;typeface=ResourcesCompat.getFont(context,R.font.plus_jakarta);setTextColor(Color.parseColor(if(value==filter)"#FFFFFF" else "#006C49"));setBackgroundResource(if(value==filter)R.drawable.r_dark else R.drawable.r_white);setPadding(dp(14),dp(12),dp(14),dp(12));minHeight=dp(48);gravity=Gravity.CENTER;isClickable=true;isFocusable=true;setOnClickListener{filter=value;renderFilters();renderList()}};b.adminFilters.addView(chip,LinearLayout.LayoutParams(-2,-2).apply{marginEnd=dp(6)})}
    }
    private fun renderList(){
        val data=snapshot?:return;if(section in listOf("dashboard","stats"))return
        val q=b.etAdminSearch.text.toString().trim().lowercase(Locale.ROOT)
        fun match(text:String)=text.lowercase(Locale.ROOT).contains(q)
        fun active(on:Boolean)=filter=="Semua"||(filter=="Aktif"&&on)||(filter=="Nonaktif"&&!on)
        val result=when(section){
            "conditions"->data.conditions.filter{match(it.name+" "+it.category)&&active(it.active)}.map{c->AdminCard(c.name,c.category,if(c.active)"AKTIF" else "NONAKTIF",c.notes,"Edit kondisi",if(c.active)"Nonaktifkan" else "Aktifkan",action={conditionForm(c)},other={work{repo.saveCondition(c.copy(active=!c.active))}})}
            "foods"->data.foods.filter{match(it.name+" "+it.category+" "+it.id)&&active(it.active)}.map{f->AdminCard(f.name,"${f.category} • ${f.id}",if(f.active)"AKTIF" else "NONAKTIF","Energi: ${f.calories?.let{"$it kkal"}?:"belum tersedia"}\nNatrium: ${f.sodium?.let{"$it mg"}?:"belum tersedia"}\nSumber: ${f.source}","Edit bahan","Detail",action={foodForm(f)},other={info(f.name,"${f.category}\n${f.source}\n\nNilai per 100 g. Angka kosong bukan berarti nol. Perubahan di sini hanya untuk ruang demo admin.")})}
            "recipes"->data.recipes.filter{match(it.name+" "+it.category)&&(filter=="Semua"||it.status==filter)}.map{r->AdminCard(r.name,r.category,r.status,r.description,"Edit resep",if(r.status=="Menunggu review")"Review" else "Detail",r.image,action={recipeForm(r)},other={reviewRecipe(r)})}
            "users"->if(filter=="Admin")data.requests.filter{match(it.name+" "+it.email)}.sortedBy{if(it.status=="pending")0 else 1}.map{a->val pending=a.status=="pending"&&data.account.role=="owner";AdminCard(a.name,a.email,when(a.status){"pending"->"MENUNGGU";"active"->"AKTIF";else->"DITOLAK"},"Peran: ${if(a.role=="owner")"Pemilik" else "Admin"}\n${if(a.status=="pending")"Belum dapat mengakses dashboard." else "Akun ruang demo admin."}",if(pending)"Setujui" else "Detail",if(pending)"Tolak" else "",action={if(pending)confirm("Setujui admin?",a.email){work{repo.decideAdmin(a.email,true)}} else info(a.name,"${a.email}\nStatus: ${a.status}\nPeran: ${a.role}")},other={confirm("Tolak permintaan?",a.email){work{repo.decideAdmin(a.email,false)}}})}
                else data.people.filter{match(it.name+" "+it.email+" "+it.id)&&active(it.active)}.map{p->AdminCard(p.name,"${p.id} • ${p.condition}",if(p.active)"AKTIF" else "NONAKTIF","${p.journals} catatan makanan contoh\n${p.email}","Kelola pengguna",if(p.active)"Nonaktifkan" else "Aktifkan",action={personDetail(p)},other={confirm(if(p.active)"Nonaktifkan akun contoh?" else "Aktifkan akun contoh?",p.name+"\nHanya memengaruhi data demo admin."){work{repo.setPersonActive(p.id,!p.active)}}})}
            else->emptyList()
        }
        cards.submit(result);b.tvAdminEmpty.visibility=if(result.isEmpty())View.VISIBLE else View.GONE
    }
    private fun label(text:String,size:Float=14f,bold:Boolean=false,color:String="#111C2D")=TextView(this).apply{this.text=text;textSize=size;setTextColor(Color.parseColor(color));typeface=ResourcesCompat.getFont(context,R.font.plus_jakarta);if(bold)setTypeface(typeface,Typeface.BOLD);setLineSpacing(dp(3).toFloat(),1f);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(8)}}
    private fun panel(background:Int=R.drawable.r_white)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(16),dp(16),dp(16));setBackgroundResource(background);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)}}
    private fun button(text:String,action:()->Unit)=Button(this).apply{this.text=text;isAllCaps=false;textSize=12f;typeface=ResourcesCompat.getFont(context,R.font.plus_jakarta);setTextColor(Color.WHITE);setBackgroundResource(R.drawable.r_dark);minHeight=dp(48);setPadding(dp(12),dp(8),dp(12),dp(8));setOnClickListener{if(!busy)action()};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(8)}}
    private fun renderDashboard(data:AdminSnapshot,stats:Boolean){
        val root=b.adminDashboardContent;root.removeAllViews()
        if(!stats)root.addView(panel(R.drawable.r_ai_banner).apply{addView(label("SELAMAT DATANG, ADMIN",10f,true,"#006C49"));addView(label(data.account.name,21f,true));addView(label(if(data.account.role=="owner")"Pemilik • Pengelola akses dan konten" else "Admin • Pengelola konten",12f));addView(label("Data contoh tersimpan di perangkat ini.",11f,false,"#53645B"))})
        val metrics=if(stats)listOf(data.people.size.toString() to "Pengguna contoh",data.people.count{it.active}.toString() to "Akun aktif",data.people.sumOf{it.journals}.toString() to "Catatan contoh",data.recipes.count{it.status=="Terbit"}.toString() to "Resep terbit") else listOf(data.people.size.toString() to "Total pengguna contoh",data.foods.size.toString() to "Bahan pangan",data.recipes.size.toString() to "Katalog resep",data.conditions.size.toString() to "Kondisi kesehatan")
        metrics.chunked(2).forEach{pair->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};pair.forEachIndexed{i,(value,title)->row.addView(panel().apply{addView(label(value,28f,true));addView(label(title,11f,false,"#53645B"))},LinearLayout.LayoutParams(0,-2,1f).apply{bottomMargin=dp(14);if(i==0)marginEnd=dp(10)})};root.addView(row)}
        if(!stats){
            val pending=data.requests.count{it.status=="pending"};val review=data.recipes.count{it.status=="Menunggu review"}
            root.addView(label("Pemantauan & Notifikasi",16f,true))
            root.addView(panel(R.drawable.r_lavender).apply{addView(label("${data.requests.count{it.status=="active"}} akun admin aktif",15f,true));addView(label("Daftarkan email anggota tim agar bisa langsung masuk ke dashboard.",12f));addView(button("+ Daftarkan Admin Baru"){startActivity(Intent(this@AdminPanelActivity,AdminRegisterActivity::class.java))});addView(button("Lihat akun admin"){select("users");filter="Admin";renderFilters();renderList()});if(pending>0)addView(label("$pending akun versi lama menunggu persetujuan pemilik.",11f))})
            root.addView(panel(R.drawable.r_amber).apply{addView(label("$review resep menunggu review",15f,true));addView(label("Periksa bahan dan langkah sebelum menerbitkan konten demo.",12f));addView(button("Review konten resep"){select("recipes");filter="Menunggu review";renderFilters();renderList()})})
        }
        root.addView(panel().apply{addView(label("Aktivitas Jurnal • 7 Hari",16f,true));addView(label("Contoh entri harian untuk pratinjau analitik.",11f,false,"#53645B"));addView(AdminTrendView(this@AdminPanelActivity,data.dailyEntries),LinearLayout.LayoutParams(-1,dp(140)))})
        root.addView(panel().apply{addView(label("Sebaran Kondisi Pengguna Contoh",16f,true));data.people.groupingBy{it.condition}.eachCount().forEach{(condition,count)->addView(label("$condition   •   $count pengguna",12f));addView(ProgressBar(this@AdminPanelActivity,null,android.R.attr.progressBarStyleHorizontal).apply{max=data.people.size.coerceAtLeast(1);progress=count;progressTintList=android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))},LinearLayout.LayoutParams(-1,dp(8)).apply{bottomMargin=dp(12)})};addView(label("Kategori profil contoh, bukan diagnosis otomatis.",10f,false,"#53645B"))})
        if(stats)root.addView(panel(R.drawable.r_dark).apply{addView(label("Siap untuk integrasi tim",18f,true,"#FFFFFF"));addView(label("Ganti AdminRepository dengan sumber data server untuk mengaktifkan pemantauan lintas HP. Grafik dan jumlah saat ini berasal dari data demo.",12f,false,"#FFFFFF"))})
        else{
            root.addView(label("Log Aktivitas Sistem",16f,true))
            data.events.take(8).forEach{event->root.addView(panel().apply{addView(label(event.title,13f,true));addView(label(event.detail,11f,false,"#53645B"));addView(label(SimpleDateFormat("dd MMM • HH:mm",Locale("id","ID")).format(Date(event.time)),10f,false,"#006C49"))})}
            root.addView(button("Simulasikan notifikasi aplikasi"){notificationForm()})
        }
    }
    private fun info(title:String,message:String){AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Tutup",null).show()}
    private fun confirm(title:String,message:String,action:()->Unit){AlertDialog.Builder(this).setTitle(title).setMessage(message).setNegativeButton("Batal",null).setPositiveButton("Ya, lanjutkan"){_,_->if(!busy)action()}.show()}
    private fun personDetail(p:AdminPerson){AlertDialog.Builder(this).setTitle(p.name).setMessage("${p.id}\n${p.email}\nKategori profil: ${p.condition}\nCatatan contoh: ${p.journals}\nStatus: ${if(p.active)"Aktif" else "Nonaktif"}\n\nData simulasi admin. Riwayat pengguna lintas HP menunggu koneksi database tim.").setNegativeButton("Tutup",null).setPositiveButton(if(p.active)"Nonaktifkan" else "Aktifkan"){_,_->confirm("Ubah status akun contoh?",p.name){work{repo.setPersonActive(p.id,!p.active)}}}.show()}
    private fun reviewRecipe(r:AdminRecipe){val builder=AlertDialog.Builder(this).setTitle(r.name).setMessage("${r.category} • ${r.status}\n\n${r.description}\n\nBAHAN\n${r.ingredients}\n\nLANGKAH\n${r.steps}\n\nPublikasi hanya berlaku di ruang demo admin.").setNegativeButton("Tutup",null);if(r.status!="Terbit")builder.setPositiveButton("Setujui & terbitkan"){_,_->confirm("Terbitkan resep demo?",r.name){work{repo.saveRecipe(r.copy(status="Terbit"))}}};builder.show()}
    private fun fields():LinearLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),dp(10))}
    private fun field(parent:LinearLayout,title:String,value:String="",multi:Boolean=false,number:Boolean=false):EditText{
        parent.addView(label(title,12f,true));val input=EditText(this).apply{setText(value);textSize=14f;setTextColor(Color.parseColor("#111C2D"));setPadding(dp(12),dp(10),dp(12),dp(10));setBackgroundResource(R.drawable.r_lavender);inputType=if(number)InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else if(multi)InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE else InputType.TYPE_CLASS_TEXT;minHeight=dp(48);if(multi){minLines=3;gravity=Gravity.TOP}else setSingleLine(true);layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)}};parent.addView(input);return input
    }
    private fun form(title:String,body:LinearLayout,onSave:()->Unit,delete:(()->Unit)?=null){
        val builder=AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply{addView(body)}).setNegativeButton("Batal",null).setPositiveButton("Simpan",null)
        if(delete!=null)builder.setNeutralButton("Hapus"){_,_->confirm("Hapus data demo?","Data hanya dihapus dari ruang demo admin.",delete)}
        val dialog=builder.create();val error=label("",12f,false,"#B32635");body.addView(error)
        dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{if(busy)return@setOnClickListener;try{onSave();dialog.dismiss()}catch(e:Exception){error.text=e.message?:"Periksa isian form."}}};dialog.show()
    }
    private fun conditionForm(c:AdminCondition?){if(busy)return;val body=fields();val name=field(body,"Nama kondisi",c?.name.orEmpty());val category=field(body,"Kategori",c?.category.orEmpty());val notes=field(body,"Informasi / pedoman konten",c?.notes.orEmpty(),true);form(if(c==null)"Tambah kondisi" else "Edit kondisi",body,{
        require(name.text.isNotBlank()&&category.text.isNotBlank()){"Nama dan kategori wajib diisi."};val value=AdminCondition(c?.id?:DemoAdminRepository.newId(),name.text.toString().trim(),category.text.toString().trim(),notes.text.toString(),c?.active?:true);work{repo.saveCondition(value)}
    },c?.let{{work{repo.deleteRecord("conditions",it.id)}}})}
    private fun foodForm(f:AdminFood?){if(busy)return;val body=fields();val name=field(body,"Nama bahan",f?.name.orEmpty());val category=field(body,"Kategori",f?.category.orEmpty());val calories=field(body,"Energi / 100 g (kkal; kosong jika belum ada)",f?.calories?.toString().orEmpty(),number=true);val sodium=field(body,"Natrium / 100 g (mg; kosong jika belum ada)",f?.sodium?.toString().orEmpty(),number=true);val source=field(body,"Sumber data",f?.source?:"Input manual admin • belum diverifikasi");form(if(f==null)"Tambah bahan pangan" else "Edit bahan pangan",body,{
        require(name.text.isNotBlank()&&category.text.isNotBlank()){"Nama dan kategori wajib diisi."}
        fun number(input:EditText,max:Double):Double?{val raw=input.text.toString().trim();if(raw.isEmpty())return null;val v=raw.replace(',','.').toDoubleOrNull();require(v!=null&&v.isFinite()&&v in 0.0..max){"Angka harus antara 0 dan $max."};return v}
        val value=AdminFood(f?.id?:DemoAdminRepository.newId(),name.text.toString().trim(),category.text.toString().trim(),number(calories,1000.0),number(sodium,100000.0),source.text.toString().trim(),f?.active?:true);work{repo.saveFood(value)}
    },f?.let{{work{repo.deleteRecord("foods",it.id)}}})}
    private fun recipeForm(r:AdminRecipe?){if(busy)return;val body=fields();val name=field(body,"Nama resep",r?.name.orEmpty());val category=field(body,"Kategori",r?.category.orEmpty());val description=field(body,"Deskripsi",r?.description.orEmpty(),true);val ingredients=field(body,"Bahan (satu bahan per baris)",r?.ingredients.orEmpty(),true);val steps=field(body,"Langkah memasak",r?.steps.orEmpty(),true);body.addView(label("Status publikasi",12f,true));val status=Spinner(this).apply{adapter=ArrayAdapter(this@AdminPanelActivity,android.R.layout.simple_spinner_dropdown_item,listOf("Draf","Menunggu review","Terbit"));setSelection(listOf("Draf","Menunggu review","Terbit").indexOf(r?.status?:"Draf"));minimumHeight=dp(48)};body.addView(status);form(if(r==null)"Resep baru" else "Edit resep",body,{
        require(name.text.isNotBlank()&&ingredients.text.isNotBlank()&&steps.text.isNotBlank()){"Nama, bahan, dan langkah wajib diisi."};val value=AdminRecipe(r?.id?:DemoAdminRepository.newId(),name.text.toString().trim(),category.text.toString().trim(),description.text.toString(),ingredients.text.toString(),steps.text.toString(),status.selectedItem.toString(),r?.image?:0);work{repo.saveRecipe(value)}
    },r?.let{{work{repo.deleteRecord("recipes",it.id)}}})}
    private fun notificationForm(){val body=fields();val message=field(body,"Pesan notifikasi demo",multi=true);body.addView(label("Simulasi ini dicatat pada log. Belum mengirim push notification ke HP pengguna.",11f,false,"#53645B"));form("Simulasi Notifikasi",body,{require(message.text.isNotBlank()&&message.length()<=500){"Pesan harus 1–500 karakter."};val text=message.text.toString();work{repo.notifyUsers(text)}})}
}
private class AdminCardsAdapter:RecyclerView.Adapter<AdminCardsAdapter.Holder>(){
    private var items=listOf<AdminCard>()
    class Holder(val b:ItemAdminRecordBinding):RecyclerView.ViewHolder(b.root)
    fun submit(value:List<AdminCard>){items=value;notifyDataSetChanged()}
    override fun getItemCount()=items.size
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(ItemAdminRecordBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(h:Holder,position:Int){val row=items[position];with(h.b){tvAdminItemTitle.text=row.title;tvAdminItemSubtitle.text=row.subtitle;tvAdminItemBadge.text=row.badge;tvAdminItemBody.text=row.body;btnAdminItemPrimary.text=row.primary;btnAdminItemPrimary.setOnClickListener{row.action()};btnAdminItemSecondary.text=row.secondary;btnAdminItemSecondary.visibility=if(row.secondary.isBlank())View.GONE else View.VISIBLE;btnAdminItemSecondary.setOnClickListener{row.other()};ivAdminItem.visibility=if(row.image>0)View.VISIBLE else View.GONE;if(row.image>0)ivAdminItem.setImageResource(when(row.image){1->R.drawable.fg_s1398_0;2->R.drawable.fg_s1398_1;else->R.drawable.fg_s1398_2})}}
}
private class AdminTrendView(context:android.content.Context,private val values:List<Int>):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas:Canvas){super.onDraw(canvas);if(values.isEmpty())return;val d=resources.displayMetrics.density;val top=12*d;val bottom=height-28*d;val step=width.toFloat()/values.size;val max=values.maxOrNull()?.coerceAtLeast(1)?:1;val path=Path()
        values.forEachIndexed{i,v->val x=step*(i+.5f);val y=bottom-(bottom-top)*v/max;paint.color=Color.rgb(198,250,233);canvas.drawRoundRect(x-step*.22f,y,x+step*.22f,bottom,4*d,4*d,paint);if(i==0)path.moveTo(x,y) else path.lineTo(x,y);paint.color=Color.rgb(83,100,91);paint.textSize=10*d;paint.textAlign=Paint.Align.CENTER;canvas.drawText(listOf("Sen","Sel","Rab","Kam","Jum","Sab","Min")[i%7],x,height-8*d,paint)}
        paint.color=Color.rgb(0,108,73);paint.style=Paint.Style.STROKE;paint.strokeWidth=2*d;canvas.drawPath(path,paint);paint.style=Paint.Style.FILL
    }
}
