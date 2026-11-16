package com.example.loginapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.loginapp.databinding.ActivityAdminLoginBinding
import com.example.loginapp.databinding.ActivityAdminRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AdminBaseActivity:ComponentActivity(){
    protected val repo:AdminRepository by lazy{AdminServices.repository(this)}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.WHITE;window.navigationBarColor=android.graphics.Color.WHITE;WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true}
    protected fun enterPanel(){startActivity(Intent(this,AdminPanelActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish()}
}
class AdminLoginActivity:AdminBaseActivity(){
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        if(repo.current()!=null){enterPanel();return}
        val b=ActivityAdminLoginBinding.inflate(layoutInflater);setContentView(b.root)
        b.btnAdminBackToUser.setOnClickListener{finish()}
        b.btnAdminRegisterLink.setOnClickListener{startActivity(Intent(this,AdminRegisterActivity::class.java))}
        b.btnAdminFillDemo.setOnClickListener{b.etAdminEmail.setText(DemoAdminRepository.DEMO_EMAIL);b.etAdminPassword.setText(DemoAdminRepository.DEMO_PASSWORD)}
        b.cbAdminShowPassword.setOnCheckedChangeListener{_,on->b.etAdminPassword.transformationMethod=if(on)null else PasswordTransformationMethod.getInstance();b.etAdminPassword.setSelection(b.etAdminPassword.length())}
        b.btnAdminSignIn.setOnClickListener{
            val address=b.etAdminEmail.text.toString();val password=b.etAdminPassword.text.toString();val remember=b.cbAdminRemember.isChecked
            if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address.trim()).matches()){b.etAdminEmail.error="Masukkan email admin yang valid.";return@setOnClickListener}
            if(password.length<8){b.etAdminPassword.error="Kata sandi minimal 8 karakter.";return@setOnClickListener}
            b.btnAdminSignIn.isEnabled=false;b.tvAdminAuthError.visibility=View.GONE
            lifecycleScope.launch{
                try{withContext(Dispatchers.IO){repo.login(address,password,remember)};b.etAdminPassword.text.clear();enterPanel()}
                catch(e:kotlinx.coroutines.CancellationException){throw e}
                catch(e:Exception){b.tvAdminAuthError.text=e.message?:"Gagal masuk. Coba lagi.";b.tvAdminAuthError.visibility=View.VISIBLE}
                finally{b.btnAdminSignIn.isEnabled=true}
            }
        }
    }
}
class AdminRegisterActivity:AdminBaseActivity(){
    override fun onCreate(state:Bundle?){
        super.onCreate(state);val b=ActivityAdminRegisterBinding.inflate(layoutInflater);setContentView(b.root)
        b.btnAdminBackToLogin.setOnClickListener{finish()}
        b.btnAdminSubmitRegistration.setOnClickListener{
            val name=b.etAdminName.text.toString().trim();val address=b.etAdminEmail.text.toString().trim();val password=b.etAdminPassword.text.toString()
            val error=when{ name.length<2->"Nama lengkap minimal 2 karakter.";!android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()->"Email belum valid.";password.length<8->"Kata sandi minimal 8 karakter.";password!=b.etAdminConfirm.text.toString()->"Konfirmasi kata sandi berbeda.";!b.cbAdminAgreement.isChecked->"Centang pernyataan akses admin terlebih dahulu.";else->"" }
            if(error.isNotEmpty()){b.tvAdminAuthError.text=error;b.tvAdminAuthError.visibility=View.VISIBLE;return@setOnClickListener}
            b.btnAdminSubmitRegistration.isEnabled=false;b.tvAdminAuthError.visibility=View.GONE
            lifecycleScope.launch{
                try{
                    val alreadySignedIn=repo.current()!=null
                    withContext(Dispatchers.IO){repo.register(name,address,password);if(!alreadySignedIn)repo.login(address,password,false)}
                    b.etAdminPassword.text.clear();b.etAdminConfirm.text.clear()
                    if(alreadySignedIn){Toast.makeText(this@AdminRegisterActivity,"Admin baru terdaftar dan bisa langsung masuk.",Toast.LENGTH_LONG).show();finish()}else{enterPanel()}
                }catch(e:kotlinx.coroutines.CancellationException){throw e}
                catch(e:Exception){b.tvAdminAuthError.text=e.message?:"Pendaftaran belum berhasil.";b.tvAdminAuthError.visibility=View.VISIBLE}
                finally{b.btnAdminSubmitRegistration.isEnabled=true}
            }
        }
    }
}
