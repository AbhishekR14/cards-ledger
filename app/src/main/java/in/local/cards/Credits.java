package in.local.cards;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/**
 * Reads an alert about money posted <em>to</em> a card: a bill payment, a reversed charge, a
 * cashback credit.
 *
 * <p>{@link Engine#parse} rejects these on purpose, since it reads spending, so this is a
 * separate reader rather than a change to it. A refund against a purchase is left to
 * the purchase parser, which already records and links those; this only takes what it turns away.
 *
 * <p>The amount, the card suffix and the date are read with Engine's own helpers, so every
 * wording the purchase parser understands is understood here too.
 */
public final class Credits {
 private static final Pattern REJECT=Pattern.compile("(?i)\\b(otp|one.time.password|verification.code|declined|failed|unsuccessful|attempt|blocked|cancelled)\\b");
 /** The purchase parser owns refunds against a purchase, so they are not credits here. */
 private static final Pattern REFUND=Pattern.compile("(?i)\\b(refund(?:ed)?|reversal|reversed)\\b");
 private static final Pattern SPEND=Pattern.compile("(?i)\\b(spent|debited|charged|purchase)\\b");
 /**
  * The wordings that mean money arrived. The spans allow a decimal point, because the amount sits
  * between the words ("payment of INR 5,000.00 towards your card"). "Successful" and "processed"
  * are deliberately absent: a purchase confirmation uses them too.
  */
 private static final Pattern CREDIT=Pattern.compile("(?i)(\\bcredited\\b|\\bcredit\\s+of\\b"
  +"|\\bpayment\\b[^\\n]{0,80}?\\b(?:received|towards|credited)\\b"
  +"|\\breceived\\b[^\\n]{0,60}?\\btowards\\b"
  +"|thank\\s+you\\s+for\\s+(?:your\\s+)?payment)");

 /** What a credit alert says. {@code note} is the wording shown in the ledger. */
 public static final class Posted {
  public long amount; public String date="",note="Payment or credit";
 }

 /**
  * Reads a credit for the given card, or returns null when the message is not one.
  *
  * @param context the sender or notification title
  * @param body the message itself
  * @param last4 the card's last four digits
  * @param time when the message arrived, for a message that carries no date
  */
 public static Posted parse(String context,String body,String last4,long time){
  if(last4==null||!last4.matches("[0-9]{4}")||body==null||body.length()>6000)return null;
  String all=(context==null?"":context)+" "+body;
  if(REJECT.matcher(all).find())return null;
  if(REFUND.matcher(body).find())return null;
  if(SPEND.matcher(body).find())return null;
  if(!CREDIT.matcher(body).find())return null;
  if(!Engine.namesCard(all,last4))return null;
  Matcher amount=Engine.AMOUNT.matcher(body);
  if(!amount.find())return null;
  Posted posted=new Posted();
  try{posted.amount=Engine.money(amount.group(1));}catch(RuntimeException ignored){return null;}
  if(posted.amount<=0||posted.amount>1000000000L)return null;
  LocalDate received=Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate();
  LocalDate stated=Engine.statedDate(body,received);
  posted.date=(stated==null?received:stated).toString();
  posted.note=note(body);
  return posted;
 }
 private static String note(String body){
  String lower=body.toLowerCase(Locale.ROOT);
  if(lower.contains("payment"))return "Payment received";
  if(lower.contains("cashback"))return "Cashback credited";
  if(lower.contains("interest"))return "Interest credited";
  return "Credited to card";
 }
}
