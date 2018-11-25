package javaScada.Modbus;

import java.util.HashMap;

public class Device {//класс, представляющий сетевое устройство
    private int address;
    private HashMap<String, Variable> vars;//сетевые переменные

    public Device(int address) {
        this.address = address;
        vars = new HashMap<>();
    }

    public void addVariable(String name, Variable var) throws Exception {
        int va1 = var.getAddress();
        int la1 = var.getLength();
        if (va1 < 1) throw new Exception(String.format("illegal address %d", va1));
        if (va1 > 9999) throw new Exception(String.format("illegal address %d", va1));
        for (Variable v : vars.values()) {
            int va2 = v.getAddress();
            int la2 = v.getLength();
            int c1 = va1 * 2 + la1;
            int c2 = va2 * 2 + la2;
            int d = (la1 > la2) ? la1 : la2;
            if (Math.abs(c2 - c1) <= d)
                throw new Exception(String.format(" Address aliasing: address1 = %d, Length = %d, address2 = %d, Length = %d\n", va1, la1, va2, la2));
        }
        vars.put(name, var);
    }

    public HashMap<String, Variable> getVars() {
        return vars;
    }

    public int getAddress() {
        return address;
    }
}
