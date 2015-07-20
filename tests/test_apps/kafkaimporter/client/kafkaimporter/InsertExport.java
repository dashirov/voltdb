/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package kafkaimporter.client.kafkaimporter;

import java.io.IOException;

import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;


public class InsertExport {
    final Client m_client;

    public InsertExport(Client client) {
        m_client = client;
    }

    public void insertExport(long key, long value) {
        try {
            m_client.callProcedure(new InsertCallback(), "InsertExport", key, value);
        } catch (IOException e) {
            System.out.println("Exception calling stored procedure InsertExport");
            e.printStackTrace();
        }
    }

    public void insertFinal(long key, long value) {
        try {
            m_client.callProcedure(new InsertCallback(), "InsertFinal", key, value);
        } catch (IOException e) {
            System.out.println("Exception calling stored procedure InsertFinal");
            e.printStackTrace();
        }
    }

    static class InsertCallback implements ProcedureCallback {

        @Override
        public void clientCallback(ClientResponse clientResponse)
                throws Exception {
            if (clientResponse.getStatus() != ClientResponse.SUCCESS) {
                System.err.println(clientResponse.getStatusString());
            }
        }

    }
}