package in.local.cards;

import java.util.*;

/**
 * One file holding everything the app knows: every card, every transaction and credit, every
 * merchant mapping, every reward plan and every card setting.
 *
 * <p>A card's own backup in its Settings screen is upstream's, one card at a time. This is the
 * whole device, so that restoring it after an uninstall leaves the app exactly as it was.
 *
 * <p>The shape and the reading of it are plain Java with no Android dependency, so the format is
 * unit tested; {@link Cards} and {@link Store} supply and receive the data around it.
 */
public final class Vault {
 public static final String FORMAT="cards-ledger-backup-v1";
 public static final int MAX_CARDS=CardBook.MAX_CARDS,MAX_ROWS=60000;
 private Vault(){}

 /** One card: its place in the grid, its settings, its ledger and its mappings. */
 public static final class CardData {
  public CardBook.Card card;
  public String plan="",last4="";
  public int statementDay;
  public long cap;
  public boolean periodSet,myntra;
  public final List<Engine.Tx> rows=new ArrayList<>();
  public final Map<String,Integer> rules=new LinkedHashMap<>();
 }
 /** Everything in the file. */
 public static final class Snapshot {
  public String theme="light";
  public final List<CardData> cards=new ArrayList<>();
  public int rowCount(){int n=0;for(CardData card:cards)n+=card.rows.size();return n;}
 }

 public static String encode(Snapshot snapshot){
  Map<String,Object> out=new LinkedHashMap<>();
  out.put("format",FORMAT);
  out.put("saved",java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")).toString());
  out.put("theme",snapshot.theme);
  List<Object> cards=new ArrayList<>();
  for(CardData data:snapshot.cards){
   Map<String,Object> card=new LinkedHashMap<>();
   card.put("id",(long)data.card.id);
   card.put("name",data.card.name);
   card.put("issuer",data.card.issuer);
   card.put("senders",data.card.senders);
   card.put("accent",(long)data.card.accent);
   card.put("capture",data.card.capture);
   card.put("last4",data.last4);
   card.put("statementDay",(long)data.statementDay);
   card.put("capPaise",data.cap);
   card.put("periodSet",data.periodSet);
   card.put("myntra",data.myntra);
   card.put("plan",data.plan);
   List<Object> rows=new ArrayList<>();
   for(Engine.Tx row:data.rows)rows.add(row(row));
   card.put("transactions",rows);
   List<Object> rules=new ArrayList<>();
   for(Map.Entry<String,Integer> rule:data.rules.entrySet()){
    Map<String,Object> entry=new LinkedHashMap<>();
    entry.put("merchant",rule.getKey());entry.put("category",(long)(int)rule.getValue());
    rules.add(entry);
   }
   card.put("merchants",rules);
   cards.add(card);
  }
  out.put("cards",cards);
  return Json.pretty(out);
 }
 private static Map<String,Object> row(Engine.Tx x){
  Map<String,Object> out=new LinkedHashMap<>();
  out.put("id",x.id);out.put("amountPaise",x.amount);out.put("merchant",x.merchant);out.put("date",x.date);
  out.put("category",(long)x.category);out.put("confirmed",x.confirmed);out.put("refund",x.refund);
  out.put("duplicate",x.duplicate);out.put("active",x.active);out.put("received",x.received);
  out.put("source",x.source);out.put("event",x.event);out.put("linked",x.linked);out.put("credit",Rows.credit(x));
  return out;
 }

 /** Reads a backup, or explains why it cannot be read. */
 public static Snapshot decode(String text){
  Map<String,Object> source=Json.object(text);
  if(!FORMAT.equals(Json.text(source,"format","")))
   throw new IllegalArgumentException("This is not a Cards Ledger backup of the whole app.");
  Snapshot snapshot=new Snapshot();
  snapshot.theme="dark".equals(Json.text(source,"theme","light"))?"dark":"light";
  List<Object> cards=Json.children(source,"cards");
  if(cards.isEmpty())throw new IllegalArgumentException("The backup holds no cards.");
  if(cards.size()>MAX_CARDS)throw new IllegalArgumentException("The backup holds more cards than this app tracks.");
  Set<Integer> ids=new HashSet<>();
  int rows=0;
  for(Object entry:cards){
   Map<String,Object> card=Json.asObject(entry);
   int id=(int)Json.number(card,"id",0);
   if(id<=0||!ids.add(id))throw new IllegalArgumentException("The backup has two cards with the same number.");
   CardData data=new CardData();
   data.card=new CardBook.Card(id,
    Json.text(card,"name","Card "+id),
    Json.text(card,"issuer",""),
    Json.text(card,"senders",""),
    (int)Json.number(card,"accent",CardBook.ACCENTS[0]),
    Json.flag(card,"capture",true));
   data.last4=Json.text(card,"last4","");
   data.statementDay=(int)Json.number(card,"statementDay",0);
   data.cap=Json.number(card,"capPaise",0);
   data.periodSet=Json.flag(card,"periodSet",false);
   data.myntra=Json.flag(card,"myntra",false);
   data.plan=Json.text(card,"plan","");
   if(data.statementDay<0||data.statementDay>31)throw new IllegalArgumentException("A card in the backup has an impossible statement day.");
   if(!data.plan.isEmpty())RewardPlan.fromJson(data.plan);
   Set<Long> rowIds=new HashSet<>();
   for(Object value:Json.children(card,"transactions")){
    if(++rows>MAX_ROWS)throw new IllegalArgumentException("The backup holds too many transactions.");
    Rows.Row row=new Rows.Row();
    Map<String,Object> stored=Json.asObject(value);
    row.id=Json.number(stored,"id",0);
    row.amount=Json.number(stored,"amountPaise",0);
    row.merchant=Json.text(stored,"merchant","");
    row.date=java.time.LocalDate.parse(Json.text(stored,"date","")).toString();
    row.category=(int)Json.number(stored,"category",0);
    row.confirmed=Json.flag(stored,"confirmed",false);
    row.refund=Json.flag(stored,"refund",false);
    row.duplicate=Json.flag(stored,"duplicate",false);
    row.active=Json.flag(stored,"active",true);
    row.received=Json.number(stored,"received",0);
    row.source=Json.text(stored,"source","manual");
    row.event=Json.text(stored,"event","");
    row.linked=Json.text(stored,"linked","");
    row.credit=Json.flag(stored,"credit",false);
    if(row.id<=0||!rowIds.add(row.id))throw new IllegalArgumentException("A card in the backup has two transactions with the same number.");
    if(row.amount<=0||row.amount>1000000000L)throw new IllegalArgumentException("A transaction in the backup has an impossible amount.");
    if(row.merchant.length()>90||row.source.length()>100)throw new IllegalArgumentException("A transaction in the backup is malformed.");
    data.rows.add(row);
   }
   for(Object value:Json.children(card,"merchants")){
    Map<String,Object> rule=Json.asObject(value);
    String merchant=Json.text(rule,"merchant","").trim();
    int category=(int)Json.number(rule,"category",0);
    if(merchant.isEmpty()||merchant.length()>90||category<=0)continue;
    data.rules.put(merchant,category);
   }
   snapshot.cards.add(data);
  }
  return snapshot;
 }
}
