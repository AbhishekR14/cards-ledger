package in.local.liveplus;

import java.util.*;

/**
 * Decides which card an incoming alert belongs to.
 *
 * <p>Parsing itself stays with {@link Engine#parse}: amount, card suffix, merchant, date, refund
 * wording and the OTP, decline and repayment rejections are upstream's and are not duplicated
 * here, so upstream's parser fixes reach every card.
 *
 * <p>Upstream's parser requires the bank's own name in the text because it only ever served one
 * HSBC card. This router applies each card's own issuer gate first and then appends that token to
 * the <em>context</em> argument, which {@code Engine.parse} concatenates ahead of the body. That
 * satisfies the gate without touching the body or weakening any check — the same shape as
 * upstream's own "Paste alert", which passes a literal {@code "HSBC"} context. The card's issuer
 * gate is what actually decides the bank, so a card never claims another bank's message.
 */
public final class Alerts {
 /** A card that capture should try, as configured on the device. */
 public static final class Target {
  public final int cardId; public final String last4,issuer; public final List<String> senders;
  public Target(int cardId,String last4,String issuer,List<String> senders){
   this.cardId=cardId;this.last4=last4==null?"":last4;this.issuer=issuer==null?"":issuer;
   this.senders=senders==null?Collections.<String>emptyList():senders;
  }
  public boolean ready(){return last4.matches("[0-9]{4}")&&!issuer.trim().isEmpty();}
 }
 public static final class Match {
  public final int cardId; public final Engine.Parsed transaction;
  Match(int cardId,Engine.Parsed transaction){this.cardId=cardId;this.transaction=transaction;}
 }

 /** The one card this alert belongs to, or null. Earlier cards in the grid win a tie, so an alert
  * is never counted twice. */
 public static Match route(String context,String body,long time,List<Target> targets){
  List<Match> all=routeAll(context,body,time,targets);
  return all.isEmpty()?null:all.get(0);
 }
 /** Every card the alert could belong to, in grid order. More than one means two cards share a
  * suffix and the same bank wording; the caller keeps the first. */
 public static List<Match> routeAll(String context,String body,long time,List<Target> targets){
  List<Match> out=new ArrayList<>();
  if(targets==null||body==null)return out;
  String safeContext=context==null?"":context;
  String haystack=fold(safeContext+" "+body),senderField=fold(safeContext);
  for(Target target:targets){
   if(!target.ready())continue;
   if(!haystack.contains(fold(target.issuer)))continue;
   if(!senderMatches(senderField,target.senders))continue;
   Engine.Parsed parsed=Engine.parse(gate(safeContext,target.issuer),body,target.last4,time);
   if(parsed!=null)out.add(new Match(target.cardId,parsed));
  }
  return out;
 }
 /** The context handed to {@link Engine#parse}: the original plus the card's bank token and the
  * token upstream's gate looks for. */
 public static String gate(String context,String issuer){
  return (context==null?"":context)+" "+(issuer==null?"":issuer)+" HSBC";
 }
 private static boolean senderMatches(String senderField,List<String> senders){
  if(senders.isEmpty())return true;
  for(String sender:senders)if(!sender.trim().isEmpty()&&senderField.contains(fold(sender)))return true;
  return false;
 }
 /** Lower-cased, punctuation flattened, so "AD-AXISBK" still carries "axis". */
 public static String fold(String value){
  return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
 }
}
