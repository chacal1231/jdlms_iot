/*
 * Copyright 2012-17 Fraunhofer ISE
 *
 * This file is part of jDLMS.
 * For more information visit http://www.openmuc.org
 *
 * jDLMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jDLMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jDLMS.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.jdlms;

import java.io.IOException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.openmuc.jdlms.internal.asn1.axdr.AxdrType;

class ResponseQueue<E extends AxdrType> {

    private final BlockingDeque<Entry> queue = new LinkedBlockingDeque<>();

    public void put(int invokeId, E data) throws InterruptedException {
        queue.putFirst(new Entry(invokeId, data));
    }

    public E poll(int invokeId, long timeout) throws IOException {

        try {
            return pollForData(invokeId, timeout);

        } catch (InterruptedException e) {
            throw new IOException("Interrupted while waiting for incoming response");
        }
    }

    private E pollForData(int invokeId, long timeout) throws InterruptedException, ResponseTimeoutException {
        Entry tmp;
        while (true) {
            if (timeout == 0) {
                tmp = queue.takeFirst();
            }
            else {
                tmp = queue.poll(timeout, TimeUnit.MILLISECONDS);
                if (tmp == null) {
                    throw new ResponseTimeoutException("Timed out while waiting for incoming response.");
                }
            }

            if (tmp.invokeId == invokeId) {
                break;
            }
            else {
                this.queue.addLast(tmp);
                Thread.sleep(100L);
            }

        }
        return tmp.data;
    }

    private class Entry {
        private final E data;
        private final int invokeId;

        public Entry(int invokeId, E pdu) {
            this.invokeId = invokeId;
            this.data = pdu;
        }
    }
}
