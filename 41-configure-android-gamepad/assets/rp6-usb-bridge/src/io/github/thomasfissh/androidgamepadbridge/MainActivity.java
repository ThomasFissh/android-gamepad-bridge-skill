package io.github.thomasfissh.androidgamepadbridge;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.hardware.usb.*;
import android.hardware.input.InputManager;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import java.util.*;

/** UI-only revision. USB forwarding, native input and OEM permissions remain in the stable classes. */
public class MainActivity extends Activity implements InputManager.InputDeviceListener {
 TextView text;CheckBox compat;Handler h=new Handler();long keys=0,moves=0;
 TreeSet<Integer> seenKeys=new TreeSet<>();int[] axes={0,1,11,14,23,22,15,16};
 float[] mins=new float[8],maxs=new float[8];String last="未收到虚拟手柄输入";
 final PadState state=new PadState();ControllerView pad;TextView deviceLabel,stats,badge;Button deviceButton;
 int selectedId=-1;boolean manualDevice=false;InputManager inputs;
 final Runnable update=new Runnable(){public void run(){refreshStatus();h.postDelayed(this,250);}};
 public void onCreate(Bundle b){
  super.onCreate(b);inputs=(InputManager)getSystemService(INPUT_SERVICE);
  LinearLayout root=PixelUi.root(this);LinearLayout header=PixelUi.header(this,root,"ic_header_gamepad","安卓手柄桥接","PIXEL CONSOLE  /  INPUT LAB");
  badge=PixelUi.badge(this,"桥接未启动",PixelUi.GOLD);header.addView(badge);
  LinearLayout body=PixelUi.row(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout monitor=PixelUi.card(this,PixelUi.MINT);body.addView(monitor,new LinearLayout.LayoutParams(0,-1,1.85f));
  LinearLayout cap=PixelUi.row(this);cap.addView(PixelUi.code(this,"LIVE CONTROLLER",PixelUi.MINT),new LinearLayout.LayoutParams(0,-2,1));
  Button reset=PixelUi.button(this,"清空统计",PixelUi.MINT,false,v->{state.clearHistory();pad.invalidate();refreshStatus();});reset.setTextSize(11);cap.addView(reset,new LinearLayout.LayoutParams(PixelUi.d(this,82),PixelUi.d(this,28)));monitor.addView(cap);PixelUi.gap(monitor,6);
  LinearLayout source=PixelUi.row(this);deviceLabel=PixelUi.text(this,"等待 Android 手柄",12,PixelUi.MUTED);deviceLabel.setSingleLine(true);deviceLabel.setEllipsize(TextUtils.TruncateAt.END);source.addView(deviceLabel,new LinearLayout.LayoutParams(0,-2,1));
  deviceButton=PixelUi.button(this,"选择手柄 ▾",PixelUi.MINT,false,v->chooseDevice());deviceButton.setTextSize(11);source.addView(deviceButton,new LinearLayout.LayoutParams(PixelUi.d(this,100),PixelUi.d(this,28)));monitor.addView(source);PixelUi.gap(monitor,2);
  pad=new ControllerView(this,state);monitor.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
  stats=PixelUi.text(this,"等待输入",11,PixelUi.MUTED);stats.setSingleLine(true);stats.setEllipsize(TextUtils.TruncateAt.END);monitor.addView(stats);
  PixelUi.gap(body,16);LinearLayout control=PixelUi.card(this,PixelUi.GOLD);body.addView(control,new LinearLayout.LayoutParams(0,-1,1));
  control.addView(PixelUi.code(this,"BRIDGE CONTROL",PixelUi.GOLD));PixelUi.gap(control,8);
  text=PixelUi.text(this,"",13,PixelUi.MUTED);text.setMaxLines(3);text.setEllipsize(TextUtils.TruncateAt.END);control.addView(text,new LinearLayout.LayoutParams(-1,PixelUi.d(this,52)));PixelUi.gap(control,6);
  compat=new CheckBox(this);compat.setText("内置手柄兼容");compat.setTextSize(14);compat.setTextColor(PixelUi.TEXT);compat.setButtonTintList(ColorStateList.valueOf(PixelUi.MINT));compat.setPadding(0,0,0,0);compat.setMinHeight(0);compat.setMinimumHeight(0);
  boolean available=NativePad.canUse()||OemInputGrant.available();
  if(!available){getSharedPreferences("bridge",0).edit().putBoolean("builtin_mode",false).apply();compat.setText("内置兼容：暂不可用");}
  compat.setEnabled(available);compat.setChecked(available&&getSharedPreferences("bridge",0).getBoolean("builtin_mode",false));
  compat.setOnCheckedChangeListener((button,checked)->getSharedPreferences("bridge",0).edit().putBoolean("builtin_mode",checked).apply());
  control.addView(compat,new LinearLayout.LayoutParams(-1,PixelUi.d(this,32)));control.addView(PixelUi.text(this,"用于死亡细胞等需要兼容的游戏",10,PixelUi.MUTED));PixelUi.gap(control,10);
  LinearLayout actions=PixelUi.row(this);Button start=PixelUi.button(this,"启动桥接",PixelUi.MINT,true,v->request());actions.addView(start,new LinearLayout.LayoutParams(0,PixelUi.d(this,42),1));PixelUi.gap(actions,8);Button stop=PixelUi.button(this,"停止",PixelUi.CORAL,false,v->stopService(new Intent(this,BridgeService.class)));actions.addView(stop,new LinearLayout.LayoutParams(0,PixelUi.d(this,42),.62f));control.addView(actions);PixelUi.gap(control,9);
  LinearLayout tools=PixelUi.row(this);
  Button diag=PixelUi.button(this,"采样 20 秒",PixelUi.GOLD,false,v->{BridgeService.diagnosticUntil=SystemClock.elapsedRealtime()+20000;android.util.Log.i("GamepadRaw","BEGIN");});diag.setTextSize(12);tools.addView(diag,new LinearLayout.LayoutParams(0,PixelUi.d(this,36),1));PixelUi.gap(tools,7);
  Button rumble=PixelUi.button(this,"震动 0.3 秒",PixelUi.GOLD,false,v->{if(BridgeService.compatActive&&BridgeService.isRunning()){BridgeService.testRumbleUntil=SystemClock.elapsedRealtime()+300;return;}for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&d.getName().equals("Gamepad Bridge Controller")){d.getVibrator().vibrate(VibrationEffect.createOneShot(300,VibrationEffect.DEFAULT_AMPLITUDE));}}});rumble.setTextSize(12);tools.addView(rumble,new LinearLayout.LayoutParams(0,PixelUi.d(this,36),1));control.addView(tools);
  control.addView(new View(this),new LinearLayout.LayoutParams(1,0,1));PixelUi.gap(control,8);
  PixelUi.addButton(control,PixelUi.button(this,"详细读数  ↗",PixelUi.MINT,false,v->details()),36);
  PixelUi.gap(root,10);TextView footer=PixelUi.text(this,"按键亮起 · 摇杆实时定位 · LT / RT 行程     |     测试按键不触发控制按钮，返回键退出",11,PixelUi.MUTED);root.addView(footer);
  updateDevices();handleStartIntent(getIntent());
 }
 protected void onResume(){super.onResume();inputs.registerInputDeviceListener(this,h);updateDevices();h.removeCallbacks(update);h.post(update);}
 protected void onPause(){h.removeCallbacks(update);inputs.unregisterInputDeviceListener(this);state.release();if(pad!=null)pad.invalidate();super.onPause();}
 public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused){state.release();if(pad!=null)pad.invalidate();}}
 public void onDestroy(){h.removeCallbacks(update);super.onDestroy();}
 boolean isPad(InputEvent event){InputDevice d=event.getDevice();return d!=null&&(d.supportsSource(InputDevice.SOURCE_GAMEPAD)||d.supportsSource(InputDevice.SOURCE_JOYSTICK));}
 String pretty(InputDevice d){if(d==null)return "等待 Android 手柄";if("Retroid Pocket Controller".equals(d.getName()))return BridgeService.compatActive&&BridgeService.isRunning()?"内置 / USB 兼容（共用）":"设备内置手柄";if("Gamepad Bridge Controller".equals(d.getName()))return "USB 手柄 · 标准桥接";return d.getName();}
 void select(InputDevice d){selectedId=d==null?-1:d.getId();state.newDevice();if(d!=null){state.analogLT=has(d,MotionEvent.AXIS_LTRIGGER)||has(d,MotionEvent.AXIS_BRAKE);state.analogRT=has(d,MotionEvent.AXIS_RTRIGGER)||has(d,MotionEvent.AXIS_GAS);}if(deviceLabel!=null)deviceLabel.setText(pretty(d));if(pad!=null)pad.invalidate();}
 boolean has(InputDevice d,int axis){return d!=null&&d.getMotionRange(axis)!=null;}
 void updateDevices(){InputDevice current=InputDevice.getDevice(selectedId),preferred=null;if(current!=null&&!current.supportsSource(InputDevice.SOURCE_GAMEPAD)&&!current.supportsSource(InputDevice.SOURCE_JOYSTICK))current=null;for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&(d.supportsSource(InputDevice.SOURCE_GAMEPAD)||d.supportsSource(InputDevice.SOURCE_JOYSTICK))){if(preferred==null||d.getName().equals("Gamepad Bridge Controller"))preferred=d;}}if(current==null||(!manualDevice&&preferred!=null&&preferred.getId()!=selectedId))select(preferred);else if(deviceLabel!=null)deviceLabel.setText(pretty(current));}
 void chooseDevice(){ArrayList<InputDevice> devices=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();for(int id:InputDevice.getDeviceIds()){InputDevice d=InputDevice.getDevice(id);if(d!=null&&(d.supportsSource(InputDevice.SOURCE_GAMEPAD)||d.supportsSource(InputDevice.SOURCE_JOYSTICK))){devices.add(d);labels.add(pretty(d)+"  ["+id+"]");}}if(devices.isEmpty()){new AlertDialog.Builder(this).setTitle("没有 Android 手柄").setMessage("内置手柄或已启动的 USB 桥接出现在系统后，即可查看实时输入。").setPositiveButton("知道了",null).show();return;}new AlertDialog.Builder(this).setTitle("选择要观察的手柄").setItems(labels.toArray(new String[0]),(dialog,which)->{manualDevice=true;select(devices.get(which));refreshStatus();}).setNegativeButton("取消",null).show();}
 public void onInputDeviceAdded(int id){updateDevices();}public void onInputDeviceChanged(int id){if(id==selectedId)select(InputDevice.getDevice(id));else updateDevices();}public void onInputDeviceRemoved(int id){updateDevices();}
 void refreshStatus(){if(text==null)return;boolean running=BridgeService.isRunning();String title=running?"桥接运行中":"桥接未启动";if(!title.contentEquals(badge.getText())){badge.setText(title);badge.setTextColor(running?PixelUi.MINT:PixelUi.GOLD);}String deviceName=pretty(InputDevice.getDevice(selectedId));if(!deviceName.contentEquals(deviceLabel.getText()))deviceLabel.setText(deviceName);String service=BridgeService.status;if(!service.contentEquals(text.getText()))text.setText(service);String line=state.last+"   ·   按下 "+state.presses+" 次   ·   轴事件 "+state.motions;if(!line.contentEquals(stats.getText()))stats.setText(line);}
 void details(){String values=String.format(Locale.US,"左摇杆 X %+.4f  Y %+.4f\n右摇杆 X %+.4f  Y %+.4f\nLT %.3f  RT %.3f\nHAT X %+.1f  Y %+.1f",state.lx,state.ly,state.rx,state.ry,state.leftTrigger(KeyEvent.KEYCODE_BUTTON_L2),state.rightTrigger(KeyEvent.KEYCODE_BUTTON_R2),state.hx,state.hy);String report="观察设备："+pretty(InputDevice.getDevice(selectedId))+"\n设备 ID："+selectedId+"\n\n"+values+"\n\n按键："+state.held+"\n累计键值："+state.seen+"\n\n桥接状态：\n"+BridgeService.status+"\n\nSelect＋Start 仍按原方案呼出菜单。此页面只观察输入，不校准、不修改键位。";ScrollView scroll=new ScrollView(this);TextView body=PixelUi.text(this,report,14,PixelUi.TEXT);body.setTextIsSelectable(true);body.setPadding(PixelUi.d(this,24),PixelUi.d(this,16),PixelUi.d(this,24),PixelUi.d(this,16));scroll.addView(body);new AlertDialog.Builder(this).setTitle("手柄与桥接读数").setView(scroll).setPositiveButton("关闭",null).show();}
 void handleStartIntent(Intent intent){if(intent!=null&&intent.getBooleanExtra("gamepad_start",false)){intent.removeExtra("gamepad_start");android.util.Log.i("GamepadLaunch","Start saved mode requested");h.postDelayed(()->request(),400);}}
 protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleStartIntent(intent);}
 void request(){if(BridgeService.isRunning() && compat.isChecked()!=BridgeService.compatActive){stopService(new Intent(this,BridgeService.class));h.postDelayed(()->request(),700);return;}UsbManager m=(UsbManager)getSystemService(USB_SERVICE);for(UsbDevice d:m.getDeviceList().values())if(d.getVendorId()==0x2345&&d.getProductId()==0xe053){if(m.hasPermission(d)){startForegroundService(new Intent(this,BridgeService.class));return;}String action=getPackageName()+".USB_PERMISSION";BroadcastReceiver r=new BroadcastReceiver(){public void onReceive(Context c,Intent i){unregisterReceiver(this);if(m.hasPermission(d))startForegroundService(new Intent(MainActivity.this,BridgeService.class));else BridgeService.status="USB 授权未通过";}};registerReceiver(r,new IntentFilter(action));m.requestPermission(d,PendingIntent.getBroadcast(this,0,new Intent(action).setPackage(getPackageName()),PendingIntent.FLAG_MUTABLE|PendingIntent.FLAG_UPDATE_CURRENT));return;}BridgeService.status="未找到 2345:e053 接收器";}
 boolean own(InputEvent e){InputDevice d=e.getDevice();return d!=null&&d.getName().equals(BridgeService.compatActive?"Retroid Pocket Controller":"Gamepad Bridge Controller");}
 public boolean dispatchKeyEvent(KeyEvent e){
  if(e.getKeyCode()!=KeyEvent.KEYCODE_BACK&&isPad(e)){
   if(e.getDeviceId()==selectedId){state.key(e.getKeyCode(),e.getAction()==KeyEvent.ACTION_DOWN,KeyEvent.keyCodeToString(e.getKeyCode()).replace("KEYCODE_BUTTON_","").replace("KEYCODE_DPAD_","方向 "));pad.invalidate();refreshStatus();}
   if(own(e)){keys++;seenKeys.add(e.getKeyCode());android.util.Log.i("GamepadInput","key="+e.getKeyCode()+" action="+e.getAction());last="key="+e.getKeyCode()+" action="+e.getAction();}
   return true;
  }
  return super.dispatchKeyEvent(e);
 }
 float triggerAxis(MotionEvent e,int primary,int alias){InputDevice d=e.getDevice();int axis=has(d,primary)?primary:alias;InputDevice.MotionRange range=d.getMotionRange(axis);float value=e.getAxisValue(axis);if(range!=null&&range.getMin()<0&&range.getMax()>range.getMin())value=(value-range.getMin())/(range.getMax()-range.getMin());return value;}
 public boolean dispatchGenericMotionEvent(MotionEvent e){
  if(isPad(e)&&e.getActionMasked()==MotionEvent.ACTION_MOVE){
   if(e.getDeviceId()==selectedId){InputDevice d=e.getDevice();int rightX=has(d,MotionEvent.AXIS_Z)?MotionEvent.AXIS_Z:MotionEvent.AXIS_RX;int rightY=has(d,MotionEvent.AXIS_RZ)?MotionEvent.AXIS_RZ:MotionEvent.AXIS_RY;state.motion(e.getAxisValue(MotionEvent.AXIS_X),e.getAxisValue(MotionEvent.AXIS_Y),e.getAxisValue(rightX),e.getAxisValue(rightY),triggerAxis(e,MotionEvent.AXIS_LTRIGGER,MotionEvent.AXIS_BRAKE),triggerAxis(e,MotionEvent.AXIS_RTRIGGER,MotionEvent.AXIS_GAS),e.getAxisValue(MotionEvent.AXIS_HAT_X),e.getAxisValue(MotionEvent.AXIS_HAT_Y));pad.invalidate();refreshStatus();}
   if(own(e)){moves++;for(int j=0;j<axes.length;j++){float v=e.getAxisValue(axes[j]);mins[j]=Math.min(mins[j],v);maxs[j]=Math.max(maxs[j],v);}last=String.format(Locale.US,"左 X %.2f Y %.2f 右 Z %.2f RZ %.2f LT %.2f RT %.2f",e.getAxisValue(0),e.getAxisValue(1),e.getAxisValue(11),e.getAxisValue(14),e.getAxisValue(23),e.getAxisValue(22));}
   return true;
  }
  return super.dispatchGenericMotionEvent(e);
 }
}
