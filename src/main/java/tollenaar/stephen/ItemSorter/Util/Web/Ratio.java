package tollenaar.stephen.ItemSorter.Util.Web;

import java.io.Serializable;

public class Ratio implements Serializable {

	private static final long serialVersionUID = -2636942689733341355L;
	private int first;
	private int second;
	private boolean firstSelector = true;
	private int count = 0;

	public Ratio(int first, int second) {
		this.first = first;
		this.second = second;
	}

	public int getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public int getSecond() {
		return second;
	}

	public void setSecond(int second) {
		this.second = second;
	}

	public boolean isFirstSelector() {
		return firstSelector;
	}

	public void setFirstSelector(boolean firstSelector) {
		this.firstSelector = firstSelector;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

    public boolean allows(boolean output, boolean below) {
        return firstSelector ? output : below;
    }

    public void addCount() {
        count++;
        if (count >= (firstSelector ? first : second)) {
            count = 0;
            firstSelector = !firstSelector;
        }
    }

	public void reverseCount() {
		this.count--;
		if (count < 0) {
			if (isFirstSelector()) {
				setCount(getSecond() - 1);
			} else {
				setCount(getFirst() - 1);
			}
			setFirstSelector(!isFirstSelector());
		}
	}

}
