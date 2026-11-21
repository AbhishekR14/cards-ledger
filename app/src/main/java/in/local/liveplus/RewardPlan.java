package in.local.liveplus;

import java.util.*;

/**
 * One card's reward scheme: the categories it pays on, their rates, and the cap they share.
 *
 * <p>Category indices match the ledger's stored {@code category} column and upstream's convention:
 * index 0 always means "Needs category" and is never paid on, so a plan's own list starts at index
 * 1. The HSBC Live+ preset reproduces {@link Engine#CATEGORIES} and {@link Engine#totals} exactly;
 * {@code RewardsTest} asserts that, so an upstream rule change surfaces as a test failure.
 *
 * <p>Plain Java with no Android dependency, so the calculator can be unit tested.
 */
public final class RewardPlan {
 public static final String UNIT_CASHBACK="cashback",UNIT_POINTS="points";
 public static final String CAP_REWARD="reward",CAP_SPEND="spend",CAP_NONE="none";
 public static final String UNKNOWN="Needs category";
 public static final int MAX_CATEGORIES=24,MAX_RATE_BPS=10000;
 public static final long MAX_CAP=1000000000L,MAX_BLOCK=1000000000L,MAX_POINTS=1000000L;

 /** One payable category. A percent category uses {@code rateBps}; a points category uses
  * {@code points} earned per {@code blockPaise} of spend. */
 public static final class Category {
  public final String name; public final int rateBps; public final long points,blockPaise;
  public final boolean capped,round;
  /** This category's own ceiling per period, in paise or points; 0 for none. Separate from the
   * plan's shared cap, because several Indian cards cap each tier on its own. */
  public final long ceiling;
  /** Merchant names that fall in this category by default, before the card's own mappings. */
  public final List<String> merchants;
  public Category(String name,int rateBps,boolean capped,boolean round){this(name,rateBps,0,0,capped,round);}
  public Category(String name,int rateBps,long points,long blockPaise,boolean capped,boolean round){
   this(name,rateBps,points,blockPaise,capped,round,0,Collections.<String>emptyList());
  }
  public Category(String name,int rateBps,long points,long blockPaise,boolean capped,boolean round,long ceiling,List<String> merchants){
   this.name=name;this.rateBps=rateBps;this.points=points;this.blockPaise=blockPaise;this.capped=capped;this.round=round;
   this.ceiling=ceiling;this.merchants=Collections.unmodifiableList(new ArrayList<>(merchants));
  }
  public boolean pointsBased(){return blockPaise>0;}
  /** The label shown in pickers and summaries, e.g. "Groceries · 10%". */
  public String label(){
   if(pointsBased())return name+" · "+points+(points==1?" pt":" pts")+" per "+rupees(blockPaise);
   return name+" · "+percent(rateBps);
  }
  /** Groups categories that must be totalled together before their rate is applied, so that a
   * plan with four categories at one rate rounds exactly once, as upstream does. */
  String bucket(){return rateBps+"/"+points+"/"+blockPaise+"/"+(capped?"c":"u")+"/"+(round?"r":"f")+"/"+ceiling;}
 }

 public final String id,name,unit,capBasis;
 public final long capAmount,pointValuePaise;
 public final List<Category> categories;

