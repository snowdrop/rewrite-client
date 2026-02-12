package dev.snowdrop;

import java.util.ArrayList;
import java.util.List;

public class SuppressWarning {
    @SuppressWarnings("unused")
    void suppressUnusedWarning() {
        int usedVal = 5;
        int unusedVal = 10;  // no warning here
        List<Integer> list = new ArrayList<>();
        list.add(usedVal);
    }
}
