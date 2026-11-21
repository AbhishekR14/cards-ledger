import in.local.liveplus.Alerts;
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
  check(hsbc.transaction.amount==123450&&hsbc.transaction.merchant.equals("SWIGGY"),"upstream's parser still extracts the purchase");
  check(hsbc.transaction.date.equals("2026-11-20"),"the alert's own date is kept");

  // A second bank, parsed by the same upstream parser through the card's issuer gate.
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

  // Upstream's own rejections must survive routing.
  check(route("AX-HSBCBK","OTP 123456 for a transaction of INR 500 on HSBC card ending 1234")==null,"an OTP is still rejected");
  check(route("AD-AXISBK","OTP 998877 is your Axis Bank one time password for card XX5678")==null,"another bank's OTP is rejected too");
  check(route("AD-AXISBK","Transaction of INR 500 on Axis Bank card XX5678 was declined")==null,"a declined purchase is rejected");
  check(route("AD-AXISBK","Payment of INR 5000 received for Axis Bank card XX5678. Thank you.")==null,"a repayment is rejected");
  check(route("AD-AXISBK","Statement for Axis Bank card XX5678: amount due INR 5000")==null,"a statement notice is rejected");
  Alerts.Match refund=route("AD-AXISBK","Refund of INR 300 credited to Axis Bank Credit Card XX5678 at BLINKIT on 21-11-2026");
  check(refund!=null&&refund.transaction.refund&&refund.cardId==2,"a refund routes and is marked as one");

  // The issuer gate is what decides the bank; it is never satisfied by the trick that satisfies
  // upstream's own gate.
  check(Alerts.gate("AD-AXISBK","Axis").toLowerCase(Locale.ROOT).contains("hsbc"),"the parser's own gate is satisfied for any bank");
  check(route("Unknown sender","INR 500 spent on Credit Card ending 1234 at CAFE")==null,"an alert naming no bank is ignored");
  // Upstream's gate reads the sender alongside the body, and this one does the same.
  Alerts.Match bySender=route("AD-AXISBK","INR 500 spent on Credit Card ending 5678 at CAFE");
  check(bySender!=null&&bySender.cardId==2,"the sender id alone can name the bank");

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

  check(Alerts.fold("AD-AXISBK").equals("ad axisbk"),"sender ids flatten to searchable words");
  check(Alerts.fold("AD-AXISBK").contains("axis"),"a sender id still carries the bank's name");
  return checks;
 }
 public static void main(String[] args){System.out.println("AlertsTest: "+run()+" checks");}
}
