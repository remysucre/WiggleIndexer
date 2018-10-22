import java.util.*;

public class Test {

    public static void main(String[] args) {
        Map<String, Integer> yearToIncome = new HashMap<>()    ;

        if(yearToIncome.get("2010") == null) {
            System.out.println("oops");
        }
       double[] ar = {1.2, 3.0, 0.8};
       for (double d : ar) {}
       for (double d : ar) {}
       for (int i=1; i<11; i++){
           System.out.println("Count is: " + i)   ;
       }
       Iterable<Integer> is = null;
       for (Iterator<Integer> i = is.iterator(); i.hasNext(); ) {}
    }
}
