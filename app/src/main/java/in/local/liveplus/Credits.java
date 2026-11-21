package in.local.liveplus;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/**
 * Reads an alert about money posted <em>to</em> a card: a bill payment, a reversed charge, a
 * cashback credit.
 *
 * <p>{@link Engine#parse} rejects these on purpose — it was written for one card's spending — so
 * this is a separate reader rather than a change to it. A refund against a purchase is left to
 * upstream, which already records and links those; this only takes what upstream turns away.
 *
 * <p>The amount and card-suffix patterns are deliberately the same shape as upstream's. They are
 * restated here because upstream's are private and this reads a different kind of message; if
 * upstream improves its own, check whether these want the same change.
 */
public final class Credits {
 private static final Pattern AMOUNT=Pattern.compile("(?i)(?:INR|Rs\\.?|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![0-9.])");
 private static final Pattern CARD=Pattern.compile("(?i)(?:card|cc)\\b[^.\\n]{0,50}?(?:ending(?:\\s+with|\\s+in)?|end|[xX*•]{2,}|no\\.?|number)[:\\s.xX*•-]*([0-9]{4})(?![0-9])");
 private static final Pattern REJECT=Pattern.compile("(?i)\\b(otp|one.time.password|verification.code|declined|failed|unsuccessful|attempt|blocked|cancelled)\\b");
 /** Upstream owns refunds against a purchase, so they are not credits here. */
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
 private static final Pattern DATE=Pattern.compile("(?i)\\b([0-9]{1,2}[-/](?:[0-9]{1,2}|[A-Z]{3})[-/][0-9]{2,4})\\b");
 private static final String[] DATE_FORMATS={"d/M/uuuu","d-M-uuuu","d/M/uu","d-M-uu","d-MMM-uuuu","d-MMM-uu"};

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
  Matcher card=CARD.matcher(all);
  boolean matched=false;
  while(card.find())if(card.group(1).equals(last4))matched=true;
  if(!matched)return null;
  Matcher amount=AMOUNT.matcher(body);
  if(!amount.find())return null;
  Posted posted=new Posted();
  try{posted.amount=Engine.money(amount.group(1));}catch(RuntimeException ignored){return null;}
  if(posted.amount<=0||posted.amount>1000000000L)return null;
  posted.date=Instant.ofEpochMilli(time).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate().toString();
  Matcher date=DATE.matcher(body);
  if(date.find())for(String format:DATE_FORMATS)try{
   LocalDate found=LocalDate.parse(date.group(1),new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(format).toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT));
   if(!found.isAfter(LocalDate.parse(posted.date).plusDays(1))&&found.getYear()>=2020){posted.date=found.toString();break;}
  }catch(RuntimeException ignored){}
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
