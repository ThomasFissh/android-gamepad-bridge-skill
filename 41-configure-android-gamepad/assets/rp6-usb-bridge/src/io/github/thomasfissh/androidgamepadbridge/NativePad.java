package io.github.thomasfissh.androidgamepadbridge;

import android.os.Process;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;

/** Uses RP6's existing controller node with the owner's scoped OEM authorization. */
final class NativePad implements Closeable {
 final FileOutputStream output;
 final OemInputGrant grant;
 final String path;
 final Map<Integer,Integer> keys=new LinkedHashMap<>(), axes=new LinkedHashMap<>();
 final int eventSize=Process.is64Bit()?24:16;
 static String findNode() throws IOException {
  String found=null;
  for(int i=0;i<64;i++) {
   String base="/sys/class/input/event"+i+"/device/";
   boolean matches;
   try {matches="Retroid Pocket Controller".equals(read(base+"name")) &&
       "2022".equals(read(base+"id/vendor")) && "3001".equals(read(base+"id/product")) &&
       "0000".equals(read(base+"id/version"));}
   catch(IOException ignored){continue;}
   if(matches){
    if(found!=null)throw new IOException("发现多个内置控制器，未开始兼容输入");
    found="/dev/input/event"+i;
   }
  }
  if(found==null)throw new IOException("未找到 RP6 内置控制器");
  return found;
 }
 static boolean canUse(){try{return new File(findNode()).canWrite();}catch(IOException e){return false;}}
 NativePad()throws IOException {
  path=findNode();
  android.system.StructStat state;
  try{state=android.system.Os.stat(path);}catch(android.system.ErrnoException e){throw new IOException(e);}
  grant=(!new File(path).canWrite() || (state.st_uid==Process.myUid()&&(state.st_mode&0777)==0660))?OemInputGrant.acquire(path):null;
  try{output=new FileOutputStream(path);}catch(IOException e){if(grant!=null)try{grant.close();}catch(IOException x){e.addSuppressed(x);}throw e;}
 }
 static String read(String p)throws IOException {return new String(Files.readAllBytes(Paths.get(p)),"UTF-8").trim();}
 void event(int type,int code,int value)throws IOException {
  ByteBuffer b=ByteBuffer.allocate(eventSize).order(ByteOrder.nativeOrder());
  if(eventSize==24)b.putLong(0).putLong(0);else b.putInt(0).putInt(0);
  b.putShort((short)type).putShort((short)code).putInt(value);output.write(b.array());
 }
 void changed(Map<Integer,Integer> state,int type,int code,int value)throws IOException {
  int previous=state.containsKey(code)?state.get(code):0;
  if(value!=previous){event(type,code,value);state.put(code,value);}
 }
 void key(int code,int value)throws IOException {changed(keys,1,code,value);}
 void axis(int code,int value)throws IOException {changed(axes,3,code,value);}
 void sync()throws IOException {event(0,0,0);}
 void back()throws IOException {key(158,1);sync();android.os.SystemClock.sleep(80);key(158,0);sync();}
 public void close()throws IOException {
  IOException failure=null;
  try {for(int k:new ArrayList<>(keys.keySet()))key(k,0);for(int a:new ArrayList<>(axes.keySet()))axis(a,0);sync();}
  catch(IOException e){failure=e;}
  finally {try{output.close();}finally{if(grant!=null)grant.close();}}
  if(failure!=null)throw failure;
 }
}
