package in.local.liveplus;

import android.app.AlertDialog;
import android.content.*;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

/**
 * Edits one card's {@link RewardPlan}: its categories and rates, the cap they share, and whether
 * it pays cashback or points. Plans can also be pasted in or copied out as JSON.
 *
 * <p>Drawn with {@link MainActivity}'s own helpers so it matches the rest of the app, and new to
 * this fork, so it never conflicts with an upstream change.
 */
final class PlanEditor {
 private final MainActivity host;
 PlanEditor(MainActivity host){this.host=host;}

 void open(){
  RewardPlan plan=host.plan;
  LinearLayout l=host.dialogBody();
  host.add(l,host.text("Categories are numbered in the order below; a purchase remembers its number, so renaming is safe and removing one renumbers the rest.",12,host.MUTED,false));
  host.gap(l,10);
  AlertDialog dialog=new AlertDialog.Builder(host).setTitle("Reward plan")
   .setView(host.scrolled(l)).setPositiveButton("Close",null).create();
  for(int i=0;i<plan.categories.size();i++){
   final int index=i+1;
   RewardPlan.Category category=plan.categories.get(i);
   host.add(l,host.button(index+". "+category.name+"\n"+host.rate(category)+(category.capped?" · shares the cap":""),
    v->{dialog.dismiss();editCategory(index);}));
  }
  host.gap(l,6);
  host.add(l,host.button("Add a category",v->{dialog.dismiss();editCategory(0);}));
  host.gap(l,14);
  host.add(l,host.text("Cap",13,host.MUTED,true));
  host.add(l,host.text(RewardPlan.CAP_NONE.equals(plan.capBasis)?"No cap"
   :host.reward(plan.capAmount)+(RewardPlan.CAP_SPEND.equals(plan.capBasis)?" of qualifying spend":" of rewards")+" per period",14,host.INK,false));
  host.add(l,host.button("Edit cap and reward unit",v->{dialog.dismiss();editCap();}));
  host.gap(l,14);
  host.add(l,host.button("Start from a preset",v->{dialog.dismiss();choosePreset();}));
  host.add(l,host.button("Plan as JSON",v->{dialog.dismiss();editJson();}));
  dialog.show();
 }

