package io.github.thomasfissh.androidgamepadbridge;

import android.os.*;
import android.system.*;
import java.io.*;

/** Uses the device owner's authorized Retroid script service for this one node only. */
final class OemInputGrant implements Closeable {
 private final String path;
 private final int uid;
 private boolean held;
 private OemInputGrant(String p,int u){path=p;uid=u;held=true;}
 private static IBinder service()throws Exception {
  return (IBinder)Class.forName("android.os.ServiceManager")
      .getMethod("getService",String.class).invoke(null,"PServerBinder");
 }
 static boolean available(){try{IBinder s=service();return s!=null&&s.isBinderAlive();}catch(Exception e){return false;}}
 private static String execute(String command)throws Exception {
  IBinder s=service();if(s==null)throw new IOException("Retroid 系统脚本服务未就绪");
  Parcel request=Parcel.obtain(),reply=Parcel.obtain();
  try{
   request.writeStringArray(new String[]{command,"1"});
   if(!s.transact(0,request,reply,0))throw new IOException("Retroid 系统服务拒绝请求");
   byte[] bytes=reply.createByteArray();return bytes==null?"":new String(bytes,"UTF-8").trim();
  }finally{request.recycle();reply.recycle();}
 }
 static OemInputGrant acquire(String path)throws IOException {
  int uid=android.os.Process.myUid();
  if(uid<10000||uid>=20000||!path.matches("/dev/input/event[0-9]+"))throw new IOException("输入授权目标不符合 RP6 范围");
  try{
   if(!path.equals(NativePad.findNode()))throw new IOException("内置控制器身份已变化");
   StructStat before=Os.stat(path);
   if(before.st_uid==uid&&before.st_gid==1004&&(before.st_mode&0777)==0660&&
      (before.st_mode&OsConstants.S_IFMT)==OsConstants.S_IFCHR)return new OemInputGrant(path,uid);
   if(before.st_uid!=0||before.st_gid!=1004||(before.st_mode&0777)!=0660||
      (before.st_mode&OsConstants.S_IFMT)!=OsConstants.S_IFCHR)throw new IOException("内置控制器权限与预期不符，未更改");
   if(!execute("/system/bin/id").startsWith("uid=0("))throw new IOException("Retroid 脚本服务未提供 root 执行身份");
   OemInputGrant grant=new OemInputGrant(path,uid);
   try{
    execute("/system/bin/chown "+uid+":1004 "+path);
    StructStat after=Os.stat(path);
    if(after.st_uid!=uid||after.st_gid!=1004||(after.st_mode&0777)!=0660)throw new IOException("Retroid 输入授权未生效");
    android.util.Log.i("GamepadGrant","Retroid input lease: "+path+" owner="+uid+" group=1004 mode=660");
    return grant;
   }catch(Exception e){grant.close();throw e;}
  }catch(Exception e){throw new IOException("Retroid 输入授权失败："+e,e);}
 }
 static void recoverStandardMode()throws IOException {
  try{
   String path=NativePad.findNode();StructStat state=Os.stat(path);
   if(state.st_uid==android.os.Process.myUid()&&state.st_gid==1004&&(state.st_mode&0777)==0660)
    new OemInputGrant(path,android.os.Process.myUid()).close();
  }catch(Exception e){throw new IOException("清理上次的输入授权失败："+e,e);}
 }
 public synchronized void close()throws IOException {
  if(!held)return;
  try{
   if(!path.equals(NativePad.findNode()))throw new IOException("控制器身份变化，未操作新节点");
   StructStat state=Os.stat(path);
   if(state.st_uid==uid&&state.st_gid==1004&&(state.st_mode&0777)==0660){
    execute("/system/bin/chown 0:1004 "+path);
    StructStat after=Os.stat(path);
    if(after.st_uid!=0||after.st_gid!=1004||(after.st_mode&0777)!=0660)throw new IOException("输入授权收回待确认");
    android.util.Log.i("GamepadGrant","Retroid input lease released: "+path+" owner=0 group=1004 mode=660");
   }
   held=false;
  }catch(Exception e){throw new IOException("收回 Retroid 输入授权失败："+e,e);}
 }
}
