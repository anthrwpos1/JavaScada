package javaScada.Modbus;

public class VarFloat extends Variable<Float> {
    private boolean leastSignificantRegisterFirst;

    public VarFloat(int address, boolean lsBitFirst, boolean lsRegisterFirst){
        super(address,2,lsBitFirst);
        leastSignificantRegisterFirst = lsRegisterFirst;
    }

    @Override
    public void putData(Float x) {
        int value = Float.floatToIntBits(x);
        short[] data = new short[2];
        data[0] = (short) (leastSignificantRegisterFirst ? (value & 0xffff0000) >>> 16 : value & 0x0000ffff);
        data[1] = (short) (leastSignificantRegisterFirst ? value & 0x0000ffff : (value & 0xffff0000) >>> 16);
        writeRawData(data);
    }

    @Override
    public Float getData() {
        short[] data = getRawData();
        int[] idata = new int[]{data[0] & 0xffff, data[1] & 0xffff};
//        System.out.print("Raw data: ");
//        for (int i = 0; i < data.length; i++) {
//            System.out.printf("%X ",data[i]);
//        }
//        System.out.println();
        int value;
        if (leastSignificantRegisterFirst) value = (idata[1] << 16) + idata[0];
        else value = (idata[0] << 16) + idata[1];
//        System.out.printf("val = %X\n",value);
        return Float.intBitsToFloat(value);
    }
}
