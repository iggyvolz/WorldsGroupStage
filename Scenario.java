import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.management.RuntimeErrorException;

import org.javatuples.Quartet;
import org.javatuples.Quintet;
import org.javatuples.Triplet;
public class Scenario {
    final static int NUM_TEAMS=4;
    final static int NUM_GAMES;
    final static long NUM_RESULTS;
    static {
        NUM_GAMES = NUM_TEAMS * (NUM_TEAMS - 1);
        NUM_RESULTS = (long) Math.pow(2, NUM_GAMES);
    }
    private long num;
    public boolean getResult(int a, int b) throws IllegalArgumentException {
        if(a == b) {
            throw new IllegalArgumentException("A team does not play themselves.");
        }
        if(a < 0 || a > 3) {
            throw new IllegalArgumentException("Illegal team " + a);
        }
        if(b < 0 || b > 3) {
            throw new IllegalArgumentException("Illegal team " + b);
        }
        long game = 1 << (a + (NUM_TEAMS*(b>a?(b-1):b)));
        // System.out.println("Game mask: " + game + " = 1 << " + (a + (NUM_GAMES*(b>a?(b-1):b))));
        return (num&game) != 0;
    }

    public int getRecord(int team) throws IllegalArgumentException {
        if(team < 0 || team > 3) {
            throw new IllegalArgumentException("Illegal team " + team);
        }
        int numWins=0;
        for(int opponent=0;opponent<4;opponent++) {
            if(opponent != team && getResult(team, opponent)) {
                // We beat an opponent
                numWins++;
            }
            if(opponent != team && !getResult(opponent, team)) {
                // Opponent lost to us
                numWins++;
            }
        }
        return numWins;
    }

    private int[] records;
    public int[] getRecords() {
        if(records == null) {
            records = new int[4];
            for(int i=0;i<4;i++) {
                records[i] = getRecord(i);
            }
            // Sanity check - there should be 12 wins
            int totalWins = records[0] + records[1] + records[2] + records[3];
            if(totalWins != 12) {
                throw new RuntimeException("Illegal number of wins "+ totalWins);
            }
        }
        return records;
    }

    int[] teamOrders;
    // Incomplete until getTiebreak() is called to resolve ties
    private int[] teamOrdering() {
        records = getRecords();
        if(teamOrders == null) {
            Integer[] orders = new Integer[]{0, 1, 2, 3};
            Arrays.sort(orders, (Integer t1, Integer t2) -> records[t2] - records[t1]);
            teamOrders = Arrays.stream(orders).mapToInt(Integer::intValue).toArray();
        }
        return teamOrders;
    }

    private enum ThreeWayTieResult {
        // Could not resolve any teams in the tie
        Deadlock,
        // Unresolved tie between upper teams
        UpperTie,
        // Unresolved tie between lower teams
        LowerTie,
        // All ties resolved
        Resolved,
    }

    // Returns true for teams which are solid, false for teams which are not
    // See 2019 rulebook 6.4.5
    private Quartet<Integer, Integer, Integer, ThreeWayTieResult> resolveThreeWayTie(int t1, int t2, int t3) {
        Integer[] teams = {t1, t2, t3};
        Integer[] records = { 0, 0, 0 };
        for(int i=0;i<3;i++) {
            for(int j=0;j<3;j++) {
                if(i == j) continue; // Skip own team's game against self
                // Increment winner in this game
                records[getResult(i,j)?i:j]++;
            }
        }

        // If all teams have 2-2 records, this is a deadlock
        if(records[0] == 2 && records[1] == 2 && records[2] == 2) {
            return new Quartet<>(t1, t2, t3, ThreeWayTieResult.Deadlock);
        }
        // Sort descending based on record
        Arrays.sort(teams, (ta, tb) -> records[t2]-records[t1]);
        // Sort records descending to match
        Arrays.sort(records, (r1, r2) -> r2-r1);

        // If the records are 3-1, 2-2, 1-3, then this is a steplock
        if(records[0] == 3 && records[1] == 2 && records[2] == 1) {
            // Return teams in order of steplock
            return new Quartet<>(teams[0], teams[1], teams[2], ThreeWayTieResult.Deadlock);
        }

        // If the records are 3-1, 3-1, 0-4, drop the last team to the bottom and tiebreak the other two teams
        if(records[0] == 3 && records[1] == 3 && records[2] == 0) {
            Triplet<Integer, Integer, TwoWayTieResult> tieResult = resolveTwoWayTie(teams[0], teams[1]);
            return new Quartet<>(tieResult.getValue0(), tieResult.getValue1(), teams[2], tieResult.getValue2() == TwoWayTieResult.Resolved ? ThreeWayTieResult.Resolved : ThreeWayTieResult.UpperTie);
        }
        // If the records are 4-0, 1-3, 1-3, advance the upper team to the top and tiebreak the other two teams
        if(records[0] == 4 && records[1] == 1 && records[2] == 1) {
            Triplet<Integer, Integer, TwoWayTieResult> tieResult = resolveTwoWayTie(teams[1], teams[2]);
            return new Quartet<>(teams[0], tieResult.getValue0(), tieResult.getValue1(), tieResult.getValue2() == TwoWayTieResult.Resolved ? ThreeWayTieResult.Resolved : ThreeWayTieResult.LowerTie);
        }
        // If the records are 4-0, 2-2, 0-4, the teams are in the correct order already
        if(records[0] == 4 && records[1] == 2 && records[2] == 0) {
            return new Quartet<>(teams[0], teams[1], teams[2], ThreeWayTieResult.Resolved);
        }
        // This is impossible
        System.out.println("This is impossible.");
        System.out.println(Arrays.toString(records));
        throw new RuntimeException();
    }

