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
}
