package in.local.liveplus;

import android.content.*;
import java.util.*;

/**
 * Preferences-backed access to the cards on this device.
 *
 * <p>Three kinds of state live in three places, which is what keeps this fork's diff against
 * upstream small:
 *
 * <ul>
 * <li><b>Global</b> ({@code app}): capture on/off, the chosen SMS app, the notification replay
 *     cache, the theme — everything the device holds once, whatever the card. This is what
 *     {@link Store#prefs(Context)} returns, so upstream's capture code already reads it.</li>
 * <li><b>Per card</b> ({@code settings}, {@code settings-2}, …): last four digits, statement day,
 *     cap and reward plan, written by the screens upstream owns.</li>
 * <li><b>The registry</b> (one key in the global file): names, banks and order of the grid.</li>
 * </ul>
 *
 * <p>Card 1 uses the original {@code ledger.db} and {@code settings} names, so an install that
 * only ever had the Live+ card keeps its ledger with no migration.
 */
public final class Cards {
 public static final String EXTRA_CARD="in.local.liveplus.CARD";
 public static final String GLOBAL="app",REGISTRY="cards";
 /** Keys that belong to the device rather than to one card. */
 private static final String[] SHARED={"theme","capture","started","captureEdition","lastCaptured","captureError"};

 public static SharedPreferences global(Context context){return context.getSharedPreferences(GLOBAL,Context.MODE_PRIVATE);}
 public static SharedPreferences prefs(Context context,int cardId){return Store.prefs(context,CardBook.preferences(cardId));}
 public static Store store(Context context,int cardId){return new Store(context,CardBook.database(cardId));}

 public static CardBook book(Context context){
  ensure(context);
  return CardBook.decode(global(context).getString(REGISTRY,""));
 }
 public static void save(Context context,CardBook book){global(context).edit().putString(REGISTRY,book.encode()).apply();}

 /** The requested card, the first card when {@code cardId} is unset, or null when none exists. */
 public static CardBook.Card card(Context context,int cardId){
  CardBook book=book(context);
  if(book.isEmpty())return null;
  CardBook.Card card=book.get(cardId);
  return card!=null?card:book.at(0);
 }

 /** The card's plan, with the cap the card's own settings screen holds. */
 public static RewardPlan plan(Context context,int cardId){
  SharedPreferences p=prefs(context,cardId);
  RewardPlan stored=RewardPlan.read(p.getString("plan",""),RewardPlan.hsbcLivePlus());
  long cap=p.getLong("cap",stored.capAmount);
  return cap==stored.capAmount?stored:stored.withCap(cap);
 }
 public static void savePlan(Context context,int cardId,RewardPlan plan){
  prefs(context,cardId).edit().putString("plan",plan.toJson()).putLong("cap",plan.capAmount).apply();
 }
 /** True when the card's own suggestions may come from upstream's HSBC merchant catalogue, which
  * returns that plan's category numbers. Rates and caps may differ; the categories may not. */
 public static boolean usesBuiltInCategories(RewardPlan plan){
  return plan.matchesBuiltInCategories();
 }

 public static CardBook.Card create(Context context,String name,String issuer,String senders,RewardPlan plan,String last4,int statementDay,boolean capture){
  CardBook book=book(context);
  CardBook.Card card=book.add(name,issuer,senders,book.accentFor(book.size()),capture);
  save(context,book);
  prefs(context,card.id).edit()
   .putString("plan",plan.toJson())
   .putLong("cap",plan.capAmount)
   .putString("last4",last4==null?"":last4)
   .putInt("day",statementDay)
   .putBoolean("periodSet",last4!=null&&last4.matches("[0-9]{4}"))
   .apply();
  return card;
 }
 /** Removes the card, its ledger and its settings. The caller confirms first. */
 public static void delete(Context context,int cardId){
  CardBook book=book(context);
  book.remove(cardId);
  save(context,book);
  try{
   SharedPreferences p=prefs(context,cardId);
   p.edit().clear().commit();
   context.deleteDatabase(CardBook.database(cardId));
   context.deleteSharedPreferences(CardBook.preferences(cardId));
  }catch(RuntimeException ignored){}
 }

 /** Cards that capture should try, in grid order. */
 public static List<Alerts.Target> targets(Context context){
  List<Alerts.Target> out=new ArrayList<>();
  for(CardBook.Card card:book(context).cards()){
   if(!card.capture)continue;
   Alerts.Target target=new Alerts.Target(card.id,prefs(context,card.id).getString("last4",""),card.issuer,card.senderList());
   if(target.ready())out.add(target);
  }
  return out;
 }
 /** True when at least one card can actually capture, so the global switch has something to do. */
 public static boolean anyReady(Context context){return !targets(context).isEmpty();}

 /**
  * Creates the first card from a single-card install, and moves device-wide settings into the
  * global file. Runs once; afterwards the registry exists and this returns immediately.
  */
 static void ensure(Context context){
  SharedPreferences g=global(context);
  if(g.contains(REGISTRY))return;
  SharedPreferences first=Store.prefs(context,CardBook.preferences(1));
  SharedPreferences.Editor shared=g.edit();
  for(String key:SHARED){
   Object value=first.getAll().get(key);
   if(value instanceof String)shared.putString(key,(String)value);
   else if(value instanceof Boolean)shared.putBoolean(key,(Boolean)value);
   else if(value instanceof Long)shared.putLong(key,(Long)value);
   else if(value instanceof Integer)shared.putInt(key,(Integer)value);
  }
  Object packages=first.getAll().get("packages");
  if(packages instanceof Set){
   Set<String> copy=new HashSet<>();
   for(Object entry:(Set<?>)packages)copy.add(String.valueOf(entry));
   shared.putStringSet("packages",copy);
  }
  CardBook book=new CardBook();
  if(!first.getString("last4","").isEmpty()){
   book.add("HSBC Live+","HSBC","",CardBook.ACCENTS[0],first.getBoolean("capture",false));
   if(first.getString("plan","").isEmpty())
    first.edit().putString("plan",RewardPlan.hsbcLivePlus().toJson()).apply();
  }
  shared.putString(REGISTRY,book.encode()).apply();
 }
}
