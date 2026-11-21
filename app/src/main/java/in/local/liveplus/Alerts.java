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
 /** True when this card's bank and sender gates accept the alert, before any parsing. */
 public static boolean accepts(String context,String body,Target target){
  if(target==null||!target.ready())return false;
  String haystack=fold((context==null?"":context)+" "+(body==null?"":body));
  return haystack.contains(fold(target.issuer))&&senderMatches(fold(context==null?"":context),target.senders);
 }

 /** One incoming SMS, matched to a card. */
 public static final class SmsMatch {
  public final int cardId; public final SmsCapture.Candidate candidate;
  SmsMatch(int cardId,SmsCapture.Candidate candidate){this.cardId=cardId;this.candidate=candidate;}
 }
 /**
  * Routes one newly delivered SMS. Multipart assembly, timestamp checks and the replay
  * fingerprint stay in {@link SmsCapture}; this only decides whose card it is.
  */
 public static SmsMatch routeSms(List<SmsCapture.Part> parts,long started,long received,int subscription,List<Target> targets){
  if(parts==null||parts.isEmpty()||targets==null)return null;
  SmsCapture.Part first=parts.get(0);
  if(first==null)return null;
  StringBuilder body=new StringBuilder();
  for(SmsCapture.Part part:parts){
   if(part==null)return null;
   body.append(part.body).append(' ');
   if(body.length()>8000)break;
  }
  for(Target target:targets){
   if(!accepts(first.sender,body.toString(),target))continue;
   SmsCapture.Candidate candidate=SmsCapture.collect(withGate(parts,target.issuer),target.last4,started,received,subscription);
   if(candidate!=null)return new SmsMatch(target.cardId,candidate);
  }
  return null;
 }
 /** The same parts with the bank token added to the sender, so upstream's own gate passes. */
 public static List<SmsCapture.Part> withGate(List<SmsCapture.Part> parts,String issuer){
  List<SmsCapture.Part> out=new ArrayList<>();
  for(SmsCapture.Part part:parts)out.add(part==null?null:new SmsCapture.Part(gate(part.sender,issuer),part.body,part.sent));
  return out;
 }
 /** The same notification with the bank token added to its title. The replay cache key and the
  * event fingerprint are built from the source, the notification key and the body, so adding to
  * the title changes neither. */
 public static NotificationCapture.Envelope withGate(NotificationCapture.Envelope envelope,String issuer){
  NotificationCapture.Envelope out=new NotificationCapture.Envelope();
  out.source=envelope.source;out.key=envelope.key;out.title=gate(envelope.title,issuer);
  out.bigText=envelope.bigText;out.text=envelope.text;out.posted=envelope.posted;
  out.summary=envelope.summary;out.redacted=envelope.redacted;
  out.messages.addAll(envelope.messages);out.lines.addAll(envelope.lines);
  return out;
 }
 /** True when the notification's own text names this card's bank. The conversation title, the
  * posting app and each message's sender make up the sender side; everything else is the body. */
 public static boolean accepts(NotificationCapture.Envelope envelope,Target target){
  StringBuilder context=new StringBuilder(envelope.title).append(' ').append(envelope.source);
  StringBuilder body=new StringBuilder(envelope.bigText).append(' ').append(envelope.text);
  for(NotificationCapture.Message message:envelope.messages){
   context.append(' ').append(message.sender);
   body.append(' ').append(message.text);
  }
  for(String line:envelope.lines)body.append(' ').append(line);
  return accepts(context.toString(),body.toString(),target);
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
