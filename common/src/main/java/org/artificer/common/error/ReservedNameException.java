/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artificer.common.error;

import org.artificer.common.i18n.Messages;


/**
 * Exception thrown when an artifact's custom property or generic relationship name duplicates one that's built-in.
 *
 * @author Brett Meyer
 */
public class ReservedNameException extends ArtificerConflictException {

    private static final long serialVersionUID = -3535532386495478538L;

    /**
     * Constructor.
     * @param name
     */
    public ReservedNameException(String name) {
        super(Messages.i18n.format("RESERVED_WORD", name)); //$NON-NLS-1$
    }

}
