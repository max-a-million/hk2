/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
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
package org.glassfish.hk2.tests.basic.injected;

import org.jvnet.hk2.annotations.Inject;
import org.glassfish.hk2.Factory;
import org.glassfish.hk2.tests.basic.annotations.MarkerA;
import org.glassfish.hk2.tests.basic.annotations.MarkerB;
import org.glassfish.hk2.tests.basic.contracts.FactoryProvidedContractA;
import org.glassfish.hk2.tests.basic.contracts.FactoryProvidedContractB;
import org.glassfish.hk2.tests.basic.contracts.FactoryProvidedContractC;
import static org.junit.Assert.*;
import static org.glassfish.hk2.tests.basic.AssertionUtils.*;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ConstructorInjectedFactoryBindingTestClass {

    final FactoryProvidedContractA a;
    final FactoryProvidedContractB b;
    final FactoryProvidedContractC c_a;
    final FactoryProvidedContractC c_b;
    final FactoryProvidedContractC c_default;
    final Factory<FactoryProvidedContractA> pa;
    final Factory<FactoryProvidedContractB> pb;
    final Factory<FactoryProvidedContractC> pc_a;
    final Factory<FactoryProvidedContractC> pc_b;
    final Factory<FactoryProvidedContractC> pc_default;

    public ConstructorInjectedFactoryBindingTestClass(@Inject FactoryProvidedContractA a, @Inject FactoryProvidedContractB b, @Inject
            @MarkerA FactoryProvidedContractC c_a, @Inject
            @MarkerB FactoryProvidedContractC c_b, @Inject(optional=true) FactoryProvidedContractC c_default, @Inject Factory<FactoryProvidedContractA> pa, @Inject Factory<FactoryProvidedContractB> pb, @Inject
            @MarkerA Factory<FactoryProvidedContractC> pc_a, @Inject
            @MarkerB Factory<FactoryProvidedContractC> pc_b, @Inject(optional=true) Factory<FactoryProvidedContractC> pc_default) {
        this.a = a;
        this.b = b;
        this.c_a = c_a;
        this.c_b = c_b;
        this.c_default = c_default;
        this.pa = pa;
        this.pb = pb;
        this.pc_a = pc_a;
        this.pc_b = pc_b;
        this.pc_default = pc_default;
    }

    public void assertInjection() {
        // binding defined using (annonymous) factory instance
        assertInjectedInstance(FactoryProvidedContractAImpl.class, this.a);
        assertInjectedFactory(FactoryProvidedContractAImpl.class, this.pa);
        // binding defined using factory class
        assertInjectedInstance(FactoryProvidedContractBImpl.class, this.b);
        assertInjectedFactory(FactoryProvidedContractBImpl.class, this.pb);
        // binding defined using factory class and qualifier annotation
        assertInjectedInstance(FactoryProvidedContractCAImpl.class, this.c_a);
        assertInjectedFactory(FactoryProvidedContractCAImpl.class, this.pc_a);
        // binding defined using factory instance and qualifier annotation
        assertInjectedInstance(FactoryProvidedContractCBImpl.class, this.c_b);
        assertInjectedFactory(FactoryProvidedContractCBImpl.class, this.pc_b);
        // verifying null is returned for non-annotated binding that was not defined
        assertNull("No binding defined for the non-annotated contract. Provisioned instance should be null.", this.c_default);
        assertTrue("No binding defined for the non-annotated contract. Provider or returned instance should be null.", this.pc_default == null || this.pc_default.get() == null);
    }
}
