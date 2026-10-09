package br.com.autochamada

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

class AutoCallService:Service(){
    companion object{const val START="br.com.autochamada.START";const val STOP="br.com.autochamada.STOP";const val NUMBER="number";private const val CHANNEL="autochamada_active";private const val ID=2451;private const val TAG="AutoChamada"}
    private lateinit var tm:TelephonyManager;private lateinit var telecom:TelecomManager
    private val handler=Handler(Looper.getMainLooper());private var number="";private var running=false;private var sawCall=false;private var callActive=false
    private val retry=Runnable{if(running && !callActive) placeCall()}
    private val modernListener=object:TelephonyCallback(),TelephonyCallback.CallStateListener{override fun onCallStateChanged(state:Int)=stateChanged(state)}
    @Suppress("DEPRECATION") private val legacyListener=object:PhoneStateListener(){override fun onCallStateChanged(state:Int,phoneNumber:String?)=stateChanged(state)}
    override fun onCreate(){super.onCreate();tm=getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager;telecom=getSystemService(Context.TELECOM_SERVICE) as TelecomManager;makeChannel()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action==STOP){running=false;handler.removeCallbacks(retry);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY}
        if(intent?.action==START){number=intent.getStringExtra(NUMBER)?.trim().orEmpty();if(number.isBlank()){stopSelf();return START_NOT_STICKY};running=true;sawCall=false;callActive=false;handler.removeCallbacks(retry);startForeground(ID,notification("Discagem automática ativa"));registerListener();placeCall()}
        return START_NOT_STICKY
    }
    private fun registerListener(){if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){stopSession("Permissão de telefone ausente");return};try{if(Build.VERSION.SDK_INT>=31)tm.registerTelephonyCallback(mainExecutor,modernListener) else { @Suppress("DEPRECATION") tm.listen(legacyListener,PhoneStateListener.LISTEN_CALL_STATE)}}catch(e:Exception){Log.e(TAG,"Falha ao monitorar estado da chamada",e)}}
    private fun stateChanged(state:Int){if(!running)return;when(state){TelephonyManager.CALL_STATE_RINGING,TelephonyManager.CALL_STATE_OFFHOOK->{sawCall=true;callActive=true;handler.removeCallbacks(retry);notify("Chamada em andamento")};TelephonyManager.CALL_STATE_IDLE->{if(sawCall){sawCall=false;callActive=false;handler.removeCallbacks(retry);notify("Nova tentativa em 3 segundos");handler.postDelayed(retry,3000)}}}}
    private fun placeCall(){if(!running||number.isBlank())return;if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){stopSession("Permissão para ligar ausente");return};try{notify("Iniciando chamada");telecom.placeCall(Uri.fromParts("tel",number,null),null)}catch(e:Exception){Log.e(TAG,"Falha ao iniciar chamada",e);notify("Falha ao ligar; nova tentativa em 3 segundos");handler.removeCallbacks(retry);handler.postDelayed(retry,3000)}}
    private fun stopSession(msg:String){running=false;handler.removeCallbacks(retry);stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    private fun makeChannel(){if(Build.VERSION.SDK_INT>=26){val c=NotificationChannel(CHANNEL,"AutoChamada em execução",NotificationManager.IMPORTANCE_LOW);c.description="Status da rediscagem automática";getSystemService(NotificationManager::class.java).createNotificationChannel(c)}}
    private fun notification(msg:String):Notification{val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val stop=PendingIntent.getService(this,1,Intent(this,AutoCallService::class.java).setAction(STOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val b=if(Build.VERSION.SDK_INT>=26)Notification.Builder(this,CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this);return b.setSmallIcon(android.R.drawable.sym_action_call).setContentTitle("AutoChamada").setContentText(msg).setContentIntent(open).setOngoing(running).addAction(Notification.Action.Builder(null,"Parar",stop).build()).build()}
    private fun notify(msg:String){getSystemService(NotificationManager::class.java).notify(ID,notification(msg))}
    @Suppress("DEPRECATION") override fun onDestroy(){running=false;handler.removeCallbacks(retry);try{if(Build.VERSION.SDK_INT>=31)tm.unregisterTelephonyCallback(modernListener) else tm.listen(legacyListener,PhoneStateListener.LISTEN_NONE)}catch(_:Exception){};super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
}
