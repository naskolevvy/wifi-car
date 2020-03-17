
import java.util.*;

public class Data{
    private static HashMap<String ,Car> cars = new HashMap<String, Car>();
    private static HashMap<String ,App> apps = new HashMap<String, App>();

    public Data(){
    }
    public HashMap<String, Car> getCars(){
        return cars;
    }
    public HashMap<String, App> getApps(){
        return apps;
    }
}