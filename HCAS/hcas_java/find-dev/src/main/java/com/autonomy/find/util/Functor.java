package com.autonomy.find.util;

/**
 * Partially restrictive fmap
 *
 * User: liam.goodacre
 * Date: 23/01/13
 * Time: 09:40
 * To change this template use File | Settings | File Templates.
 */
public interface Functor<A> {
    <B> Functor<B> fmap(F1<A, B> a);
}
