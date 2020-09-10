import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.javatuples.Quintet;

class Main {
    public static void main(String[] args) {
        Set<Scenario> scenarios = Scenario.getAll();
        List<Quintet<Integer,Integer,Integer,Integer,TieType>> results = new ArrayList<Quintet<Integer,Integer,Integer,Integer,TieType>>();
        for(Scenario s:scenarios) {
            results.add(s.getResults());
        }
        for(TieType type:TieType.values()) {
            long count = results.stream().filter(x -> x.getValue4() == type).count();
            System.out.println("There are " + count + " " + type + " ("+Math.round(count * 10000.f / scenarios.size())/100.f+"%)");
        }
    }
}