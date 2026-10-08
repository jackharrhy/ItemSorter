package tollenaar.stephen.ItemSorter.Util.Web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatioTest {

    @Test
    void addCountSwitchesSidesAfterEachShare() {
        Ratio ratio = new Ratio(2, 1);
        assertTrue(ratio.isFirstSelector());

        ratio.addCount();
        assertTrue(ratio.isFirstSelector(), "one of two items sent to the first side");
        assertEquals(1, ratio.getCount());

        ratio.addCount();
        assertFalse(ratio.isFirstSelector(), "first side's share is full");
        assertEquals(0, ratio.getCount());

        ratio.addCount();
        assertTrue(ratio.isFirstSelector(), "second side's share of one is full");
        assertEquals(0, ratio.getCount());
    }

    @Test
    void reverseCountUndoesAddCount() {
        Ratio ratio = new Ratio(2, 3);
        ratio.addCount();
        ratio.reverseCount();

        assertTrue(ratio.isFirstSelector());
        assertEquals(0, ratio.getCount());
    }

    @Test
    void reverseCountStepsBackAcrossTheSwitch() {
        Ratio ratio = new Ratio(2, 3);
        ratio.reverseCount();

        assertFalse(ratio.isFirstSelector());
        assertEquals(2, ratio.getCount(), "lands on the last slot of the second side's share");
    }
    @Test void routingAndRollbackHoldAcrossSeveralCompleteCycles() {
        Ratio ratio = new Ratio(2, 3);
        for (int step = 0; step < 20; step++) {
            boolean output = step % 5 < 2;
            assertEquals(output, ratio.allows(true, false));
            assertEquals(!output, ratio.allows(false, true));
            assertFalse(ratio.allows(false, false));
            int count = ratio.getCount();
            ratio.addCount();
            ratio.reverseCount();
            assertEquals(output, ratio.isFirstSelector());
            assertEquals(count, ratio.getCount());
            ratio.addCount();
        }
    }
}
