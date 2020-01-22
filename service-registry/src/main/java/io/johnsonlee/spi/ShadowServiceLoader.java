package io.johnsonlee.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author johnsonlee
 */
public final class ShadowServiceLoader<S> implements Iterable<S> {

    private final Class<S> service;

    private final List<S> providers;

    private ShadowServiceLoader(final Class<S> service) {
        this.service = service;
        this.providers = new ArrayList<>(ServiceRegistry.get(service));
    }

    @Override
    public Iterator<S> iterator() {
        return ServiceRegistry.get(service).iterator();
    }

    public void reload() {
        // do nothing
    }

    public static <S> ShadowServiceLoader<S> load(final Class<S> service) {
        return new ShadowServiceLoader<>(service);
    }

    public static <S> ShadowServiceLoader<S> load(final Class<S> service, final ClassLoader loader) {
        return load(service);
    }

    public static <S> ShadowServiceLoader<S> loadInstalled(final Class<S> service) {
        return load(service);
    }

}
