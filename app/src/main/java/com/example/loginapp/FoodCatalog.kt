package com.example.loginapp

import android.content.Context
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

data class FoodEntry(val id:String,val name:String,val category:String,val calories:Double?,val protein:Double?,val fat:Double?,val carbs:Double?,val fiber:Double?,val sodium:Double?,val sugar:Double?,val source:String,val sourcePage:Int?,val aliases:List<String>,val popular:Boolean,val readyToEat:Boolean){
    val searchable=normalizeFood(name+" "+aliases.joinToString(" "))
    fun scaled(value:Double?,grams:Int):Int?=value?.times(grams/100.0)?.roundToInt()
    fun facts(grams:Int=100):String = listOf("Energi" to (scaled(calories,grams)?.let{"$it kkal"}?:"belum tersedia"),"Protein" to amount(protein,grams,"g"),"Lemak" to amount(fat,grams,"g"),"Karbohidrat" to amount(carbs,grams,"g"),"Serat" to amount(fiber,grams,"g"),"Natrium" to (scaled(sodium,grams)?.let{"$it mg"}?:"belum tersedia"),"Gula" to (scaled(sugar,grams)?.let{"$it g"}?:"belum tersedia")).joinToString("\n"){"${it.first}: ${it.second}"}
    private fun amount(value:Double?,grams:Int,unit:String)=value?.let{String.format(Locale.ROOT,"%.1f %s",it*grams/100.0,unit)}?:"belum tersedia"
}
fun normalizeFood(text:String):String=Normalizer.normalize(text,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+")," ").trim()
object FoodCatalog {
    @Volatile private var entries:List<FoodEntry>?=null
    fun all(context:Context):List<FoodEntry> = entries ?: synchronized(this){entries ?: load(context).also{entries=it}}
    private fun load(context:Context):List<FoodEntry>{
        val root=context.assets.open("indonesia_foods.json").bufferedReader().use{JSONObject(it.readText())}.getJSONArray("foods")
        fun JSONObject.number(key:String)=if(isNull(key))null else optDouble(key).takeIf{it.isFinite()}
        return (0 until root.length()).map{i->val j=root.getJSONObject(i);val aliases=j.getJSONArray("aliases");FoodEntry(j.getString("id"),j.getString("name"),j.getString("category"),j.number("calories"),j.number("protein"),j.number("fat"),j.number("carbs"),j.number("fiber"),j.number("sodium"),j.number("sugar"),j.getString("source"),if(j.isNull("sourcePage"))null else j.getInt("sourcePage"),(0 until aliases.length()).map{aliases.getString(it)},j.optBoolean("popular"),j.optBoolean("readyToEat"))}
    }
    fun find(context:Context,id:String?)=all(context).firstOrNull{it.id==id}
    fun search(context:Context,query:String,category:String="Semua kategori",readyOnly:Boolean=false):List<FoodEntry>{
        val q=normalizeFood(query).replace(Regex("\\bmie\\b"),"mi");val terms=q.split(" ").filter{it.isNotBlank()}
        return all(context).filter{(category=="Semua kategori"||it.category==category)&&(!readyOnly||it.readyToEat)&&terms.all{token->it.searchable.replace(Regex("\\bmie\\b"),"mi").contains(token)}}.sortedWith(compareByDescending<FoodEntry>{normalizeFood(it.name)==q||it.aliases.any{a->normalizeFood(a)==q}}.thenByDescending{it.popular}.thenBy{it.name.lowercase(Locale.ROOT)})
    }
    fun contextMatches(context:Context,question:String):List<FoodEntry>{
        val q=normalizeFood(question)
        val direct=all(context).filter{normalizeFood(it.name).length>3&&(q.contains(normalizeFood(it.name))||it.aliases.any{a->a.length>3&&q.contains(normalizeFood(a))})}.sortedByDescending{normalizeFood(it.name).length}
        if(direct.isNotEmpty())return direct.take(8)
        val stop=setOf("berapa","kalori","gula","natrium","makan","makanan","yang","dan","atau","untuk","saya","aku","tolong","bantu","cari","tentang","kandungan","gizi","apa","bagaimana","dengan","dong","gram","porsi","bandingkan","dari","boleh")
        val tokens=q.split(' ').filter{it.length>2&&it !in stop&&!it.all(Char::isDigit)}
        return all(context).map{food->food to tokens.count{food.searchable.contains(it)}}.filter{it.second>0}.sortedWith(compareByDescending<Pair<FoodEntry,Int>>{it.second}.thenByDescending{it.first.popular}).take(8).map{it.first}
    }
}
