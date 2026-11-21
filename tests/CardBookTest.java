import in.local.liveplus.CardBook;

public class CardBookTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void rejects(Runnable action,String name){
  checks++;
  try{action.run();}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("accepted "+name);
 }
 public static int run(){
  // Card 1 keeps upstream's file names, so a single-card install needs no migration.
  check(CardBook.database(1).equals("ledger.db"),"the first card keeps upstream's database");
  check(CardBook.preferences(1).equals("settings"),"the first card keeps upstream's preferences");
  check(CardBook.database(2).equals("ledger-2.db"),"later cards get their own database");
  check(CardBook.preferences(7).equals("settings-7"),"later cards get their own preferences");

  final CardBook book=new CardBook();
  check(book.isEmpty()&&book.nextId()==1,"a new book starts empty at id 1");
  CardBook.Card live=book.add("HSBC Live+","HSBC","",CardBook.ACCENTS[0],true);
  CardBook.Card axis=book.add("Axis Ace","Axis","AXISBK",CardBook.ACCENTS[1],true);
  check(live.id==1&&axis.id==2,"ids are handed out in order");
  check(live.database().equals("ledger.db")&&axis.database().equals("ledger-2.db"),"each card names its own ledger");
  check(book.get(2).name.equals("Axis Ace"),"cards look up by id");
  check(book.get(9)==null,"an unknown id reads as absent");
  check(axis.senderList().size()==1&&axis.senderList().get(0).equals("AXISBK"),"sender keywords split out");
  check(live.senderList().isEmpty(),"no sender keyword means any sender");
  check(book.add("Multi","Bank"," A , B ,, C ",0,true).senderList().size()==3,"blank sender entries are dropped");

  rejects(new Runnable(){public void run(){book.add("","Bank","",0,true);}},"a card with no name");
  rejects(new Runnable(){public void run(){book.add("   ","Bank","",0,true);}},"a card named only with spaces");
  rejects(new Runnable(){public void run(){book.add("HSBC Live+","HSBC","",0,true);}},"a duplicate card name");
  rejects(new Runnable(){public void run(){book.add("hsbc live+","HSBC","",0,true);}},"a duplicate name in another case");
  rejects(new Runnable(){public void run(){book.add("Valid","","",0,true);}},"a card with no issuer");
  StringBuilder long_=new StringBuilder();
  for(int i=0;i<CardBook.MAX_NAME+1;i++)long_.append('x');
  final String overlong=long_.toString();
  rejects(new Runnable(){public void run(){book.add(overlong,"Bank","",0,true);}},"a name past the limit");

  book.replace(book.get(2).with("Axis Ace 2","Axis","AXISBK",5,false));
  check(book.get(2).name.equals("Axis Ace 2")&&!book.get(2).capture,"a card is edited in place");
  check(book.get(2).id==2,"editing keeps the card's id and its ledger");
  rejects(new Runnable(){public void run(){book.replace(book.get(2).with("HSBC Live+","Axis","",0,true));}},"renaming onto another card's name");

  check(book.size()==3,"three cards are listed");
  book.move(1,1);
  check(book.at(0).id==2&&book.at(1).id==1,"a card moves later in the grid");
  book.move(1,-5);
  check(book.at(0).id==1,"a move past the start clamps");
  book.remove(3);
  check(book.size()==2&&book.get(3)==null,"a card is removed");
  book.remove(99);
  check(book.size()==2,"removing an unknown card changes nothing");
  check(book.nextId()==3,"ids are not reused after a removal");

  CardBook copy=CardBook.decode(book.encode());
  check(copy.size()==book.size(),"a book survives a round trip");
  check(copy.at(0).id==book.at(0).id&&copy.at(1).id==book.at(1).id,"order survives a round trip");
  check(copy.get(2).senders.equals("AXISBK")&&!copy.get(2).capture,"card details survive a round trip");

  check(CardBook.decode(null).isEmpty(),"no stored registry reads as empty");
  check(CardBook.decode("{ broken").isEmpty(),"a damaged registry reads as empty rather than throwing");
  check(CardBook.decode("{\"cards\":[{\"id\":1},{\"id\":1},{\"id\":0}]}").size()==1,"duplicate and invalid ids are dropped");
  check(CardBook.decode("{\"cards\":[{\"id\":4}]}").get(4).name.equals("Card 4"),"a card with no name gets one");
  check(CardBook.decode("{\"cards\":[{\"id\":4}]}").get(4).capture,"capture defaults to on for a restored card");
  StringBuilder many=new StringBuilder("{\"cards\":[");
  for(int i=1;i<=CardBook.MAX_CARDS+5;i++)many.append(i>1?",":"").append("{\"id\":").append(i).append("}");
  check(CardBook.decode(many.append("]}").toString()).size()==CardBook.MAX_CARDS,"a registry is capped at the card limit");
  return checks;
 }
 public static void main(String[] args){System.out.println("CardBookTest: "+run()+" checks");}
}
