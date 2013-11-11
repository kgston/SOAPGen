import com.smutbank.*;
import java.util.*;

public class test {
    
    public static void main(String[] args) throws Exception {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for(Thread t : threadSet) {
            System.out.println(t.getName());
        }
        System.out.println("--------");
        SoapGen sg = new SoapGen();
        sg.close();
        
        System.out.println("--------");
        threadSet = Thread.getAllStackTraces().keySet();
        for(Thread t : threadSet) {
            System.out.println(t.getName());
        }
    }
    
}