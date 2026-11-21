package in.local.liveplus;

import java.util.*;

/**
 * Minimal strict JSON reader and writer with no Android or third-party dependency.
 *
 * <p>Android ships {@code org.json}, but the reward plan and card registry must also run in the
 * plain-Java test suite, which has no JSON library on its classpath. Upstream's own export and
 * restore paths keep using {@code org.json} and are untouched.
 *
 * <p>Parsed values are {@code Map<String,Object>}, {@code List<Object>}, {@code String},
 * {@code Long}, {@code Double}, {@code Boolean} or {@code null}. Input is bounded in length and
 * nesting depth; anything malformed raises {@link IllegalArgumentException} with a position.
 */
public final class Json {
 public static final int MAX_LENGTH=200000,MAX_DEPTH=24;
 private final String text; private int at;
 private Json(String text){this.text=text;}

 public static Object parse(String text){
  if(text==null)throw new IllegalArgumentException("No text to read");
  if(text.length()>MAX_LENGTH)throw new IllegalArgumentException("Text is too long");
  Json reader=new Json(text);
  reader.space();
  Object value=reader.value(0);
  reader.space();
  if(reader.at<text.length())throw reader.error("Unexpected text after the value");
  return value;
 }
 /** Reads a value that must be a JSON object. */
 public static Map<String,Object> object(String text){
  Object value=parse(text);
  if(!(value instanceof Map))throw new IllegalArgumentException("Expected a JSON object");
  @SuppressWarnings("unchecked") Map<String,Object> map=(Map<String,Object>)value;
  return map;
 }

 private IllegalArgumentException error(String message){return new IllegalArgumentException(message+" at character "+(at+1));}
 private void space(){while(at<text.length()){char c=text.charAt(at);if(c==' '||c=='\t'||c=='\n'||c=='\r')at++;else break;}}
 private char peek(){if(at>=text.length())throw error("Text ended early");return text.charAt(at);}
 private boolean word(String token){
  if(!text.startsWith(token,at))return false;
  at+=token.length();return true;
 }
 private Object value(int depth){
  if(depth>MAX_DEPTH)throw error("Nested too deeply");
  char c=peek();
  if(c=='{')return map(depth);
  if(c=='[')return list(depth);
  if(c=='"')return string();
  if(word("true"))return Boolean.TRUE;
  if(word("false"))return Boolean.FALSE;
  if(word("null"))return null;
  if(c=='-'||(c>='0'&&c<='9'))return number();
  throw error("Unexpected character '"+c+"'");
 }
 private Map<String,Object> map(int depth){
  Map<String,Object> out=new LinkedHashMap<>();
  at++;space();
  if(peek()=='}'){at++;return out;}
  while(true){
   space();
   if(peek()!='"')throw error("Expected a quoted member name");
   String name=string();
   space();
   if(peek()!=':')throw error("Expected ':'");
   at++;space();
   if(out.size()>=512)throw error("Too many members");
   out.put(name,value(depth+1));
   space();
   char c=peek();
   if(c==','){at++;continue;}
   if(c=='}'){at++;return out;}
   throw error("Expected ',' or '}'");
  }
 }
 private List<Object> list(int depth){
  List<Object> out=new ArrayList<>();
  at++;space();
  if(peek()==']'){at++;return out;}
  while(true){
   space();
   if(out.size()>=4096)throw error("Too many entries");
   out.add(value(depth+1));
   space();
   char c=peek();
   if(c==','){at++;continue;}
   if(c==']'){at++;return out;}
   throw error("Expected ',' or ']'");
  }
 }
 private String string(){
  at++;StringBuilder out=new StringBuilder();
  while(true){
   if(at>=text.length())throw error("Text ended inside a string");
   char c=text.charAt(at++);
   if(c=='"')return out.toString();
   if(c!='\\'){
    if(c<0x20)throw error("Unescaped control character in a string");
    out.append(c);continue;
   }
   if(at>=text.length())throw error("Text ended inside an escape");
   char escape=text.charAt(at++);
   switch(escape){
    case '"':out.append('"');break;
    case '\\':out.append('\\');break;
    case '/':out.append('/');break;
    case 'b':out.append('\b');break;
    case 'f':out.append('\f');break;
    case 'n':out.append('\n');break;
    case 'r':out.append('\r');break;
    case 't':out.append('\t');break;
    case 'u':{
     if(at+4>text.length())throw error("Short \\u escape");
     String digits=text.substring(at,at+4);
     if(!digits.matches("[0-9a-fA-F]{4}"))throw error("Bad \\u escape");
     out.append((char)Integer.parseInt(digits,16));at+=4;break;
    }
    default:throw error("Unknown escape '\\"+escape+"'");
   }
   if(out.length()>MAX_LENGTH)throw error("String is too long");
  }
 }
 private Object number(){
  int start=at;
  if(at<text.length()&&text.charAt(at)=='-')at++;
  int digits=0,first=at;
  while(at<text.length()&&text.charAt(at)>='0'&&text.charAt(at)<='9'){at++;digits++;}
  if(digits==0)throw error("Expected a digit");
  if(digits>1&&text.charAt(first)=='0')throw error("A number cannot start with 0");
  boolean whole=true;
  if(at<text.length()&&text.charAt(at)=='.'){
   whole=false;at++;int fraction=0;
   while(at<text.length()&&text.charAt(at)>='0'&&text.charAt(at)<='9'){at++;fraction++;}
   if(fraction==0)throw error("Expected a digit after '.'");
  }
  if(at<text.length()&&(text.charAt(at)=='e'||text.charAt(at)=='E')){
   whole=false;at++;
   if(at<text.length()&&(text.charAt(at)=='+'||text.charAt(at)=='-'))at++;
   int exponent=0;
   while(at<text.length()&&text.charAt(at)>='0'&&text.charAt(at)<='9'){at++;exponent++;}
   if(exponent==0)throw error("Expected a digit in the exponent");
  }
  String token=text.substring(start,at);
  if(whole)try{return Long.valueOf(Long.parseLong(token));}catch(NumberFormatException ignored){}
  return Double.valueOf(Double.parseDouble(token));
 }

