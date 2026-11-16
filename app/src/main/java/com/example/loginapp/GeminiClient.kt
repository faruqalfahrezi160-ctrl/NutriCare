package com.example.loginapp

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

data class ChatMessage(val role:String,val text:String,val origin:String="local",val time:Long=System.currentTimeMillis())
class AiSettings(context:Context,email:String){
    private val scope=MessageDigest.getInstance("SHA-256").digest(email.toByteArray()).joinToString(""){"%02x".format(it)}
    private val prefs=context.getSharedPreferences("ai_$scope",Context.MODE_PRIVATE)
    private val alias="nutricare_ai_$scope"
    var online:Boolean get()=prefs.getBoolean("online",false);set(v){prefs.edit().putBoolean("online",v).apply()}
    var model:String get()=prefs.getString("model","auto").orEmpty().let{if(it=="gemini-3.8-flash"||it.isBlank())"auto" else it};set(v){prefs.edit().putString("model",v).apply()}
    var resolvedModel:String get()=prefs.getString("resolved_model","").orEmpty();set(v){prefs.edit().putString("resolved_model",v).apply()}
    fun key():String{
        memory[scope]?.let{return it}
        if(Build.VERSION.SDK_INT<23)return ""
        return runCatching{
            val encrypted=prefs.getString("key",null)?:return@runCatching ""
            val iv=Base64.decode(prefs.getString("iv","")!!,Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,secret(),GCMParameterSpec(128,iv))
            String(cipher.doFinal(Base64.decode(encrypted,Base64.NO_WRAP)),Charsets.UTF_8).also{memory[scope]=it}
        }.getOrDefault("")
    }
    fun saveKey(value:String,persist:Boolean){
        require(value.length in 20..512&&!value.any{it.isWhitespace()||it.isISOControl()}){"Format kunci API belum valid."}
        if(persist&&Build.VERSION.SDK_INT>=23){
            val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,secret())
            val encrypted=cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            prefs.edit().putString("key",Base64.encodeToString(encrypted,Base64.NO_WRAP)).putString("iv",Base64.encodeToString(cipher.iv,Base64.NO_WRAP)).apply()
        }else prefs.edit().remove("key").remove("iv").apply()
        memory[scope]=value
    }
    fun clearMemory(){memory.remove(scope)}
    fun forget(){memory.remove(scope);prefs.edit().remove("key").remove("iv").putBoolean("online",false).apply()}
    @RequiresApi(23)
    private fun secret():SecretKey{
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (store.getKey(alias,null) as? SecretKey)?.let{return it}
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()
    }
    companion object{private val memory=ConcurrentHashMap<String,String>()}
}

class ChatHistory(context:Context,email:String){
    private val prefs=context.getSharedPreferences("nutricare_chat",Context.MODE_PRIVATE)
    private val key="chat_"+MessageDigest.getInstance("SHA-256").digest(email.toByteArray()).joinToString(""){"%02x".format(it)}
    fun read():List<ChatMessage> = runCatching{val a=JSONArray(prefs.getString(key,"[]"));(0 until a.length()).map{val j=a.getJSONObject(it);ChatMessage(j.getString("role"),j.getString("text"),j.optString("origin","local"),j.optLong("time"))}}.getOrDefault(emptyList())
    fun save(messages:List<ChatMessage>){val a=JSONArray();messages.takeLast(60).forEach{a.put(JSONObject().put("role",it.role).put("text",it.text).put("origin",it.origin).put("time",it.time))};prefs.edit().putString(key,a.toString()).apply()}
}

