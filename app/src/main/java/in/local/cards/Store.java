package in.local.cards;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import org.json.*;

public final class Store extends SQLiteOpenHelper {
 public Store(Context c){super(c,"ledger.db",null,2);}
 // One ledger file per card; card 1 is "ledger.db". See Cards and CardBook.
 public Store(Context c,String name){super(c,name,null,2);}
 // Capture state, the chosen SMS app and the theme belong to the device, not to one
 // card, so the no-argument call returns the global file and the capture code reads it
 // unchanged. A card's own ledger settings come from prefs(context, CardBook.preferences(id)).
 public static SharedPreferences prefs(Context c){return c.getSharedPreferences(Cards.GLOBAL,Context.MODE_PRIVATE);}
 public static SharedPreferences prefs(Context c,String name){return c.getSharedPreferences(name,Context.MODE_PRIVATE);}
 @Override public void onCreate(SQLiteDatabase db){
  // The trailing credit column records money posted to the card rather than spent.
  db.execSQL("CREATE TABLE tx (id INTEGER PRIMARY KEY AUTOINCREMENT, amount INTEGER NOT NULL, merchant TEXT NOT NULL, date TEXT NOT NULL, category INTEGER NOT NULL, confirmed INTEGER NOT NULL, refund INTEGER NOT NULL, duplicate INTEGER NOT NULL, active INTEGER NOT NULL, received INTEGER NOT NULL, source TEXT NOT NULL, event TEXT UNIQUE, linked TEXT NOT NULL, credit INTEGER NOT NULL DEFAULT 0)");
  db.execSQL("CREATE TABLE rules (merchant TEXT PRIMARY KEY, category INTEGER NOT NULL)");
 }
 // Version 2 adds the credit column to a ledger written by version 1.
 @Override public void onUpgrade(SQLiteDatabase d,int a,int b){
  if(a<2)d.execSQL("ALTER TABLE tx ADD COLUMN credit INTEGER NOT NULL DEFAULT 0");
 }
 public synchronized List<Engine.Tx> all(){
  List<Engine.Tx> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM tx ORDER BY date DESC, id DESC",null)){
   while(c.moveToNext()){Rows.Row x=new Rows.Row();x.id=c.getLong(0);x.amount=c.getLong(1);x.merchant=c.getString(2);x.date=c.getString(3);x.category=c.getInt(4);x.confirmed=c.getInt(5)==1;x.refund=c.getInt(6)==1;x.duplicate=c.getInt(7)==1;x.active=c.getInt(8)==1;x.received=c.getLong(9);x.source=c.getString(10);x.event=c.isNull(11)?"":c.getString(11);x.linked=c.getString(12);x.credit=c.getColumnCount()>13&&c.getInt(13)==1;out.add(x);}
  }return out;
 }
 public synchronized long save(Engine.Tx x){
  ContentValues v=new ContentValues();v.put("amount",x.amount);v.put("merchant",x.merchant);v.put("date",x.date);v.put("category",x.category);v.put("confirmed",x.confirmed?1:0);v.put("refund",x.refund?1:0);v.put("duplicate",x.duplicate?1:0);v.put("active",x.active?1:0);v.put("received",x.received);v.put("source",x.source);v.put("linked",x.linked);v.put("credit",Rows.credit(x)?1:0);if(x.event.isEmpty())v.putNull("event");else v.put("event",x.event);
  if(x.id>0){getWritableDatabase().update("tx",v,"id=?",new String[]{""+x.id});return x.id;}
  long id=getWritableDatabase().insertWithOnConflict("tx",null,v,SQLiteDatabase.CONFLICT_IGNORE);if(id>0)x.id=id;return id;
 }
 public void rule(String merchant,int category){ContentValues v=new ContentValues();v.put("merchant",Engine.key(merchant));v.put("category",category);getWritableDatabase().insertWithOnConflict("rules",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
 public void forgetRule(String merchant){getWritableDatabase().delete("rules","merchant=?",new String[]{Engine.key(merchant)});}
 public int rule(String merchant){try(Cursor c=getReadableDatabase().rawQuery("SELECT category FROM rules WHERE merchant=?",new String[]{Engine.key(merchant)})){return c.moveToFirst()?c.getInt(0):-1;}}
 public Map<String,Integer> rules(){
  Map<String,Integer> out=new LinkedHashMap<>();
  try(Cursor c=getReadableDatabase().rawQuery("SELECT merchant,category FROM rules ORDER BY merchant",null)){
   while(c.moveToNext())out.put(c.getString(0),c.getInt(1));
  }
  return out;
 }
 public void saveRule(String previous,String merchant,int category){saveRule(previous,merchant,category,Engine.CATEGORIES.length);}
 // A card's reward plan can list more or fewer categories than HSBC Live+'s.
 public void saveRule(String previous,String merchant,int category,int categories){
  if(merchant.trim().isEmpty()||merchant.length()>90||category<=0||category>=categories)throw new IllegalArgumentException("Invalid merchant category");
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{rule(merchant,category);if(previous!=null&&!Engine.key(previous).equals(Engine.key(merchant)))forgetRule(previous);db.setTransactionSuccessful();}finally{db.endTransaction();}
 }
 public void categorize(Engine.Tx x,boolean myntraPromo){categorize(x,myntraPromo,true,null);}
 // The built-in merchant catalogue returns HSBC Live+ category numbers, so it only
 // applies to a plan with those categories. Otherwise the card's own saved mappings come first
 // and the plan's own merchant lists fill the gaps.
 public void categorize(Engine.Tx x,boolean myntraPromo,boolean suggest){categorize(x,myntraPromo,suggest,null);}
 public void categorize(Engine.Tx x,boolean myntraPromo,boolean suggest,RewardPlan plan){
  if(x.refund){x.category=0;x.confirmed=false;return;}
  int remembered=rule(x.merchant);
  x.category=suggest?Engine.category(x.merchant,java.time.LocalDate.parse(x.date),myntraPromo,remembered)
   :(remembered>0?remembered:0);
  if(x.category==0&&plan!=null)x.category=plan.suggest(x.merchant);
  x.confirmed=suggest?remembered>0&&remembered<Engine.CATEGORIES.length:remembered>0;
 }
 public JSONArray exportRows() throws JSONException {
  JSONArray a=new JSONArray();for(Engine.Tx x:all()){JSONObject o=new JSONObject();o.put("credit",Rows.credit(x));o.put("id",x.id);o.put("amountPaise",x.amount);o.put("merchant",x.merchant);o.put("date",x.date);o.put("category",x.category);o.put("confirmed",x.confirmed);o.put("refund",x.refund);o.put("duplicate",x.duplicate);o.put("active",x.active);o.put("received",x.received);o.put("source",x.source);o.put("event",x.event);o.put("linked",x.linked);a.put(o);}return a;
 }
 // Removing a reward category renumbers the ones after it, so stored rows follow.
 // Purchases in the removed category go back to needing one.
 public synchronized void remapCategories(int removed){
  if(removed<=0)return;
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{
   Object[] target={removed};
   db.execSQL("UPDATE tx SET category=0, confirmed=0 WHERE category=?",target);
   db.execSQL("UPDATE tx SET category=category-1 WHERE category>?",target);
   db.execSQL("DELETE FROM rules WHERE category=?",target);
   db.execSQL("UPDATE rules SET category=category-1 WHERE category>?",target);
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
 }
 // Writes rows back with the ids they were exported under, for a restore.
 public synchronized void restore(List<Engine.Tx> rows){
  SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
  try{
   for(Engine.Tx x:rows){
    ContentValues v=new ContentValues();
    v.put("id",x.id);v.put("amount",x.amount);v.put("merchant",x.merchant);v.put("date",x.date);v.put("category",x.category);
    v.put("confirmed",x.confirmed?1:0);v.put("refund",x.refund?1:0);v.put("duplicate",x.duplicate?1:0);v.put("active",x.active?1:0);
    v.put("received",x.received);v.put("source",x.source);v.put("linked",x.linked);v.put("credit",Rows.credit(x)?1:0);
    if(!x.event.isEmpty())v.put("event",x.event);
    db.insertWithOnConflict("tx",null,v,SQLiteDatabase.CONFLICT_REPLACE);
   }
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
 }
 public void wipe(){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("tx",null,null);db.delete("rules",null,null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
