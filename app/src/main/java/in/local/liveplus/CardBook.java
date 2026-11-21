package in.local.liveplus;

import java.util.*;

/**
 * The list of tracked cards: identity and presentation only.
 *
 * <p>Each card's ledger settings (last four digits, statement day, cap, reward plan) stay in that
 * card's own preferences file, written by the screens upstream already owns. Card 1 keeps the
 * original {@code ledger.db} and {@code settings} names, so a single-card install carries over
 * with no migration.
 *
 * <p>Plain Java with no Android dependency; {@link Cards} adapts it to preferences.
 */
public final class CardBook {
 public static final int MAX_CARDS=12,MAX_NAME=28,MAX_ISSUER=24,MAX_SENDERS=120;
 public static final int[] ACCENTS={
  0xFF0E2D35,0xFF3B2A52,0xFF123A2A,0xFF4A2330,0xFF2A3350,0xFF4A3A16,0xFF153F46,0xFF3A2140};

 public static final class Card {
  public final int id; public final String name,issuer,senders; public final int accent; public final boolean capture;
  public Card(int id,String name,String issuer,String senders,int accent,boolean capture){
   this.id=id;this.name=name;this.issuer=issuer;this.senders=senders;this.accent=accent;this.capture=capture;
  }
  public Card with(String name,String issuer,String senders,int accent,boolean capture){
   return new Card(id,name,issuer,senders,accent,capture);
  }
  /** The database file backing this card. Card 1 keeps upstream's name. */
  public String database(){return CardBook.database(id);}
  /** The preferences file backing this card. Card 1 keeps upstream's name. */
  public String preferences(){return CardBook.preferences(id);}
  /** Sender keywords, split and trimmed; empty means "any sender". */
  public List<String> senderList(){
   List<String> out=new ArrayList<>();
   for(String part:senders.split(",")){String token=part.trim();if(!token.isEmpty())out.add(token);}
   return out;
  }
 }
 public static String database(int id){return id<=1?"ledger.db":"ledger-"+id+".db";}
 public static String preferences(int id){return id<=1?"settings":"settings-"+id;}

 private final List<Card> cards=new ArrayList<>();
 public CardBook(){}
 public CardBook(List<Card> initial){for(Card card:initial)cards.add(card);}

 public List<Card> cards(){return Collections.unmodifiableList(cards);}
 public int size(){return cards.size();}
 public boolean isEmpty(){return cards.isEmpty();}
 public Card get(int id){for(Card card:cards)if(card.id==id)return card;return null;}
 public Card at(int position){return cards.get(position);}
 public int nextId(){int next=1;for(Card card:cards)next=Math.max(next,card.id+1);return next;}

 public Card add(String name,String issuer,String senders,int accent,boolean capture){
  if(cards.size()>=MAX_CARDS)throw new IllegalArgumentException("This app tracks up to "+MAX_CARDS+" cards.");
  Card card=new Card(nextId(),clean(name,MAX_NAME,"Give the card a name."),clean(issuer,MAX_ISSUER,"Enter the bank name as it appears in its messages."),
   trim(senders,MAX_SENDERS),accent,capture);
  requireFreeName(card.name,card.id);
  cards.add(card);
  return card;
 }
 public void replace(Card card){
  requireFreeName(clean(card.name,MAX_NAME,"Give the card a name."),card.id);
  for(int i=0;i<cards.size();i++)if(cards.get(i).id==card.id){
   cards.set(i,new Card(card.id,clean(card.name,MAX_NAME,"Give the card a name."),
    clean(card.issuer,MAX_ISSUER,"Enter the bank name as it appears in its messages."),
    trim(card.senders,MAX_SENDERS),card.accent,card.capture));
   return;
  }
  throw new IllegalArgumentException("That card is no longer in the list.");
 }
 public void remove(int id){
  for(Iterator<Card> each=cards.iterator();each.hasNext();)if(each.next().id==id){each.remove();return;}
 }
 /** Moves a card one place earlier or later in the grid. */
 public void move(int id,int by){
  for(int i=0;i<cards.size();i++)if(cards.get(i).id==id){
   int target=Math.max(0,Math.min(cards.size()-1,i+by));
   if(target==i)return;
   cards.add(target,cards.remove(i));
   return;
  }
 }
 public int accentFor(int position){return ACCENTS[Math.floorMod(position,ACCENTS.length)];}

 private void requireFreeName(String name,int id){
  for(Card card:cards)if(card.id!=id&&Engine.key(card.name).equals(Engine.key(name)))
   throw new IllegalArgumentException("Another card is already called \""+card.name+"\".");
 }
 private static String clean(String value,int limit,String message){
  String trimmed=value==null?"":value.trim().replaceAll("\\s+"," ");
  if(trimmed.isEmpty()||trimmed.length()>limit)throw new IllegalArgumentException(message);
  return trimmed;
 }
 private static String trim(String value,int limit){
  String trimmed=value==null?"":value.trim().replaceAll("\\s+"," ");
  return trimmed.length()>limit?trimmed.substring(0,limit):trimmed;
 }

 public String encode(){
  List<Object> rows=new ArrayList<>();
  for(Card card:cards){
   Map<String,Object> row=new LinkedHashMap<>();
   row.put("id",(long)card.id);row.put("name",card.name);row.put("issuer",card.issuer);
   if(!card.senders.isEmpty())row.put("senders",card.senders);
   row.put("accent",(long)card.accent);row.put("capture",card.capture);
   rows.add(row);
  }
  Map<String,Object> out=new LinkedHashMap<>();
  out.put("format","cards-ledger-v1");out.put("cards",rows);
  return Json.write(out);
 }
 /** Best effort: a damaged registry must never leave the app with no way back to its ledgers. */
 public static CardBook decode(String text){
  CardBook book=new CardBook();
  if(text==null||text.trim().isEmpty())return book;
  try{
   Set<Integer> seen=new HashSet<>();
   for(Object entry:Json.children(Json.object(text),"cards")){
    if(book.cards.size()>=MAX_CARDS)break;
    Map<String,Object> row=Json.asObject(entry);
    int id=(int)Json.number(row,"id",0);
    if(id<=0||!seen.add(id))continue;
    String name=Json.text(row,"name","Card "+id).trim();
    if(name.isEmpty())name="Card "+id;
    if(name.length()>MAX_NAME)name=name.substring(0,MAX_NAME);
    String issuer=Json.text(row,"issuer","").trim();
    if(issuer.length()>MAX_ISSUER)issuer=issuer.substring(0,MAX_ISSUER);
    String senders=Json.text(row,"senders","").trim();
    if(senders.length()>MAX_SENDERS)senders=senders.substring(0,MAX_SENDERS);
    book.cards.add(new Card(id,name,issuer,senders,(int)Json.number(row,"accent",ACCENTS[book.cards.size()%ACCENTS.length]),Json.flag(row,"capture",true)));
   }
  }catch(RuntimeException ignored){return new CardBook();}
  return book;
 }
}
