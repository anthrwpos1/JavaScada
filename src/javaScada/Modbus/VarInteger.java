package javaScada.Modbus;

public class VarInteger extends Variable<Short> {
    public VarInteger (int address, boolean lsBitFirst){
        super(address,1,lsBitFirst);
    }

    @Override
    public void putData(Short value) {
        writeRawData(new short[]{value});
    }

    @Override
    public Short getData() {
        return getRawData()[0];
    }
}
