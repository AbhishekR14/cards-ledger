import in.local.liveplus.Engine;
import in.local.liveplus.RewardPlan;
import java.util.*;

public class RewardPlanTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void rejects(String json,String name){
  checks++;
  try{RewardPlan.fromJson(json);}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("accepted "+name);
 }
 public static int run(){
  RewardPlan hsbc=RewardPlan.hsbcLivePlus();

  // The Live+ tile must keep showing upstream's own category wording and indices.
  check(Arrays.equals(hsbc.labels(),Engine.CATEGORIES),"the HSBC preset reproduces Engine.CATEGORIES exactly");
  check(hsbc.count()==Engine.CATEGORIES.length,"the preset has upstream's category count");
  check(hsbc.category(0)==null,"index 0 stays the unknown sentinel");
  check(hsbc.category(1).capped&&hsbc.category(4).capped,"the four accelerated categories share the cap");
  check(!hsbc.category(5).capped&&!hsbc.category(6).capped,"base and excluded spend are outside the cap");
  check(hsbc.category(7)==null,"an index past the plan reads as unknown");
  check(hsbc.singleCappedRate()!=null&&hsbc.singleCappedRate().rateBps==1000,"one capped rate, so headroom is expressible as spend");

  // Built-in merchant suggestions follow the category list, since that is what their numbers
  // mean. Editing a rate or the cap keeps them; renaming or reshaping the list drops them.
  check(hsbc.matchesBuiltInCategories(),"the preset matches upstream's categories");
  check(hsbc.withCap(50000).matchesBuiltInCategories(),"changing the cap keeps the built-in categories");
  check(RewardPlan.fromJson(hsbc.toJson()).matchesBuiltInCategories(),"a round trip keeps the built-in categories");
  check(!RewardPlan.flatCashback().matchesBuiltInCategories(),"another plan does not claim upstream's categories");
  check(!RewardPlan.rewardPoints().matchesBuiltInCategories(),"a points plan does not claim upstream's categories");
  List<RewardPlan.Category> renamed=new ArrayList<>(hsbc.categories);
  renamed.set(1,new RewardPlan.Category("Supermarkets",1000,true,false));
  check(!new RewardPlan(hsbc.id,hsbc.name,hsbc.unit,hsbc.capBasis,hsbc.capAmount,0,renamed).matchesBuiltInCategories(),
   "renaming a category drops the built-in suggestions, whatever the plan's id says");
  List<RewardPlan.Category> rerated=new ArrayList<>(hsbc.categories);
  rerated.set(0,new RewardPlan.Category("Dining / food delivery",500,true,false));
  check(new RewardPlan(hsbc.id,hsbc.name,hsbc.unit,hsbc.capBasis,hsbc.capAmount,0,rerated).matchesBuiltInCategories(),
   "changing a rate keeps the built-in suggestions");
  List<RewardPlan.Category> shortened=new ArrayList<>(hsbc.categories);
  shortened.remove(2);
  check(!new RewardPlan(hsbc.id,hsbc.name,hsbc.unit,hsbc.capBasis,hsbc.capAmount,0,shortened).matchesBuiltInCategories(),
   "removing a category drops the built-in suggestions");

  check(RewardPlan.percent(1000).equals("10%"),"whole percentages drop the decimal");
  check(RewardPlan.percent(150).equals("1.5%"),"fractional percentages keep one decimal");
  check(RewardPlan.percent(0).equals("0%"),"a zero rate reads as 0%");
  check(RewardPlan.percent(25).equals("0.25%"),"a quarter percent reads in full");
  check(RewardPlan.rupees(15000).equals("₹150"),"whole rupees drop the paise");
  check(RewardPlan.rupees(15050).equals("₹150.50"),"part rupees keep both decimals");

  RewardPlan copy=RewardPlan.fromJson(hsbc.toJson());
  check(copy.toJson().equals(hsbc.toJson()),"a plan survives a JSON round trip");
  check(Arrays.equals(copy.labels(),hsbc.labels()),"labels survive a JSON round trip");
  check(RewardPlan.fromJson(hsbc.toPrettyJson()).toJson().equals(hsbc.toJson()),"indented JSON reads back");
  check(copy.category(1).capped&&!copy.category(1).round,"the accelerated bucket keeps truncating after a round trip");

  RewardPlan points=RewardPlan.rewardPoints();
  check(points.pointsUnit(),"a points plan reports its unit");
  check(points.category(1).label().equals("Accelerated · 10 pts per ₹150"),"points categories read in the picker");
  check(RewardPlan.fromJson(points.toJson()).category(1).points==10,"points survive a round trip");

  RewardPlan capped=hsbc.withCap(500000);
  check(capped.capAmount==500000&&capped.categories.size()==hsbc.categories.size(),"a card's own cap overlays the preset");
  check(RewardPlan.preset("hsbc-liveplus").toJson().equals(hsbc.toJson()),"presets look up by id");
  check(RewardPlan.preset("nonsense").toJson().equals(RewardPlan.flatCashback().toJson()),"an unknown preset id falls back");
  check(RewardPlan.read("not json",hsbc).toJson().equals(hsbc.toJson()),"unreadable stored text falls back to the preset");
  check(RewardPlan.read("",hsbc).toJson().equals(hsbc.toJson()),"no stored text falls back to the preset");
  check(RewardPlan.read(points.toJson(),hsbc).pointsUnit(),"stored text wins over the fallback");
  for(RewardPlan preset:RewardPlan.presets())
   check(RewardPlan.fromJson(preset.toJson()).toJson().equals(preset.toJson()),"preset "+preset.id+" round trips");

  rejects("{\"categories\":[]}","a plan with no categories");
  rejects("{\"categories\":[{\"name\":\"\",\"rateBps\":100}]}","a category with no name");
  rejects("{\"categories\":[{\"name\":\"Needs category\",\"rateBps\":100}]}","the reserved sentinel name");
  rejects("{\"categories\":[{\"name\":\"A\",\"rateBps\":100},{\"name\":\"a\",\"rateBps\":200}]}","two categories with the same name");
  rejects("{\"categories\":[{\"name\":\"A\",\"points\":4,\"blockPaise\":15000,\"rateBps\":100}]}","a category earning both a percentage and points");
  rejects("{\"categories\":[{\"name\":\"A\",\"points\":4}]}","points with no spend block");
  rejects("{\"categories\":[{\"name\":\"A\",\"rateBps\":100,\"capped\":true}],\"cap\":{\"basis\":\"none\"}}","a capped category with no cap");
  rejects("{\"unit\":\"miles\",\"categories\":[{\"name\":\"A\",\"rateBps\":100}]}","an unknown reward unit");
  rejects("{\"cap\":{\"basis\":\"monthly\",\"amountPaise\":100},\"categories\":[{\"name\":\"A\",\"rateBps\":100}]}","an unknown cap basis");
  rejects("{\"cap\":{\"basis\":\"reward\",\"amountPaise\":-5},\"categories\":[{\"name\":\"A\",\"rateBps\":100}]}","a negative cap");
  StringBuilder many=new StringBuilder("{\"categories\":[");
  for(int i=0;i<RewardPlan.MAX_CATEGORIES+1;i++)many.append(i>0?",":"").append("{\"name\":\"C").append(i).append("\",\"rateBps\":100}");
  rejects(many.append("]}").toString(),"more categories than a plan holds");
  return checks;
 }
 public static void main(String[] args){System.out.println("RewardPlanTest: "+run()+" checks");}
}
