package visibility.algorithm;

import java.util.Comparator;
import java.util.Random;
import java.util.function.IntFunction;

public class ArrayUtils {

    public static <E> int partition(E[] arr, int left, int right, int pivot, Comparator<? super E> cmp) {
        E pivotVal = arr[pivot];
        swap(arr, pivot, right);
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (cmp.compare(arr[i], pivotVal) < 0) {
                swap(arr, i, storeIndex);
                storeIndex++;
            }
        }
        swap(arr, right, storeIndex);
        return storeIndex;
    }

    /**
     * This method and all its called methods are ripped of from Rosetta code.
     */
    public static <E> E quickSelect(E[] arr, int n, Comparator<? super E> cmp) {
        int left = 0;
        int right = arr.length - 1;
        Random rand = new Random();
        while (right >= left) {
            int pivotIndex = partition(arr, left, right, rand.nextInt(right - left + 1) + left, cmp);
            if (pivotIndex == n) {
                return arr[pivotIndex];
            } else if (pivotIndex < n) {
                left = pivotIndex + 1;
            } else {
                right = pivotIndex - 1;
            }
        }
        return null;
    }

    public static <E> void swap(E[] arr, int i1, int i2) {
        if (i1 != i2) {
            E temp = arr[i1];
            arr[i1] = arr[i2];
            arr[i2] = temp;
        }
    }

    public static <E> E[] concat(E[] a, E[] b, IntFunction<E[]> makeArr) {
        E[] ret = makeArr.apply(a.length + b.length);
        System.arraycopy(a, 0, ret, 0, a.length);
        System.arraycopy(b, 0, ret, a.length, b.length);
        return ret;
    }
}
