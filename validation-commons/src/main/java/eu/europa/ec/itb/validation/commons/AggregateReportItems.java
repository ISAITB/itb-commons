package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.BAR;
import com.gitb.tr.ObjectFactory;
import com.gitb.tr.TestAssertionReportType;
import jakarta.xml.bind.JAXBElement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to aggregate validation report items in order to produce an aggregate report.
 */
public class AggregateReportItems {

    private static final Function<JAXBElement<TestAssertionReportType>, String> DEFAULT_CLASSIFIER = element -> String.format("[%s]|[%s]", element.getName().getLocalPart(), ((BAR)element.getValue()).getDescription());
    private final Map<String, AggregateReportItem> itemMap = new LinkedHashMap<>();
    private final ObjectFactory objectFactory;
    private final LocalisationHelper localiser;

    /**
     * Constructor.
     *
     * @param objectFactory The JAXB factory to build report items with.
     * @param localiser The localisation helper to use to localise texts.
     */
    public AggregateReportItems(ObjectFactory objectFactory, LocalisationHelper localiser) {
        this.objectFactory = objectFactory;
        this.localiser = localiser;
    }

    /**
     * Update aggregates for the provided report item. Aggregation takes place by comparing
     * the items' severity and description.
     *
     * @param element The JAXB element from the detailed report.
     */
    public void updateForReportItem(JAXBElement<TestAssertionReportType> element) {
        updateForReportItem(element, DEFAULT_CLASSIFIER);
    }

    /**
     * Update aggregates for the provided report item. Aggregation takes place by comparing
     * using a custom classification function.
     *
     * @param element The JAXB element from the detailed report.
     * @param classifierFn The classification function to use.
     */
    public void updateForReportItem(JAXBElement<TestAssertionReportType> element, Function<JAXBElement<TestAssertionReportType>, String> classifierFn) {
        if (element.getValue() instanceof BAR elementValue) {
            itemMap.computeIfAbsent(classifierFn.apply(element), k -> new AggregateReportItem(cloneElement(element.getName().getLocalPart(), elementValue))).addOne();
        } else {
            throw new IllegalStateException("Report items encountered having an unexpected class type ["+element.getValue().getClass()+"]");
        }
    }

    /**
     * Get the list of report items for the aggregated report.
     *
     * @return The report items.
     */
    public List<JAXBElement<TestAssertionReportType>> getReportItems() {
        return itemMap.values().stream().map(aggregateItem -> {
            var bar = (BAR)aggregateItem.firstItem.getValue();
            if (aggregateItem.counter > 1) {
                bar.setDescription(String.format("[%s] %s", localiser.localise("validator.label.reportItemTotalOccurrences", aggregateItem.counter), bar.getDescription()));
            }
            return aggregateItem.firstItem;
        }).toList();
    }

    /**
     * Clone the provided report item element.
     *
     * @param wrapperName The local name of the wrapping JAXB element.
     * @param source The original item's element.
     * @return The cloned element.
     */
    private JAXBElement<TestAssertionReportType> cloneElement(String wrapperName, BAR source) {
        var target = new BAR();
        target.setDescription(source.getDescription());
        target.setAssertionID(source.getAssertionID());
        target.setType(source.getType());
        target.setValue(source.getValue());
        target.setLocation(source.getLocation());
        target.setTest(source.getTest());
        return switch (wrapperName) {
            case "error" -> objectFactory.createTestAssertionGroupReportsTypeError(target);
            case "warning" -> objectFactory.createTestAssertionGroupReportsTypeWarning(target);
            default -> objectFactory.createTestAssertionGroupReportsTypeInfo(target);
        };
    }

    /**
     * Class to encapsulate the information for an aggregated report item (first occurrence and total count).
     */
    private static class AggregateReportItem {

        private final JAXBElement<TestAssertionReportType> firstItem;
        private long counter = 0;

        /**
         * Constructor.
         *
         * @param firstItem The first item to display.
         */
        private AggregateReportItem(JAXBElement<TestAssertionReportType> firstItem) {
            this.firstItem = firstItem;
        }

        /**
         * Increment the occurences.
         */
        private void addOne() {
            counter += 1;
        }

    }

}