/** Injectable transport keeps recovery tests independent of a real API key. */
fun interface GeminiTransport {
    fun call(method:String,path:String,key:String,body:JSONObject?):GeminiHttpResult
}
data class GeminiHttpResult(val status:Int,val body:String)
class GeminiApiException(val status:Int,val reason:String):IllegalStateException(when {
    reason=="API_KEY_INVALID"->"Kunci Gemini tidak valid. Ganti kunci melalui Pengaturan."
    status==401||status==403->"Kunci atau izin Gemini ditolak. Periksa proyek dan pembatasan kunci di Google AI Studio."
    status==429->"Kuota Gemini habis atau batas permintaan tercapai. Periksa kuota akun atau coba lagi nanti."
    status==404->"Model Gemini belum tersedia untuk kunci ini. Pilih Otomatis di Pengaturan."
    status==400->"Permintaan Gemini ditolak. Periksa kunci, wilayah, dan konfigurasi akun di Google AI Studio."
    else->"Layanan Gemini bermasalah (HTTP $status). Coba lagi nanti."
})
object GeminiClient {
    private val modelPattern=Regex("[A-Za-z0-9._-]{1,100}")
    private val network=GeminiTransport { method,path,key,body ->
        val connection=URL("https://generativelanguage.googleapis.com/v1beta/$path").openConnection() as HttpsURLConnection
        try {
            connection.requestMethod=method;connection.connectTimeout=15000;connection.readTimeout=45000
            connection.setRequestProperty("x-goog-api-key",key)
            if(body!=null){
                connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json; charset=UTF-8")
                connection.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
            }
            val status=connection.responseCode
            val stream=if(status in 200..299)connection.inputStream else connection.errorStream
            GeminiHttpResult(status,stream?.bufferedReader(Charsets.UTF_8)?.use{it.readText()}.orEmpty())
        }finally{connection.disconnect()}
    }
    private fun checked(result:GeminiHttpResult):String {
        if(result.status in 200..299)return result.body
        // Extract only typed reasons. Never display/log provider bodies containing credentials.
        val error=runCatching{JSONObject(result.body).optJSONObject("error")}.getOrNull()
        val details=error?.optJSONArray("details")
        val reason=if(details==null)"" else (0 until details.length()).mapNotNull{details.optJSONObject(it)?.optString("reason")}.firstOrNull{it.isNotEmpty()}.orEmpty()
        throw GeminiApiException(result.status,reason)
    }
    fun requestBody(messages:List<ChatMessage>,instruction:String):JSONObject {
        val content=JSONArray()
        messages.filter{(it.role=="user"||it.role=="assistant")&&it.origin!="error"}.takeLast(20).dropWhile{it.role!="user"}.forEach{m->
            val role=if(m.role=="assistant")"model" else "user"
            val previous=content.optJSONObject(content.length()-1)
            if(previous?.optString("role")==role)previous.getJSONArray("parts").put(JSONObject().put("text",m.text))
            else content.put(JSONObject().put("role",role).put("parts",JSONArray().put(JSONObject().put("text",m.text))))
        }
        return JSONObject().put("contents",content).put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",instruction))))
            .put("generationConfig",JSONObject().put("maxOutputTokens",4096))
    }
    fun parseResponse(raw:String):String {
        val root=JSONObject(raw);val candidate=root.optJSONArray("candidates")?.optJSONObject(0)
        val parts=candidate?.optJSONObject("content")?.optJSONArray("parts")
        val text=if(parts==null)"" else (0 until parts.length()).mapNotNull{val p=parts.optJSONObject(it);if(p?.optBoolean("thought")==true)null else p?.optString("text")?.takeIf{it.isNotBlank()}}.joinToString("\n")
        if(text.isBlank())throw IllegalStateException("Gemini tidak memberikan jawaban teks. Coba ubah pertanyaan.")
        return text
    }
    fun textModels(raw:String):List<String> {
        val items=JSONObject(raw).optJSONArray("models")?:return emptyList()
        return (0 until items.length()).mapNotNull { index ->
            val m=items.optJSONObject(index)?:return@mapNotNull null
            val name=m.optString("name").removePrefix("models/")
            val methods=m.optJSONArray("supportedGenerationMethods")?:return@mapNotNull null
            val supportsText=(0 until methods.length()).any{methods.optString(it)=="generateContent"}
            val excluded=listOf("image","tts","audio","live","robotics","embedding","computer-use","deep-research","custom")
            name.takeIf{supportsText&&it.startsWith("gemini-")&&modelPattern.matches(it)&&excluded.none{part->it.contains(part)}}
        }.distinct().sortedWith(compareBy<String> {if(it.contains("flash"))0 else 1}.thenBy {if(it.contains("preview")||it.contains("exp"))1 else 0}.thenByDescending{Regex("\\d+(?:\\.\\d+)?").find(it)?.value?.toDoubleOrNull()?:0.0}.thenBy{it})
    }
    fun availableModels(key:String,transport:GeminiTransport=network):List<String> {
        val models=mutableListOf<String>();var token="";val visited=mutableSetOf<String>()
        do {
            val path="models?pageSize=1000"+if(token.isEmpty())"" else "&pageToken="+java.net.URLEncoder.encode(token,"UTF-8")
            val raw=checked(transport.call("GET",path,key,null));models+=textModels(raw)
            token=JSONObject(raw).optString("nextPageToken")
        }while(token.isNotEmpty()&&visited.add(token)&&visited.size<10)
        return models.distinct()
    }
    fun ask(key:String,model:String,messages:List<ChatMessage>,instruction:String,transport:GeminiTransport=network,onModel:(String)->Unit={}):String {
        require(model=="auto"||modelPattern.matches(model)){"Nama model tidak valid."}
        val body=requestBody(messages,instruction)
        fun generate(name:String):String {
            val response=checked(transport.call("POST","models/$name:generateContent",key,body))
            return parseResponse(response).also{onModel(name)}
        }
        var unavailable:GeminiApiException?=null
        if(model!="auto")try{return generate(model)}catch(e:GeminiApiException){
            if(e.status!=404)throw e
            unavailable=e
        }
        val models=availableModels(key,transport).filter{it!=model}
        if(models.isEmpty())throw IllegalStateException("Tidak ada model chat Gemini untuk kunci ini. Periksa akses API di Google AI Studio.")
        // Only model-not-found is safe to retry on another model; never repeat quota/auth failures.
        for(name in models.take(3))try{return generate(name)}catch(e:GeminiApiException){if(e.status!=404)throw e;unavailable=e}
        throw unavailable?:IllegalStateException("Model chat Gemini belum tersedia.")
    }
}

