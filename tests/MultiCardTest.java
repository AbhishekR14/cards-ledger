/**
 * Runs the multi-card suites: plans, rewards, the card registry, alert routing, credits and the
 * whole-app backup. The parser and capture suites run under EngineTest.
 */
public class MultiCardTest {
 public static void main(String[] args){
  int checks=JsonTest.run()+RewardPlanTest.run()+RewardsTest.run()+CardBookTest.run()+AlertsTest.run()
   +CreditsTest.run()+VaultTest.run();
  System.out.println("PASS: "+checks+" JSON, reward plan, reward calculation, card registry, alert routing, credit and backup checks");
 }
}
