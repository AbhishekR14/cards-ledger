package in.local.cards;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * The app's motion, in one place so every screen moves the same way.
 *
 * <p>Timings follow Material's: short and decelerating, so things arrive quickly and settle
 * rather than drift. Everything here honours the system's "Remove animations" setting: with
 * animators off, a view simply appears in its final state.
 */
final class Motion {
 private Motion(){}

 /** Material's emphasised deceleration: a quick start that settles gently. */
 static final Interpolator SETTLE=new PathInterpolator(0.05f,0.7f,0.1f,1f);
 /** Standard easing, for things that change in place. */
 static final Interpolator EASE=new PathInterpolator(0.2f,0f,0f,1f);
 static final long SHORT=180,MEDIUM=280,LONG=520;

 static boolean on(){return ValueAnimator.areAnimatorsEnabled();}

 /** Rises into place and fades in, after {@code order} steps of a short stagger. */
 static void rise(View view,int order,float distance){
  if(!on())return;
  view.setAlpha(0f);view.setTranslationY(distance);
  view.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(order,8)*45L)
   .setDuration(MEDIUM+60).setInterpolator(SETTLE).start();
 }
 /** Each child of {@code parent} rises in, one after another. */
 static void stagger(ViewGroup parent,float distance){
  for(int i=0;i<parent.getChildCount();i++)rise(parent.getChildAt(i),i,distance);
 }
 /** Slides in from the side it came from: +1 from the right, -1 from the left. */
 static void slideIn(View view,int direction,float distance){
  if(!on()||direction==0)return;
  view.setAlpha(0f);view.setTranslationX(direction*distance);
  view.animate().alpha(1f).translationX(0f).setStartDelay(0).setDuration(MEDIUM).setInterpolator(SETTLE).start();
 }
 /** A progress bar fills from empty to {@code value}. */
 static void fill(ProgressBar bar,int value){
  if(!on()){bar.setProgress(value);return;}
  bar.setProgress(0);
  ObjectAnimator fill=ObjectAnimator.ofInt(bar,"progress",0,value);
  fill.setStartDelay(160);fill.setDuration(LONG+200);fill.setInterpolator(SETTLE);fill.start();
 }
 /** Formats an amount for {@link #countUp}. */
 interface Format{String of(long value);}
 /** A figure counts up from zero to {@code value}, keeping its final text if animations are off. */
 static void countUp(TextView view,long value,Format format){
  view.setText(format.of(value));
  if(!on()||value==0)return;
  ValueAnimator count=ValueAnimator.ofFloat(0f,1f);
  count.setStartDelay(120);count.setDuration(LONG+180);count.setInterpolator(SETTLE);
  count.addUpdateListener(a->view.setText(format.of(Math.round(value*(float)a.getAnimatedValue()))));
  count.start();
 }
 /**
  * Shrinks slightly while held, the way a Material card answers a press. The listener returns
  * false, so taps and long presses still reach the view.
  */
 @SuppressLint("ClickableViewAccessibility")
 static void pressable(View view){
  view.setOnTouchListener((v,event)->{
   if(!on())return false;
   switch(event.getActionMasked()){
    case MotionEvent.ACTION_DOWN:v.animate().scaleX(0.965f).scaleY(0.965f).setStartDelay(0).setDuration(SHORT/2).setInterpolator(EASE).start();break;
    case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(SHORT).setInterpolator(SETTLE).start();break;
    default:break;
   }
   return false;
  });
 }
}
