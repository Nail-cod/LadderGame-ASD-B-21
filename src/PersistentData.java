package src;

import java.io.Serializable;
import java.util.HashMap;

public class PersistentData implements Serializable {
    private static final long serialVersionUID = 1L;
    HashMap<String, PlayerStats> statsMap = new HashMap<>();
}