 public RewardPlan(String id,String name,String unit,String capBasis,long capAmount,long pointValuePaise,List<Category> categories){
  this.id=id;this.name=name;this.unit=unit;this.capBasis=capBasis;this.capAmount=capAmount;
  this.pointValuePaise=pointValuePaise;this.categories=Collections.unmodifiableList(new ArrayList<>(categories));
  validate();
 }
 private void validate(){
  if(empty(id)||id.length()>40)throw new IllegalArgumentException("Give the plan an id of up to 40 characters.");
  if(empty(name)||name.length()>40)throw new IllegalArgumentException("Give the plan a name of up to 40 characters.");
  if(!UNIT_CASHBACK.equals(unit)&&!UNIT_POINTS.equals(unit))throw new IllegalArgumentException("Reward unit must be cashback or points.");
  if(!CAP_REWARD.equals(capBasis)&&!CAP_SPEND.equals(capBasis)&&!CAP_NONE.equals(capBasis))throw new IllegalArgumentException("Cap basis must be reward, spend or none.");
  if(capAmount<0||capAmount>MAX_CAP)throw new IllegalArgumentException("Enter a cap from ₹0 up to ₹1,00,00,000.");
  if(pointValuePaise<0||pointValuePaise>MAX_CAP)throw new IllegalArgumentException("Enter a realistic value for one point.");
  if(categories.isEmpty())throw new IllegalArgumentException("Add at least one reward category.");
  if(categories.size()>MAX_CATEGORIES)throw new IllegalArgumentException("A plan holds up to "+MAX_CATEGORIES+" categories.");
  Set<String> names=new HashSet<>();
  boolean anyCapped=false;
  for(Category category:categories){
   if(empty(category.name)||category.name.length()>40)throw new IllegalArgumentException("Give every category a name of up to 40 characters.");
   if(category.name.trim().equalsIgnoreCase(UNKNOWN))throw new IllegalArgumentException("\""+UNKNOWN+"\" is reserved.");
   if(!names.add(Engine.key(category.name)))throw new IllegalArgumentException("Two categories cannot share the name \""+category.name+"\".");
   if(category.rateBps<0||category.rateBps>MAX_RATE_BPS)throw new IllegalArgumentException("Enter a rate from 0% to 100%.");
   if(category.points<0||category.points>MAX_POINTS)throw new IllegalArgumentException("Enter a realistic number of points.");
   if(category.blockPaise<0||category.blockPaise>MAX_BLOCK)throw new IllegalArgumentException("Enter a realistic spend block.");
   if(category.pointsBased()&&category.rateBps!=0)throw new IllegalArgumentException("A category earns either a percentage or points, not both.");
   if(category.points>0&&category.blockPaise<=0)throw new IllegalArgumentException("Points categories need the spend block that earns them.");
   if(category.ceiling<0||category.ceiling>MAX_CAP)throw new IllegalArgumentException("Enter a category ceiling from ₹0 up to ₹1,00,00,000.");
   if(category.merchants.size()>40)throw new IllegalArgumentException("A category lists up to 40 merchants.");
   for(String merchant:category.merchants)
    if(merchant==null||merchant.trim().isEmpty()||merchant.length()>60)throw new IllegalArgumentException("Give every listed merchant a name of up to 60 characters.");
   anyCapped|=category.capped;
  }
  if(anyCapped&&CAP_NONE.equals(capBasis))throw new IllegalArgumentException("Categories share a cap, so set a cap amount.");
 }
 private static boolean empty(String value){return value==null||value.trim().isEmpty();}

 public int count(){return categories.size()+1;}
 /** Category index {@code i} as stored on a transaction; index 0 is the unknown sentinel. */
 public Category category(int index){return index>=1&&index<=categories.size()?categories.get(index-1):null;}
 /** Picker labels, index-aligned with the stored category column. */
 public String[] labels(){
  String[] out=new String[count()];out[0]=UNKNOWN;
  for(int i=0;i<categories.size();i++)out[i+1]=categories.get(i).label();
  return out;
 }
 /** True when the plan pays one single rate on everything it caps, so remaining headroom can be
  * stated as spend ("₹7,393 left at 10%") rather than only as reward left. */
 public Category singleCappedRate(){
  Category found=null;
  for(Category category:categories){
   if(!category.capped)continue;
   if(found==null){found=category;continue;}
   if(!found.bucket().equals(category.bucket()))return null;
  }
  return found;
 }
 public RewardPlan withCap(long amount){
  return new RewardPlan(id,name,unit,CAP_NONE.equals(capBasis)&&amount>0?CAP_REWARD:capBasis,amount,pointValuePaise,categories);
 }
 public RewardPlan withName(String newName){return new RewardPlan(id,newName,unit,capBasis,capAmount,pointValuePaise,categories);}
 public boolean pointsUnit(){return UNIT_POINTS.equals(unit);}
 /**
  * True when this plan's categories are still upstream's, in upstream's order, so the built-in
  * merchant catalogue's category numbers mean here what they mean there. Rates, caps and the
  * reward unit may differ: they do not change what a category <em>is</em>.
  */
 public boolean matchesBuiltInCategories(){
  List<Category> builtIn=hsbcLivePlus().categories;
  if(categories.size()!=builtIn.size())return false;
  for(int i=0;i<builtIn.size();i++)
   if(!Engine.key(categories.get(i).name).equals(Engine.key(builtIn.get(i).name)))return false;
  return true;
 }

 /**
  * The category this plan suggests for a merchant name, or 0 when it has nothing to say. The
  * longest listed name wins, so "Amazon Pay" beats a bare "Amazon" in another category. A card's
  * own saved mappings are checked first and always win.
  */
 public int suggest(String merchant){
  String folded=fold(merchant);
  if(folded.isEmpty())return 0;
  int best=0,length=0;
  for(int index=1;index<count();index++)
   for(String name:category(index).merchants){
    String token=fold(name);
    if(token.isEmpty()||token.length()<=length||!(" "+folded+" ").contains(" "+token+" "))continue;
    best=index;length=token.length();
   }
  return best;
 }
 /** Lower-cased with punctuation flattened, so "AMAZON PAY IN R" carries the words "amazon pay". */
 static String fold(String value){
  return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
 }

