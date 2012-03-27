/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.jvnet.hk2.internal;

import java.util.HashSet;
import java.util.LinkedList;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.InjectionPointValidator;
import org.glassfish.hk2.api.MultiException;

/**
 * @author jwells
 *
 */
public class DynamicConfigurationImpl implements DynamicConfiguration {
    private final ServiceLocatorImpl locator;
    private final LinkedList<SystemDescriptor<?>> allDescriptors = new LinkedList<SystemDescriptor<?>>();
    private final HashSet<InjectionPointValidator> allValidators = new HashSet<InjectionPointValidator>();
    
    private final Object lock = new Object();
    private boolean committed = false;
    private boolean commitable = true;

    /* package */ DynamicConfigurationImpl(ServiceLocatorImpl locator) {
        this.locator = locator;
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Configuration#bind(org.glassfish.hk2.api.Descriptor)
     */
    @Override
    public ActiveDescriptor<?> bind(Descriptor key) {
        checkState();
        if ((key == null) || (key.getImplementation() == null)) throw new IllegalArgumentException();
        
        SystemDescriptor<?> sd = new SystemDescriptor<Object>(key, new Long(locator.getLocatorId()));
        
        allDescriptors.add(sd);
        
        return sd;
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Configuration#bind(org.glassfish.hk2.api.FactoryDescriptors)
     */
    @Override
    public FactoryDescriptors bind(FactoryDescriptors factoryDescriptors) {
        if (factoryDescriptors == null) throw new IllegalArgumentException("factoryDescriptors is null");
        
        // Now a bunch of validations
        Descriptor asService = factoryDescriptors.getFactoryAsService();
        Descriptor asFactory = factoryDescriptors.getFactoryAsAFactory();
        
        if (asService == null) throw new IllegalArgumentException("getFactoryAsService returned null");
        if (asFactory == null) throw new IllegalArgumentException("getFactoryAsFactory returned null");
        
        String implClassService = asService.getImplementation();
        String implClassFactory = asFactory.getImplementation();
        
        if (!Utilities.safeEquals(implClassService, implClassFactory)) {
            throw new IllegalArgumentException("The implementation classes must match (" +
                implClassService + "/" + implClassFactory + ")");
        }
        
        if (!asService.getDescriptorType().equals(DescriptorType.CLASS)) {
            throw new IllegalArgumentException("The getFactoryAsService descriptor must be of type CLASS");
        }
        if (!asFactory.getDescriptorType().equals(DescriptorType.FACTORY)) {
            throw new IllegalArgumentException("The getFactoryAsFactory descriptor must be of type FACTORY");
        }
        
        // Bind the factory first, so normally people get the factory, not the service
        final ActiveDescriptor<?> boundAsFactory = bind(asFactory);
        final ActiveDescriptor<?> boundAsService = bind(asService);
        
        return new FactoryDescriptors() {

            @Override
            public Descriptor getFactoryAsService() {
                return boundAsService;
            }

            @Override
            public Descriptor getFactoryAsAFactory() {
                return boundAsFactory;
            }
            
        };
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Configuration#addActiveDescriptor(org.glassfish.hk2.api.ActiveDescriptor)
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(ActiveDescriptor<T> activeDescriptor)
            throws IllegalArgumentException {
        checkState();
        if (activeDescriptor == null || !activeDescriptor.isReified()) {
            throw new IllegalArgumentException();
        }
        
        SystemDescriptor<T> retVal = new SystemDescriptor<T>(activeDescriptor,
                new Long(locator.getLocatorId()));
        
        allDescriptors.add(retVal);
        
        return retVal;
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Configuration#addValidator(org.glassfish.hk2.api.InjectionPointValidator)
     */
    @Override
    public void addValidator(InjectionPointValidator validator)
            throws IllegalArgumentException {
        if (validator == null) throw new IllegalArgumentException();
        
        allValidators.add(validator);
    }
    
    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.Configuration#addActiveDescriptor(java.lang.Class)
     */
    @Override
    public <T> ActiveDescriptor<T> addActiveDescriptor(Class<T> rawClass)
            throws IllegalArgumentException {
        ActiveDescriptor<T> ad = Utilities.createAutoDescriptor(rawClass, locator);
        
        return addActiveDescriptor(ad);
    }

    /* (non-Javadoc)
     * @see org.glassfish.hk2.api.DynamicConfiguration#commit()
     */
    @Override
    public void commit() throws MultiException {
        synchronized (lock) {
            checkState();
            if (!commitable) throw new IllegalStateException();
            
            committed = true;
        }
        
        locator.addConfiguration(this);
    }
    
    private void checkState() {
        synchronized (lock) {
            if (committed) throw new IllegalStateException();
        }
    }

    /**
     * @return the allDescriptors
     */
    LinkedList<SystemDescriptor<?>> getAllDescriptors() {
        return allDescriptors;
    }
    
    /**
     * @return the allResolvers
     */
    HashSet<InjectionPointValidator> getAllValidators() {
        return allValidators;
    }
    
    /* package */ void setCommitable(boolean commitable) {
        this.commitable = commitable;
        
    }

    

    
}
