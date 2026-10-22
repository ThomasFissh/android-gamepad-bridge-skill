package io.github.thomasfissh.androidgamepadbridge;

import java.util.HashSet;
import java.util.Set;

/** Display state only: never writes, calibrates, remaps, or forwards controller events. */
final class PadState {
 final Set<Integer> held=new HashSet<>(), seen=new HashSet<>();
 float lx,ly,rx,ry,lt,rt,hx,hy;
 boolean analogLT,analogRT;
 long presses,motions;
 String last="等待手柄输入";
 void key(int code,boolean down,String label){
  if(down){if(held.add(code))presses++;seen.add(code);}else held.remove(code);
  last=label+(down?" 按下":" 松开");
 }
 void motion(float x,float y,float z,float rz,float left,float right,float hatX,float hatY){
  lx=signed(x);ly=signed(y);rx=signed(z);ry=signed(rz);lt=trigger(left);rt=trigger(right);hx=signed(hatX);hy=signed(hatY);motions++;
 }
 boolean down(int code){return held.contains(code);}
 float leftTrigger(int digitalCode){return analogLT?lt:(down(digitalCode)?1:0);}
 float rightTrigger(int digitalCode){return analogRT?rt:(down(digitalCode)?1:0);}
 void release(){held.clear();lx=ly=rx=ry=lt=rt=hx=hy=0;}
 void newDevice(){release();seen.clear();presses=motions=0;last="等待手柄输入";analogLT=analogRT=false;}
 void clearHistory(){seen.clear();presses=motions=0;last="统计已清空，输入状态保留";}
 static float signed(float n){return Float.isNaN(n)||Float.isInfinite(n)?0:Math.max(-1,Math.min(1,n));}
 static float trigger(float n){return Float.isNaN(n)||Float.isInfinite(n)?0:Math.max(0,Math.min(1,n));}
}
