/**
 * This class file was automatically generated by the AXDR compiler that is part of jDLMS (http://www.openmuc.org)
 */

package org.openmuc.jdlms.internal.asn1.cosem;

import java.io.IOException;
import java.io.InputStream;

import org.openmuc.jasn1.ber.BerByteArrayOutputStream;
import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrEnum;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrNull;
import org.openmuc.jdlms.internal.asn1.axdr.types.AxdrSequenceOf;

public class WriteResponse extends AxdrSequenceOf<WriteResponse.SubChoice> {

    @Override
    protected SubChoice createListElement() {
        return new SubChoice();
    }

    protected WriteResponse(int length) {
        super(length);
    }

    public WriteResponse() {
    } // Call empty base constructor

    public static class SubChoice implements AxdrType {

        public byte[] code = null;

        public static enum Choices {
            _ERR_NONE_SELECTED(-1),
            SUCCESS(0),
            DATA_ACCESS_ERROR(1),
            BLOCK_NUMBER(2),;

            private int value;

            private Choices(int value) {
                this.value = value;
            }

            public int getValue() {
                return this.value;
            }

            public static Choices valueOf(long tagValue) {
                Choices[] values = Choices.values();

                for (Choices c : values) {
                    if (c.value == tagValue) {
                        return c;
                    }
                }
                return _ERR_NONE_SELECTED;
            }
        }

        private Choices choice;

        public AxdrNull success = null;

        public AxdrEnum data_access_error = null;

        public Unsigned16 block_number = null;

        public SubChoice() {
        }

        public SubChoice(byte[] code) {
            this.code = code;
        }

        @Override
        public int encode(BerByteArrayOutputStream axdrOStream) throws IOException {
            if (code != null) {
                for (int i = code.length - 1; i >= 0; i--) {
                    axdrOStream.write(code[i]);
                }
                return code.length;

            }
            if (choice == Choices._ERR_NONE_SELECTED) {
                throw new IOException("Error encoding AxdrChoice: No item in choice was selected.");
            }

            int codeLength = 0;

            if (choice == Choices.BLOCK_NUMBER) {
                codeLength += block_number.encode(axdrOStream);
                AxdrEnum c = new AxdrEnum(2);
                codeLength += c.encode(axdrOStream);
                return codeLength;
            }

            if (choice == Choices.DATA_ACCESS_ERROR) {
                codeLength += data_access_error.encode(axdrOStream);
                AxdrEnum c = new AxdrEnum(1);
                codeLength += c.encode(axdrOStream);
                return codeLength;
            }

            if (choice == Choices.SUCCESS) {
                codeLength += success.encode(axdrOStream);
                AxdrEnum c = new AxdrEnum(0);
                codeLength += c.encode(axdrOStream);
                return codeLength;
            }

            // This block should be unreachable
            throw new IOException("Error encoding AxdrChoice: No item in choice was encoded.");
        }

        @Override
        public int decode(InputStream iStream) throws IOException {
            int codeLength = 0;
            AxdrEnum choosen = new AxdrEnum();

            codeLength += choosen.decode(iStream);
            resetChoices();
            this.choice = Choices.valueOf(choosen.getValue());

            if (choice == Choices.SUCCESS) {
                success = new AxdrNull();
                codeLength += success.decode(iStream);
                return codeLength;
            }

            if (choice == Choices.DATA_ACCESS_ERROR) {
                data_access_error = new AxdrEnum();
                codeLength += data_access_error.decode(iStream);
                return codeLength;
            }

            if (choice == Choices.BLOCK_NUMBER) {
                block_number = new Unsigned16();
                codeLength += block_number.decode(iStream);
                return codeLength;
            }

            throw new IOException("Error decoding AxdrChoice: Identifier matched to no item.");
        }

        public void encodeAndSave(int encodingSizeGuess) throws IOException {
            BerByteArrayOutputStream axdrOStream = new BerByteArrayOutputStream(encodingSizeGuess);
            encode(axdrOStream);
            code = axdrOStream.getArray();
        }

        public Choices getChoiceIndex() {
            return this.choice;
        }

        public void setsuccess(AxdrNull newVal) {
            resetChoices();
            choice = Choices.SUCCESS;
            success = newVal;
        }

        public void setdata_access_error(AxdrEnum newVal) {
            resetChoices();
            choice = Choices.DATA_ACCESS_ERROR;
            data_access_error = newVal;
        }

        public void setblock_number(Unsigned16 newVal) {
            resetChoices();
            choice = Choices.BLOCK_NUMBER;
            block_number = newVal;
        }

        private void resetChoices() {
            choice = Choices._ERR_NONE_SELECTED;
            success = null;
            data_access_error = null;
            block_number = null;
        }

        @Override
        public String toString() {
            if (choice == Choices.SUCCESS) {
                return "choice: {success: " + success + "}";
            }

            if (choice == Choices.DATA_ACCESS_ERROR) {
                return "choice: {data_access_error: " + data_access_error + "}";
            }

            if (choice == Choices.BLOCK_NUMBER) {
                return "choice: {block_number: " + block_number + "}";
            }

            return "unknown";
        }

    }

}
