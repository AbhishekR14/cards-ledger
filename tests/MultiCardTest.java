/**
 * Runs the suites this fork adds. Upstream's own suites stay under EngineTest, which is left
 * untouched so that a sync never conflicts there.
 */
public class MultiCardTest {
 public static void main(String[] args){
  int checks=JsonTest.run()+RewardPlanTest.run()+RewardsTest.run()+CardBookTest.run()+AlertsTest.run()
   +CreditsTest.run()+VaultTest.run();
  System.out.println("PASS: "+checks+" JSON, reward plan, reward calculation, card registry, alert routing, credit and backup checks");
 }
}
