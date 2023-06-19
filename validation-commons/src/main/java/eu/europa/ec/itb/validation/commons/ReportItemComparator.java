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
