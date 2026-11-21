package in.local.liveplus;

import java.time.LocalDate;
import java.util.*;

/**
 * Period totals for any {@link RewardPlan}.
 *
 * <p>Transaction selection, refund netting, duplicate handling and the review count follow
 * {@link Engine#totals} exactly; the difference is that rates, buckets and the cap come from the
 * card's plan instead of being fixed to HSBC Live+. With the {@code hsbc-liveplus} preset the two
 * agree to the paise, which {@code RewardsTest} checks against randomised ledgers.
 *
 * <p>Categories that share a rate, cap flag and rounding form one bucket, and the rate is applied
 * once to the bucket total. That is what keeps four categories at 10% rounding identically to
 * upstream's single {@code accelerated/10}.
 */
public final class Rewards {
 /** One rendered row: a bucket of categories paying at the same rate. */
 public static final class Line {
  public final String label; public final long spend,reward; public final boolean capped,paying;
  Line(String label,long spend,long reward,boolean capped,boolean paying){
   this.label=label;this.spend=spend;this.reward=reward;this.capped=capped;this.paying=paying;
  }
 }
 public static final class Totals {
  /** Spend, in paise. */
  public long totalSpend,cappedSpend,baseSpend,excludedSpend,unknownSpend,refunds;
  /** Reward, in paise for a cashback plan or in points for a points plan. */
  public long cappedReward,cappedRewardBeforeCap,baseReward,reward;
  /** Headroom. {@code -1} when the plan cannot express it. */
  public long remainingSpend=-1,remainingReward=-1;
  public long[] spendByCategory=new long[0];
  public final List<Line> lines=new ArrayList<>();
  public int review;
  /** Cashback value of {@link #reward}, in paise, for a points plan with a stated point value. */
  public long valuePaise(RewardPlan plan){
   return plan.pointsUnit()?reward*plan.pointValuePaise:reward;
  }
 }

 public static Totals totals(List<Engine.Tx> rows,LocalDate start,LocalDate end,RewardPlan plan){
  Totals t=new Totals();
  t.spendByCategory=new long[plan.count()];
  for(Engine.Tx x:rows){
   if(!x.active)continue;
   LocalDate date=LocalDate.parse(x.date);
   if(date.isBefore(start)||date.isAfter(end))continue;
   if(x.refund){if(x.linked.isEmpty()){t.refunds+=x.amount;t.review++;}continue;}
   long net=x.amount;
   for(Engine.Tx refund:rows)if(refund.active&&refund.refund&&refund.linked.equals(String.valueOf(x.id)))net-=refund.amount;
   net=Math.max(0,net);
   if(!x.duplicate)t.totalSpend+=net;
   if(Engine.needsReview(x))t.review++;
   RewardPlan.Category category=plan.category(x.category);
   if(x.duplicate||x.category==0||category==null){t.unknownSpend+=net;continue;}
   t.spendByCategory[x.category]+=net;
  }

  // Collect spend per bucket, keeping the plan's own order.
  Map<String,Long> bucketSpend=new LinkedHashMap<>();
  Map<String,RewardPlan.Category> buckets=new LinkedHashMap<>();
  for(int index=1;index<plan.count();index++){
   RewardPlan.Category category=plan.category(index);
   String bucket=category.bucket();
   buckets.put(bucket,category);
   bucketSpend.put(bucket,(bucketSpend.containsKey(bucket)?bucketSpend.get(bucket):0L)+t.spendByCategory[index]);
  }

  long capSpendLeft=RewardPlan.CAP_SPEND.equals(plan.capBasis)?plan.capAmount:Long.MAX_VALUE;
  for(Map.Entry<String,RewardPlan.Category> entry:buckets.entrySet()){
   RewardPlan.Category category=entry.getValue();
   long spend=bucketSpend.get(entry.getKey());
   boolean paying=category.rateBps>0||category.points>0;
   long earning=spend,reward;
   if(category.capped&&capSpendLeft!=Long.MAX_VALUE){earning=Math.min(spend,Math.max(0,capSpendLeft));capSpendLeft-=earning;}
   reward=earn(category,earning);
   if(category.capped){t.cappedSpend+=spend;t.cappedRewardBeforeCap+=reward;}
   else if(paying){t.baseSpend+=spend;t.baseReward+=reward;}
   else t.excludedSpend+=spend;
   t.lines.add(new Line(label(category),spend,reward,category.capped,paying));
  }

  t.cappedReward=RewardPlan.CAP_REWARD.equals(plan.capBasis)?Math.min(plan.capAmount,t.cappedRewardBeforeCap):t.cappedRewardBeforeCap;
  t.reward=t.cappedReward+t.baseReward;

  RewardPlan.Category single=plan.singleCappedRate();
  if(RewardPlan.CAP_SPEND.equals(plan.capBasis))t.remainingSpend=Math.max(0,plan.capAmount-t.cappedSpend);
  else if(RewardPlan.CAP_REWARD.equals(plan.capBasis)&&single!=null&&!single.pointsBased()&&single.rateBps>0)
   t.remainingSpend=Math.max(0,plan.capAmount*10000/single.rateBps-t.cappedSpend);
  else if(RewardPlan.CAP_REWARD.equals(plan.capBasis)&&single!=null&&single.pointsBased()&&single.points>0)
   t.remainingSpend=Math.max(0,plan.capAmount*single.blockPaise/single.points-t.cappedSpend);
  if(RewardPlan.CAP_REWARD.equals(plan.capBasis))t.remainingReward=Math.max(0,plan.capAmount-t.cappedReward);
  return t;
 }

 /**
  * The reward a bucket earns on {@code spend}, in paise or points.
  *
  * <p>Rounding mirrors upstream rather than improving on it: {@code floor} truncates, as
  * {@code Engine.totals} does for accelerated cashback with its {@code accelerated/10}, and
  * {@code round} goes through the same {@code Math.round(spend*rate)} double that upstream uses
  * for the base rate. Reproducing that exactly is what lets the Live+ tile's figures agree with
  * upstream to the paise.
  */
 public static long earn(RewardPlan.Category category,long spend){
  if(spend<=0)return 0;
  if(category.pointsBased()){
   if(category.blockPaise<=0||category.points<=0)return 0;
   return category.round?Math.round(spend*((double)category.points/category.blockPaise)):spend/category.blockPaise*category.points;
  }
  if(category.rateBps<=0)return 0;
  return category.round?Math.round(spend*(category.rateBps/10000.0)):spend*category.rateBps/10000;
 }

 /** Upstream's wording for the three HSBC buckets, generalised to any rate. */
 public static String label(RewardPlan.Category category){
  if(category.pointsBased())return "Spend earning "+category.points+(category.points==1?" pt":" pts")+" per "+RewardPlan.rupees(category.blockPaise);
  if(category.rateBps<=0)return "Excluded spend · 0%";
  if(category.capped)return "Eligible spend at "+RewardPlan.percent(category.rateBps);
  return "Other eligible spend · "+RewardPlan.percent(category.rateBps);
 }
}
