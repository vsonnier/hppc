package com.carrotsearch.hppc.sorting;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.hppc.AbstractKTypeTest;


/**
 * Unit tests for {@link KTypeSort}.
 */
public class KTypeSortTest<KType> extends AbstractKTypeTest<KType>
{
    enum DataDistribution
    {
        ORDERED, SAWTOOTH, RANDOM, STAGGER, PLATEAU, SHUFFLE
    }

    enum Algorithm
    {
        QUICKSORT, QUICKSORT_COMPARATOR
    }

    public KTypeSortTest()
    {
        //nothing
    }

    @Test
    public void testQuicksort()
    {
        sortCertification(Algorithm.QUICKSORT);
    }

    @Test
    public void testQuicksortComparator()
    {
        sortCertification(Algorithm.QUICKSORT_COMPARATOR);
    }

    /**
     * Run a "sort certification" test.
     */
    private void sortCertification(final Algorithm algorithm)
    {
        final int[] n_values =
            {
                100, 1023, 1024, 1025, 1024 * 32
            };

        for (final int n : n_values)
        {
            for (int m = 1; m < 2 * n; m *= 2)
            {
                for (final DataDistribution dist : DataDistribution.values())
                {
                    final KType[] x = generate(dist, n, m);

                    final String testName = dist + "-" + n + "-" + m;
                    testOn(algorithm, x, testName + "-normal");
                    testOn(algorithm, reverse(x, 0, n), testName + "-reversed");
                    testOn(algorithm, reverse(x, 0, n / 2), testName + "-reversed_front");
                    testOn(algorithm, reverse(x, n / 2, n), testName + "-reversed_back");
                    testOn(algorithm, sort(x), testName + "-sorted");
                    testOn(algorithm, dither(x), testName + "-dither");
                }
            }
        }
    }

    /**
     * Generate <code>n</code>-length data set distributed according to <code>dist</code>.
     * 
     * @param m Step for sawtooth, stagger, plateau and shuffle.
     */
    private KType[] generate(final DataDistribution dist, final int n, final int m)
    {
        // Start from a constant seed (repeatable tests).
        final Random rand = new Random(0xBADCAFE);

        //generate an array of KType
        final KType[] x = asArray(new int[n]);

        for (int i = 0, j = 0, k = 1; i < n; i++)
        {
            switch (dist)
            {
                case ORDERED:
                    x[i] = castComparable(i);
                    break;
                case SAWTOOTH:
                    x[i] = castComparable(i % m);
                    break;
                case RANDOM:
                    x[i] = castComparable(rand.nextInt() % m);
                    break;
                case STAGGER:
                    x[i] = castComparable((i * m + i) % n);
                    break;
                case PLATEAU:
                    x[i] = castComparable(Math.min(i, m));
                    break;
                case SHUFFLE:
                    x[i] = castComparable((rand.nextInt() % m) != 0 ? (j += 2) : (k += 2));
                    break;
                default:
                    throw new RuntimeException();
            }
        }

        return x;
    }

    private KType[] sort(KType[] x)
    {
        x = copy(x);

        /*! #if (! $TemplateOptions.KTypeBoolean) !*/
        Arrays.sort(x);
        /*! #else
        x = specialBooleanSort(x);
        #end !*/

        return x;
    }

    private KType[] dither(KType[] x)
    {
        x = copy(x);
        for (int i = 0; i < x.length; i++)
            x[i] = castComparable(castType(x[i]) + i % 5);

        return x;
    }

    private KType[] reverse(KType[] x, final int start, final int end)
    {
        x = copy(x);
        for (int i = start, j = end - 1; i < j; i++, j--)
        {
            final KType v = x[i];
            x[i] = x[j];
            x[j] = v;
        }
        return x;
    }

    @SuppressWarnings("unchecked")
    private void testOn(final Algorithm algo, final KType[] order, final String testName)
    {
        //natural ordering comparator
        final KTypeComparator<KType> comp = new KTypeComparator<KType>() {

            @Override
            public int compare(final KType e1, final KType e2)
            {
                int res = 0;

                if (castType(e1) < castType(e2))
                {
                    res = -1;
                }
                else if (castType(e1) > castType(e2))
                {
                    res = 1;
                }

                return res;
            }
        };

        switch (algo)
        {
            case QUICKSORT:
                //the supplied KType[] are also Numbers in generics, so are
                //Comparable
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                final Comparable[] orderComparable = newComparableArray(order);
                /*! #else
                KType[] orderComparable = (KType[]) order;
                #end !*/
                KTypeSort.quicksort(orderComparable);
                assertOrder((KType[]) orderComparable, orderComparable.length, testName);
                break;

            case QUICKSORT_COMPARATOR:
                KTypeSort.quicksort(order, comp);
                assertOrder(order, order.length, testName);
                break;
            default:
                Assert.fail();
                throw new RuntimeException();
        }


    }

    /**
     * Test natural ordering
     * @param expected
     * @param actual
     * @param length
     */
    private void assertOrder(final KType[] order, final int length, final String testName)
    {
        for (int i = 1; i < length; i++)
        {
            if (castType(order[i - 1]) > castType(order[i]))
            {
                Assert.assertTrue(String.format("%s: Not ordered: (previous, next) = (%d, %d) at index %d",
                        testName, castType(order[i - 1]), castType(order[i]), i), false);
            }
        }
    }

    private KType[] copy(final KType[] x)
    {
        return newArray(x);
    }

    private boolean[] specialBooleanSort(final boolean[] inputBoolean)
    {
        //sort as is :
        // a) count the number of false : nbFalse
        // b) count the number of true : nbTrue
        //then the sorted result is made of nbFalse "false" elements,
        //followed by nbTrue "true" elements.

        int nbFalse = 0;
        int nbTrue = 0;

        for (int ii = 0; ii < inputBoolean.length; ii++)
        {
            if (inputBoolean[ii])
            {
                nbTrue++;
            }
            else
            {
                nbFalse++;
            }
        }

        //sorted
        final boolean[] out = new boolean[inputBoolean.length];

        for (int ii = 0; ii < nbFalse; ii++)
        {
            out[ii] = false;
        }

        for (int ii = nbFalse; ii < nbFalse + nbTrue; ii++)
        {
            out[ii] = true;
        }

        return out;
    }


}