 // Readers that keep callers free of casts and null checks.
 public static Map<String,Object> child(Map<String,Object> source,String name){
  Object value=source==null?null:source.get(name);
  if(!(value instanceof Map))return new LinkedHashMap<>();
  @SuppressWarnings("unchecked") Map<String,Object> map=(Map<String,Object>)value;
  return map;
 }
 public static List<Object> children(Map<String,Object> source,String name){
  Object value=source==null?null:source.get(name);
  if(!(value instanceof List))return new ArrayList<>();
  @SuppressWarnings("unchecked") List<Object> list=(List<Object>)value;
  return list;
 }
 public static Map<String,Object> asObject(Object value){
  if(!(value instanceof Map))throw new IllegalArgumentException("Expected a JSON object");
  @SuppressWarnings("unchecked") Map<String,Object> map=(Map<String,Object>)value;
  return map;
 }
 public static String text(Map<String,Object> source,String name,String fallback){
  Object value=source==null?null:source.get(name);
  return value instanceof String?(String)value:fallback;
 }
 public static long number(Map<String,Object> source,String name,long fallback){
  Object value=source==null?null:source.get(name);
  if(value instanceof Long)return (Long)value;
  if(value instanceof Double){
   double d=(Double)value;
   if(d>=Long.MIN_VALUE&&d<=Long.MAX_VALUE)return Math.round(d);
  }
  return fallback;
 }
 public static boolean flag(Map<String,Object> source,String name,boolean fallback){
  Object value=source==null?null:source.get(name);
  return value instanceof Boolean?(Boolean)value:fallback;
 }

 public static String write(Object value){StringBuilder out=new StringBuilder();write(out,value,0);return out.toString();}
 private static void write(StringBuilder out,Object value,int depth){
  if(depth>MAX_DEPTH)throw new IllegalArgumentException("Nested too deeply");
  if(value==null){out.append("null");return;}
  if(value instanceof String){quote(out,(String)value);return;}
  if(value instanceof Boolean||value instanceof Long||value instanceof Integer){out.append(value);return;}
  if(value instanceof Double||value instanceof Float){
   double d=((Number)value).doubleValue();
   if(Double.isNaN(d)||Double.isInfinite(d))throw new IllegalArgumentException("Cannot write "+d);
   if(d==Math.floor(d)&&Math.abs(d)<1e15)out.append((long)d);else out.append(d);
   return;
  }
  if(value instanceof Map){
   out.append('{');boolean first=true;
   for(Map.Entry<?,?> entry:((Map<?,?>)value).entrySet()){
    if(!first)out.append(',');first=false;
    quote(out,String.valueOf(entry.getKey()));out.append(':');write(out,entry.getValue(),depth+1);
   }
   out.append('}');return;
  }
  if(value instanceof Iterable){
   out.append('[');boolean first=true;
   for(Object entry:(Iterable<?>)value){
    if(!first)out.append(',');first=false;
    write(out,entry,depth+1);
   }
   out.append(']');return;
  }
  throw new IllegalArgumentException("Cannot write "+value.getClass().getName());
 }
 private static void quote(StringBuilder out,String value){
  out.append('"');
  for(int i=0;i<value.length();i++){
   char c=value.charAt(i);
   switch(c){
    case '"':out.append("\\\"");break;
    case '\\':out.append("\\\\");break;
    case '\n':out.append("\\n");break;
    case '\r':out.append("\\r");break;
    case '\t':out.append("\\t");break;
    case '\b':out.append("\\b");break;
    case '\f':out.append("\\f");break;
    default:
     if(c<0x20||c==0x2028||c==0x2029)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));
     else out.append(c);
   }
  }
  out.append('"');
 }
 /** Re-writes a value with two-space indentation, for anything a person reads or edits. */
 public static String pretty(Object value){StringBuilder out=new StringBuilder();pretty(out,value,0);return out.toString();}
 private static void pretty(StringBuilder out,Object value,int depth){
  String pad=indent(depth+1),close=indent(depth);
  if(value instanceof Map&&!((Map<?,?>)value).isEmpty()){
   out.append("{\n");boolean first=true;
   for(Map.Entry<?,?> entry:((Map<?,?>)value).entrySet()){
    if(!first)out.append(",\n");first=false;
    out.append(pad);quote(out,String.valueOf(entry.getKey()));out.append(": ");pretty(out,entry.getValue(),depth+1);
   }
   out.append('\n').append(close).append('}');return;
  }
  if(value instanceof Iterable&&((Iterable<?>)value).iterator().hasNext()){
   out.append("[\n");boolean first=true;
   for(Object entry:(Iterable<?>)value){
    if(!first)out.append(",\n");first=false;
    out.append(pad);pretty(out,entry,depth+1);
   }
   out.append('\n').append(close).append(']');return;
  }
  write(out,value,depth);
 }
 private static String indent(int depth){
  StringBuilder out=new StringBuilder();
  for(int i=0;i<depth&&i<MAX_DEPTH;i++)out.append("  ");
  return out.toString();
 }
}
