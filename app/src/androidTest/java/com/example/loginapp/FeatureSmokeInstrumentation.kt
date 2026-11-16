package com.example.loginapp

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Build
import org.json.JSONObject
import java.security.MessageDigest
import java.security.KeyStore

/** Device-side tests for real catalog parsing, missing values, migration, and Gemini JSON. No live API key is used. */
class FeatureSmokeInstrumentation:Instrumentation(){
    override fun onCreate(arguments:Bundle?){super.onCreate(arguments);start()}
    override fun onStart(){
        val result=Bundle();var checks=0
        fun verify(ok:Boolean,name:String){check(ok){name};checks++}
        val email="smoke@nutricare.invalid"
        val context=object:ContextWrapper(targetContext){override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("smoke_"+name,mode)}
        val testScope=MessageDigest.getInstance("SHA-256").digest(email.toByteArray()).joinToString(""){"%02x".format(it)}
        listOf("nutricare_local_v2","ai_$testScope","nutricare_chat","nutricare_admin_demo_v1").forEach{context.getSharedPreferences(it,Context.MODE_PRIVATE).edit().clear().commit()}
        try{
            val all=FoodCatalog.all(context)
            verify(all.size==1579,"Catalog entry count")
            verify(all.map{it.id}.distinct().size==all.size,"Unique IDs")
            verify(all.map{it.category}.distinct().size==14,"Categories")
            verify(all.count{it.source=="TKPI 2017"}==1146,"TKPI entries")
            val rice=FoodCatalog.find(context,"AP001")!!
            verify(rice.calories==180.0&&rice.sodium==1.0,"Verified rice TKPI values")
            verify(rice.sugar==null,"Missing sugar stays null")
            verify(rice.scaled(rice.calories,200)==360,"Per 100 g scaling")
            verify(FoodCatalog.search(context,"nasi putih").first().id=="AP001","Rice alias")
            verify(FoodCatalog.search(context,"mie ayam").isNotEmpty(),"Mi/mie normalization")
            verify(FoodCatalog.search(context,"seblak").size>=3,"Popular snacks")
            verify(FoodCatalog.search(context,"boba").isNotEmpty(),"Popular drinks")
            verify(FoodCatalog.search(context,"",readyOnly=true).all{it.readyToEat},"Ready-to-eat filter")
            verify(FoodCatalog.search(context,"zz-no-match-991").isEmpty(),"Empty result")
            verify(CatalogAssistant.reply(context,"kalori nasi putih 200 gram").contains("360 kkal"),"Offline grounded calculation")
            verify(CatalogAssistant.reply(context,"rendah gula").contains("tidak bisa"),"No carbohydrate-to-sugar substitution")
            val store=LocalStore(context)
            verify(store.register("Smoke",email,"Password123","Umum / Sehat"),"Registration")
            verify(store.login(email,"Password123")&&!store.login(email,"wrong"),"Password authentication")
            val meal=Meal(name="Nasi",calories=360,sugar=0,sodium=2,sugarKnown=false,foodId="AP001",grams=200,nutritionSource="TKPI 2017")
            store.save(email,listOf(meal));val restored=LocalStore(context).meals(email).single()
            verify(!restored.sugarKnown&&restored.sugarLabel()=="belum tersedia","Unknown is not zero")
            verify(restored.grams==200&&restored.foodId=="AP001","Catalog provenance persists")
            verify(listOf(restored).nutritionIncomplete(),"Incomplete totals status")
            context.getSharedPreferences("nutricare_local_v2",Context.MODE_PRIVATE).edit().putString("meals_legacy","[{\"id\":\"old\",\"name\":\"Legacy\",\"time\":\"Siang\",\"portions\":1,\"calories\":123,\"sugar\":4,\"sodium\":55}]").commit()
            verify(store.meals("legacy").single().sugarKnown,"Backward compatible nutrition flags")
            val request=GeminiClient.requestBody(listOf(ChatMessage("user","Hai"),ChatMessage("assistant","Halo"),ChatMessage("user","Nasi")),"Rules")
            verify(request.getJSONArray("contents").getJSONObject(1).getString("role")=="model","Gemini chat roles")
            verify(request.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")=="Rules","Gemini system instruction")
            val response="{\"candidates\":[{\"content\":{\"parts\":[{\"thought\":true,\"text\":\"hidden\"},{\"text\":\"Jawaban\"}]}}]}"
            verify(GeminiClient.parseResponse(response)=="Jawaban","Ignore thought blocks")
            verify(runCatching{GeminiClient.parseResponse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}")}.isFailure,"Blocked response is explicit")
            verify(runCatching{GeminiClient.ask("dummy","../bad",emptyList(),"")}.isFailure,"Model path validation before network")
            val config=AiSettings(context,email);val dummy="TEST_ONLY_NOT_A_REAL_API_KEY_1234567890"
            config.saveKey(dummy,Build.VERSION.SDK_INT>=23);config.clearMemory()
            if(Build.VERSION.SDK_INT>=23){verify(config.key()==dummy,"Android Keystore encrypt/decrypt");val scope=MessageDigest.getInstance("SHA-256").digest(email.toByteArray()).joinToString(""){"%02x".format(it)};verify(context.getSharedPreferences("ai_$scope",Context.MODE_PRIVATE).all.values.none{it.toString().contains(dummy)},"No plaintext API key in preferences")}
            config.forget();verify(config.key().isEmpty()&&!config.online,"Remove API credentials")
            val history=ChatHistory(context,email);history.save(listOf(ChatMessage("user","Test")));verify(history.read().single().text=="Test","Per-account chat persistence")
            verify(ChatHistory(context,"other@invalid").read().isEmpty(),"Chat account isolation")
            verify(CatalogAssistant.reply(context,"Coba berikan rekomendasi makanan rendah kalori").contains("kkal / 100 g"),"Offline low calorie recommendations")
            verify(CatalogAssistant.reply(context,"Makanan 200gr","kalori nasi putih").contains("360 kkal"),"Follow-up gram context")
            verify(CatalogAssistant.reply(context,"Makanan 200gr").contains("Makanan apa"),"Missing food asks for a name")
            val modelList=JSONObject().put("models",org.json.JSONArray()
                .put(JSONObject().put("name","models/gemini-8-flash-image").put("supportedGenerationMethods",org.json.JSONArray().put("generateContent")))
                .put(JSONObject().put("name","models/gemini-8-flash").put("supportedGenerationMethods",org.json.JSONArray().put("generateContent")))
                .put(JSONObject().put("name","models/gemini-8-pro").put("supportedGenerationMethods",org.json.JSONArray().put("generateContent")))
                .put(JSONObject().put("name","models/gemini-embedding").put("supportedGenerationMethods",org.json.JSONArray().put("embedContent")))).toString()
            verify(GeminiClient.textModels(modelList)==listOf("gemini-8-flash","gemini-8-pro"),"Automatic selection excludes non-chat models")
            val calls=mutableListOf<String>();var selected=""
            val fake=GeminiTransport{method,path,_,_->calls+="$method $path";when{
                path.startsWith("models?")->GeminiHttpResult(200,modelList)
                path.contains("retired")->GeminiHttpResult(404,"{}")
                else->GeminiHttpResult(200,response)
            }}
            verify(GeminiClient.ask("test","retired",listOf(ChatMessage("user","Hai")),"Rules",fake,{selected=it})=="Jawaban"&&selected=="gemini-8-flash","404 recovers using available text model")
            verify(calls.size==3&&calls[1].startsWith("GET models?"),"Recovery actually lists models before retry")
            calls.clear();GeminiClient.ask("test","auto",listOf(ChatMessage("user","Hai")),"Rules",fake)
            verify(calls.size==2&&calls[0].startsWith("GET"),"Auto selection does not guess model IDs")
            var requests=0
            val quota=GeminiTransport{_,_,_,_->requests++;GeminiHttpResult(429,"{}")}
            verify(runCatching{GeminiClient.ask("test","some-model",listOf(ChatMessage("user","Hi")),"Rules",quota)}.exceptionOrNull() is GeminiApiException&&requests==1,"Quota failures are not retried")
            requests=0
            val invalid=GeminiTransport{_,_,_,_->requests++;GeminiHttpResult(400,"{\"error\":{\"details\":[{\"reason\":\"API_KEY_INVALID\"}]}}")}
            verify(runCatching{GeminiClient.ask("test","auto",listOf(ChatMessage("user","Hi")),"Rules",invalid)}.exceptionOrNull()?.message?.contains("Kunci Gemini tidak valid")==true&&requests==1,"Invalid key receives specific message")
            config.model="gemini-3.8-flash";verify(config.model=="auto","Old default migrates automatically")
            val collapsed=GeminiClient.requestBody(listOf(ChatMessage("user","A"),ChatMessage("system","Error","error"),ChatMessage("user","B")),"Rules").getJSONArray("contents")
            verify(collapsed.length()==1&&collapsed.getJSONObject(0).getJSONArray("parts").length()==2,"Retry history merges consecutive user turns")
            val logo=android.graphics.BitmapFactory.decodeResource(targetContext.resources,R.drawable.nutricare_logo)
            verify(logo!=null&&logo.width==1254,"Provided logo bundled")
            val admin=DemoAdminRepository(context)
            verify(runCatching{admin.snapshot()}.isFailure,"Admin dashboard rejects unauthenticated access")
            val requestAdmin=admin.register("Admin Tester"," NEWADMIN@demo.invalid ","AdminPass123")
            verify(requestAdmin.email=="newadmin@demo.invalid"&&requestAdmin.status=="active"&&requestAdmin.role=="admin","Admin registration is normalized and immediately active")
            verify(admin.login(requestAdmin.email,"AdminPass123",false).email==requestAdmin.email,"New admin can immediately log in")
            verify(runCatching{admin.register("Duplicate","newadmin@demo.invalid","AdminPass123")}.isFailure,"Duplicate admin rejected")
            verify(runCatching{admin.login(DemoAdminRepository.DEMO_EMAIL,"wrong-password",false)}.isFailure,"Wrong admin password rejected")
            val owner=admin.login(DemoAdminRepository.DEMO_EMAIL,DemoAdminRepository.DEMO_PASSWORD,true)
            verify(owner.role=="owner"&&admin.snapshot().people.size==6,"Demo owner can monitor seeded data")
            verify(DemoAdminRepository(context).current()?.email==owner.email,"Remembered admin session restored")
            verify(runCatching{admin.decideAdmin(requestAdmin.email,true)}.isFailure,"Active admin cannot be approved again")
            verify(admin.snapshot().requests.first{it.email==requestAdmin.email}.status=="active","Registered admin remains active")
            admin.logout();admin.login(requestAdmin.email,"AdminPass123",false)
            admin.register("Second","second@demo.invalid","Password123")
            verify(admin.current()?.email==requestAdmin.email,"Registering colleague preserves current admin session")
            verify(runCatching{admin.decideAdmin("second@demo.invalid",true)}.isFailure,"Ordinary admin cannot approve roles")
            verify(DemoAdminRepository(context).current()==null,"Nonremembered admin session stays in memory")
            val sample=AdminCondition("test-condition","Test","General","Demo")
            admin.saveCondition(sample);verify(admin.snapshot().conditions.any{it.id==sample.id},"Create condition")
            admin.saveCondition(sample.copy(notes="Updated"));verify(admin.snapshot().conditions.single{it.id==sample.id}.notes=="Updated","Update condition")
            admin.deleteRecord("conditions",sample.id);verify(admin.snapshot().conditions.none{it.id==sample.id},"Delete condition")
            admin.saveFood(AdminFood("test-food","Test food","Test",null,null,"Manual"))
            verify(admin.snapshot().foods.single{it.id=="test-food"}.calories==null,"Admin unknown nutrition remains null")
            verify(runCatching{admin.saveFood(AdminFood("invalid-food","Invalid","Test",Double.NaN,null,"Manual"))}.isFailure,"Invalid nutrition rejected")
            admin.setPersonActive("NC-1001",false);verify(!admin.snapshot().people.first{it.id=="NC-1001"}.active,"User demo status changes")
            val recipe=AdminRecipe("test-recipe","Test recipe","General","Demo","Rice","Cook","Menunggu review")
            admin.saveRecipe(recipe);admin.saveRecipe(recipe.copy(status="Terbit"));verify(admin.snapshot().recipes.single{it.id==recipe.id}.status=="Terbit","Recipe review publishes demo content")
            admin.notifyUsers("Demo only");verify(admin.snapshot().events.first().title=="Simulasi notifikasi","Notifications are explicitly simulated")
            verify(LocalStore(context).meals(email).single().id==meal.id,"Admin mutations preserve user journals")
            admin.logout();verify(runCatching{admin.saveCondition(sample)}.isFailure,"Logout revokes admin mutation access")
            result.putString("result","PASS $checks feature checks; no live Gemini request performed");finish(Activity.RESULT_OK,result)
        }catch(e:Throwable){result.putString("result","FAIL after $checks checks: ${e.message}");finish(Activity.RESULT_CANCELED,result)}finally{
            val scope=MessageDigest.getInstance("SHA-256").digest(email.toByteArray()).joinToString(""){"%02x".format(it)}
            listOf("nutricare_local_v2","ai_$scope","nutricare_chat","nutricare_admin_demo_v1").forEach{context.getSharedPreferences(it,Context.MODE_PRIVATE).edit().clear().commit()}
            runCatching{KeyStore.getInstance("AndroidKeyStore").apply{load(null);deleteEntry("nutricare_ai_$scope")}}
        }
    }
}


