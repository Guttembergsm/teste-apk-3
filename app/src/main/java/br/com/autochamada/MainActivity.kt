package br.com.autochamada

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.security.MessageDigest

class MainActivity : Activity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private lateinit var shownNumber: TextView
    private val requestCode = 501
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); prefs=getSharedPreferences("autochamada", MODE_PRIVATE); home() }
    private fun home() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; setPadding(dp(24),dp(30),dp(24),dp(24)); setBackgroundColor(Color.rgb(246,248,251)) }
        fun text(s:String,size:Float)=TextView(this).apply { text=s; textSize=size; gravity=Gravity.CENTER; setTextColor(Color.rgb(34,45,60)) }
        fun gap(h:Int) { root.addView(TextView(this), LinearLayout.LayoutParams(1,dp(h))) }
        fun btn(s:String,c:Int,click:()->Unit)=Button(this).apply { text=s; isAllCaps=false; textSize=17f; setTextColor(Color.WHITE); setBackgroundColor(c); setOnClickListener{click()} }
        root.addView(text("AutoChamada",30f)); gap(6); root.addView(text("Discagem automática pelo chip SIM",15f)); gap(25)
        shownNumber=text(prefs.getString("number", "Número não configurado").orEmpty(),18f)
        root.addView(shownNumber,LinearLayout.LayoutParams(-1,dp(48))); gap(14)
        root.addView(btn("▶  CHAMAR",Color.rgb(22,130,83)){ startCalling() },LinearLayout.LayoutParams(-1,dp(56))); gap(12)
        root.addView(btn("■  PARAR TENTATIVAS",Color.rgb(185,54,54)){ stopCalling() },LinearLayout.LayoutParams(-1,dp(56))); gap(16)
        status=text("Status: parado",15f); root.addView(status); gap(22)
        root.addView(btn("⚙  Configurações do administrador",Color.rgb(58,92,145)){ adminLogin() },LinearLayout.LayoutParams(-1,dp(52))); gap(18)
        root.addView(text("Após o fim de uma chamada, o app tenta novamente em 3 segundos. Parar interrompe novas tentativas, mas não encerra uma ligação ativa.",13f))
        setContentView(root)
    }
    private fun startCalling() {
        val number=prefs.getString("number", "").orEmpty().trim()
        if(number.isBlank()){ Toast.makeText(this,"Configure o número primeiro.",Toast.LENGTH_LONG).show(); adminLogin(); return }
        val needed= mutableListOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)
        if(Build.VERSION.SDK_INT>=33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing=needed.filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED}
        if(missing.isNotEmpty()){ requestPermissions(missing.toTypedArray(),requestCode); return }
        begin(number)
    }
    private fun begin(number:String) {
        val i=Intent(this,AutoCallService::class.java).apply { action=AutoCallService.START; putExtra(AutoCallService.NUMBER,number) }
        try { if(Build.VERSION.SDK_INT>=26) startForegroundService(i) else startService(i); status.text="Status: discagem automática ativa"; Toast.makeText(this,"Discagem automática iniciada.",Toast.LENGTH_SHORT).show() }
        catch(e:Exception){ Toast.makeText(this,"Não foi possível iniciar: ${e.localizedMessage ?: "erro do Android"}",Toast.LENGTH_LONG).show() }
    }
    private fun stopCalling(){ startService(Intent(this,AutoCallService::class.java).setAction(AutoCallService.STOP)); status.text="Status: parado"; Toast.makeText(this,"Novas tentativas interrompidas.",Toast.LENGTH_SHORT).show() }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray){ super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==this.requestCode){ if(grantResults.isNotEmpty() && grantResults.all{it==PackageManager.PERMISSION_GRANTED}) startCalling() else Toast.makeText(this,"Autorize as permissões de telefone.",Toast.LENGTH_LONG).show() } }
    private fun adminLogin(){ val input=EditText(this).apply{hint="Senha do administrador";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}; AlertDialog.Builder(this).setTitle("Acesso administrativo").setMessage("Senha inicial: 1234").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Entrar"){_,_-> val saved=prefs.getString("passhash",null); val valid=if(saved==null) input.text.toString()=="1234" else hash(input.text.toString())==saved; if(valid) adminScreen() else Toast.makeText(this,"Senha incorreta.",Toast.LENGTH_SHORT).show() }.show() }
    private fun adminScreen(){ val num=EditText(this).apply{hint="Número, ex.: +5511999999999";inputType=InputType.TYPE_CLASS_PHONE;setText(prefs.getString("number", "").orEmpty())}; val pass=EditText(this).apply{hint="Nova senha (opcional)";inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD}; val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(8),dp(20),0);addView(TextView(this@MainActivity).apply{text="Número para chamar"});addView(num);addView(TextView(this@MainActivity).apply{text="Alterar senha"});addView(pass)}; AlertDialog.Builder(this).setTitle("Configurações").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar"){_,_-> val n=num.text.toString().trim(); if(n.isBlank()){Toast.makeText(this,"Informe o número.",Toast.LENGTH_LONG).show();return@setPositiveButton}; val e=prefs.edit().putString("number",n); if(pass.text.isNotBlank()) e.putString("passhash",hash(pass.text.toString()));e.apply();shownNumber.text=n;Toast.makeText(this,"Configurações salvas.",Toast.LENGTH_SHORT).show()}.show() }
    private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
}
