/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.hk2.tests.locator.messaging.operation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

/**
 * @author jwells
 *
 */
@EventReceivingOperation @MessageReceiver
public class EventReceivingService {
    private final static Map<Integer, List<Integer>> EVENT_MAP = new HashMap<Integer, List<Integer>>();
    private final static AtomicInteger ID_GENERATOR = new AtomicInteger();
    
    private final int id;
    
    public EventReceivingService() {
        id = ID_GENERATOR.getAndIncrement();
    }
    
    public int doOperation() {
        return id;
    }
    
    public void subscriber(@SubscribeTo int eventId) {
        synchronized (EventReceivingService.class) {
            List<Integer> events = EVENT_MAP.get(id);
            if (events == null) {
                events = new LinkedList<Integer>();
                EVENT_MAP.put(id, events);
            }
            
            events.add(eventId);
        }
    }
    
    public static Map<Integer, List<Integer>> getEventMap() {
        synchronized (EventReceivingService.class) {
            HashMap<Integer, List<Integer>> retVal = new HashMap<Integer, List<Integer>>();
            
            for (Map.Entry<Integer, List<Integer>> entry : EVENT_MAP.entrySet()) {
                List<Integer> valueCopy = new LinkedList<Integer>(entry.getValue());
                
                retVal.put(entry.getKey(), valueCopy);
            }
            
            return retVal;
        }
        
    }

}
