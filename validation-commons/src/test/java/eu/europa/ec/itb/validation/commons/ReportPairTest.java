package eu.europa.ec.itb.validation.commons;

import com.gitb.tr.TAR;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportPairTest {

    @Test
    void testAssignments() {
        var instance1 = new TAR();
        var instance2 = new TAR();
        var pair = new ReportPair(instance1, instance2);
        assertSame(instance1, pair.getDetailedReport());
        assertSame(instance2, pair.getAggregateReport());
        assertThrows(NullPointerException.class, () -> new ReportPair(null, instance2));
        assertThrows(NullPointerException.class, () -> new ReportPair(null, null));
        assertDoesNotThrow(() -> new ReportPair(instance1, null));
    }

}
