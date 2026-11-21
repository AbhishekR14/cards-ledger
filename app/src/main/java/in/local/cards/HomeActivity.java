package in.local.cards;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

/**
 * The card grid: one tile per credit card, plus a tile to add another.
 *
 * <p>Tapping a tile opens {@link MainActivity}, the ledger screen, scoped to that card.
 */
public class HomeActivity extends Activity {
 int MINT=Color.rgb(185,237,204),ON_ACCENT=Color.rgb(20,45,53),ON_TILE=Color.rgb(185,237,204),ON_NOTICE=Color.BLACK;
 int INK,MUTED,BG,SURFACE,NOTICE,CORAL,SUCCESS,TAB_BORDER;
 boolean darkMode;
 SharedPreferences g;
 LinearLayout root,body;
 final BroadcastReceiver changed=new BroadcastReceiver(){public void onReceive(Context c,Intent i){draw();}};

 @Override public void onCreate(Bundle state){
  g=Cards.global(this);
  darkMode=Palette.dark(this,g.getString("theme","system"));
  setTheme(darkMode?R.style.AppTheme_Dark:R.style.AppTheme_Light);
  super.onCreate(state);
  palette();
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  getWindow().getDecorView().setSystemUiVisibility(darkMode?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  // OnResume draws; the first draw after opening lets the tiles rise in.
  entrance=true;
 }
 boolean entrance;
 /** Opens a screen with the forward slide that its back swipe reverses. */
 void open(Intent intent){startActivity(intent);overridePendingTransition(R.anim.enter_forward,R.anim.exit_forward);}
 @Override public void onResume(){
  super.onResume();
  IntentFilter filter=new IntentFilter("in.local.cards.CHANGED");
  if(Build.VERSION.SDK_INT>=33)registerReceiver(changed,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changed,filter);
  if(darkMode!=Palette.dark(this,g.getString("theme","system"))){recreate();return;}
  draw();
 }
 @Override public void onPause(){unregisterReceiver(changed);super.onPause();}

 int themeColor(int attribute){android.util.TypedValue value=new android.util.TypedValue();getTheme().resolveAttribute(attribute,value,true);return value.data;}
 void palette(){
  Palette palette=Palette.of(this,darkMode);
  INK=palette.ink;MUTED=palette.muted;BG=palette.background;SURFACE=palette.surface;
  NOTICE=palette.notice;CORAL=palette.attention;SUCCESS=palette.success;TAB_BORDER=palette.outline;
  MINT=palette.accent;ON_ACCENT=palette.onAccent;ON_TILE=palette.onTile;ON_NOTICE=palette.onNotice;
 }
 int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+0.5f);}
 TextView text(String value,int size,int color,boolean bold){
  TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("tnum");t.setLineSpacing(dp(2),1);
  if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
  return t;
 }
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 void add(LinearLayout parent,View child){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);if(child instanceof Button){lp.topMargin=dp(5);lp.bottomMargin=dp(5);}parent.addView(child,lp);}
 void gap(LinearLayout parent,int height){View v=new View(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
 Button button(String label,View.OnClickListener click){
  Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(ON_ACCENT);b.setTextSize(14);b.setMinHeight(dp(48));
  int ripple=Color.argb(56,Color.red(ON_ACCENT),Color.green(ON_ACCENT),Color.blue(ON_ACCENT));
  b.setBackgroundTintList(null);
  b.setBackground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(ripple),background(MINT,24),background(Color.WHITE,24)));
  b.setPadding(dp(20),dp(10),dp(20),dp(10));
  b.setStateListAnimator(null);b.setElevation(0);b.setOnClickListener(click);return b;
 }
 void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
 String cash(long paise){return java.text.NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(paise/100d);}

 void draw(){
  CardBook book=Cards.book(this);
  root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),dp(12),dp(20),0);setContentView(root);
  root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(20),Math.max(dp(12),insets.getSystemWindowInsetTop())+dp(14),dp(20),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
  top.addView(text("Cards Ledger",28,INK,true),new LinearLayout.LayoutParams(0,-2,1));
  Button settings=button("Settings",v->open(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_DEVICE,true)));
  settings.setContentDescription("Device settings: capture, appearance and about");
  top.addView(settings,new LinearLayout.LayoutParams(-2,dp(44)));
  add(root,top);
  boolean live=g.getBoolean("capture",false)&&CaptureMode.access(this);
  add(root,text(live?"Capture is on · "+book.size()+(book.size()==1?" card":" cards"):"Your cards, in view.",13,live?SUCCESS:MUTED,false));
  gap(root,14);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  body=column();body.setPadding(0,0,0,dp(24));scroll.addView(body);
  if(book.isEmpty())welcome();
  grid(book);
  if(entrance){
   // Each tile rises in after the one before it, reading across the grid.
   int order=0;
   for(int i=0;i<body.getChildCount();i++){
    View child=body.getChildAt(i);
    if(child instanceof LinearLayout&&((LinearLayout)child).getOrientation()==LinearLayout.HORIZONTAL){
     LinearLayout row=(LinearLayout)child;
     for(int j=0;j<row.getChildCount();j++)Motion.rise(row.getChildAt(j),order++,dp(28));
    }else Motion.rise(child,order++,dp(28));
   }
   entrance=false;
  }
 }
 void welcome(){
  LinearLayout c=column();c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(background(NOTICE,26));
  add(c,text("Add your first card",21,ON_NOTICE,true));gap(c,6);
  add(c,text("Each card keeps its own ledger, merchant mappings and reward plan. Start with the card you already track, then add the others.\n\nRestoring a whole-app backup instead? Open Settings above and choose Restore everything.",14,ON_NOTICE,false));
  LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.bottomMargin=dp(14);
  body.addView(c,layout);
 }
 void grid(CardBook book){
  List<View> tiles=new ArrayList<>();
  for(CardBook.Card card:book.cards())tiles.add(tile(card));
  tiles.add(addTile(book));
  for(int i=0;i<tiles.size();i+=2){
   LinearLayout row=new LinearLayout(this);
   for(int column=0;column<2;column++){
    LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(0,dp(176),1);
    layout.bottomMargin=dp(12);
    if(column==1)layout.setMarginStart(dp(12));
    if(i+column<tiles.size())row.addView(tiles.get(i+column),layout);
    else row.addView(new View(this),layout);
   }
   add(body,row);
  }
 }
 View tile(final CardBook.Card card){
  SharedPreferences p=Cards.prefs(this,card.id);
  RewardPlan plan=Cards.plan(this,card.id);
  int statementDay=p.getInt("day",0);
  LocalDate today=LocalDate.now(ZoneId.of("Asia/Kolkata"));
  LocalDate start=Engine.cycleStart(today,statementDay),end=Engine.cycleEnd(start,statementDay);
  Rewards.Totals totals;
  try(Store store=Cards.store(this,card.id)){totals=Rewards.totals(store.all(),start,end,plan);}
  boolean capped=!RewardPlan.CAP_NONE.equals(plan.capBasis)&&plan.capAmount>0;
  long earned=capped?totals.cappedReward:totals.reward;
  String four=p.getString("last4","");
  LinearLayout tile=column();
  tile.setPadding(dp(16),dp(16),dp(16),dp(16));
  tile.setBackground(Palette.gradient(card.accent,dp(26)));
  tile.setForeground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(Color.argb(46,255,255,255)),null,background(Color.WHITE,26)));
  tile.setElevation(dp(2));
  Motion.pressable(tile);
  // The tile leads with what was spent this period; the reward it earned sits under it.
  String earnedText=plan.pointsUnit()?totals.reward+(totals.reward==1?" pt":" pts"):cash(earned);
  add(tile,text(card.name,17,Color.WHITE,true));
  add(tile,text(card.issuer+(four.isEmpty()?"":"  ···· "+four),12,Color.rgb(202,220,222),false));
  gap(tile,8);
  add(tile,text(cash(totals.netSpend),24,Color.WHITE,true));
  add(tile,text(totals.reversed>0?"net spent, after "+cash(totals.reversed)+" credited":"spent this period",11,Color.rgb(202,220,222),false));
  gap(tile,6);
  add(tile,text(capped?earnedText+" of "+(plan.pointsUnit()?plan.capAmount+" pts":cash(plan.capAmount)):earnedText+" earned",13,ON_TILE,true));
  if(capped){
   ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
   bar.setMax(1000);bar.setProgress((int)Math.min(1000,earned*1000/Math.max(1,plan.capAmount)));
   bar.setProgressTintList(ColorStateList.valueOf(ON_TILE));bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.argb(60,255,255,255)));
   LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(6);
   tile.addView(bar,layout);
  }
  LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.BOTTOM);
  if(totals.review>0)footer.addView(text("● "+totals.review+" to review",11,Color.rgb(255,173,144),true),new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams footerLayout=new LinearLayout.LayoutParams(-1,0,1);
  footerLayout.topMargin=dp(4);
  tile.addView(footer,footerLayout);
  tile.setOnClickListener(v->open(new Intent(this,MainActivity.class).putExtra(Cards.EXTRA_CARD,card.id)));
  tile.setOnLongClickListener(v->{manage(card);return true;});
  tile.setContentDescription(card.name+", "+card.issuer+(four.isEmpty()?"":" ending "+four)+", "
   +cash(totals.netSpend)+" net spent this period, "+earnedText+" earned"
   +(totals.review>0?", "+totals.review+" to review":"")+". Tap to open, press and hold to manage.");
  return tile;
 }
 View addTile(final CardBook book){
  LinearLayout tile=column();
  tile.setGravity(Gravity.CENTER);
  tile.setPadding(dp(16),dp(16),dp(16),dp(16));
  GradientDrawable outline=background(SURFACE,26);outline.setStroke(dp(1),TAB_BORDER);
  tile.setForeground(new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(Color.argb(40,Color.red(INK),Color.green(INK),Color.blue(INK))),null,background(Color.WHITE,26)));
  Motion.pressable(tile);
  tile.setBackground(outline);
  TextView plus=text("+",30,INK,true);plus.setGravity(Gravity.CENTER);
  add(tile,plus);
  TextView label=text(book.isEmpty()?"Add a card":"Add another card",13,MUTED,false);label.setGravity(Gravity.CENTER);
  add(tile,label);
  tile.setOnClickListener(v->addCard());
  tile.setContentDescription("Add a card");
  return tile;
 }

 void addCard(){
  LinearLayout l=column();l.setPadding(dp(24),dp(6),dp(24),dp(6));
  final EditText name=input(l,"Card name, for example Axis Ace","",1);
  final EditText issuer=input(l,"Bank name as its messages write it","",1);
  final EditText last4=input(l,"Last four digits","",2);
  last4.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
  final EditText day=input(l,"Statement closing day (1–31), or 0 for calendar month","0",2);
  add(l,text("Reward plan",13,MUTED,true));
  final List<RewardPlan> presets=RewardPlan.presets();
  String[] names=new String[presets.size()];
  for(int i=0;i<names.length;i++)names[i]=presets.get(i).name;
  final Spinner plan=new Spinner(this);
  plan.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
  add(l,plan);gap(l,8);
  add(l,text("Pick the closest one; you can edit every rate afterwards in the card's own settings.",12,MUTED,false));
  final CheckBox capture=new CheckBox(this);
  capture.setText("Add this card's alerts automatically");capture.setChecked(true);
  add(l,capture);
  ScrollView scroll=new ScrollView(this);scroll.addView(l);
  final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Add a card").setView(scroll)
   .setPositiveButton("Add",null).setNegativeButton("Cancel",null).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
   try{
    String digits=last4.getText().toString().trim();
    if(!digits.isEmpty()&&!digits.matches("[0-9]{4}")){toast("Enter all four digits, or leave them blank for now.");return;}
    int statementDay=SettingsValues.day(day.getText().toString());
    CardBook.Card card=Cards.create(this,name.getText().toString(),issuer.getText().toString(),"",
     presets.get(plan.getSelectedItemPosition()),digits,statementDay,capture.isChecked());
    dialog.dismiss();
    offerRestore(card);
   }catch(IllegalArgumentException error){toast(error.getMessage());}
  }));
  dialog.show();
 }
 void offerRestore(final CardBook.Card card){
  draw();
  new AlertDialog.Builder(this).setTitle(card.name+" added")
   .setMessage("Open it now, or restore a backup into it if you are moving from another install of this app.")
   .setNegativeButton("Open the card",(d,w)->open(new Intent(this,MainActivity.class).putExtra(Cards.EXTRA_CARD,card.id)))
   .setPositiveButton("Restore a backup",(d,w)->open(new Intent(this,MainActivity.class).putExtra(Cards.EXTRA_CARD,card.id).putExtra(MainActivity.EXTRA_RESTORE,true)))
   .show();
 }
 void manage(final CardBook.Card card){
  new AlertDialog.Builder(this).setTitle(card.name).setItems(
   new String[]{"Rename and recolour","Move earlier","Move later","Open card settings","Remove this card"},
   (d,which)->{
    if(which==0){rename(card);return;}
    if(which==1||which==2){
     CardBook book=Cards.book(this);book.move(card.id,which==1?-1:1);Cards.save(this,book);draw();return;
    }
    if(which==3){open(new Intent(this,MainActivity.class).putExtra(Cards.EXTRA_CARD,card.id).putExtra(MainActivity.EXTRA_SETTINGS,true));return;}
    confirmRemove(card);
   }).setNegativeButton("Close",null).show();
 }
 void rename(final CardBook.Card card){
  LinearLayout l=column();l.setPadding(dp(24),dp(6),dp(24),dp(6));
  final EditText name=input(l,"Card name",card.name,1);
  add(l,text("Tile colour",13,MUTED,true));
  final Spinner colour=new Spinner(this);
  String[] names=new String[CardBook.ACCENTS.length];
  int selected=0;
  for(int i=0;i<names.length;i++){
   names[i]="Colour "+(i+1);
   if(CardBook.ACCENTS[i]==card.accent)selected=i;
  }
  colour.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));
  colour.setSelection(selected);
  add(l,colour);
  ScrollView scroll=new ScrollView(this);scroll.addView(l);
  new AlertDialog.Builder(this).setTitle("Rename and recolour").setView(scroll)
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Save",(d,w)->{
    try{
     CardBook book=Cards.book(this);
     CardBook.Card current=book.get(card.id);
     if(current==null)return;
     book.replace(current.with(name.getText().toString(),current.issuer,current.senders,CardBook.ACCENTS[colour.getSelectedItemPosition()],current.capture));
     Cards.save(this,book);draw();
    }catch(IllegalArgumentException error){toast(error.getMessage());}
   }).show();
 }
 void confirmRemove(final CardBook.Card card){
  new AlertDialog.Builder(this).setTitle("Remove "+card.name+"?")
   .setMessage("This deletes that card's transactions, merchant mappings and settings on this phone. Other cards are untouched. Export a backup from the card's own settings first if you want to keep it.")
   .setNegativeButton("Cancel",null)
   .setPositiveButton("Remove",(d,w)->{Cards.delete(this,card.id);toast(card.name+" removed");draw();})
   .show();
 }
 EditText input(LinearLayout parent,String label,String value,int type){
  TextView caption=text(label,13,MUTED,false);caption.setPadding(dp(4),0,dp(4),dp(6));add(parent,caption);
  EditText field=new EditText(this);
  field.setText(value);field.setTextSize(16);field.setTextColor(INK);field.setHintTextColor(MUTED);field.setInputType(type);field.setSingleLine(true);
  add(parent,field);gap(parent,14);
  return field;
 }
}