 public static String percent(int rateBps){
  if(rateBps%100==0)return (rateBps/100)+"%";
  String value=String.valueOf(rateBps/100.0);
  if(value.endsWith(".0"))value=value.substring(0,value.length()-2);
  return value+"%";
 }
 public static String rupees(long paise){
  java.math.BigDecimal amount=java.math.BigDecimal.valueOf(paise,2);
  return "₹"+(amount.stripTrailingZeros().scale()<=0?amount.toBigInteger().toString():amount.toPlainString());
 }

 // ---- JSON ----------------------------------------------------------------

 public Map<String,Object> encode(){
  Map<String,Object> out=new LinkedHashMap<>();
  out.put("id",id);out.put("name",name);out.put("unit",unit);
  if(pointsUnit())out.put("pointValuePaise",pointValuePaise);
  Map<String,Object> cap=new LinkedHashMap<>();
  cap.put("basis",capBasis);cap.put("amountPaise",capAmount);
  out.put("cap",cap);
  List<Object> rows=new ArrayList<>();
  for(Category category:categories){
   Map<String,Object> row=new LinkedHashMap<>();
   row.put("name",category.name);
   if(category.pointsBased()){row.put("points",category.points);row.put("blockPaise",category.blockPaise);}
   else row.put("rateBps",(long)category.rateBps);
   row.put("capped",category.capped);
   if(category.ceiling>0)row.put("ceilingPaise",category.ceiling);
   row.put("rounding",category.round?"round":"floor");
   if(!category.merchants.isEmpty())row.put("merchants",new ArrayList<Object>(category.merchants));
   rows.add(row);
  }
  out.put("categories",rows);
  return out;
 }
 public String toJson(){return Json.write(encode());}
 public String toPrettyJson(){return Json.pretty(encode());}

 public static RewardPlan fromJson(String text){
  Map<String,Object> source=Json.object(text);
  List<Category> categories=new ArrayList<>();
  List<Object> rows=Json.children(source,"categories");
  if(rows.isEmpty())throw new IllegalArgumentException("The plan lists no categories.");
  for(Object entry:rows){
   Map<String,Object> row=Json.asObject(entry);
   long points=Json.number(row,"points",0),block=Json.number(row,"blockPaise",0);
   long rate=Json.number(row,"rateBps",-1);
   if(rate<0)rate=points>0?0:0;
   List<String> merchants=new ArrayList<>();
   for(Object merchant:Json.children(row,"merchants"))if(merchant instanceof String&&!((String)merchant).trim().isEmpty())merchants.add(((String)merchant).trim());
   categories.add(new Category(
    Json.text(row,"name",""),
    (int)Math.max(0,Math.min(MAX_RATE_BPS,rate)),
    points,block,
    Json.flag(row,"capped",false),
    !"floor".equals(Json.text(row,"rounding","round")),
    Json.number(row,"ceilingPaise",0),
    merchants));
  }
  Map<String,Object> cap=Json.child(source,"cap");
  long amount=Json.number(cap,"amountPaise",0);
  String basis=Json.text(cap,"basis",amount>0?CAP_REWARD:CAP_NONE);
  return new RewardPlan(
   Json.text(source,"id","custom"),
   Json.text(source,"name","Custom plan"),
   Json.text(source,"unit",UNIT_CASHBACK),
   basis,amount,
   Json.number(source,"pointValuePaise",0),
   categories);
 }
 /** Reads a stored plan, falling back to a preset when nothing valid is saved. */
 public static RewardPlan read(String text,RewardPlan fallback){
  if(text==null||text.trim().isEmpty())return fallback;
  try{return fromJson(text);}catch(RuntimeException ignored){return fallback;}
 }

 // ---- Presets -------------------------------------------------------------

