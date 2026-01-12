/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.TestAssertionReportType;
import jakarta.xml.bind.JAXBElement;

import java.util.Comparator;

/**
 * Comparator to allow sorting of report items.
 */
public class ReportItemComparator implements Comparator<JAXBElement<TestAssertionReportType>> {

    /**
     * @see Comparator#compare(Object, Object)
     *
     * @param o1 First item.
     * @param o2 Second item.
     * @return Comparison check.
     */
    @Override
    public int compare(JAXBElement<TestAssertionReportType> o1, JAXBElement<TestAssertionReportType> o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return -1;
        } else if (o2 == null) {
            return 1;
        } else {
            String name1 = o1.getName().getLocalPart();
            String name2 = o2.getName().getLocalPart();
            if (name1.equals(name2)) {
                return 0;
            } else if ("error".equals(name1)) {
                return -1;
            } else if ("error".equals(name2)) {
                return 1;
            } else if ("warning".equals(name1)) {
                return -1;
            } else if ("warning".equals(name2)) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
