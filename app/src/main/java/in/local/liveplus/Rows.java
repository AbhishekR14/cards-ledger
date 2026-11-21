package in.local.liveplus;

import java.util.*;

/**
 * A ledger row: upstream's {@link Engine.Tx} plus what this fork records beyond a purchase.
 *
 * <p>Upstream only ever recorded spending, so its parser rejects a payment or a credit outright.
 * This fork keeps them, because a card's statement is not only what you spent. A credit is held
 * in the same table and read back as a {@link Row}, which is an {@code Engine.Tx}, so every piece
 * of upstream's code still handles it — the calculations here simply leave credits out of spend
 * and rewards.
 *
 * <p>{@code Engine.Tx} itself is upstream's and is not edited, so the extra field lives in this
 * subclass and is read through {@link #credit(Engine.Tx)}.
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
