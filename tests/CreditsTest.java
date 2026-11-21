import in.local.cards.Alerts;
import in.local.cards.Credits;
import in.local.cards.Engine;
import java.time.*;
import java.util.*;

public class CreditsTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static final long NOW=LocalDate.of(2026,11,21).atTime(12,0).atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
 static Credits.Posted read(String body){return Credits.parse("AD-ICICIB",body,"1001",NOW);}

 public static int run(){
  // The wordings a bank uses for money arriving on the card.
  Credits.Posted payment=read("Dear Customer, your payment of INR 5,000.00 towards ICICI Bank Credit Card XX1001 has been received on 21-Nov-26.");
  check(payment!=null,"a payment towards the card is read");
  check(payment.amount==500000,"the payment amount is read");
  check(payment.date.equals("2026-11-21"),"the payment date is read");
  check(payment.note.equals("Payment received"),"a payment is labelled as one");

  Credits.Posted received=read("Payment of Rs.2500.00 received on your Credit Card ending 1001. Thank you.");
  check(received!=null&&received.amount==250000,"\"payment received\" is read");

  Credits.Posted credited=read("INR 1,500.75 credited to your Credit Card XX1001 on 20-Nov-26.");
  check(credited!=null&&credited.amount==150075,"\"credited to your card\" is read");
  check(credited.date.equals("2026-11-20"),"the alert's own date wins over the arrival time");
  check(credited.note.equals("Credited to card"),"a bare credit is labelled plainly");

  Credits.Posted cashback=read("Cashback of INR 250.00 has been credited to your Credit Card XX1001.");
  check(cashback!=null&&cashback.note.equals("Cashback credited"),"a cashback credit is labelled as one");

  // What is not a credit.
  check(read("INR 500.00 spent using ICICI Bank Card XX1001 on 21-Nov-26 on AMAZON PAY IN R.")==null,"spending is not a credit");
  check(read("Refund of INR 500 credited to your Credit Card XX1001 at AMAZON")==null,"a refund is left to upstream, which links it to its purchase");
  check(read("OTP 445566 for the payment of INR 500 credited to Card XX1001")==null,"an OTP is rejected");
  check(read("Payment of INR 500 received on your Credit Card ending 9999")==null,"another card's payment is ignored");
  check(read("Your Credit Card XX1001 statement is ready. Amount due INR 5,000.")==null,"a statement notice is not a credit");
  check(read("Your Credit Card XX1001 payment of INR 500 was declined")==null,"a declined payment is rejected");
  check(Credits.parse("AD-ICICIB","INR 500 credited to your Credit Card XX1001","12",NOW)==null,"a card with no four digits captures nothing");
  check(Credits.parse("AD-ICICIB",null,"1001",NOW)==null,"no text is handled");

  // Routing: a credit reaches the card and is marked as one.
  List<Alerts.Target> cards=Arrays.asList(new Alerts.Target(3,"1001","ICICI",Collections.<String>emptyList()));
  Alerts.Match match=Alerts.route("AD-ICICIB","Payment of INR 5,000.00 received on your ICICI Bank Credit Card XX1001.",NOW,cards);
  check(match!=null&&match.cardId==3,"a credit routes to its card");
  check(match.credit,"the match is marked as a credit");
  check(match.transaction.amount==500000&&!match.transaction.refund,"the credit carries its amount and is not a refund");

  Alerts.Match spend=Alerts.route("AD-ICICIB","INR 50.00 spent using ICICI Bank Card XX1001 on 21-Nov-26 on AMAZON PAY IN R.",NOW,cards);
  check(spend!=null&&!spend.credit,"a purchase is still a purchase");

  // Through the incoming-SMS path, which assembles and fingerprints the message.
  Alerts.SmsMatch sms=Alerts.routeSms(Arrays.asList(new in.local.cards.SmsCapture.Part("AD-ICICIB","Payment of INR 1,000.00 received on your ICICI Bank Credit Card XX1001.",NOW)),NOW-1000,NOW,-1,cards);
  check(sms!=null&&sms.credit&&sms.candidate.transaction.amount==100000,"a credit arrives through the SMS path");
  check(!sms.candidate.event.isEmpty(),"a credit carries a replay fingerprint");
  Alerts.SmsMatch same=Alerts.routeSms(Arrays.asList(new in.local.cards.SmsCapture.Part("AD-ICICIB","Payment of INR 1,000.00 received on your ICICI Bank Credit Card XX1001.",NOW)),NOW-1000,NOW,-1,cards);
  check(sms.candidate.event.equals(same.candidate.event),"the same message fingerprints the same way, so it is not added twice");
  check(Alerts.routeSms(Arrays.asList(new in.local.cards.SmsCapture.Part("AD-ICICIB","Payment of INR 1,000.00 received on your ICICI Bank Credit Card XX1001.",NOW)),NOW+9000,NOW,-1,cards)==null,
   "a credit older than the capture start is ignored");
  return checks;
 }
 public static void main(String[] args){System.out.println("CreditsTest: "+run()+" checks");}
}
