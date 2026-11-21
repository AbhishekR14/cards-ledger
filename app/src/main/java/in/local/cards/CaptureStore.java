package in.local.cards;

import android.content.*;

/** Shared persistence only. notification and SMS delivery are separate build components. */
public final class CaptureStore {
 public static synchronized int save(Context context,Engine.Parsed parsed,String event,long received,boolean possibleDuplicate,String source){
  return save(context,Cards.book(context).isEmpty()?1:Cards.book(context).at(0).id,parsed,event,received,possibleDuplicate,source);
 }
 // The alert router picks the card; the transaction is written to that card's ledger
 // with that card's own merchant mappings.
 public static synchronized int save(Context context,int cardId,Engine.Parsed parsed,String event,long received,boolean possibleDuplicate,String source){
  return save(context,cardId,parsed,event,received,possibleDuplicate,source,false);
 }
 // A credit is money posted to the card, so it is stored without a reward category
 // and left out of spending.
 public static synchronized int save(Context context,int cardId,Engine.Parsed parsed,String event,long received,boolean possibleDuplicate,String source,boolean credit){
  try(Store store=Cards.store(context,cardId)){
   SharedPreferences p=Cards.prefs(context,cardId);
   Rows.Row x=new Rows.Row();x.amount=parsed.amount;x.merchant=parsed.merchant;x.date=parsed.date;x.refund=parsed.refund;x.received=received;x.source=source;x.event=event;x.credit=credit;
   RewardPlan plan=Cards.plan(context,cardId);
   // A credit of exactly what an earlier purchase cost probably reverses it, so it takes that
   // purchase's category as a suggestion, which takes the reward back and asks for review.
   if(credit){x.category=Rows.matchCategory(store.all(),x.amount,x.date);x.confirmed=x.category==0;}
   else{store.categorize(x,p.getBoolean("myntra",false),Cards.usesBuiltInCategories(plan),plan);x.duplicate=possibleDuplicate;if(x.duplicate)x.confirmed=false;}
   for(Engine.Tx existing:store.all()){
    if(existing.event.equals(x.event))return 2;
    if(!credit&&!existing.refund&&!x.refund&&existing.active&&existing.amount==x.amount&&Math.abs(existing.received-x.received)<=120000&&Engine.key(existing.merchant).equals(Engine.key(x.merchant))){x.duplicate=true;x.confirmed=false;}
   }
   // Capture state is device-wide, so the marker goes to the global file.
   if(store.save(x)>0){Store.prefs(context).edit().putLong("lastCaptured",System.currentTimeMillis()).apply();context.sendBroadcast(new Intent("in.local.cards.CHANGED").setPackage(context.getPackageName()).putExtra(Cards.EXTRA_CARD,cardId));return 1;}
  }catch(Exception ignored){}
  Store.prefs(context).edit().putBoolean("captureError",true).apply();return 0;
 }
 public static void prepareEdition(Context context){
  SharedPreferences p=Store.prefs(context);String previous=p.getString("captureEdition","notifications");
  SharedPreferences.Editor editor=p.edit().putString("captureEdition",CaptureMode.ID).remove("capture_diagnostics");
  if(!CaptureMode.ID.equals(previous)){
   editor.putBoolean("capture",false).putBoolean("captureError",false).remove("started").remove("lastCaptured").remove("capture_diagnostics");
   for(String key:p.getAll().keySet())if(key.startsWith("capture_seen_"))editor.remove(key);
  }
  editor.apply();
 }
}