 /** Mirrors {@link Engine#CATEGORIES} and the cashback maths in {@link Engine#totals}. The four
  * accelerated categories share one bucket and truncate, exactly as upstream's {@code /10} does;
  * the base rate rounds, as upstream's {@code Math.round} does. */
 public static RewardPlan hsbcLivePlus(){
  List<Category> categories=new ArrayList<>();
  categories.add(new Category("Dining / food delivery",1000,true,false));
  categories.add(new Category("Groceries",1000,true,false));
  categories.add(new Category("Shopping",1000,true,false));
  categories.add(new Category("Utilities",1000,true,false));
  categories.add(new Category("Other eligible",150,false,true));
  categories.add(new Category("Excluded",0,false,true));
  return new RewardPlan("hsbc-liveplus","HSBC Live+",UNIT_CASHBACK,CAP_REWARD,120000,0,categories);
 }
 public static RewardPlan flatCashback(){
  List<Category> categories=new ArrayList<>();
  categories.add(new Category("All eligible spend",100,false,true));
  categories.add(new Category("Excluded",0,false,true));
  return new RewardPlan("flat-cashback","Flat cashback",UNIT_CASHBACK,CAP_NONE,0,0,categories);
 }
 public static RewardPlan acceleratedCashback(){
  List<Category> categories=new ArrayList<>();
  categories.add(new Category("Accelerated",500,true,true));
  categories.add(new Category("Other eligible",100,false,true));
  categories.add(new Category("Excluded",0,false,true));
  return new RewardPlan("accelerated-cashback","Accelerated cashback",UNIT_CASHBACK,CAP_REWARD,50000,0,categories);
 }
 public static RewardPlan rewardPoints(){
  List<Category> categories=new ArrayList<>();
  categories.add(new Category("Accelerated",0,10,15000,false,false));
  categories.add(new Category("Other eligible",0,2,15000,false,false));
  categories.add(new Category("Excluded",0,0,0,false,false));
  return new RewardPlan("reward-points","Reward points",UNIT_POINTS,CAP_NONE,0,25,categories);
 }
 private static Category percent(String name,int rateBps,long ceiling,String... merchants){
  return new Category(name,rateBps,0,0,false,true,ceiling,Arrays.asList(merchants));
 }
 private static Category points(String name,long points,long blockPaise,String... merchants){
  return new Category(name,0,points,blockPaise,false,false,0,Arrays.asList(merchants));
 }
 private static final String EXCLUDED="Excluded";
 /**
  * Plans for the cards this fork's author carries. Rates were read from the issuers' own pages on
  * 21 November 2026 and the terms move often, so a card's Settings screen is the authority: check
  * the plan against your own statement and edit it there. docs/MULTI-CARD.md records what is
  * solid and what is not.
  */
 public static RewardPlan amazonPayIcici(){
  return new RewardPlan("icici-amazon-pay","Amazon Pay ICICI",UNIT_CASHBACK,CAP_NONE,0,0,Arrays.asList(
   percent("Amazon (Prime)",500,0,"Amazon","Amazon Pay","Amazon Pay IN","Amazon In"),
   percent("Amazon Pay partners",200,0),
   percent("Other eligible",100,0),
   percent(EXCLUDED,0,0,"petrol","fuel","rent payment","Indian Oil","IndianOil","BPCL","HPCL")));
 }
 public static RewardPlan swiggyHdfc(){
  return new RewardPlan("hdfc-swiggy","Swiggy HDFC",UNIT_CASHBACK,CAP_NONE,0,0,Arrays.asList(
   percent("Swiggy app",1000,150000,"Swiggy","Instamart","Swiggy Instamart","Dineout","Swiggy Dineout","Swiggy Genie"),
   percent("Online spends",500,150000),
   percent("Other spends",100,0),
   percent(EXCLUDED,0,0,"petrol","fuel","rent payment")));
 }
 public static RewardPlan phonePeHdfcUltimo(){
  return new RewardPlan("hdfc-phonepe-ultimo","PhonePe HDFC Ultimo",UNIT_CASHBACK,CAP_NONE,0,0,Arrays.asList(
   percent("PhonePe bills and recharges",1000,100000,"PhonePe"),
   percent("Select online brands",500,50000),
   percent("UPI scan and pay",100,50000),
   percent("Other or excluded",0,0)));
 }
 public static RewardPlan axisNeo(){
  return new RewardPlan("axis-neo","Axis NEO",UNIT_POINTS,CAP_NONE,0,20,Arrays.asList(
   points("Everyday spends",1,20000),
   points("Excluded: fuel, rent, utilities",0,0,"petrol","fuel","rent payment","insurance")));
 }
 public static RewardPlan idfcFirstMillennia(){
  return new RewardPlan("idfc-first-millennia","IDFC FIRST Millennia",UNIT_POINTS,CAP_NONE,0,25,Arrays.asList(
   points("Accelerated: over ₹20,000 a month",10,20000),
   points("Everyday spends",3,20000),
   points("FASTag and railways",1,20000,"FASTag","IRCTC"),
   points(EXCLUDED,0,0,"petrol","fuel","rent payment")));
 }
 public static List<RewardPlan> presets(){
  return Arrays.asList(hsbcLivePlus(),amazonPayIcici(),swiggyHdfc(),phonePeHdfcUltimo(),axisNeo(),
   idfcFirstMillennia(),acceleratedCashback(),flatCashback(),rewardPoints());
 }
 public static RewardPlan preset(String id){
  for(RewardPlan plan:presets())if(plan.id.equals(id))return plan;
  return flatCashback();
 }
}
