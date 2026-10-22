package io.github.thomasfissh.androidgamepadbridge;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Small native pixel design system. Chinese labels retain the system font for legibility. */
final class PixelUi {
 static final int BG=0xff0c1428, PANEL=0xff15223a, LINE=0xff304564, TEXT=0xffeaf6f2,
  MUTED=0xffa0b3cb, MINT=0xff79e4c1, GOLD=0xfff4cb7a, CORAL=0xffef927e;
 static int d(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static LinearLayout col(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(1);return v;}
 static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 static void gap(LinearLayout v,int dp){Space s=new Space(v.getContext());v.addView(s,new LinearLayout.LayoutParams(v.getOrientation()==1?1:d(v.getContext(),dp),v.getOrientation()==1?d(v.getContext(),dp):1));}
 static TextView text(Context c,String s,int size,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setIncludeFontPadding(false);return t;}
 static TextView code(Context c,String s,int color){TextView t=text(c,s,11,color);t.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);t.setLetterSpacing(.09f);return t;}
 static ImageView icon(Context c,String name){ImageView i=new ImageView(c);i.setImageResource(c.getResources().getIdentifier(name,"drawable",c.getPackageName()));return i;}
 static LinearLayout root(Activity a){a.getWindow().getDecorView().setSystemUiVisibility(5894);a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);LinearLayout v=col(a);v.setBackground(new Backdrop(a));v.setPadding(d(a,28),d(a,30),d(a,28),d(a,14));a.setContentView(v);return v;}
 static LinearLayout header(Activity a,LinearLayout root,String name,String title,String kicker){
  LinearLayout h=row(a);ImageView ico=icon(a,name);h.addView(ico,new LinearLayout.LayoutParams(d(a,48),d(a,48)));gap(h,14);
  LinearLayout words=col(a);words.addView(code(a,kicker,MINT));gap(words,5);TextView t=text(a,title,26,TEXT);t.setTypeface(null,Typeface.BOLD);words.addView(t);h.addView(words,new LinearLayout.LayoutParams(0,-2,1));root.addView(h,new LinearLayout.LayoutParams(-1,d(a,58)));gap(root,14);return h;
 }
 static LinearLayout card(Context c,int accent){LinearLayout v=col(c);v.setPadding(d(c,18),d(c,16),d(c,18),d(c,16));v.setBackground(new Tile(c,PANEL,LINE,accent));return v;}
 static Button button(Context c,String s,int accent,boolean solid,View.OnClickListener click){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(solid?BG:TEXT);b.setTypeface(null,Typeface.BOLD);b.setPadding(d(c,12),0,d(c,12),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setStateListAnimator(null);b.setOnClickListener(click);b.setFocusable(true);
  StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},new Tile(c,0xff1b2940,LINE,LINE));states.addState(new int[]{android.R.attr.state_pressed},new Tile(c,solid?0xffc8f4df:0xff304663,GOLD,GOLD));states.addState(new int[]{android.R.attr.state_focused},new Tile(c,solid?accent:0xff233854,GOLD,GOLD));states.addState(new int[]{},new Tile(c,solid?accent:0xff1a2b45,solid?accent:LINE,solid?accent:accent));b.setBackground(states);return b;
 }
 static void addButton(LinearLayout parent,Button b,int height){parent.addView(b,new LinearLayout.LayoutParams(-1,d(parent.getContext(),height)));}
 static TextView badge(Context c,String s,int color){TextView t=code(c,s,color);t.setPadding(d(c,12),d(c,9),d(c,12),d(c,9));t.setBackground(new Tile(c,PANEL,LINE,color));return t;}
 static class Tile extends Drawable {
  final Paint p=new Paint();final int fill,border,accent;final float u;
  Tile(Context c,int f,int b,int a){fill=f;border=b;accent=a;u=d(c,2);}
  void shape(Canvas c,float inset,int color){Rect b=getBounds();float l=b.left+inset,t=b.top+inset,r=b.right-inset,bt=b.bottom-inset,n=u*2;p.setColor(color);c.drawRect(l+n,t,r-n,bt,p);c.drawRect(l,t+n,r,bt-n,p);c.drawRect(l+u,t+u,r-u,bt-u,p);}
  public void draw(Canvas c){shape(c,0,border);shape(c,u,fill);Rect b=getBounds();p.setColor(accent);c.drawRect(b.left+u*3,b.top,b.left+u*10,b.top+u,p);}
  public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
 }
 static class Backdrop extends Drawable {
  final Paint p=new Paint();final Context c;Backdrop(Context c){this.c=c;}
  public void draw(Canvas canvas){Rect r=getBounds();canvas.drawColor(BG);int u=d(c,2);p.setColor(0xff17243b);for(int y=d(c,14);y<r.height();y+=d(c,24))for(int x=d(c,14);x<r.width();x+=d(c,24))canvas.drawRect(x,y,x+u,y+u,p);
   p.setColor(0xff102039);int step=d(c,24);for(int x=0;x<r.width();x+=step){int h=d(c,10+(x/step*7)%19);canvas.drawRect(x,r.bottom-h,x+step,r.bottom,p);}
   p.setColor(0xff38566a);canvas.drawRect(r.right-d(c,82),d(c,22),r.right-d(c,78),d(c,26),p);p.setColor(GOLD);canvas.drawRect(r.right-d(c,48),d(c,32),r.right-d(c,44),d(c,36),p);
  }
  public void setAlpha(int a){}public void setColorFilter(ColorFilter f){}public int getOpacity(){return PixelFormat.OPAQUE;}
 }
}
