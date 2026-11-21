import in.local.cards.CardBook;
import in.local.cards.Engine;
import in.local.cards.RewardPlan;
import in.local.cards.Rows;
import in.local.cards.Vault;
import java.time.LocalDate;
import java.util.*;

public class VaultTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void rejects(String json,String name){
  checks++;
  try{Vault.decode(json);}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("accepted "+name);
 }
 static Rows.Row purchase(long id,long amount,int category,String merchant){
  Rows.Row row=new Rows.Row();
  row.id=id;row.amount=amount;row.category=category;row.merchant=merchant;row.date="2026-11-05";
  row.confirmed=true;row.received=1764000000000L;row.source="SMS";row.event="e"+id;
  return row;
 }

 static Vault.Snapshot sample(){
  Vault.Snapshot snapshot=new Vault.Snapshot();
  snapshot.theme="dark";

  Vault.CardData live=new Vault.CardData();
  live.card=new CardBook.Card(1,"HSBC Live+","HSBC","",CardBook.ACCENTS[0],true);
  live.last4="4805";live.statementDay=0;live.cap=120000;live.periodSet=true;live.myntra=true;
  live.plan=RewardPlan.hsbcLivePlus().toJson();
  live.rows.add(purchase(1,460700,1,"SWIGGY"));
  Rows.Row refund=purchase(2,10000,0,"SWIGGY");refund.refund=true;refund.linked="1";
  live.rows.add(refund);
  live.rules.put("swiggy",1);
  live.rules.put("blinkit",2);

  Vault.CardData amazon=new Vault.CardData();
  amazon.card=new CardBook.Card(4,"ICICI Amazon","ICICI","AD-ICICIB",CardBook.ACCENTS[1],false);
  amazon.last4="1001";amazon.statementDay=18;amazon.cap=0;amazon.periodSet=true;
  amazon.plan=RewardPlan.amazonPayIcici().toJson();
  amazon.rows.add(purchase(1,5000,1,"AMAZON PAY IN R"));
  Rows.Row paid=purchase(2,500000,0,"Payment received");paid.credit=true;
  amazon.rows.add(paid);
  snapshot.cards.add(live);
  snapshot.cards.add(amazon);
  return snapshot;
 }

 public static int run(){
  Vault.Snapshot before=sample();
  Vault.Snapshot after=Vault.decode(Vault.encode(before));

  check(after.theme.equals("dark"),"the theme survives a backup");
  Vault.Snapshot following=sample();following.theme="system";
  check(Vault.decode(Vault.encode(following)).theme.equals("system"),"following the system survives a backup instead of turning light");
  check(Vault.decode("{\"format\":\"cards-ledger-backup-v1\",\"theme\":\"sepia\",\"cards\":[{\"id\":1}]}").theme.equals("system"),"an unknown theme falls back to following the system");
  check(Vault.decode("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1}]}").theme.equals("system"),"a backup without a theme follows the system");
  check(after.cards.size()==2,"both cards survive");
  check(after.rowCount()==4,"every entry survives");

  Vault.CardData live=after.cards.get(0),amazon=after.cards.get(1);
  check(live.card.id==1&&amazon.card.id==4,"card numbers survive, so each ledger finds its file again");
  check(live.card.name.equals("HSBC Live+")&&amazon.card.issuer.equals("ICICI"),"names and banks survive");
  check(amazon.card.senders.equals("AD-ICICIB")&&!amazon.card.capture,"sender filters and the capture toggle survive");
  check(live.last4.equals("4805")&&amazon.statementDay==18,"card settings survive");
  check(live.cap==120000&&live.myntra&&live.periodSet,"the cap and the card's own switches survive");
  check(RewardPlan.fromJson(live.plan).id.equals("hsbc-liveplus"),"the reward plan survives");
  check(RewardPlan.fromJson(amazon.plan).category(1).merchants.contains("Amazon Pay"),"a plan's merchant list survives");
  check(live.rules.size()==2&&live.rules.get("blinkit")==2,"merchant mappings survive");
  check(amazon.rules.isEmpty(),"a card with no mappings restores with none");

  Engine.Tx purchase=live.rows.get(0),refund=live.rows.get(1);
  check(purchase.id==1&&purchase.amount==460700&&purchase.merchant.equals("SWIGGY"),"a purchase survives whole");
  check(purchase.category==1&&purchase.confirmed&&purchase.source.equals("SMS")&&purchase.event.equals("e1"),"its category, source and replay id survive");
  check(refund.refund&&refund.linked.equals("1"),"a refund keeps its link to the purchase");
  check(Rows.credit(amazon.rows.get(1)),"a payment to the card is still marked as one");
  check(!Rows.credit(amazon.rows.get(0)),"a purchase is not mistaken for a payment");
  check(Vault.encode(after).equals(Vault.encode(before)),"a backup re-encodes identically");

  // Rows helpers the calculations rely on.
  check(Rows.purchases(amazon.rows).size()==1,"purchases() leaves payments out");
  check(Rows.credits(amazon.rows,LocalDate.of(2026,11,1),LocalDate.of(2026,11,30))==500000,"credits() totals what was paid in the period");
  check(Rows.credits(amazon.rows,LocalDate.of(2026,10,1),LocalDate.of(2026,10,31))==0,"a payment outside the period is not counted");
  Rows.Row deleted=purchase(9,100,0,"x");deleted.credit=true;deleted.active=false;
  check(Rows.credits(Arrays.<Engine.Tx>asList(deleted),LocalDate.of(2026,11,1),LocalDate.of(2026,11,30))==0,"a deleted payment is not counted");

  // A backup this app should not act on.
  rejects("{\"format\":\"liveplus-ledger-v1\",\"cards\":[]}","a single-card backup from the old app");
  rejects("{\"format\":\"cards-ledger-backup-v1\"}","a backup with no cards");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1},{\"id\":1}]}","two cards with the same number");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":0}]}","a card with no number");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1,\"statementDay\":45}]}","an impossible statement day");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1,\"plan\":\"{\\\"categories\\\":[]}\"}]}","a card whose plan is broken");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1,\"transactions\":[{\"id\":1,\"amountPaise\":0,\"date\":\"2026-11-05\"}]}]}","an entry with no amount");
  rejects("{\"format\":\"cards-ledger-backup-v1\",\"cards\":[{\"id\":1,\"transactions\":[{\"id\":1,\"amountPaise\":100,\"date\":\"2026-11-05\"},{\"id\":1,\"amountPaise\":100,\"date\":\"2026-11-05\"}]}]}","two entries with the same number");
  rejects("not json at all","text that is not a backup");
  return checks;
 }
 public static void main(String[] args){System.out.println("VaultTest: "+run()+" checks");}
}
