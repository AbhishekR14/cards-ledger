package in.local.liveplus;

import android.content.*;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.*;
import java.util.concurrent.*;

/** OS-only incoming-SMS broadcast receiver. No SMS provider or notification listener. */
public final class SmsReceiver extends BroadcastReceiver {
 private static final ExecutorService WORK=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(64));
 @Override public void onReceive(Context context,Intent intent){
  if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction()))return;
  SharedPreferences p=Store.prefs(context);
  if(!CaptureMode.ID.equals(p.getString("captureEdition",""))||!p.getBoolean("capture",false)||!CaptureMode.access(context))return;
  long received=System.currentTimeMillis(),started=p.getLong("started",Long.MAX_VALUE);
  List<SmsCapture.Part> parts=new ArrayList<>();
  try{
   SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);if(messages==null)return;
   for(SmsMessage message:messages){if(message==null)return;parts.add(new SmsCapture.Part(message.getOriginatingAddress(),message.getMessageBody(),message.getTimestampMillis()));}
  }catch(RuntimeException ignored){p.edit().putBoolean("captureError",true).apply();return;}
  int subscription=intent.getIntExtra("subscription",-1);Context app=context.getApplicationContext();PendingResult pending=goAsync();
  try{WORK.execute(()->{
   try{
    if(!p.getBoolean("capture",false)||p.getLong("started",Long.MAX_VALUE)!=started||!CaptureMode.access(app))return;
    // MULTI-CARD: the message is offered to each capturing card in grid order; the first whose
    // bank and card suffix both match keeps it, so it is never counted twice.
    Alerts.SmsMatch match=Alerts.routeSms(parts,started,received,subscription,Cards.targets(app));
    if(match!=null)CaptureStore.save(app,match.cardId,match.candidate.transaction,match.candidate.event,match.candidate.received,false,"SMS",match.credit);
   }catch(RuntimeException ignored){p.edit().putBoolean("captureError",true).apply();}finally{pending.finish();}
  });}catch(RejectedExecutionException ignored){pending.finish();p.edit().putBoolean("captureError",true).apply();}
 }
}