import in.local.liveplus.Json;
import java.util.*;

public class JsonTest {
 static int checks;
 static void check(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
 static void rejects(String text,String name){
  checks++;
  try{Json.parse(text);}catch(IllegalArgumentException expected){return;}
  throw new AssertionError("accepted "+name);
 }
 public static int run(){
  Map<String,Object> read=Json.object("{\"a\":1,\"b\":\"two\",\"c\":true,\"d\":null,\"e\":[1,2,3],\"f\":{\"g\":-4.5}}");
  check(Json.number(read,"a",0)==1,"whole numbers read as longs");
  check("two".equals(Json.text(read,"b","")),"strings read");
  check(Json.flag(read,"c",false),"booleans read");
  check(read.containsKey("d")&&read.get("d")==null,"null is kept as a member");
  check(Json.children(read,"e").size()==3,"arrays read");
  check(Json.number(Json.child(read,"f"),"g",0)==-4,"fractions round to the nearest long");
  check(Json.number(read,"missing",7)==7,"a missing member falls back");
  check(Json.text(read,"a","fallback").equals("fallback"),"a member of the wrong type falls back");

  check("\"a\\\"b\\\\c\\nd\"".equals(Json.write("a\"b\\c\nd")),"strings escape quotes, slashes and newlines");
  check(Json.parse("\"\\u0041\\t\\/\"").equals("A\t/"),"escapes read back");
  check(Json.write(Arrays.asList(1L,"x",true,null)).equals("[1,\"x\",true,null]"),"arrays write");
  Map<String,Object> order=new LinkedHashMap<>();order.put("z",1L);order.put("a",2L);
  check(Json.write(order).equals("{\"z\":1,\"a\":2}"),"members keep their order");
  check(Json.pretty(order).equals("{\n  \"z\": 1,\n  \"a\": 2\n}"),"pretty output indents");
  check(Json.pretty(new LinkedHashMap<String,Object>()).equals("{}"),"an empty object stays on one line");

  Object round=Json.parse(Json.write(read));
  check(Json.write(round).equals(Json.write(read)),"a parsed value writes back identically");

  rejects("{\"a\":1,}","a trailing comma");
  rejects("{a:1}","an unquoted name");
  rejects("{'a':1}","single quotes");
  rejects("[1 2]","a missing comma");
  rejects("{\"a\":01}","a leading zero is still a number followed by junk");
  rejects("{\"a\":.5}","a bare fraction");
  rejects("{\"a\":1e}","an empty exponent");
  rejects("nope","a bare word");
  rejects("","empty text");
  rejects(null,"no text");
  rejects("{\"a\":\"unterminated","an unterminated string");
  rejects("{\"a\":\"line\nbreak\"}","a raw newline inside a string");
  rejects("{\"a\":1}{\"b\":2}","trailing text after the value");
  rejects("[1,2]extra","trailing text after an array");
  StringBuilder deep=new StringBuilder();
  for(int i=0;i<Json.MAX_DEPTH+4;i++)deep.append('[');
  rejects(deep.toString(),"text nested past the depth limit");
  StringBuilder long_=new StringBuilder("\"");
  for(int i=0;i<Json.MAX_LENGTH+10;i++)long_.append('x');
  rejects(long_.append('"').toString(),"text past the length limit");

  checks++;
  try{Json.object("[1,2]");throw new AssertionError("accepted an array as an object");}catch(IllegalArgumentException expected){}
  return checks;
 }
 public static void main(String[] args){System.out.println("JsonTest: "+run()+" checks");}
}
