package com.autonomy.find.util;

import com.autonomy.aci.content.identifier.reference.Reference;
import com.google.gson.internal.Pair;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;
import org.codehaus.jackson.map.type.MapType;
import org.codehaus.jackson.map.type.TypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * String Utilities
 */
public class CollUtils {


    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final TypeFactory typeFactory = TypeFactory.defaultInstance();

    /**
     * Concatenates elements of a sequence converted to strings, with
     * a separator between them.
     * <p/>
     * intersperse(" + ", [1, 5, 2, 7, 3])
     * //=> "1 + 5 + 2 + 7 + 3"
     *
     * @param sep   - The string separator
     * @param items - The collection to traverse
     * @param <A>   - Type of the objects to convert to string with _.toString()
     * @return - The resulting interspersed string
     */
    public static <A> String intersperse(
            final String sep,
            final Collection<? extends A> items
    ) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final A item : items) {
            if (!first) {
                result.append(sep);
            }
            result.append(item.toString());
            first = false;
        }

        return result.toString();
    }

    /**
     * Maps `new Reference(_).toString()` over a list of strings.
     *
     * @param input - List of reference strings to encode
     * @return - The resulting list of encoded references
     */
    public static List<String> mapReference(
            final List<String> input
    ) {
        final List<String> output = new LinkedList<String>();

        for (final String item : input) {
            output.add(new Reference(item).toString());
        }

        return output;
    }

    /**
     * Maps `new Reference(_).toString()` over a list of strings.
     *
     * @param input - List of reference strings to encode
     * @return - The resulting list of encoded references
     */
    public static List<String> mapReferencePlus(
            final List<String> input
    ) {
        final List<String> output = new LinkedList<String>();

        for (final String item : input) {
            output.add(new Reference(item).toString().replace("+", "%2B"));
        }

        return output;
    }


    /**
     * Applies a function object f to each element in a list.
     * Returns a list of the result values.
     *
     * @param f   - The function object to apply
     * @param xs  - The list of values
     * @param <A> - The input type
     * @param <B> - The output type
     * @return Result of mapping f over xs
     */
    public static <A, B> List<B> map(F1<A, B> f, List<A> xs) {
        final List<B> result = new LinkedList<B>();
        for (final A x : xs) {
            result.add(f.apply(x));
        }
        return result;
    }


    /**
     * Key swaps between two maps.
     * A key swap involves transforming the keys in a map by
     * looking them up in another map.
     *
     * Example:
     * from = { a |-> 1, b |-> 2 }
     * by = { a |-> A, b |-> B }
     * to = { A |-> 1, B |-> 2 }
     *
     * @param from
     * @param by
     * @param <A>
     * @param <B>
     * @param <C>
     * @return
     */
    public static <A, B, C> Map<C, B> keySwap(
            final Map<A, B> from,
            final Map<A, C> by
    ) {
        final Map<C, B> to = new HashMap<>();
        for (final Map.Entry<A, C> entry : by.entrySet()) {
            to.put(entry.getValue(), from.get(entry.getKey()));
        }
        return to;
    }


    public static <A> MTree<A> compileLinksToTree(
            final Map<A, ? extends Collection<A>> links,
            final A key
    ) {
        final List<MTree<A>> children = new LinkedList<>();

        final Collection<A> vs = links.get(key);
        if (vs != null) {
            for (final A childKey : vs) {
                children.add(compileLinksToTree(links, childKey));
            }
        }
        return MTree.node(key, children);
    }


    /**
     * Translates a json string into a list.
     *
     * @param content
     * @return
     * @throws java.io.IOException
     */
    public static List<String> jsonToList(final String content, final Class val) throws IOException {
        final CollectionType type = typeFactory.constructCollectionType(LinkedList.class, val);
        return jsonMapper.readValue(content, type);
    }


    /**
     * Translates a json string into a map.
     *
     * @param content
     * @return
     * @throws IOException
     */
    public static <K, V> Map<K, V> jsonToMap(final String content, final Class<K> key, final Class<V> val) throws IOException {
        final MapType type = typeFactory.constructMapType(LinkedHashMap.class, key, val);
        return jsonMapper.readValue(content, type);
    }

    /**
     * Attempts to convert a list of strings to a list of integers.
     *
     * @param values
     * @return
     */
    public static List<Double> stringsToDoubles(
			final List<String> values
	) {
        return map(stringToDouble, values);
    }

    /**
     * Function object to parse and integer from a string.
     */
    public final static F1<String, Double> stringToDouble = new F1<String, Double>() {
        public Double apply(final String strValue) {
            return Double.parseDouble(strValue);
        }
    };


    public static <A> LinkedList<A> list(final A... elements) {
        return list(Arrays.asList(elements));
    }

    public static <A> LinkedList<A> list(final Collection<? extends A> elements) {
        return new LinkedList<>(elements);
    }

    public static <A> ArrayList<A> arrayList(final A... elements) {
        return arrayList(Arrays.asList(elements));
    }

    public static <A> ArrayList<A> arrayList(final Collection<? extends A> elements) {
        return new ArrayList<>(elements);
    }

    public static <K, V> HashMap<K, V> pairMap(final Pair<K, V>... pairs) {
        final HashMap<K, V> result = new HashMap<>();
        for (final Pair<K, V> pair : pairs) {
            result.put(pair.first, pair.second);
        }
        return result;
    }

    public static <K, V> Pair<K, V> pair(final K key, final V val) {
        return new Pair<>(key, val);
    }
}

