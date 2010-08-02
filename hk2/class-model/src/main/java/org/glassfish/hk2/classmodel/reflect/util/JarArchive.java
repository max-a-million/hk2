/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright 2010 Sun Microsystems, Inc. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License. You can obtain
 *  a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 *  or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 *  Sun designates this particular file as subject to the "Classpath" exception
 *  as provided by Sun in the GPL Version 2 section of the License file that
 *  accompanied this code.  If applicable, add the following below the License
 *  Header, with the fields enclosed by brackets [] replaced by your own
 *  identifying information: "Portions Copyrighted [year]
 *  [name of copyright owner]"
 *
 *  Contributor(s):
 *
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package org.glassfish.hk2.classmodel.reflect.util;

import org.glassfish.hk2.classmodel.reflect.ArchiveAdapter;

import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Jar based archive abstraction
 */
public class JarArchive implements ArchiveAdapter {

    private final JarFile jar;
    private final URI uri;

    public JarArchive(URI uri) throws IOException
    {
        File f = new File(uri);
        this.uri = uri;
        this.jar = new JarFile(f);    
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
     public void onEachEntry(EntryTask task) throws IOException {
         Enumeration<JarEntry> enumEntries = jar.entries();
         while(enumEntries.hasMoreElements()) {
             JarEntry ja = enumEntries.nextElement();
             if (ja.getName().endsWith(".jar")) {
                 InputStreamArchiveAdapter subArchive = null;
                 try {
                     URI subURI = null;
                     try {
                         subURI = new URI("jar:"+uri+"!/"+ja.getName());
                     } catch (URISyntaxException e) {
                         try {
                             subURI = new URI(ja.getName());
                         } catch (URISyntaxException e1) {
                             // ignore...
                         }
                     }
                     subArchive = new InputStreamArchiveAdapter(subURI,
                             jar.getInputStream(jar.getEntry(ja.getName())));
                     subArchive.onEachEntry(task);
                 } finally {
                     if (subArchive!=null) {
                         subArchive.close();
                    }
                 }
             }
             InputStream is = null;
             try {
                 is = jar.getInputStream(ja);
                 task.on(new Entry(ja.getName(), ja.getSize(), ja.isDirectory()), is);
             } finally {
                 if (is!=null) {
                     is.close();
                 }
             }
         }

     }
    

    @Override
    public Manifest getManifest() throws IOException {
        return jar.getManifest();
    }

    @Override
    public void close() throws IOException {
      jar.close();
    }
}
