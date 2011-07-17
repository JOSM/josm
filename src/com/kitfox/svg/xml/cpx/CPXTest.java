/*
 * CPXTest.java
 *
 *
 *  The Salamander Project - 2D and 3D graphics libraries in Java
 *  Copyright (C) 2004 Mark McKay
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  Mark McKay can be contacted at mark@kitfox.com.  Salamander and other
 *  projects can be found at http://www.kitfox.com
 *
 * Created on February 12, 2004, 2:45 PM
 */

package com.kitfox.svg.xml.cpx;

import java.io.*;
import java.net.*;

/**
 * @author Mark McKay
 * @author <a href="mailto:mark@kitfox.com">Mark McKay</a>
 */
public class CPXTest {

    /** Creates a new instance of CPXTest */
    public CPXTest() {

//        FileInputStream fin = new FileInputStream();
        writeTest();
        readTest();
    }

    public void writeTest()
    {
        try {

            InputStream is = CPXTest.class.getResourceAsStream("/data/readme.txt");
//System.err.println("Is " + is);

            FileOutputStream fout = new FileOutputStream("C:\\tmp\\cpxFile.cpx");
            CPXOutputStream cout = new CPXOutputStream(fout);

            byte[] buffer = new byte[1024];
            int numBytes;
            while ((numBytes = is.read(buffer)) != -1)
            {
                cout.write(buffer, 0, numBytes);
            }
            cout.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void readTest()
    {
        try {

//            InputStream is = CPXTest.class.getResourceAsStream("/rawdata/test/cpx/text.txt");
//            InputStream is = CPXTest.class.getResourceAsStream("/rawdata/test/cpx/cpxFile.cpx");
            FileInputStream is = new FileInputStream("C:\\tmp\\cpxFile.cpx");
            CPXInputStream cin = new CPXInputStream(is);

            BufferedReader br = new BufferedReader(new InputStreamReader(cin));
            String line;
            while ((line = br.readLine()) != null)
            {
                System.err.println(line);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new CPXTest();
    }

}
