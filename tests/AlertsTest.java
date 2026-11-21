import in.local.cards.Alerts;
import in.local.cards.Engine;
import in.local.cards.NotificationCapture;
import in.local.cards.SmsCapture;
import java.time.*;
import java.util.*;

public class AlertsTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static final long NOW=LocalDate.of(2026,11,21).atTime(12,0).atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();

 static Alerts.Target target(int id,String last4,String issuer,String... senders){
  return new Alerts.Target(id,last4,issuer,Arrays.asList(senders));
 }
 static final Alerts.Target HSBC=target(1,"1234","HSBC");
 static final Alerts.Target AXIS=target(2,"5678","Axis");
 static final Alerts.Target ICICI=target(3,"1234","ICICI");
 static final List<Alerts.Target> CARDS=Arrays.asList(HSBC,AXIS,ICICI);

 static Alerts.Match route(String context,String body){return Alerts.route(context,body,NOW,CARDS);}

 public static int run(){
  Alerts.Match hsbc=route("AX-HSBCBK","INR 1,234.50 spent on your HSBC Credit Card ending 1234 at SWIGGY on 20/11/2026. Avl limit INR 40,000.");
  check(hsbc!=null&&hsbc.cardId==1,"an HSBC alert reaches the HSBC card");
  check(hsbc.transaction.amount==123450&&hsbc.transaction.merchant.equals("SWIGGY"),"Engine's parser still extracts the purchase");
  check(hsbc.transaction.date.equals("2026-11-20"),"the alert's own date is kept");

  // A second bank, parsed by the same Engine parser through the card's issuer gate.
  Alerts.Match axis=route("AD-AXISBK","INR 2,500.00 spent on Axis Bank Credit Card no. XX5678 at BLINKIT on 21-11-2026");
  check(axis!=null&&axis.cardId==2,"an Axis alert reaches the Axis card");
  check(axis.transaction.amount==250000&&axis.transaction.merchant.equals("BLINKIT"),"a non-HSBC purchase parses in full");

  check(route("AD-AXISBK","INR 2,500.00 spent on Axis Bank Credit Card no. XX1234 at BLINKIT")==null,
   "an Axis alert for a suffix the Axis card does not use is ignored");
  check(route("VM-HDFCBK","INR 900 spent on HDFC Bank Card ending 1234 at CAFE")==null,
   "a bank with no card configured is ignored");

  // Two cards can share a suffix; the issuer gate separates them and the grid order breaks a tie.
  Alerts.Match icici=route("AD-ICICIB","INR 700.00 spent on ICICI Bank Credit Card XX1234 at ZEPTO");
  check(icici!=null&&icici.cardId==3,"a shared suffix routes by bank, not by digits");
  check(Alerts.routeAll("AD-ICICIB","INR 700 spent on ICICI Bank Credit Card XX1234 at ZEPTO",NOW,CARDS).size()==1,
   "only the bank that matches claims the alert");
  List<Alerts.Match> both=Alerts.routeAll("HSBC and ICICI","INR 700 spent on HSBC ICICI Card ending 1234 at ZEPTO",NOW,CARDS);
  check(both.size()==2&&both.get(0).cardId==1,"when two cards both match, the earlier card wins");

  // Engine's own rejections must survive routing.
  check(route("AX-HSBCBK","OTP 123456 for a transaction of INR 500 on HSBC card ending 1234")==null,"an OTP is still rejected");
  check(route("AD-AXISBK","OTP 998877 is your Axis Bank one time password for card XX5678")==null,"another bank's OTP is rejected too");
  check(route("AD-AXISBK","Transaction of INR 500 on Axis Bank card XX5678 was declined")==null,"a declined purchase is rejected");
  // A repayment used to be thrown away. It is now recorded as money paid to the card.
  Alerts.Match repaid=route("AD-AXISBK","Payment of INR 5000 received for Axis Bank card XX5678. Thank you.");
  check(repaid!=null&&repaid.cardId==2&&repaid.credit,"a repayment is recorded as a credit, not discarded");
  check(repaid.transaction.amount==500000,"the repayment keeps its amount");
  check(route("AD-AXISBK","Statement for Axis Bank card XX5678: amount due INR 5000")==null,"a statement notice is rejected");
  Alerts.Match refund=route("AD-AXISBK","Refund of INR 300 credited to Axis Bank Credit Card XX5678 at BLINKIT on 21-11-2026");
  check(refund!=null&&refund.transaction.refund&&refund.cardId==2,"a refund routes and is marked as one");

  // The issuer gate is what decides the bank; it is never satisfied by the trick that satisfies
  // Engine's own gate.
  check(Alerts.gate("AD-AXISBK","Axis").toLowerCase(Locale.ROOT).contains("hsbc"),"the parser's own gate is satisfied for any bank");
  check(route("Unknown sender","INR 500 spent on Credit Card ending 1234 at CAFE")==null,"an alert naming no bank is ignored");
  // Engine's gate reads the sender alongside the body, and this one does the same.
  Alerts.Match bySender=route("AD-AXISBK","INR 500 spent on Credit Card ending 5678 at CAFE");
  check(bySender!=null&&bySender.cardId==2,"the sender id alone can name the bank");

  // Two cards at the same bank, told apart by their digits alone: a RuPay and a Visa card on the
  // same account write identical alerts except for the suffix.
  List<Alerts.Target> twins=Arrays.asList(target(7,"1111","Federal"),target(8,"2222","Federal"));
  Alerts.Match rupay=Alerts.route("AD-FEDBNK","INR 300.00 spent on Federal Bank RuPay Credit Card XX1111 via UPI at ZEPTO",NOW,twins);
  Alerts.Match visa=Alerts.route("AD-FEDBNK","INR 300.00 spent on Federal Bank Credit Card XX2222 at ZEPTO",NOW,twins);
  check(rupay!=null&&rupay.cardId==7,"the first card takes the alert with its own digits");
  check(visa!=null&&visa.cardId==8,"the second card takes the alert with its own digits");
  check(Alerts.routeAll("AD-FEDBNK","INR 300.00 spent on Federal Bank Credit Card XX2222 at ZEPTO",NOW,twins).size()==1,
   "one bank with two cards claims an alert once");
  check(Alerts.route("AD-FEDBNK","INR 300.00 spent on Federal Bank Credit Card XX3333 at ZEPTO",NOW,twins)==null,
   "a third card at the same bank is not captured by either");

  // Sender filtering, for a bank whose name is too generic on its own.
  List<Alerts.Target> filtered=Arrays.asList(target(4,"4321","Bank","AD-ONEBNK"));
  check(Alerts.route("AD-ONEBNK","INR 100 spent on Bank Credit Card ending 4321 at CAFE",NOW,filtered)!=null,"a listed sender is accepted");
  check(Alerts.route("AD-OTHER","INR 100 spent on Bank Credit Card ending 4321 at CAFE",NOW,filtered)==null,"an unlisted sender is ignored");

  // Cards that are not ready for capture never claim anything.
  check(Alerts.route("AX-HSBCBK","INR 500 spent on HSBC Credit Card ending 1234 at CAFE",NOW,
   Arrays.asList(target(5,"","HSBC")))==null,"a card with no digits captures nothing");
  check(Alerts.route("AX-HSBCBK","INR 500 spent on HSBC Credit Card ending 1234 at CAFE",NOW,
   Arrays.asList(target(6,"1234","")))==null,"a card with no bank captures nothing");
  check(Alerts.route("AX-HSBCBK","INR 500 spent on HSBC Credit Card ending 1234 at CAFE",NOW,
   Collections.<Alerts.Target>emptyList())==null,"no cards means no capture");
  check(Alerts.route(null,null,NOW,CARDS)==null,"missing text is handled");

  // Merchant wording that Engine's parser does not read, because HSBC does not use it.
  Alerts.Match realIcici=route("AD-ICICIT","INR 50.00 spent using ICICI Bank Card XX1234 on 21-Nov-26 on AMAZON PAY IN R. Avl Limit: INR 5,97,445.42. If not you, call 1800 2662/SMS BLOCK 1234 to 9215676766.");
  check(realIcici!=null,"the ICICI alert from the phone parses");
  check(realIcici.transaction.merchant.equals("AMAZON PAY IN R"),"\"on <date> on <merchant>\" reads the merchant");
  check(realIcici.transaction.amount==5000&&realIcici.transaction.date.equals("2026-11-21"),"amount and date still come from Engine's parser");

  Alerts.Match axisLine=route("AD-AXISBK","Spent Card no. XX5678 INR 250.00 21-11-26 19:42:11 BLINKIT Avl Lmt INR 98,000");
  check(axisLine!=null&&axisLine.transaction.merchant.equals("BLINKIT"),"a merchant between the time and the available limit is read");
  Alerts.Match dated=route("AD-AXISBK","INR 99.00 spent on Axis Bank Card XX5678 21-Nov-26 ZEPTO MARKETPLACE Avl Lmt INR 10,000");
  check(dated!=null&&dated.transaction.merchant.equals("ZEPTO MARKETPLACE"),"a merchant between the date and the available limit is read");

  // Engine's own reading always wins, and a fallback never invents a name.
  Alerts.Match hsbcNamed=route("AX-HSBCBK","INR 100 spent on your HSBC Credit Card ending 1234 at SWIGGY on 21/11/2026");
  check(hsbcNamed.transaction.merchant.equals("SWIGGY"),"Engine's merchant is never overridden");
  Alerts.Match bare=route("AD-AXISBK","INR 75.00 spent on Axis Bank Credit Card XX5678");
  check(bare!=null&&bare.transaction.merchant.equals("Unknown merchant"),"an alert naming no merchant stays unknown");
  Engine.Parsed digitsOnly=new Engine.Parsed();digitsOnly.merchant="Unknown merchant";
  Alerts.name(digitsOnly,"INR 10 spent on Card XX1234 on 21-Nov-26 on 1800 2662");
  check(digitsOnly.merchant.equals("Unknown merchant"),"a run of digits is not taken for a merchant name");

  // HDFC writes the suffix with a single x, or bare after "Card", and dates alerts year first.
  java.util.List<Alerts.Target> hdfc=java.util.Arrays.asList(target(9,"1234","HDFC"));
  Alerts.Match single=Alerts.route("VM-HDFCBK","Rs.6399 spent on HDFC Bank Card x1234 at RAZ*SWIGGY on 2026-11-13:00:30:59.Not U? To Block & Reissue Call 18002586161/SMS BLOCK CC 1234 to 7308080808",NOW,hdfc);
  check(single!=null&&single.cardId==9,"a single-x suffix, as in \"Card x1234\", names the card");
  check(single.transaction.amount==639900,"the HDFC amount is read");
  check(single.transaction.merchant.equals("RAZ*SWIGGY"),"the HDFC merchant is read");
  check(single.transaction.date.equals("2026-11-13"),"a year-first date with the time run on is read");
  check(in.local.cards.RewardPlan.swiggyHdfc().suggest(single.transaction.merchant)==1,"a payment-gateway prefix still lands Swiggy in the Swiggy tier");
  Alerts.Match bareSuffix=Alerts.route("VM-HDFCBK","Spent Rs.295 On HDFC Bank Card 1234 At Swiggy On 2026-11-15:00:29:13.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 1234 to 7308080808",NOW,hdfc);
  check(bareSuffix!=null&&bareSuffix.cardId==9,"a bare suffix, as in \"Card 1234\", names the card");
  check(bareSuffix.transaction.amount==29500&&bareSuffix.transaction.merchant.equals("Swiggy"),"the bare-suffix alert is read whole");
  check(bareSuffix.transaction.date.equals("2026-11-15"),"its year-first date is read");
  check(Alerts.route("VM-HDFCBK","Spent Rs.295 On HDFC Bank Card 5678 At Swiggy On 2026-11-15:00:29:13.Not You? Call 18002586161/SMS BLOCK CC 5678 to 7308080808",NOW,hdfc)==null,
   "a bare suffix for another card is not taken for this one");
  check(Alerts.route("VM-HDFCBK","Rs.1234 spent on HDFC Bank Card x5678 at SWIGGY on 2026-11-13:00:30:59",NOW,hdfc)==null,
   "an amount equal to the suffix is not taken for the card number");
  check(Alerts.route("VM-HDFCBK","Rs.250 spent on HDFC Bank Card used at MAX 1234 on 2026-11-13",NOW,hdfc)==null,
   "the X ending a word such as MAX is not read as a mask");
  check(Alerts.route("VM-HDFCBK","Rs.250 spent on HDFC Bank Card**1234 at CAFE on 2026-11-13",NOW,hdfc)!=null,
   "asterisks straight after the word still mask the suffix");
  check(Alerts.route("VM-HDFCBK","Rs.250 spent on HDFC Bank Card no 1234 at CAFE",NOW,hdfc)!=null,"\"Card no 1234\" names the card");
  check(Alerts.route("VM-HDFCBK","Rs.250 spent on HDFC Bank Card at CAFE on 2026-12-30:10:00:00",NOW,java.util.Arrays.asList(target(9,"1234","HDFC")))==null,
   "an alert naming no suffix at all is not taken");
  Alerts.Match future=Alerts.route("VM-HDFCBK","Spent Rs.295 On HDFC Bank Card 1234 At Swiggy On 2027-01-15:00:29:13",NOW,hdfc);
  check(future!=null&&future.transaction.date.equals("2026-11-21"),"an implausible future date falls back to the day the message arrived");
  java.util.List<Alerts.Target> hdfcCredit=java.util.Arrays.asList(target(9,"1234","HDFC"));
  Alerts.Match paid=Alerts.route("VM-HDFCBK","Payment of Rs.6399.00 received on your HDFC Bank Card x1234 on 2026-11-18:10:00:00. Thank you.",NOW,hdfcCredit);
  check(paid!=null&&paid.credit&&paid.transaction.date.equals("2026-11-18"),"a credit uses the same suffix and date reading");

  check(Alerts.fold("AD-AXISBK").equals("ad axisbk"),"sender ids flatten to searchable words");
  check(Alerts.fold("AD-AXISBK").contains("axis"),"a sender id still carries the bank's name");

  // Incoming SMS: assembly, timestamps and the replay fingerprint stay with SmsCapture.
  java.util.List<SmsCapture.Part> axisSms=java.util.Arrays.asList(
   new SmsCapture.Part("AD-AXISBK","INR 2,500.00 spent on Axis Bank Credit Card no. XX5678 ",NOW),
   new SmsCapture.Part("AD-AXISBK","at BLINKIT on 21-11-2026",NOW));
  Alerts.SmsMatch sms=Alerts.routeSms(axisSms,NOW-1000,NOW,-1,CARDS);
  check(sms!=null&&sms.cardId==2,"a multipart SMS routes to its card");
  check(sms.candidate.transaction.amount==250000&&sms.candidate.transaction.merchant.equals("BLINKIT"),"both parts are read as one message");
  check(!sms.candidate.event.isEmpty(),"the routed SMS still carries a replay fingerprint");
  check(Alerts.routeSms(axisSms,NOW+5000,NOW,-1,CARDS)==null,"an SMS older than the capture start is ignored");
  check(Alerts.routeSms(java.util.Arrays.asList(new SmsCapture.Part("AD-AXISBK","OTP 445566 for your Axis Bank card XX5678",NOW)),NOW-1000,NOW,-1,CARDS)==null,
   "an OTP SMS is still rejected");
  check(Alerts.routeSms(java.util.Arrays.asList(new SmsCapture.Part("VM-HDFCBK","INR 100 spent on HDFC Bank Card ending 5678 at CAFE",NOW)),NOW-1000,NOW,-1,CARDS)==null,
   "an SMS from a bank with no card is ignored");
  check(Alerts.routeSms(null,NOW-1000,NOW,-1,CARDS)==null,"no parts means no match");

  // Notifications: the bank token goes into the title, which neither the replay cache key nor the
  // event fingerprint is built from.
  NotificationCapture.Envelope envelope=new NotificationCapture.Envelope();
  envelope.source="com.example.messages";envelope.key="0|com.example.messages|1|null|10";envelope.posted=NOW;
  envelope.title="AD-AXISBK";
  envelope.bigText="INR 500.00 spent on Axis Bank Credit Card XX5678 at CAFE on 21-11-2026";
  check(Alerts.accepts(envelope,AXIS),"the Axis card accepts an Axis notification");
  check(!Alerts.accepts(envelope,HSBC),"the HSBC card ignores an Axis notification");
  check(NotificationCapture.cacheKey(Alerts.withGate(envelope,"Axis")).equals(NotificationCapture.cacheKey(envelope)),
   "gating a notification leaves its replay cache key alone");
  NotificationCapture.Batch batch=NotificationCapture.collect(Alerts.withGate(envelope,"Axis"),"5678",NOW-1000,null);
  check(batch.candidates.size()==1,"a gated notification yields one candidate");
  check(batch.candidates.get(0).transaction.amount==50000,"the candidate carries the purchase");
  check(NotificationCapture.collect(envelope,"5678",NOW-1000,null).candidates.isEmpty(),
   "without the card's bank gate Engine's parser rejects another bank outright");
  return checks;
 }
 public static void main(String[] args){System.out.println("AlertsTest: "+run()+" checks");}
}