 private void editCategory(final int index){
  RewardPlan plan=host.plan;
  final RewardPlan.Category existing=index>0?plan.category(index):null;
  LinearLayout l=host.dialogBody();
  final EditText name=host.input(l,"Category name",existing==null?"":existing.name,1);
  host.add(l,host.text("Earns",13,host.MUTED,true));
  final Spinner kind=new Spinner(host);
  kind.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,new String[]{"A percentage of the spend","Points per block of spend"}));
  kind.setSelection(existing!=null&&existing.pointsBased()?1:0);
  host.add(l,kind);host.gap(l,10);
  final EditText rate=host.input(l,"Percentage, for example 10 or 1.5",existing==null||existing.pointsBased()?"":RewardPlan.percent(existing.rateBps).replace("%",""),8194);
  final EditText points=host.input(l,"Points earned",existing!=null&&existing.pointsBased()?String.valueOf(existing.points):"",2);
  final EditText block=host.input(l,"Per how many ₹ of spend",existing!=null&&existing.pointsBased()?host.BigDecimalValue(existing.blockPaise):"",8194);
  final CheckBox capped=new CheckBox(host);
  capped.setText("Shares this card's cap");capped.setChecked(existing!=null&&existing.capped);
  host.add(l,capped);
  final CheckBox down=new CheckBox(host);
  down.setText("Round down rather than to the nearest");down.setChecked(existing!=null&&!existing.round);
  host.add(l,down);
  host.add(l,host.text("Rounding applies once to all categories paying this same rate, as the bank's own statement does.",12,host.MUTED,false));
  AlertDialog dialog=new AlertDialog.Builder(host).setTitle(existing==null?"Add a category":"Edit category "+index)
   .setView(host.scrolled(l)).setPositiveButton("Save",null).setNegativeButton("Cancel",(d,w)->open())
   .setNeutralButton(existing==null?null:"Remove",null).create();
  dialog.setOnShowListener(d->{
   dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
    try{
     boolean pointsBased=kind.getSelectedItemPosition()==1;
     RewardPlan.Category edited=new RewardPlan.Category(name.getText().toString().trim(),
      pointsBased?0:bps(rate.getText().toString()),
      pointsBased?whole(points.getText().toString()):0,
      pointsBased?Engine.money(block.getText().toString().trim()):0,
      capped.isChecked(),!down.isChecked());
     List<RewardPlan.Category> categories=new ArrayList<>(host.plan.categories);
     if(existing==null)categories.add(edited);else categories.set(index-1,edited);
     save(rebuild(host.plan,categories));
     dialog.dismiss();open();
    }catch(IllegalArgumentException error){host.toast(error.getMessage());
    }catch(RuntimeException error){host.toast("Enter a name and a rate for this category.");}
   });
   if(existing!=null)dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->new AlertDialog.Builder(host)
    .setTitle("Remove this category?")
    .setMessage("Purchases in \""+existing.name+"\" go back to needing a category, and the categories after it move up one place.")
    .setNegativeButton("Cancel",null)
    .setPositiveButton("Remove",(a,b)->{
     List<RewardPlan.Category> categories=new ArrayList<>(host.plan.categories);
     categories.remove(index-1);
     if(categories.isEmpty()){host.toast("A plan keeps at least one category.");return;}
     try{
      RewardPlan next=rebuild(host.plan,categories);
      host.store.remapCategories(index);
      save(next);
     }catch(IllegalArgumentException error){host.toast(error.getMessage());return;}
     dialog.dismiss();open();
    }).show());
  });
  dialog.show();
 }

 private void editCap(){
  RewardPlan plan=host.plan;
  LinearLayout l=host.dialogBody();
  host.add(l,host.text("This card pays in",13,host.MUTED,true));
  final Spinner unit=new Spinner(host);
  unit.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,new String[]{"Cashback","Reward points"}));
  unit.setSelection(plan.pointsUnit()?1:0);
  host.add(l,unit);host.gap(l,10);
  final EditText value=host.input(l,"One point is worth, in ₹",plan.pointValuePaise>0?host.BigDecimalValue(plan.pointValuePaise):"",8194);
  host.add(l,host.text("Cap",13,host.MUTED,true));
  final Spinner basis=new Spinner(host);
  basis.setAdapter(new ArrayAdapter<>(host,android.R.layout.simple_spinner_dropdown_item,
   new String[]{"No cap","Cap the rewards earned","Cap the qualifying spend"}));
  basis.setSelection(RewardPlan.CAP_SPEND.equals(plan.capBasis)?2:RewardPlan.CAP_REWARD.equals(plan.capBasis)?1:0);
  host.add(l,basis);host.gap(l,10);
  final EditText amount=host.input(l,"Cap amount per period",plan.capAmount>0?host.BigDecimalValue(plan.capAmount):"",8194);
  host.add(l,host.text("A reward cap is the most this card pays in a period. A spend cap is the most spend that earns the accelerated rate. Only categories marked as sharing the cap count towards it.",12,host.MUTED,false));
  new AlertDialog.Builder(host).setTitle("Cap and reward unit").setView(host.scrolled(l))
   .setNegativeButton("Cancel",(d,w)->open())
   .setPositiveButton("Save",(d,w)->{
    try{
     String chosen=basis.getSelectedItemPosition()==2?RewardPlan.CAP_SPEND:basis.getSelectedItemPosition()==1?RewardPlan.CAP_REWARD:RewardPlan.CAP_NONE;
     long capAmount=RewardPlan.CAP_NONE.equals(chosen)?0:Engine.money(amount.getText().toString().trim());
     String text=value.getText().toString().trim();
     long pointValue=text.isEmpty()?0:Engine.money(text);
     save(new RewardPlan(host.plan.id,host.plan.name,unit.getSelectedItemPosition()==1?RewardPlan.UNIT_POINTS:RewardPlan.UNIT_CASHBACK,
      chosen,capAmount,pointValue,host.plan.categories));
     open();
    }catch(IllegalArgumentException error){host.toast(error.getMessage());open();
    }catch(RuntimeException error){host.toast("Enter the cap as an amount, for example 1200.");open();}
   }).show();
 }

 private void choosePreset(){
  final List<RewardPlan> presets=RewardPlan.presets();
  String[] names=new String[presets.size()];
  for(int i=0;i<names.length;i++)names[i]=presets.get(i).name;
  new AlertDialog.Builder(host).setTitle("Start from a preset").setItems(names,(d,which)->{
   final RewardPlan chosen=presets.get(which);
   new AlertDialog.Builder(host).setTitle(chosen.name)
    .setMessage("Replace this card's categories and rates with this preset? Purchases keep their category numbers, so check anything already recorded.\n\n"+summary(chosen))
    .setNegativeButton("Cancel",(a,b)->open())
    .setPositiveButton("Use this plan",(a,b)->{save(chosen);open();}).show();
  }).setNegativeButton("Cancel",(d,w)->open()).show();
 }

 private void editJson(){
  LinearLayout l=host.dialogBody();
  host.add(l,host.text("The plan as JSON. Paste one in to replace it, or copy it to keep a backup.",13,host.MUTED,false));
  final EditText text=new EditText(host);
  text.setText(host.plan.toPrettyJson());text.setTextSize(12);text.setTextColor(host.INK);text.setGravity(Gravity.TOP);text.setMinLines(8);
  host.add(l,text);
  new AlertDialog.Builder(host).setTitle("Plan as JSON").setView(host.scrolled(l))
   .setNegativeButton("Cancel",(d,w)->open())
   .setNeutralButton("Copy",(d,w)->{
    try{
     ClipboardManager clipboard=(ClipboardManager)host.getSystemService(Context.CLIPBOARD_SERVICE);
     if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Reward plan",host.plan.toPrettyJson()));
     host.toast("Plan copied");
    }catch(RuntimeException ignored){host.toast("Could not copy the plan.");}
    open();
   })
   .setPositiveButton("Apply",(d,w)->{
    try{save(RewardPlan.fromJson(text.getText().toString()));host.toast("Reward plan updated");open();}
    catch(IllegalArgumentException error){host.toast(error.getMessage());}
   }).show();
 }

 private String summary(RewardPlan plan){
  StringBuilder out=new StringBuilder();
  for(RewardPlan.Category category:plan.categories)out.append("· ").append(category.label()).append(category.capped?" (shares the cap)":"").append('\n');
  return out.toString().trim();
 }
 private RewardPlan rebuild(RewardPlan plan,List<RewardPlan.Category> categories){
  return new RewardPlan(plan.id,plan.name,plan.unit,plan.capBasis,plan.capAmount,plan.pointValuePaise,categories);
 }
 private void save(RewardPlan plan){
  Cards.savePlan(host,host.cardId,plan);
  host.plan=Cards.plan(host,host.cardId);
  host.draw();
 }
 /** "10" or "1.5" as basis points. */
 private static int bps(String value){
  String text=value==null?"":value.trim().replace("%","");
  if(text.isEmpty())throw new IllegalArgumentException("Enter a rate, for example 1.5 for 1.5%.");
  long scaled=Engine.money(text);
  if(scaled<0||scaled>RewardPlan.MAX_RATE_BPS)throw new IllegalArgumentException("Enter a rate from 0% to 100%.");
  return (int)scaled;
 }
 private static long whole(String value){
  String text=value==null?"":value.trim();
  if(!text.matches("[0-9]{1,7}"))throw new IllegalArgumentException("Enter the number of points as a whole number.");
  return Long.parseLong(text);
 }
}
