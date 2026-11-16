package com.example.loginapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.loginapp.databinding.ActivityFoodCatalogBinding
import com.example.loginapp.databinding.ItemFoodCatalogBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FoodCatalogActivity:XmlActivity(){
    private lateinit var binding:ActivityFoodCatalogBinding
    private var filtering:Job?=null
    private val adapter=CatalogAdapter{showFood(it)}
    override fun onCreate(state:Bundle?){
        super.onCreate(state);if(loginRequired())return
        binding=ActivityFoodCatalogBinding.inflate(layoutInflater);setContentView(binding.root)
        binding.btnBack.setOnClickListener{finish()};binding.btnChat.setOnClickListener{open(AiChatActivity::class.java)}
        binding.rvFoods.layoutManager=LinearLayoutManager(this);binding.rvFoods.adapter=adapter
        lifecycleScope.launch{
            val categories=withContext(Dispatchers.IO){listOf("Semua kategori")+FoodCatalog.all(this@FoodCatalogActivity).map{it.category}.distinct().sorted()}
            binding.spCategory.adapter=ArrayAdapter(this@FoodCatalogActivity,android.R.layout.simple_spinner_dropdown_item,categories)
            binding.spCategory.onItemSelectedListener=object:AdapterView.OnItemSelectedListener{override fun onItemSelected(parent:AdapterView<*>?,view:View?,position:Int,id:Long)=filter();override fun onNothingSelected(parent:AdapterView<*>?)=Unit}
            binding.etCatalogSearch.afterChange{filter()};binding.cbReady.setOnCheckedChangeListener{_,_->filter()};filter()
        }
    }
    private fun filter(){
        filtering?.cancel();filtering=lifecycleScope.launch{
            delay(120);val query=binding.etCatalogSearch.text.toString();val category=binding.spCategory.selectedItem?.toString()?:"Semua kategori";val ready=binding.cbReady.isChecked
            val found=withContext(Dispatchers.Default){FoodCatalog.search(this@FoodCatalogActivity,query,category,ready)}
            adapter.submitList(found);binding.tvFoodCount.text="${found.size} / ${FoodCatalog.all(this@FoodCatalogActivity).size} makanan";binding.tvCatalogEmpty.visibility=if(found.isEmpty())View.VISIBLE else View.GONE
        }
    }
    private fun showFood(food:FoodEntry){
        val grams=EditText(this).apply{inputType=android.text.InputType.TYPE_CLASS_NUMBER;setText("100");hint="Berat makanan dalam gram";setSelectAllOnFocus(true)}
        val body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(32,12,32,8);addView(TextView(this@FoodCatalogActivity).apply{text="${food.category}\n\n${food.facts()}\n\nBasis: 100 g bagian yang dapat dimakan. Gula tidak sama dengan karbohidrat.\n\nSumber: ${food.source}"+(food.sourcePage?.let{" • PDF halaman $it"}?:"")+"\n\nBerat sajian (gram):";setTextIsSelectable(true)});addView(grams)}
        val scroll=ScrollView(this).apply{addView(body)}
        val dialog=AlertDialog.Builder(this).setTitle(food.name).setView(scroll).setNegativeButton("Tutup",null).setNeutralButton("Tanya asisten"){_,_->startActivity(Intent(this,AiChatActivity::class.java).putExtra("email",email).putExtra("food_id",food.id))}.setPositiveButton("Catat makanan",null).create()
        dialog.setOnShowListener{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{
            val weight=grams.text.toString().toIntOrNull();if(weight==null||weight !in 1..5000){grams.error="Isi berat 1–5.000 gram.";return@setOnClickListener}
            if(intent.getBooleanExtra("pick_food",false)){setResult(RESULT_OK,Intent().putExtra("food_id",food.id).putExtra("food_grams",weight));finish()}else startActivity(Intent(this,FormInputActivity::class.java).putExtra("email",email).putExtra("food_id",food.id).putExtra("food_grams",weight))
            dialog.dismiss()
        }};dialog.show()
    }
}
private class CatalogAdapter(private val selected:(FoodEntry)->Unit):ListAdapter<FoodEntry,CatalogAdapter.Holder>(object:DiffUtil.ItemCallback<FoodEntry>(){override fun areItemsTheSame(a:FoodEntry,b:FoodEntry)=a.id==b.id;override fun areContentsTheSame(a:FoodEntry,b:FoodEntry)=a==b}){
    class Holder(val b:ItemFoodCatalogBinding):RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(ItemFoodCatalogBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(holder:Holder,position:Int){val f=getItem(position);with(holder.b){tvFoodTitle.text=f.name;tvFoodCategory.text=f.category+(if(f.readyToEat)" • Siap santap" else " • Bahan pangan");tvFoodBadge.text=f.name.take(2).uppercase();tvFoodEnergy.text=f.calories?.let{"${it.toInt()} kkal / 100 g"}?:"Nilai gizi belum tersedia";tvFoodSource.text=if(f.source=="TKPI 2017")"TKPI 2017 • ${f.id}" else "Makanan populer • isi gizi sesuai label/racikan";root.setOnClickListener{selected(f)}}}
}