    private enum TwoWayTieResult {
        // Could not resolve the tie
        Deadlock,
        // All ties resolved
        Resolved,
    }

    private Triplet<Integer, Integer, TwoWayTieResult> resolveTwoWayTie(int t1, int t2) {
        // If t1 swept t2, return t1 first
        if(getResult(t1, t2) && !getResult(t2, t1)) {
            return new Triplet<>(t1,t2,TwoWayTieResult.Resolved);
        }
        // If t2 swept t1, return t2 first
        if(getResult(t2, t1) && !getResult(t1, t2)) {
            return new Triplet<>(t2,t1,TwoWayTieResult.Resolved);
        }
        // Tie cannot be resolved
        return new Triplet<>(t1,t2,TwoWayTieResult.Deadlock);
    }

    public Quintet<Integer,Integer,Integer,Integer,TieType> getResults() {
        getRecords();
        teamOrdering();
        // System.out.println(Arrays.toString(records));
        if(records[teamOrders[0]] > records[teamOrders[1]] && records[teamOrders[1]] > records[teamOrders[2]]) {
            // Woo, no tiebreakers
            return new Quintet<>(teamOrders[0], teamOrders[1], teamOrders[2], teamOrders[3], TieType.None);
        }
        // Tiebreaking scenario: all teams have the same 3-3 record
        if(records[teamOrders[0]] == 3 && records[teamOrders[1]] == 3 && records[teamOrders[2]] == 3  && records[teamOrders[3]] == 3) {
            return new Quintet<>(teamOrders[0], teamOrders[1], teamOrders[2], teamOrders[3], TieType.FourWayTie);
        }
        // Tiebreaking scenario: three way tie with 6-0, 2-4, 2-4, 2-4
        if(records[teamOrders[0]] == 6 && records[teamOrders[1]] == 2 && records[teamOrders[2]] == 2 && records[teamOrders[3]] == 2) {
            Quartet<Integer,Integer,Integer,Scenario.ThreeWayTieResult> tiebreakerResult = resolveThreeWayTie(teamOrders[1], teamOrders[2], teamOrders[3]);
            return new Quintet<>(teamOrders[0], tiebreakerResult.getValue0(), tiebreakerResult.getValue1(), tiebreakerResult.getValue2(), switch(tiebreakerResult.getValue3()){
                // 2, 3, 4 are tied
                case Deadlock -> TieType.BottomThreeWayTie;
                // 2, 3 are tied
                case UpperTie -> TieType.MiddleTwoWayTie;
                // 3, 4 are tied (we don't care)
                case LowerTie -> TieType.None;
                case Resolved -> TieType.None;
            });
        }
        // Tiebreaking scenario: three way tie with 4-2, 4-2, 4-2, 0-6
        if(records[teamOrders[0]] == 4 && records[teamOrders[1]] == 4 && records[teamOrders[2]] == 4 && records[teamOrders[3]] == 0) {
            Quartet<Integer,Integer,Integer,Scenario.ThreeWayTieResult> tiebreakerResult = resolveThreeWayTie(teamOrders[0], teamOrders[1], teamOrders[2]);
            return new Quintet<>(tiebreakerResult.getValue0(), tiebreakerResult.getValue1(), tiebreakerResult.getValue2(), teamOrders[3], switch(tiebreakerResult.getValue3()){
                // 1, 2, 3 are tied
                case Deadlock -> TieType.TopThreeWayTie;
                // 1, 2 are tied
                case UpperTie -> TieType.TopTwoWayTie;
                // 2, 3 are tied
                case LowerTie -> TieType.MiddleTwoWayTie;
                case Resolved -> TieType.None;
            });
        }
        // Tiebreaking scenario: 2nd and 3rd are equal
        if(records[teamOrders[1]] == records[teamOrders[2]]) {
            Triplet<Integer,Integer,Scenario.TwoWayTieResult> tiebreakerResult = resolveTwoWayTie(teamOrders[1], teamOrders[2]);
            return new Quintet<>(teamOrders[0], tiebreakerResult.getValue0(), tiebreakerResult.getValue1(), teamOrders[3], switch(tiebreakerResult.getValue2()){
                // 2, 3 are tied
                case Deadlock -> TieType.MiddleTwoWayTie;
                case Resolved -> TieType.None;
            });
        }
        // Tiebreaking scenario: 1st and 2nd are equal
        if(records[teamOrders[0]] == records[teamOrders[1]]) {
            Triplet<Integer,Integer,Scenario.TwoWayTieResult> tiebreakerResult = resolveTwoWayTie(teamOrders[0], teamOrders[1]);
            return new Quintet<>(tiebreakerResult.getValue0(), tiebreakerResult.getValue1(), teamOrders[2], teamOrders[3], switch(tiebreakerResult.getValue2()){
                // 1, 2 are tied
                case Deadlock -> TieType.TopTwoWayTie;
                case Resolved -> TieType.None;
            });
        }
        // Impossible
        System.out.println("Could not resolve tiebreakers; no tiebreaker applies but one is needed");
        System.out.println(Arrays.toString(records));
        System.out.println(Arrays.toString(teamOrders));
        throw new RuntimeException();
    }

    private Scenario(long num) {
        this.num = num;
    }
    public static Set<Scenario> getAll() {
        Set<Scenario> all = new HashSet<>();
        for(long i=0; i<NUM_RESULTS; i++) {
            all.add(new Scenario(i));
        }
        return all;
    }
}
