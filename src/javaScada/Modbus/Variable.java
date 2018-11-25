package javaScada.Modbus;

public abstract class Variable<T> {
    private int address;
    private int length;
    private short[] inData;
    private short[] outData;
    private T inValue;
    private T outValue;
    private boolean leastSignificantBitFirst;//обратный порядок битов

    public Variable(int address, int length, boolean slbFirst){
        this.address = address;
        this.length = length;
        leastSignificantBitFirst = slbFirst;
        outData = new short[length];
    }

    short[] getRawData() {//используется для чтения полученных от устройства данных
        if (leastSignificantBitFirst) return reverse(outData);
        return outData;
    }

    void writeRawData(short[] data) {//используется для записи данных на отправку
        if (leastSignificantBitFirst) inData = reverse(data);
        else inData = data;
    }

    short[] extractRawData(){//используется для получения данных на отправку
        if (leastSignificantBitFirst) return reverse(inData);
        return inData;
    }

    void putRawData(short[] data){//используется для размещения принятых от устройства данных
        if (leastSignificantBitFirst) outData = reverse(data);
        else outData = data;
    }

    public abstract void putData (T value);
    public abstract T getData();

    private short reverse (short b){
        int c;
        c = ((b & 0x5555) << 1) + ((b & 0xAAAA) >>> 1);
        c = ((c & 0x3333) << 2) + ((c & 0xCCCC) >>> 2);
        c = ((c & 0x0F0F) << 4) + ((c & 0xF0F0) >>> 4);
        c = ((c & 0x00FF) << 8) + ((c & 0xFF00) >>> 8);
        return (short) c;
    }

    private short[] reverse (short[] b){
        short[] c = new short[b.length];
        for (int i = 0; i < b.length; i++) {
            c[i] = reverse(b[i]);
        }
        return c;
    }

    public int getAddress (){
        return address;
    }

    public int getLength() {
        return length;
    }
}
