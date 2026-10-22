package io.github.thomasfissh.androidgamepadbridge;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.Locale;

/** Original native vector drawing, bound directly to the selected Android input device. */
final class ControllerView extends View {
 final PadState state;
 final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 static final int BODY=0xff22334f, BORDER=0xff536b91, WELL=0xff0d182c, PURPLE=0xffb2a0ef;
 ControllerView(Context c,PadState s){super(c);state=s;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 void fill(Canvas c,float x,float y,float w,float h,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(x,y,x+w,y+h,p);}
 void label(Canvas c,String text,float x,float y,int size,int color,boolean mono){p.setColor(color);p.setTypeface(mono?Typeface.create(Typeface.MONOSPACE,Typeface.BOLD):Typeface.create(Typeface.DEFAULT,Typeface.BOLD));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);p.setStyle(Paint.Style.FILL);c.drawText(text,x,y,p);}
 Path pixelRect(float x,float y,float w,float h,float n){Path b=new Path();b.moveTo(x+n,y);b.lineTo(x+w-n,y);b.lineTo(x+w-n,y+n);b.lineTo(x+w,y+n);b.lineTo(x+w,y+h-n);b.lineTo(x+w-n,y+h-n);b.lineTo(x+w-n,y+h);b.lineTo(x+n,y+h);b.lineTo(x+n,y+h-n);b.lineTo(x,y+h-n);b.lineTo(x,y+n);b.lineTo(x+n,y+n);b.close();return b;}
 void tile(Canvas c,float x,float y,float w,float h,int color,int edge){Path b=pixelRect(x,y,w,h,4);p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(b,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(edge);c.drawPath(b,p);p.setStyle(Paint.Style.FILL);}
 void cap(Canvas c,String text,float x,float y,float w,int code,int color){boolean down=state.down(code);tile(c,x,y,w,24,down?color:WELL,down?color:BORDER);label(c,text,x+w/2,y+16,12,down?PixelUi.BG:PixelUi.MUTED,true);}
 void trigger(Canvas c,String text,float x,float value,int color){label(c,text,x-23,26,12,PixelUi.MUTED,true);tile(c,x,12,124,18,WELL,BORDER);if(value>0)fill(c,x+4,16,116*value,10,color);label(c,Math.round(value*100)+"%",x+148,26,11,value>0?color:PixelUi.MUTED,true);}
 void stick(Canvas c,float x,float y,float xx,float yy,int key,int accent,String name){
  p.setStyle(Paint.Style.FILL);p.setColor(WELL);c.drawCircle(x,y,43,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(state.down(key)?PixelUi.GOLD:BORDER);c.drawCircle(x,y,42,p);p.setStrokeWidth(1);p.setColor(0xff344765);c.drawCircle(x,y,29,p);c.drawLine(x-40,y,x+40,y,p);c.drawLine(x,y-40,x,y+40,p);p.setStyle(Paint.Style.FILL);
  float dx=x+xx*29,dy=y+yy*29;tile(c,dx-10,dy-10,20,20,state.down(key)?PixelUi.GOLD:accent,PixelUi.TEXT);fill(c,dx-3,dy-3,6,6,WELL);
  label(c,name,x,y+58,11,state.down(key)?PixelUi.GOLD:PixelUi.MUTED,true);
 }
 void face(Canvas c,String text,float x,float y,int code,int accent){boolean down=state.down(code);tile(c,x-15,y-15,30,30,down?accent:WELL,down?accent:BORDER);label(c,text,x,y+5,15,down?PixelUi.BG:accent,true);}
 void direction(Canvas c,float x,float y,int which,boolean down){tile(c,x-9,y-9,18,18,down?PixelUi.MINT:WELL,down?PixelUi.MINT:BORDER);p.setColor(down?PixelUi.BG:PixelUi.MUTED);Path t=new Path();if(which==0){t.moveTo(x,y-4);t.lineTo(x-5,y+3);t.lineTo(x+5,y+3);}else if(which==1){t.moveTo(x,y+4);t.lineTo(x-5,y-3);t.lineTo(x+5,y-3);}else if(which==2){t.moveTo(x-4,y);t.lineTo(x+3,y-5);t.lineTo(x+3,y+5);}else{t.moveTo(x+4,y);t.lineTo(x-3,y-5);t.lineTo(x-3,y+5);}t.close();p.setStyle(Paint.Style.FILL);c.drawPath(t,p);}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);float scale=Math.min(getWidth()/600f,getHeight()/300f);if(scale<=0)return;c.save();c.translate((getWidth()-600*scale)/2,(getHeight()-300*scale)/2);c.scale(scale,scale);
  trigger(c,"LT",90,state.leftTrigger(KeyEvent.KEYCODE_BUTTON_L2),PixelUi.MINT);trigger(c,"RT",400,state.rightTrigger(KeyEvent.KEYCODE_BUTTON_R2),PixelUi.GOLD);
  Path body=new Path();float[] points={110,56,490,56,490,70,514,70,514,86,530,86,530,112,546,112,546,238,530,238,530,256,486,256,486,238,466,238,466,240,134,240,134,238,114,238,114,256,70,256,70,238,54,238,54,112,70,112,70,86,86,86,86,70,110,70};body.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)body.lineTo(points[i],points[i+1]);body.close();c.save();c.translate(0,5);p.setColor(0xff07101f);p.setStyle(Paint.Style.FILL);c.drawPath(body,p);c.restore();p.setColor(BODY);c.drawPath(body,p);p.setColor(BORDER);p.setStrokeWidth(3);p.setStyle(Paint.Style.STROKE);c.drawPath(body,p);p.setStyle(Paint.Style.FILL);
  fill(c,245,60,110,3,PURPLE);cap(c,"L1",102,40,108,KeyEvent.KEYCODE_BUTTON_L1,PixelUi.MINT);cap(c,"R1",390,40,108,KeyEvent.KEYCODE_BUTTON_R1,PixelUi.GOLD);
  stick(c,162,117,state.lx,state.ly,KeyEvent.KEYCODE_BUTTON_THUMBL,PixelUi.MINT,"L3");
  stick(c,380,195,state.rx,state.ry,KeyEvent.KEYCODE_BUTTON_THUMBR,PURPLE,"R3");
  direction(c,162,190,0,state.down(KeyEvent.KEYCODE_DPAD_UP)||state.hy<-.5f);direction(c,162,230,1,state.down(KeyEvent.KEYCODE_DPAD_DOWN)||state.hy>.5f);direction(c,142,210,2,state.down(KeyEvent.KEYCODE_DPAD_LEFT)||state.hx<-.5f);direction(c,182,210,3,state.down(KeyEvent.KEYCODE_DPAD_RIGHT)||state.hx>.5f);
  face(c,"Y",448,88,KeyEvent.KEYCODE_BUTTON_Y,PixelUi.GOLD);face(c,"X",416,120,KeyEvent.KEYCODE_BUTTON_X,0xff8cc7fa);face(c,"B",480,120,KeyEvent.KEYCODE_BUTTON_B,PixelUi.CORAL);face(c,"A",448,152,KeyEvent.KEYCODE_BUTTON_A,PixelUi.MINT);
  cap(c,"SE",253,125,34,KeyEvent.KEYCODE_BUTTON_SELECT,PixelUi.MINT);cap(c,"ST",309,125,34,KeyEvent.KEYCODE_BUTTON_START,PixelUi.GOLD);
  label(c,"PAD",298,98,19,PURPLE,true);fill(c,286,108,6,3,PixelUi.MINT);fill(c,296,108,6,3,PixelUi.MINT);fill(c,306,108,6,3,PixelUi.GOLD);
  cap(c,"MODE",273,177,50,KeyEvent.KEYCODE_BUTTON_MODE,PURPLE);
  label(c,String.format(Locale.US,"L  %+.2f  %+.2f",state.lx,state.ly),162,281,13,PixelUi.MINT,true);label(c,String.format(Locale.US,"R  %+.2f  %+.2f",state.rx,state.ry),420,281,13,PURPLE,true);
  c.restore();
 }
}
