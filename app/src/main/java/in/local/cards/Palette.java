package in.local.cards;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;

/**
 * The colours the app draws with.
 *
 * <p>On Android 12 and newer these come from the system's own palette, the one Android derives
 * from the wallpaper, so the app takes on the phone's colours. Older versions keep the theme's
 * fixed colours. Nothing here needs a support library: the
 * dynamic colours are plain framework resources.
 */
public final class Palette {
 public final int background,surface,surfaceHigh,outline,ink,muted,accent,onAccent,success,attention,notice,onNotice;
 /** Reads on a card tile, which is always a deep colour with white text. */
 public final int onTile;
 public final boolean dark,dynamic;

 private Palette(int background,int surface,int surfaceHigh,int outline,int ink,int muted,
   int accent,int onAccent,int success,int attention,int notice,int onNotice,int onTile,boolean dark,boolean dynamic){
  this.onTile=onTile;
  this.background=background;this.surface=surface;this.surfaceHigh=surfaceHigh;this.outline=outline;
  this.ink=ink;this.muted=muted;this.accent=accent;this.onAccent=onAccent;this.success=success;
  this.attention=attention;this.notice=notice;this.onNotice=onNotice;this.dark=dark;this.dynamic=dynamic;
 }

 /** True when the app should draw dark, following the stored choice or the phone's own setting. */
 public static boolean dark(Context context,String theme){
  if("dark".equals(theme))return true;
  if("light".equals(theme))return false;
  return (context.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
 }

 public static Palette of(Activity activity,boolean dark){
  if(Build.VERSION.SDK_INT>=31)return system(activity,dark);
  return themed(activity,dark);
 }

 /** Android's wallpaper palette, as Material You uses it. */
 private static Palette system(Activity activity,boolean dark){
  return new Palette(
   color(activity,dark?android.R.color.system_neutral1_900:android.R.color.system_neutral1_50),
   color(activity,dark?android.R.color.system_neutral1_800:android.R.color.system_neutral1_0),
   color(activity,dark?android.R.color.system_neutral2_700:android.R.color.system_accent2_50),
   color(activity,dark?android.R.color.system_neutral2_700:android.R.color.system_neutral2_300),
   color(activity,dark?android.R.color.system_neutral1_50:android.R.color.system_neutral1_900),
   color(activity,dark?android.R.color.system_neutral2_200:android.R.color.system_neutral2_700),
   color(activity,dark?android.R.color.system_accent1_200:android.R.color.system_accent1_600),
   color(activity,dark?android.R.color.system_accent1_800:android.R.color.system_accent1_0),
   theme(activity,R.attr.ledgerSuccess),
   theme(activity,R.attr.ledgerAttention),
   color(activity,dark?android.R.color.system_accent2_800:android.R.color.system_accent2_100),
   color(activity,dark?android.R.color.system_accent2_50:android.R.color.system_accent2_900),
   color(activity,android.R.color.system_accent1_100),
   dark,true);
 }
 /** The app's fixed colours, for a phone without a system palette. */
 private static Palette themed(Activity activity,boolean dark){
  return new Palette(
   theme(activity,android.R.attr.colorBackground),
   theme(activity,R.attr.ledgerSurface),
   theme(activity,R.attr.ledgerTabBackground),
   theme(activity,R.attr.ledgerTabBorder),
   theme(activity,android.R.attr.textColorPrimary),
   theme(activity,android.R.attr.textColorSecondary),
   dark?Color.rgb(185,237,204):Color.rgb(20,45,53),
   dark?Color.rgb(20,45,53):Color.WHITE,
   theme(activity,R.attr.ledgerSuccess),
   theme(activity,R.attr.ledgerAttention),
   theme(activity,R.attr.ledgerNotice),
   theme(activity,android.R.attr.textColorPrimary),
   Color.rgb(185,237,204),
   dark,false);
 }
 /** A card's colour as a soft diagonal gradient, lifting toward the top corner. */
 public static GradientDrawable gradient(int color,float radius){
  GradientDrawable drawable=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{lift(color,0.18f),color,darken(color,0.12f)});
  drawable.setCornerRadius(radius);
  return drawable;
 }
 static int lift(int color,float by){
  return Color.rgb((int)(Color.red(color)+(255-Color.red(color))*by),(int)(Color.green(color)+(255-Color.green(color))*by),(int)(Color.blue(color)+(255-Color.blue(color))*by));
 }
 static int darken(int color,float by){
  return Color.rgb((int)(Color.red(color)*(1-by)),(int)(Color.green(color)*(1-by)),(int)(Color.blue(color)*(1-by)));
 }
 private static int color(Activity activity,int id){
  try{return activity.getColor(id);}catch(RuntimeException ignored){return Color.GRAY;}
 }
 private static int theme(Activity activity,int attribute){
  android.util.TypedValue value=new android.util.TypedValue();
  activity.getTheme().resolveAttribute(attribute,value,true);
  return value.data;
 }

}
