package in.local.cards;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/** Pure Java parsing and calculations; no network or Android dependencies. */
public final class Engine {
 public static final String[] CATEGORIES={"Needs category","Dining / food delivery · 10%","Groceries · 10%","Shopping · 10%","Utilities · 10%","Other eligible · 1.5%","Excluded · 0%"};
 public static long money(String s) { return new BigDecimal(s.replace(",", "").trim()).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact(); }
 public static class Tx {
  public long id, amount, received; public String merchant="",date="",source="manual",event="",linked=""; public int category=0; public boolean confirmed=false,refund=false,duplicate=false,active=true;
 }
 public static class Parsed { public long amount; public String merchant,date; public boolean refund,dateFromAlert; }
 static final Pattern AMOUNT=Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![0-9.])");
 /**
  * A card suffix after a marker: "ending 1234", "no. 1234", "XX1234", "**1234", or a single
  * "x1234" as HDFC writes it. An x only counts as a mask when no letter precedes it, so the X
  * ending a word such as "MAX" is never read as one.
  */
 private static final Pattern CARD_MARKED=Pattern.compile("(?i)(?:card|cc)\\b[^.\\n]{0,50}?(?:ending(?:\\s+with|\\s+in)?|end|[*•]+|(?<![A-Za-z])[xX][xX*•]*|no\\.?|number)[:\\s.xX*•-]*([0-9]{4})(?![0-9])");
 /** A suffix straight after the word, as in HDFC's "Card 1234" and "BLOCK CC 1234". */
 private static final Pattern CARD_BARE=Pattern.compile("(?i)\\b(?:card|cc)(?:\\s+(?:no\\.?|number|num))?[\\s:#-]+([0-9]{4})(?![0-9])");
 /** True when the text names a card ending in {@code last4}, in any of the wordings banks use. */
 static boolean namesCard(String text,String last4){
  for(Pattern pattern:new Pattern[]{CARD_MARKED,CARD_BARE}){
   Matcher found=pattern.matcher(text);
   while(found.find())if(found.group(1).equals(last4))return true;
  }
  return false;
 }
 private static final Pattern DATE=Pattern.compile("(?i)\\b([0-9]{1,2}[-/](?:[0-9]{1,2}|[A-Z]{3})[-/][0-9]{2,4})\\b");
 /** HDFC dates its alerts year first, often with the time run on: "2026-11-13:00:30:59". */
 private static final Pattern ISO_DATE=Pattern.compile("\\b(20[0-9]{2}-[0-9]{2}-[0-9]{2})(?![0-9])");
 private static final String[] DATE_FORMATS={"d/M/uuuu","d-M-uuuu","d/M/uu","d-M-uu","d-MMM-uuuu","d-MMM-uu"};
 /**
  * The date an alert states, or null when it states none that is plausible: a date no later than
  * the day after the message arrived, and no earlier than 2020.
  */
 static LocalDate statedDate(String body,LocalDate received){
  Matcher d=DATE.matcher(body);
  if(d.find())for(String fmt:DATE_FORMATS)try{
   LocalDate date=LocalDate.parse(d.group(1),new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(fmt).toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT));
   if(!date.isAfter(received.plusDays(1))&&date.getYear()>=2020)return date;
  }catch(Exception ignored){}
  Matcher iso=ISO_DATE.matcher(body);
  if(iso.find())try{
   LocalDate date=LocalDate.parse(iso.group(1),new DateTimeFormatterBuilder().appendPattern("uuuu-MM-dd").toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT));
   if(!date.isAfter(received.plusDays(1))&&date.getYear()>=2020)return date;
  }catch(Exception ignored){}
  return null;
 }
 public static Parsed parse(String context,String body,String last4,long time) {
  if(!last4.matches("[0-9]{4}") || body==null || body.length()>6000) return null;
  String all=context+" "+body, lower=all.toLowerCase(Locale.ROOT);
  if(!lower.contains("hsbc") || !lower.contains("card")) return null;
  if(Pattern.compile("(?i)\\b(otp|one.time.password|verification.code|declined|failed|unsuccessful|attempt|blocked|cancelled)\\b").matcher(all).find()) return null;
  if(!namesCard(all,last4)) return null;
  boolean refund=Pattern.compile("(?i)\\b(refund(?:ed)?|reversal|reversed)\\b").matcher(body).find();
  if(!refund && Pattern.compile("(?i)(\\bcredited\\b|payment.*received|amount due|statement|bill due)").matcher(body).find()) return null;
  if(!refund && !Pattern.compile("(?i)\\b(spent|used|purchase|debited|transaction|txn|charged)\\b").matcher(body).find()) return null;
  Matcher a=AMOUNT.matcher(body); if(!a.find()) return null;
  Parsed p=new Parsed(); try {p.amount=money(a.group(1));}catch(Exception e){return null;}
  if(p.amount<=0 || p.amount>1000000000L) return null;
  p.refund=refund; p.merchant="Unknown merchant";
  Matcher m=Pattern.compile("(?i)\\b(?:at|towards|merchant[: ]+)\\s+(.+?)(?=\\s+(?:on|dated|for INR|using|avl|available|not you|if not|ref(?:erence)?|txn|transaction)\\b|[.;\\n]|$)").matcher(body);
  if(m.find()) p.merchant=m.group(1).trim().replaceAll("[\\s,]+$", "");
  if(p.merchant.length()>90) p.merchant=p.merchant.substring(0,90);
  LocalDate received=Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
  LocalDate stated=statedDate(body,received);
  p.date=(stated==null?received:stated).toString();p.dateFromAlert=stated!=null;
  return p;
 }
 public static String key(String merchant) { return merchant.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," "); }
 private static String hintKey(String merchant) { return key(merchant).replaceAll("[^a-z0-9]+"," ").trim().replaceAll("\\s+"," "); }
 private static class MerchantHints {
  final int category; final String[] names; final Pattern pattern;
  MerchantHints(int category,String... names){
   this.category=category;this.names=names;List<String> alternatives=new ArrayList<>();
   for(String name:names)alternatives.add(Pattern.quote(hintKey(name)));
   pattern=Pattern.compile("(?<![a-z])(?:"+String.join("|",alternatives)+")(?![a-z])");
  }
  boolean matches(String name){return pattern.matcher(name).find();}
 }
 private static final MerchantHints EXCLUDED_HINTS=new MerchantHints(6,
  "petrol","fuel","insurance","LIC of India","Life Insurance Corporation","HDFC Life","SBI Life","ICICI Prudential","Max Life","Bajaj Allianz","ICICI Lombard","Star Health","rent payment","wallet load","Indian Oil","IndianOil","Bharat Petroleum","Hindustan Petroleum","BPCL","HPCL");
 private static final MerchantHints MYNTRA_HINTS=new MerchantHints(5,"Myntra");
 private static final MerchantHints BASE_HINTS=new MerchantHints(5,"Amazon","Flipkart");
 // Specific grocery aliases must run before broad dining aliases such as Swiggy.
 private static final MerchantHints[] CATEGORY_HINTS={
  new MerchantHints(2,"Blinkit","Blink Commerce","BlinkCommerce","Grofers","Instamart","BigBasket","Big Basket","Zepto","KiranaKart","Kirana Kart","Licious","Delightful Gourmet","FreshToHome","Fresh to Home","Country Delight","DMart","D Mart","Avenue Supermarts","supermarket","grocer","grocery","groceries"),
  new MerchantHints(1,"Swiggy","Zomato","restaurant","restaurants","cafe","coffee","Domino","Dominos","Domino's","Jubilant Foodworks","McDonald","McDonalds","McDonald's","Hardcastle Restaurants","Westlife Foodworld","Burger King","Pizza Hut","KFC","Sapphire Foods","Devyani International","Starbucks","Subway","Barbeque Nation","Haldiram","Haldirams","Bikanervala","Wow Momo","Rebel Foods","Faasos","Behrouz","EatClub","Box8","Mojo Pizza","Chaayos","Chai Point"),
  new MerchantHints(3,"Croma","Infiniti Retail","Reliance Digital","Vijay Sales","Decathlon","Shoppers Stop","Pantaloons","Westside","Uniqlo","H&M","Hennes Mauritz","Max Fashion","Lifestyle Stores","Nykaa","FSN E Commerce"),
  new MerchantHints(4,"Airtel","Bharti Airtel","Reliance Jio","Jio Infocomm","Jio Fiber","JioFiber","Vodafone Idea","BSNL","MTNL","ACT Fibernet","Atria Convergence","Tata Play","Tata Sky","Hathway","You Broadband","BESCOM","MSEDCL","Tata Power","Adani Electricity","BSES","TANGEDCO","TNEB","Torrent Power","CESC")
 };
 public static Map<String,Integer> builtInMerchants(){
  Map<String,Integer> names=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  for(MerchantHints hints:new MerchantHints[]{EXCLUDED_HINTS,MYNTRA_HINTS,BASE_HINTS})for(String name:hints.names)names.put(name,hints.category);
  for(MerchantHints hints:CATEGORY_HINTS)for(String name:hints.names)names.put(name,hints.category);
  return names;
 }
 public static int suggest(String merchant, LocalDate date,boolean myntraPromo) {
  String s=hintKey(merchant);
  if(EXCLUDED_HINTS.matches(s))return 6;
  if(MYNTRA_HINTS.matches(s))return myntraPromo&&!date.isBefore(LocalDate.of(2026,7,31))&&!date.isAfter(LocalDate.of(2026,10,31))?3:5;
  if(BASE_HINTS.matches(s))return 5;
  // A payment-bank descriptor alone does not establish a telecom purchase.
  if(s.contains("payments bank")||s.contains("payment bank"))return 0;
  for(MerchantHints hints:CATEGORY_HINTS)if(hints.matches(s))return hints.category;
  return 0;
 }
 public static int category(String merchant,LocalDate date,boolean myntraPromo,int remembered){
  return remembered>0&&remembered<CATEGORIES.length?remembered:suggest(merchant,date,myntraPromo);
 }
 public static LocalDate cycleStart(LocalDate date,int statementDay) {
  if(statementDay==0) return date.withDayOfMonth(1);
  YearMonth m=YearMonth.from(date); LocalDate end=m.atDay(Math.min(statementDay,m.lengthOfMonth()));
  if(date.isAfter(end))return end.plusDays(1);
  YearMonth prev=m.minusMonths(1);return prev.atDay(Math.min(statementDay,prev.lengthOfMonth())).plusDays(1);
 }
 public static LocalDate cycleEnd(LocalDate start,int statementDay) {
  if(statementDay==0)return YearMonth.from(start).atEndOfMonth();
  YearMonth m=YearMonth.from(start);LocalDate end=m.atDay(Math.min(statementDay,m.lengthOfMonth()));
  if(end.isBefore(start)){m=m.plusMonths(1);end=m.atDay(Math.min(statementDay,m.lengthOfMonth()));}return end;
 }
 public static boolean needsReview(Tx x) { return x.refund?x.linked.isEmpty():!x.confirmed||x.duplicate||x.category==0; }
 public static int reviewCount(List<Tx> rows,LocalDate start,LocalDate end) {
  int count=0;
  for(Tx x:rows){
   if(!x.active||!needsReview(x))continue;
   LocalDate date=LocalDate.parse(x.date);
   if(!date.isBefore(start)&&!date.isAfter(end))count++;
  }
  return count;
 }
 public static class Totals {public long totalSpend,accelerated,other,excluded,unknown,refunds,acceleratedCashback,otherCashback,cashback,remaining; public int review;}
 public static Totals totals(List<Tx> rows,LocalDate start,LocalDate end,long cap) {
  Totals t=new Totals();
  for(Tx x:rows){ if(!x.active)continue;LocalDate d=LocalDate.parse(x.date);if(d.isBefore(start)||d.isAfter(end))continue;
   if(x.refund){if(x.linked.isEmpty()){t.refunds+=x.amount;t.review++;}continue;}
   long net=x.amount;
   for(Tx r:rows)if(r.active&&r.refund&&r.linked.equals(String.valueOf(x.id)))net-=r.amount;
   net=Math.max(0,net);
   if(!x.duplicate)t.totalSpend+=net;
   if(needsReview(x))t.review++;
   if(x.duplicate||x.category==0){t.unknown+=net;continue;}
   if(x.category>=1&&x.category<=4)t.accelerated+=net;
   else if(x.category==5)t.other+=net;else t.excluded+=net;
  }
  t.acceleratedCashback=Math.min(cap,t.accelerated/10);
  t.otherCashback=Math.round(t.other*0.015);
  t.cashback=t.acceleratedCashback+t.otherCashback;t.remaining=Math.max(0,cap*10-t.accelerated);return t;
 }
}
