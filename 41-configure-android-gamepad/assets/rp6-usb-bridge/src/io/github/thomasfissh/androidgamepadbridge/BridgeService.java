package io.github.thomasfissh.androidgamepadbridge;
import android.app.*;import android.os.*;import android.content.*;import android.hardware.usb.*;import android.util.Log;
import java.io.*;import java.nio.*;import java.util.*;import java.util.concurrent.TimeoutException;import org.json.*;
public class BridgeService extends Service {
 static volatile long diagnosticUntil=0, testRumbleUntil=0;
 static volatile BridgeService activeService;
 static boolean isRunning(){BridgeService s=activeService;return s!=null&&s.running;}
 static volatile boolean compatActive=false; boolean compatMode=false;
 volatile long effectUntil=0;
 UsbEndpoint rumbleEndpoint;int lastRumble=-1;
 static volatile String status="尚未开始。请点击下方按钮。";
 volatile boolean running=false;UsbDeviceConnection connection;java.lang.Process virtualProcess;Thread worker;long packets=0,forwarded=0;int seen=0;long lastSummary=0;String failure=null;
 void note(String s){status=s;Log.i("GamepadBridge",s);try(FileWriter w=new FileWriter(new File(getFilesDir(),"probe.log"),true)){w.write(System.currentTimeMillis()+" "+s+"\n");}catch(Exception ignored){}}
 public IBinder onBind(Intent i){return null;}
 public void onCreate(){super.onCreate();activeService=this;NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("probe","安卓手柄桥接",NotificationManager.IMPORTANCE_LOW));startForeground(71,new Notification.Builder(this,"probe").setContentTitle("安卓手柄桥接运行中").setContentText("原装 2.4G 接收器 · 可在应用中停止").setSmallIcon(android.R.drawable.ic_media_play).build());}
 public int onStartCommand(Intent i,int flags,int id){if(!running){compatMode=getSharedPreferences("bridge",0).getBoolean("builtin_mode",false);if(compatMode&&!NativePad.canUse()&&!OemInputGrant.available()){compatMode=false;getSharedPreferences("bridge",0).edit().putBoolean("builtin_mode",false).apply();}compatActive=compatMode;running=true;worker=new Thread(()->runBridge(),"gamepad-usb-reader");worker.start();}return START_NOT_STICKY;}
 int s16(byte[] b,int off){return (short)((b[off]&255)|((b[off+1]&255)<<8));}
 int flip(int v){return Math.max(-32768,Math.min(32767,-v));}
 void triple(JSONArray ev,int t,int c,int v){ev.put(t);ev.put(c);ev.put(v);}
 void runBridge(){UsbRequest req=null;BufferedWriter out=null;NativePad nativePad=null;
  try{
   UsbManager m=(UsbManager)getSystemService(USB_SERVICE);UsbDevice dev=null;for(UsbDevice d:m.getDeviceList().values())if(d.getVendorId()==0x2345&&d.getProductId()==0xe053)dev=d;
   if(dev==null)throw new IOException("接收器未连接");if(!m.hasPermission(dev))throw new IOException("缺少用户 USB 授权");
   UsbInterface intf=null;UsbEndpoint ep=null;for(int i=0;i<dev.getInterfaceCount();i++){UsbInterface f=dev.getInterface(i);if(f.getId()==0&&f.getInterfaceClass()==255&&f.getInterfaceSubclass()==93){intf=f;for(int j=0;j<f.getEndpointCount();j++){UsbEndpoint e=f.getEndpoint(j);if(e.getDirection()==UsbConstants.USB_DIR_IN)ep=e;else rumbleEndpoint=e;}}}
   if(intf==null||ep==null)throw new IOException("未找到 Xbox 360 风格输入接口");
   connection=m.openDevice(dev);if(connection==null)throw new IOException("USB 打开失败");if(!connection.claimInterface(intf,false))throw new IOException("USB 接口占用，未强制分离驱动");
   note("USB 已打开："+dev.getDeviceName()+" interface="+intf.getId()+" endpoint="+ep.getAddress());
   if(compatMode){
    nativePad=new NativePad();note("内置手柄兼容模式 · "+nativePad.path+" · 普通应用 UID="+android.os.Process.myUid());
   }else{
   try{OemInputGrant.recoverStandardMode();}catch(Exception e){Log.w("GamepadGrant","前次兼容授权的收回待确认",e);}
   virtualProcess=new ProcessBuilder("/system/bin/uinput","-").redirectErrorStream(true).start();
   final java.lang.Process child=virtualProcess;
   new Thread(()->{try{InputStream in=child.getInputStream();StringBuilder json=new StringBuilder();int ch;
    while((ch=in.read())!=-1){if(ch=='{')json.setLength(0);json.append((char)ch);if(ch=='}'){
     try{JSONObject msg=new JSONObject(json.toString());if("vibrating".equals(msg.optString("reason"))){effectUntil=msg.optInt("status")>0?android.os.SystemClock.elapsedRealtime()+1000:0;Log.i("GamepadRumble","FF "+msg);}}catch(Exception ignored){}json.setLength(0);
    }if(json.length()>4096)json.setLength(0);}
   }catch(Exception ignored){}},"uinput-log").start();
   out=new BufferedWriter(new OutputStreamWriter(child.getOutputStream(),"UTF-8"));
   out.write(VirtualPad.registration().toString());out.newLine();out.write(VirtualPad.navigation().toString());out.newLine();out.flush();
   boolean ready=false;
   for(int attempt=0;attempt<50&&running;attempt++){
    for(int inputId:android.view.InputDevice.getDeviceIds()){
     android.view.InputDevice pad=android.view.InputDevice.getDevice(inputId);
     if(pad!=null&&"Gamepad Bridge Controller".equals(pad.getName())&&(pad.getSources()&0x1000010)==0x1000010){ready=true;break;}
    }
    if(ready)break;Thread.sleep(100);
   }
   if(!ready)throw new IOException("虚拟手柄创建失败，请查看诊断日志");
   note("虚拟手柄已就绪，运行 UID="+android.os.Process.myUid());
   }
   req=new UsbRequest();if(!req.initialize(connection,ep))throw new IOException("USB request 初始化失败");
   ByteBuffer buf=ByteBuffer.allocateDirect(Math.max(32,ep.getMaxPacketSize()));byte[] last=null, diagnosticLast=null;boolean chordLatched=false;
   note((compatMode?"内置手柄兼容模式":"标准模式")+" · 桥接运行中 · 可返回桌面启动游戏");
   while(running){
    int rumble=(android.os.SystemClock.elapsedRealtime()<Math.max(effectUntil,testRumbleUntil))?100:0;if(rumble!=lastRumble){sendRumble(rumble);lastRumble=rumble;}
    buf.clear();if(!req.queue(buf))throw new IOException("USB queue 失败");UsbRequest done=null;while(running&&done==null){try{done=connection.requestWait(1000);}catch(TimeoutException ignored){}}
    if(!running)break;if(done==null)throw new IOException("USB 已断开");int len=buf.position();buf.flip();byte[] b=new byte[len];buf.get(b);packets++;
    if(android.os.SystemClock.elapsedRealtime()<diagnosticUntil && !Arrays.equals(diagnosticLast,b)){Log.i("GamepadRaw", "RAW "+hex(b));diagnosticLast=b.clone();}
    if(packets<=4)note("USB 原始包 "+packets+" 长度="+len+" "+hex(b));
    if(len<20||b[0]!=0||(b[1]&255)!=20){if(packets%100==0)note("收到非标准报文，等待解析："+hex(b));continue;}
    if(Arrays.equals(last,b))continue;last=b.clone();int bits=(b[2]&255)|((b[3]&255)<<8);seen|=bits;
    boolean chord=(bits&0x30)==0x30;
    if(chord&&!chordLatched){
     if(compatMode){nativePad.back();}else{for(int v:new int[]{1,0}){out.write(new JSONObject().put("id",72).put("command","inject").put("events",new JSONArray(new int[]{1,158,v,0,0,0})).toString());out.newLine();}out.flush();}Log.i("GamepadShortcut","Select+Start -> BACK");chordLatched=true;
    }
    if(chordLatched){if((bits&0x30)==0)chordLatched=false;bits &= ~0x30;}

    if(compatMode){
     int[] masks={0x1000,0x2000,0x4000,0x8000,0x100,0x200,0x20,0x10,0x400,0x40,0x80};int[] codes={304,305,307,308,310,311,314,315,316,317,318};
     for(int j=0;j<masks.length;j++)nativePad.key(codes[j],(bits&masks[j])!=0?1:0);
     nativePad.key(312,(b[4]&255)>127?1:0);nativePad.key(313,(b[5]&255)>127?1:0);
     nativePad.axis(0,Math.max(-32767,s16(b,6)));nativePad.axis(1,Math.max(-32767,flip(s16(b,8))));
     nativePad.axis(2,Math.max(-32767,s16(b,10)));nativePad.axis(5,Math.max(-32767,flip(s16(b,12))));
     nativePad.axis(10,(b[4]&255)*32767/255);nativePad.axis(9,(b[5]&255)*32767/255);
     nativePad.axis(16,((bits&8)!=0?1:0)-((bits&4)!=0?1:0));nativePad.axis(17,((bits&2)!=0?1:0)-((bits&1)!=0?1:0));nativePad.sync();forwarded++;
    }else{
    JSONArray ev=new JSONArray();int[] masks={0x1000,0x2000,0x4000,0x8000,0x100,0x200,0x20,0x10,0x400,0x40,0x80};int[] codes={304,305,307,308,310,311,314,315,316,317,318};for(int j=0;j<masks.length;j++)triple(ev,1,codes[j],(bits&masks[j])!=0?1:0);
    triple(ev,3,0,s16(b,6));triple(ev,3,1,flip(s16(b,8)));triple(ev,3,3,s16(b,10));triple(ev,3,4,flip(s16(b,12)));triple(ev,3,2,b[4]&255);triple(ev,3,5,b[5]&255);triple(ev,3,16,((bits&8)!=0?1:0)-((bits&4)!=0?1:0));triple(ev,3,17,((bits&2)!=0?1:0)-((bits&1)!=0?1:0));triple(ev,0,0,0);
    out.write(new JSONObject().put("id",71).put("command","inject").put("events",ev).toString());out.newLine();out.flush();forwarded++;
    }
    long now=System.currentTimeMillis();if(now-lastSummary>1000){note("USB包="+packets+" 已桥接="+forwarded+" 按键累计=0x"+Integer.toHexString(seen)+"\n当前键=0x"+Integer.toHexString(bits)+" LT="+(b[4]&255)+" RT="+(b[5]&255)+"\n左="+s16(b,6)+","+s16(b,8)+" 右="+s16(b,10)+","+s16(b,12));lastSummary=now;}
   }
  }catch(Exception e){if(running){failure=e.toString();note("错误："+failure);}}
  finally{if(nativePad!=null)try{nativePad.close();}catch(Exception e){Log.w("GamepadBridge","内置输入释放",e);}sendRumble(0);running=false;if(req!=null){try{req.cancel();req.close();}catch(Exception ignored){}}if(connection!=null)connection.close();if(virtualProcess!=null)virtualProcess.destroy();if(out!=null)try{out.close();}catch(Exception ignored){}note((failure==null?"已停止。":"错误："+failure+"；已停止。")+"USB包="+packets+" 已桥接="+forwarded+" 按键累计=0x"+Integer.toHexString(seen));stopSelf();}
 }
 synchronized void sendRumble(int strength){
  if(connection==null||rumbleEndpoint==null)return;
  try{byte[] data={0,8,0,(byte)strength,(byte)strength,0,0,0};int n=connection.bulkTransfer(rumbleEndpoint,data,data.length,100);Log.i("GamepadRumble","USB OUT strength="+strength+" bytes="+n);}catch(Exception e){Log.w("GamepadRumble","OUT "+e);}
 }
 String hex(byte[] b){StringBuilder s=new StringBuilder();for(int i=0;i<b.length;i++)s.append(String.format(java.util.Locale.US,"%02x",b[i]&255));return s.toString();}
 public void onDestroy(){if(activeService==this)activeService=null;running=false;sendRumble(0);if(connection!=null)connection.close();if(virtualProcess!=null)virtualProcess.destroy();super.onDestroy();}
}
