package in.local.cards;

import java.util.*;

/**
 * A ledger row: an {@link Engine.Tx} plus what the app records beyond a purchase.
 *
 * <p>The purchase parser rejects a payment or a credit outright, because it reads spending. The
 * app keeps them anyway, since a card's statement is not only what you spent. A credit is held in
 * the same table and read back as a {@link Row}, which is an {@code Engine.Tx}, so every piece of
 * code that handles a purchase still handles it; the calculations decide what a credit counts as.
 *
 * <p>The extra field lives in this subclass and is read through {@link #credit(Engine.Tx)}.
 */
public final class Rows {
 private Rows(){}

 public static final class Row extends Engine.Tx {
  /** Money posted to the card rather than spent on it: a bill payment, a reversal, a cashback. */
  public boolean credit;
 }

 /** True when this row is money posted to the card rather than spend. */
 public static boolean credit(Engine.Tx row){return row instanceof Row&&((Row)row).credit;}

 /** A copy of {@code row} that can carry the credit flag. */
 public static Row asRow(Engine.Tx row){
  if(row instanceof Row)return (Row)row;
  Row out=new Row();
  out.id=row.id;out.amount=row.amount;out.merchant=row.merchant;out.date=row.date;out.category=row.category;
  out.confirmed=row.confirmed;out.refund=row.refund;out.duplicate=row.duplicate;out.active=row.active;
  out.received=row.received;out.source=row.source;out.event=row.event;out.linked=row.linked;
  return out;
 }
 public static Row credited(long amount,String date,String note,long received,String source,String event){
  Row row=new Row();
  row.credit=true;row.amount=amount;row.date=date;row.merchant=note;row.received=received;
  row.source=source;row.event=event;row.confirmed=true;row.category=0;
  return row;
 }

 /** Only the spending rows, for calculations and counts that upstream wrote for purchases. */
 public static List<Engine.Tx> purchases(List<Engine.Tx> rows){
  List<Engine.Tx> out=new ArrayList<>();
  for(Engine.Tx row:rows)if(!credit(row))out.add(row);
  return out;
 }
 /**
  * True when a row still wants a look. A purchase follows upstream's rule. A credit only does
  * when the category it reverses was suggested rather than chosen, because a wrong suggestion
  * would take a reward back that was never lost.
  */
 public static boolean needsReview(Engine.Tx row){
  if(credit(row))return row.category>0&&!row.confirmed;
  return Engine.needsReview(row);
 }
 public static int reviewCount(List<Engine.Tx> rows,java.time.LocalDate start,java.time.LocalDate end){
  int count=0;
  for(Engine.Tx row:rows){
   if(!row.active||!needsReview(row))continue;
   java.time.LocalDate date=java.time.LocalDate.parse(row.date);
   if(!date.isBefore(start)&&!date.isAfter(end))count++;
  }
  return count;
 }
 /** How far back a credit looks for the purchase it might be reversing. */
 public static final int MATCH_DAYS=60;
 /**
  * The category of the purchase a credit is most likely reversing: the latest active purchase of
  * exactly the same amount, dated on or before the credit and no more than {@link #MATCH_DAYS}
  * earlier. 0 when there is none, which leaves the credit as a plain payment.
  */
 public static int matchCategory(List<Engine.Tx> rows,long amount,String date){
  java.time.LocalDate when;
  try{when=java.time.LocalDate.parse(date);}catch(RuntimeException ignored){return 0;}
  Engine.Tx best=null;java.time.LocalDate bestDate=null;
  for(Engine.Tx row:rows){
   if(credit(row)||row.refund||!row.active||row.duplicate||row.category<=0||row.amount!=amount)continue;
   java.time.LocalDate bought;
   try{bought=java.time.LocalDate.parse(row.date);}catch(RuntimeException ignored){continue;}
   if(bought.isAfter(when)||bought.isBefore(when.minusDays(MATCH_DAYS)))continue;
   if(best==null||bought.isAfter(bestDate)||(bought.equals(bestDate)&&row.id>best.id)){best=row;bestDate=bought;}
  }
  return best==null?0:best.category;
 }
 /** What was posted to the card in the period, in paise. */
 public static long credits(List<Engine.Tx> rows,java.time.LocalDate start,java.time.LocalDate end){
  long total=0;
  for(Engine.Tx row:rows){
   if(!credit(row)||!row.active)continue;
   java.time.LocalDate date=java.time.LocalDate.parse(row.date);
   if(!date.isBefore(start)&&!date.isAfter(end))total+=row.amount;
  }
  return total;
 }
}
