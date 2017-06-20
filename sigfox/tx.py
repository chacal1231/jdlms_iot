import serial, time, mmap

#File SETUP
lines = []
var = []
var1 = []
with open("/home/mcmahonpc/Desktop/ikusi/jdlms_iot/log.txt", "r") as input_file:
 for line in input_file:
            if "01 01 01 08 00 FF" in line:
                for i in range(51,58):
                    var.append(line[i])
                    Ene_Ac_Imp = ''.join(map(str, var))
            elif "01 01 03 08 00 FF" in line:
                for i in range(51,58):
                    var1.append(line[i])
                    Ene_Reac_Imp = ''.join(map(str, var1))

print("La energia activa importada es " + Ene_Ac_Imp)
print("La energia re-activa importada es " + Ene_Reac_Imp)

'''
#Serial SETUP
ser = serial.Serial()
ser.port = "/dev/ttyUSB7"
ser.baudrate = 9600
ser.bytesize = serial.EIGHTBITS #number of bits per bytes
ser.parity = serial.PARITY_NONE #set parity check: no parity
ser.stopbits = serial.STOPBITS_ONE #number of stop bits
ser.timeout = 1            #non-block read
ser.xonxoff = False     #disable software flow control
ser.rtscts = False     #disable hardware (RTS/CTS) flow control
ser.dsrdtr = False       #disable hardware (DSR/DTR) flow control
ser.writeTimeout = 2     #timeout for write

try: 
    ser.open()
except Exception, e:
    print "error open serial port: " + str(e)
    exit()

if ser.isOpen():

    try:
        ser.flushInput() #flush input buffer, discarding all its contents
        ser.flushOutput()#flush output buffer, aborting current output 
                 #and discard all that is in buffer

        #write data
        ser.write("AT+CSQ")
        print("write data: AT+CSQ")

       time.sleep(0.5)  #give the serial port sometime to receive the data

       numOfLines = 0

       while True:
          response = ser.readline()
          print("read data: " + response)

        numOfLines = numOfLines + 1

        if (numOfLines >= 5):
            break

        ser.close()
    except Exception, e1:
        print "error communicating...: " + str(e1)

else:
    print "cannot open serial port "
    '''