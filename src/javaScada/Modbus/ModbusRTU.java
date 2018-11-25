package javaScada.Modbus;

import jssc.SerialPort;
import jssc.SerialPortException;

/*  Класс для обмена данными с устройствами по Modbus
 *
 */
import java.util.*;

public class ModbusRTU extends SerialPort {
    public int timeout = 1000;//при желании можно поменять
    private int timeoutCount = 0;
    private HashMap<String, Device> devices;    //таблица устройств
    private int baudrate, dataBits, stopBits, parity;

    public ModbusRTU(String portName, int baudrate, int dataBits, int stopBits, int parity) throws SerialPortException {
        super(portName);
        this.baudrate = baudrate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
        devices = new HashMap<>();
        openPort();
        setParams(baudrate, dataBits, stopBits, parity);
    }

    public void addDevice(String name, Device d) throws SerialPortException {
        if (devices.containsKey(name))
            throw new SerialPortException(getPortName(),
                    "addDevice @ ModbusRTU",
                    "Device " + name + " already exists");
        for (Device device : devices.values()) {
            if (d.getAddress() == device.getAddress())
                throw new SerialPortException(getPortName(),
                        "addDevice @ ModbusRTU",
                        "Device with address" + String.valueOf(d.getAddress()) + "already exists");
        }
        devices.put(name, d);
    }

    public void pollDevice(String name) throws SerialPortException {   //опрос устройства
        Device d = devices.get(name);
        if (d == null)
            throw new SerialPortException(getPortName(), "pollDevice :: String", "device " + name + " not found");
        ArrayList<Variable> vars = new ArrayList<>(d.getVars().values());
        Collections.sort(vars, Comparator.comparing(Variable::getAddress));//сортируем регистры по возрастанию адреса
        pollDevice(d.getAddress(), vars, 0);
    }

    private void pollDevice(int devAddr, ArrayList<Variable> vars, int shift)
            throws SerialPortException {//опрос нескольких регистров подряд
        int registersToPoll = 1;
        Variable first = vars.get(shift);//первый регистр
        int addr1 = first.getAddress();
        int bytes = first.getLength();//счетчик числа байт на запись
        int len = bytes;//длина регистра
        for (int i = shift + 1; i < vars.size(); i++) {
            Variable current = vars.get(i);//следующий регистр
            int addr2 = current.getAddress();
            if (addr2 > addr1 + len)
                break;//если расположен не вплотную к прежнему - формирование пакета регистров завершено
            registersToPoll++;
            bytes += current.getLength();
            addr1 = addr2;
            len = current.getLength();
        }
        byte[] response = readRequest(devAddr, first.getAddress(), bytes);//отправка запроса и прием ответа
        int currentByte = 3;
        for (int i = shift; i < shift + registersToPoll; i++) {
            Variable var = vars.get(i);
            len = var.getLength();
            short[] registers = new short[len];
            for (int j = 0; j < len; j++) {
                registers[j] = (short) (
                        (response[currentByte + j * 2] << 8) +
                                (response[currentByte + j * 2 + 1] & 0xff) & 0xffff
                );
            }
            var.putRawData(registers);
            currentByte += len * 2;
        }
        if (shift + registersToPoll < vars.size()) pollDevice(devAddr, vars, shift + registersToPoll);
    }

    private byte[] readRequest(int address, int fromRegister, int bytes) //опрос переменных контроллера
            throws SerialPortException {
        byte[] message = new byte[6];
        byte[] response = null;
        message[0] = (byte) (address);//формируем посылку запроса
        message[1] = 0x03;
        message[2] = (byte) ((fromRegister & 0x0000ff00) >>> 8);
        message[3] = (byte) (fromRegister & 0x000000ff);
        message[4] = (byte) ((bytes & 0x0000ff00) >>> 8);
        message[5] = (byte) (bytes & 0x000000ff);
        byte[] crc = checkSum(message);
        byte[] outdata = new byte[8];
        System.arraycopy(message, 0, outdata, 0, 6);
        System.arraycopy(crc, 0, outdata, 6, 2);
        writeBytes(outdata);//отправляем
        int cycles = 0;
        int hasRead = 0;
        int needToRead = 5 + bytes * 2;
        while (hasRead < needToRead) {
            hasRead = getInputBufferBytesCount();
            if (hasRead >= needToRead) timeoutCount = 0;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            if (cycles > (timeout / 100)) {
                timeoutCount++;
                System.err.printf("has read only %d bytes\n", getInputBufferBytesCount());
                break;
            }
            cycles++;
        }
        if (timeoutCount > 10) {
            closePort();
            System.err.println("try to reopen port...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            openPort();
            setParams(baudrate, dataBits, stopBits, parity);
        }
        if (timeoutCount > 12) throw new SerialPortException(getPortName(), "readRequest", "cannot to read device");
        if (hasRead < needToRead) {
            if (hasRead > 0) readBytes(getInputBufferBytesCount());
            throw new SerialPortException(getPortName(), "readRequest", "timeout");
        }
        timeoutCount = 0;
        byte[] inData = readBytes(5 + bytes * 2);//получаем ответ
        int n = inData.length - 2;
        response = new byte[n];
        System.arraycopy(inData, 0, response, 0, n);
        System.arraycopy(inData, n, crc, 0, 2);
        byte[] check = checkSum(response);//проверяем контрольную сумму. Если всё ок - возвращаем результат.
        if (crc[0] == check[0] && crc[1] == check[1]) return response;
        throw new SerialPortException(getPortName(), "readRequest",
                String.format("bad Data Received: check == %X %X; calculated crc= %X %X",
                        check[1], check[0], crc[1], crc[0]));
    }

    private byte[] checkSum(byte[] message) {//CRC-16 контрольная сумма. Подсмотрена где-то в интернете и исправлена.
        int sum = 0xffff;
        byte[] arr = message;
        for (int i = 0; i < arr.length; i++) {
            int arrElem = ((int) arr[i]) & 0xff;
            sum = (sum ^ arrElem);
            for (int j = 0; j < 8; j++) {
                if ((sum & 0x1) == 1) {
                    sum >>>= 1;
                    sum = (sum ^ 0xA001);
                } else {
                    sum >>>= 1;
                }
            }
        }
        return new byte[]{(byte) (sum & 0x000000ff), (byte) ((sum & 0x0000ff00) >>> 8)};
    }
}
