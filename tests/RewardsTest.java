import in.local.liveplus.Engine;
import in.local.liveplus.RewardPlan;
import in.local.liveplus.Rewards;
import in.local.liveplus.Rows;
import java.time.LocalDate;
import java.util.*;

public class RewardsTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static final LocalDate START=LocalDate.of(2026,11,1),END=LocalDate.of(2026,11,30);

 static Engine.Tx tx(long id,long amount,int category,String date){
  Engine.Tx x=new Engine.Tx();x.id=id;x.amount=amount;x.category=category;x.confirmed=category!=0;
  x.merchant="Merchant "+id;x.date=date;return x;
 }
 static Engine.Tx refund(long id,long amount,String date,String linked){
  Engine.Tx x=tx(id,amount,0,date);x.refund=true;x.linked=linked;x.confirmed=!linked.isEmpty();return x;
 }

 /** Every field the Overview screen reads must match upstream's own calculation. */
 static void parity(List<Engine.Tx> rows,long cap,String name){
  Engine.Totals expected=Engine.totals(rows,START,END,cap);
  Rewards.Totals actual=Rewards.totals(rows,START,END,RewardPlan.hsbcLivePlus().withCap(cap));
  check(expected.totalSpend==actual.totalSpend,name+": total spend");
  check(expected.accelerated==actual.cappedSpend,name+": accelerated spend");
  check(expected.other==actual.baseSpend,name+": base spend");
  check(expected.excluded==actual.excludedSpend,name+": excluded spend");
  check(expected.unknown==actual.unknownSpend,name+": unclassified spend");
  check(expected.refunds==actual.refunds,name+": unlinked refunds");
  check(expected.acceleratedCashback==actual.cappedReward,name+": accelerated cashback");
  check(expected.otherCashback==actual.baseReward,name+": base cashback");
  check(expected.cashback==actual.reward,name+": total cashback");
  check(expected.remaining==actual.remainingSpend,name+": remaining eligible spend");
  check(expected.review==actual.review,name+": review count");
 }

 static Rows.Row credit(long id,long amount,int category,boolean confirmed,String date){
  Rows.Row row=new Rows.Row();row.id=id;row.amount=amount;row.category=category;row.confirmed=confirmed;
  row.credit=true;row.merchant="Credited to card";row.date=date;return row;
 }
 static Rows.Row purchaseOn(long id,long amount,int category,String date){
  Rows.Row row=new Rows.Row();row.id=id;row.amount=amount;row.category=category;row.confirmed=true;
  row.merchant="Merchant "+id;row.date=date;return row;
 }
 public static int run(){
  RewardPlan hsbc=RewardPlan.hsbcLivePlus();

  // Hand-written cases first, so a failure points at a specific rule.
  parity(Collections.<Engine.Tx>emptyList(),120000,"empty ledger");
  parity(Arrays.asList(tx(1,460700,1,"2026-11-05")),120000,"one accelerated purchase");
  parity(Arrays.asList(tx(1,100,5,"2026-11-05")),120000,"base rate on an exact half paisa");
  parity(Arrays.asList(tx(1,4999,5,"2026-11-05")),120000,"base rate rounds a fraction");
  parity(Arrays.asList(tx(1,49000,5,"2026-11-05")),120000,"base rate on a clean multiple");
  parity(Arrays.asList(tx(1,9999,1,"2026-11-05")),120000,"accelerated rate truncates");
  parity(Arrays.asList(tx(1,2000000,2,"2026-11-05")),120000,"accelerated cashback stops at the cap");
  parity(Arrays.asList(tx(1,100,1,"2026-10-31"),tx(2,100,1,"2026-12-01")),120000,"purchases outside the period");
  parity(Arrays.asList(tx(1,5000,0,"2026-11-05")),120000,"an unclassified purchase");
  parity(Arrays.asList(tx(1,50000,3,"2026-11-05"),refund(2,20000,"2026-11-09","1")),120000,"a linked partial refund");
  parity(Arrays.asList(tx(1,50000,3,"2026-11-05"),refund(2,50000,"2026-11-09","1")),120000,"a linked full refund");
  parity(Arrays.asList(refund(1,30000,"2026-11-09","")),120000,"an unlinked refund");
  Engine.Tx duplicate=tx(1,70000,2,"2026-11-05");duplicate.duplicate=true;
  parity(Arrays.asList(duplicate,tx(2,70000,2,"2026-11-05")),120000,"a possible duplicate");
  Engine.Tx inactive=tx(1,80000,1,"2026-11-05");inactive.active=false;
  parity(Arrays.asList(inactive),120000,"a deleted purchase");
  parity(Arrays.asList(tx(1,30000,1,"2026-11-05"),tx(2,30000,2,"2026-11-06"),tx(3,30000,3,"2026-11-07"),tx(4,30000,4,"2026-11-08")),120000,
   "four accelerated categories round once, not four times");

  // The screenshot's own figures.
  Rewards.Totals shown=Rewards.totals(Arrays.asList(tx(1,460700,1,"2026-11-05"),tx(2,49000,5,"2026-11-06")),START,END,hsbc);
  check(shown.cappedReward==46070,"₹4,607.00 at 10% earns ₹460.70");
  check(shown.baseReward==735,"₹490.00 at 1.5% earns ₹7.35");
  check(shown.reward==46805,"the period total is ₹468.05");
  check(shown.remainingSpend==739300,"₹7,393.00 of eligible spend is left at 10%");
  check(shown.totalSpend==509700,"total spend is ₹5,097.00");

  // Several categories sharing a rate keep the generic wording; a lone category is named.
  check(shown.lines.size()==3,"the HSBC plan renders three buckets");
  check(shown.lines.get(0).label.equals("Eligible spend at 10%"),"four categories at one rate keep upstream's label");
  check(shown.lines.get(1).label.equals("Other eligible · 1.5%"),"a lone category is named after itself");
  check(shown.lines.get(2).label.equals("Excluded · 0%"),"the excluded category is named after itself");
  check(shown.lines.get(0).capped&&!shown.lines.get(1).capped,"only the accelerated bucket is capped");

  // Randomised ledgers: the real guard against an upstream rule change drifting from the preset.
  Random random=new Random(20261121L);
  for(int round=0;round<400;round++){
   List<Engine.Tx> rows=new ArrayList<>();
   int count=random.nextInt(14);
   for(int i=1;i<=count;i++){
    LocalDate date=START.plusDays(random.nextInt(36)-3);
    Engine.Tx x=tx(i,1+random.nextInt(900000),random.nextInt(7),date.toString());
    x.confirmed=random.nextBoolean();
    x.duplicate=random.nextInt(8)==0;
    x.active=random.nextInt(10)!=0;
    if(random.nextInt(6)==0){
     x.refund=true;x.category=0;
     x.linked=i>1&&random.nextBoolean()?String.valueOf(1+random.nextInt(i-1)):"";
     // A refund may not exceed what remains of the purchase it points at.
     if(!x.linked.isEmpty())for(Engine.Tx original:rows)if(String.valueOf(original.id).equals(x.linked))x.amount=Math.max(1,original.amount/2);
    }
    rows.add(x);
   }
   long cap=new long[]{0,1,120000,500000,100000000L}[random.nextInt(5)];
   parity(rows,cap,"random ledger "+round);
  }

  // Plans that are not HSBC Live+.
  RewardPlan flat=RewardPlan.flatCashback();
  Rewards.Totals flatTotals=Rewards.totals(Arrays.asList(tx(1,100000,1,"2026-11-05"),tx(2,100000,2,"2026-11-06")),START,END,flat);
  check(flatTotals.baseSpend==100000,"a two-category plan only pays on its own categories");
  check(flatTotals.excludedSpend==100000,"the zero-rate category collects excluded spend");
  check(flatTotals.reward==1000,"a flat 1% pays on base spend");
  check(flatTotals.remainingSpend==-1&&flatTotals.remainingReward==-1,"an uncapped plan states no headroom");

  RewardPlan spendCap=RewardPlan.fromJson("{\"id\":\"c\",\"name\":\"Spend cap\",\"unit\":\"cashback\","
   +"\"cap\":{\"basis\":\"spend\",\"amountPaise\":1000000},"
   +"\"categories\":[{\"name\":\"Accelerated\",\"rateBps\":500,\"capped\":true},{\"name\":\"Excluded\",\"rateBps\":0}]}");
  Rewards.Totals capped=Rewards.totals(Arrays.asList(tx(1,1500000,1,"2026-11-05")),START,END,spendCap);
  check(capped.cappedSpend==1500000,"a spend cap still reports the full spend");
  check(capped.cappedReward==50000,"a spend cap pays only on the qualifying part");
  check(capped.remainingSpend==0,"a spent-out spend cap leaves no headroom");
  check(Rewards.totals(Arrays.asList(tx(1,400000,1,"2026-11-05")),START,END,spendCap).remainingSpend==600000,"a spend cap reports what is left");

  RewardPlan twoRates=RewardPlan.fromJson("{\"id\":\"c\",\"name\":\"Two rates\",\"unit\":\"cashback\","
   +"\"cap\":{\"basis\":\"reward\",\"amountPaise\":50000},"
   +"\"categories\":[{\"name\":\"Dining\",\"rateBps\":500,\"capped\":true},{\"name\":\"Travel\",\"rateBps\":200,\"capped\":true},{\"name\":\"Excluded\",\"rateBps\":0}]}");
  Rewards.Totals mixed=Rewards.totals(Arrays.asList(tx(1,100000,1,"2026-11-05"),tx(2,100000,2,"2026-11-06")),START,END,twoRates);
  check(mixed.cappedReward==7000,"two capped rates are paid at their own rates");
  check(mixed.remainingSpend==-1,"two capped rates cannot state headroom as one spend figure");
  check(mixed.remainingReward==43000,"two capped rates state headroom as reward left");

  // A tier with its own ceiling stops there without touching the others.
  RewardPlan swiggy=RewardPlan.swiggyHdfc();
  Rewards.Totals big=Rewards.totals(Arrays.asList(tx(1,3000000,1,"2026-11-05"),tx(2,3000000,2,"2026-11-06"),tx(3,100000,3,"2026-11-07")),START,END,swiggy);
  check(big.lines.get(0).reward==150000,"the 10% tier stops at its own ₹1,500 ceiling");
  check(big.lines.get(1).reward==150000,"the 5% tier has its own ceiling, not a shared one");
  check(big.lines.get(2).reward==1000,"the 1% tier is uncapped");
  check(big.reward==301000,"ceilings apply per tier and then add up");
  check(Rewards.totals(Arrays.asList(tx(1,100000,1,"2026-11-05")),START,END,swiggy).reward==10000,
   "spend below the ceiling earns the full rate");

  // Every tier of a plan like Amazon ICICI used to read "Other eligible spend"; each is named now.
  Rewards.Totals tiers=Rewards.totals(Collections.<Engine.Tx>emptyList(),START,END,RewardPlan.amazonPayIcici());
  check(tiers.lines.get(0).label.equals("Amazon (Prime) · 5%"),"the 5% tier is named");
  check(tiers.lines.get(1).label.equals("Amazon Pay partners · 2%"),"the 2% tier is named");
  check(tiers.lines.get(2).label.equals("Other eligible · 1%"),"the 1% tier is named");

  // Credits: a payment touches nothing; a credit with a category takes that category's reward back.
  RewardPlan amazon=RewardPlan.amazonPayIcici();
  Rows.Row bought=new Rows.Row();bought.id=1;bought.amount=100000;bought.category=1;bought.confirmed=true;bought.merchant="AMAZON";bought.date="2026-11-05";
  Rows.Row back=credit(2,40000,1,true,"2026-11-09");
  Rows.Row bill=credit(3,500000,0,true,"2026-11-10");
  Rewards.Totals reversedTotals=Rewards.totals(Arrays.<Engine.Tx>asList(bought,back,bill),START,END,amazon);
  check(reversedTotals.lines.get(0).spend==60000,"a reversal comes off its category's spend");
  check(reversedTotals.lines.get(0).reward==3000,"and its reward: 5% of ₹600, not of ₹1,000");
  check(reversedTotals.reward==3000,"the period's reward is net of what was taken back");
  check(reversedTotals.reversed==40000,"the reversed amount is reported");
  check(reversedTotals.paid==500000,"a bill payment is reported apart");
  check(reversedTotals.totalSpend==100000,"what was spent stays what was spent");
  check(reversedTotals.netSpend==60000,"net spend is spent less reversed, not less payments");
  check(reversedTotals.review==0,"a reversal you chose needs no review");

  Rewards.Totals onlyBill=Rewards.totals(Arrays.<Engine.Tx>asList(bought,bill),START,END,amazon);
  check(onlyBill.reward==5000&&onlyBill.netSpend==100000,"a payment changes neither reward nor spend");

  Rewards.Totals suggested=Rewards.totals(Arrays.<Engine.Tx>asList(bought,credit(2,40000,1,false,"2026-11-09")),START,END,amazon);
  check(suggested.review==1,"a suggested reversal asks for review");
  check(suggested.reward==3000,"a suggested reversal still counts, as a suggested category does");

  Rewards.Totals clawback=Rewards.totals(Arrays.<Engine.Tx>asList(credit(4,50000,2,true,"2026-11-09")),START,END,amazon);
  check(clawback.lines.get(1).spend==-50000,"a reversal with nothing to reverse in the period leaves the tier negative");
  check(clawback.reward==-1000,"and takes the reward back, as a statement would");
  check(clawback.netSpend==-50000,"net spend goes below zero with it");

  Rewards.Totals outside=Rewards.totals(Arrays.<Engine.Tx>asList(bought,credit(5,40000,1,true,"2026-12-03")),START,END,amazon);
  check(outside.reward==5000&&outside.reversed==0,"a reversal belongs to the period it posts in");

  Rows.Row deletedCredit=credit(6,40000,1,true,"2026-11-09");deletedCredit.active=false;
  check(Rewards.totals(Arrays.<Engine.Tx>asList(bought,deletedCredit),START,END,amazon).reward==5000,"a deleted reversal takes nothing back");

  Rows.Row stale=credit(7,40000,9,true,"2026-11-09");
  Rewards.Totals staleTotals=Rewards.totals(Arrays.<Engine.Tx>asList(bought,stale),START,END,amazon);
  check(staleTotals.reward==5000&&staleTotals.paid==40000,"a credit naming a category the plan no longer has counts as a payment");

  // HSBC's shared cap still applies after a reversal.
  Rows.Row heavy=new Rows.Row();heavy.id=1;heavy.amount=2000000;heavy.category=1;heavy.confirmed=true;heavy.merchant="SWIGGY";heavy.date="2026-11-05";
  Rewards.Totals afterReversal=Rewards.totals(Arrays.<Engine.Tx>asList(heavy,credit(2,1000000,1,true,"2026-11-09")),START,END,hsbc);
  check(afterReversal.cappedReward==100000,"after a reversal the capped tier earns on what is left, under the cap");

  // Matching a credit to the purchase it probably reverses.
  List<Engine.Tx> history=new ArrayList<>();
  history.add(purchaseOn(1,226310,1,"2026-11-15"));
  history.add(purchaseOn(2,99900,2,"2026-11-16"));
  check(Rows.matchCategory(history,226310,"2026-11-19")==1,"a credit matching an earlier purchase takes its category");
  check(Rows.matchCategory(history,226311,"2026-11-19")==0,"a different amount matches nothing");
  check(Rows.matchCategory(history,226310,"2026-11-14")==0,"a purchase after the credit is not what it reversed");
  check(Rows.matchCategory(history,226310,"2027-02-01")==0,"a purchase more than 60 days back is not matched");
  history.add(purchaseOn(3,226310,3,"2026-11-17"));
  check(Rows.matchCategory(history,226310,"2026-11-19")==3,"the latest matching purchase wins");
  Rows.Row gone=purchaseOn(4,555500,2,"2026-11-18");gone.active=false;history.add(gone);
  check(Rows.matchCategory(history,555500,"2026-11-19")==0,"a deleted purchase is not matched");
  Rows.Row uncategorised=purchaseOn(5,777700,0,"2026-11-18");history.add(uncategorised);
  check(Rows.matchCategory(history,777700,"2026-11-19")==0,"a purchase with no category has nothing to lend");
  history.add(credit(6,888800,2,true,"2026-11-18"));
  check(Rows.matchCategory(history,888800,"2026-11-19")==0,"an earlier credit is not taken for a purchase");
  check(Rows.matchCategory(history,226310,"not a date")==0,"a malformed date matches nothing");

  // Review counts include credits that want a look, and only those.
  List<Engine.Tx> credits=Arrays.<Engine.Tx>asList(credit(1,100,1,false,"2026-11-05"),credit(2,100,1,true,"2026-11-05"),credit(3,100,0,true,"2026-11-05"));
  check(Rows.reviewCount(credits,START,END)==1,"only the suggested reversal is counted for review");
  check(!Rows.needsReview(credit(9,100,0,true,"2026-11-05")),"a payment never needs review");

  RewardPlan points=RewardPlan.rewardPoints();
  Rewards.Totals earned=Rewards.totals(Arrays.asList(tx(1,160000,1,"2026-11-05")),START,END,points);
  check(earned.reward==100,"points accrue on whole spend blocks only");
  check(earned.valuePaise(points)==2500,"points convert at the plan's stated value");
  check(Rewards.totals(Arrays.asList(tx(1,14999,1,"2026-11-05")),START,END,points).reward==0,"a part block earns nothing");
  return checks;
 }
 public static void main(String[] args){System.out.println("RewardsTest: "+run()+" checks");}
}
