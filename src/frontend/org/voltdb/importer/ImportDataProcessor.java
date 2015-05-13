/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.importer;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.voltdb.CatalogContext;

/**
 * Interface ImportDataProcessor imposes on processors.
 *
 */
public interface ImportDataProcessor  {

    public static final String IMPORT_MODULE = "__IMPORT_MODULE__";
    public static final String IMPORTER_CLASS = "impl";
    public static final String IMPORTER_SERVICE_CLASS = "org.voltdb.importer.ImportHandlerProxy";
    /**
     * Inform the processor that initialization is complete; commence work.
     * @param context
     * @param partitions list of partitions we are responsible for.
     */
    public void readyForData(CatalogContext context, List<Integer> partitions);

    /**
     * The system is terminating. Cleanup and exit the processor.
     */
    public void shutdown();

    /**
     * Pass processor specific processor configuration properties
     * @param config an instance of {@linkplain Properties}
     */
    public void setProcessorConfig(Map<String, Properties> config);

}