import serial, time, mmap

#File SETUP
lines = []
with open("../log.txt", "r") as input_file:
 for line in input_file:
            if "01 01 01 08 00 FF" in line:
                var = line.split(",")
            elif "00 00 01 00 00 FF" in line:
                var2 = line.split(",")
            elif "01 01 03 08 00 FF" in line:
                var3 = line.split(",")
            elif "01 01 02 08 00 FF" in line: 
                var4 = line.split(",")                
            elif "01 01 04 08 00 FF" in line:
                var5 = line.split(",")
            elif "01 01 0D 07 00 FF" in line:
                var6 = line.split(",")
            elif "01 01 0E 07 00 FF" in line:
                var7 = line.split(",")
            elif "01 01 20 07 00 FF" in line:
                var8 = line.split(",")
            elif "01 01 34 07 00 FF" in line:
                var9 = line.split(",")
            elif "01 01 48 07 00 FF" in line:
                var10 = line.split(",")
            elif "01 01 1F 07 00 FF" in line:
                var11 = line.split(",")
            elif "01 01 33 07 00 FF" in line:
                var12 = line.split(",")
            elif "01 01 47 07 00 FF" in line:
                var13 = line.split(",")

Ener_Ac_Im      = int(var[2])
Ener_Ac_Im_Es   = (int(var[4]) & (2**8-1))
Hora            = var2[2]
Ener_Reac_Im    = int(var3[2])
Ener_Reac_Im_Es = (int(var3[4]) & (2**8-1))
Ener_Ac_Ex      = int(var4[2])
Ener_Ac_Ex_Es   = (int(var4[4]) & (2**8-1))
Ener_Reac_Ex    = int(var5[2])
Ener_Reac_Ex_Es = (int(var5[4]) & (2**8-1))
FP              = int(var6[2])
FP_Es           = (int(var6[4]) & (2**8-1))
Frec            = int(var7[2])
Frec_Es         = (int(var7[4]) & (2**8-1))
Ten_l1          = int(var8[2])
Ten_l1_Es       = (int(var8[4]) & (2**8-1))
Ten_l2          = int(var9[2])
Ten_l2_Es       = (int(var9[4]) & (2**8-1))
Ten_l3          = int(var10[2])
Ten_l3_Es       = (int(var10[4]) & (2**8-1))
Corri_l1        = int(var11[2])
Corri_l1_Es     = (int(var11[4]) & (2**8-1))
Corri_l2        = int(var12[2])
Corri_l2_Es     = (int(var12[4]) & (2**8-1))
Corri_l3        = int(var13[2])
Corri_l3_Es     = (int(var13[4]) & (2**8-1))
#print("Hora "+ Hora.replace(" ", ""))
#print("Energia Activa importada " + str(hex(Ener_Ac_Im)[2:]).upper() + " Escala " + str(hex(Ener_Ac_Im_Es)[2:]))

Cadena = Hora.replace(" ", "") + str(hex(Ener_Ac_Im)[2:]).upper() + str(hex(Ener_Ac_Im_Es)[2:]) + \
str(hex(Ener_Reac_Im)[2:]) + str(hex(Ener_Reac_Im_Es)[2:]) + str(hex(Ener_Ac_Ex)[2:]) + \
str(hex(Ener_Ac_Ex_Es)[2:]) + str(hex(Ener_Reac_Ex)[2:]) + str(hex(Ener_Reac_Ex_Es)[2:]) + \
str(hex(FP)[2:]) + str(hex(FP_Es)[2:]) + str(hex(Frec)[2:]) + str(hex(Frec_Es)[2:]) + \
str(hex(Ten_l1)[2:]) + str(hex(Ten_l1_Es)[2:]) + str(hex(Ten_l2)[2:]) + str(hex(Ten_l2_Es)[2:]) + \
str(hex(Ten_l3)[2:]) + str(hex(Ten_l3_Es)[2:]) + str(hex(Corri_l1)[2:]) + str(hex(Corri_l1_Es)[2:]) + \
str(hex(Corri_l2)[2:]) + str(hex(Corri_l2_Es)[2:]) + str(hex(Corri_l3)[2:]) + str(hex(Corri_l3_Es)[2:])

print("python:%s"%Cadena)

"""
x = [Cadena[start:start+num] for start in range(0, len(Cadena), num)]
for i in range(0,(len(x))):
	#write data
        print("AT$GI?\r\n")
        time.sleep(1)
        print("AT$RC\r\n")
        time.sleep(1)
        print('AT$SF=%s\r\n' % x[i])
        #print(x[i])
        #print("\r\n")
        time.sleep(5)
"""
#Serial SETUP
ser = serial.Serial()
ser.port = "/dev/ttyUSB1"
ser.baudrate = 9600
ser.bytesize = serial.EIGHTBITS
ser.parity = serial.PARITY_NONE
ser.stopbits = serial.STOPBITS_ONE
ser.timeout = 1
ser.xonxoff = False
ser.rtscts = False
ser.dsrdtr = False
ser.writeTimeout = 2

try: 
    ser.open()
except Exception, e:
    print "error open serial port: " + str(e)
    exit()

if ser.isOpen():

    try:
        ser.flushInput()
        ser.flushOutput()
        ser.write("AT$GI?\r\n")
        time.sleep(1)
        ser.write("AT$RC\r\n")
        time.sleep(1)
        num = 24
        x = [Cadena[start:start+num] for start in range(0, len(Cadena), num)]
        for i in range(0,(len(x))):
            #write data
            ser.write('AT$SF=%s\r\n' % x[i])
            print(x[i])
            #ser.write("AT$SF=")
            #ser.write(x[i])
            #ser.write("\r\n")
            time.sleep(2)

        ser.close()
    except Exception, e1:
        print "error communicating...: " + str(e1)

else:
    print "cannot open serial port "


#https://docs.pycom.io/pycom_esp32/library/network.Sigfox.html
#There will be a 20 second delay after every 2 packets.

