package io.johnsonlee.spi;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Represents a shadow of {@link ServiceLoader}
 *
 * @param <S> The type of service
 * @author johnsonlee
 */
public final class ShadowServiceLoader<S> implements Iterable<S> {

    private final Class<S> service;

    private final List<S> providers;

    private ShadowServiceLoader(final Class<S> service) {
        this.service = service;
        this.providers = Collections.unmodifiableList(ServiceRegistry.get(service));
    }

    @Override
    public Iterator<S> iterator() {
        return this.providers.iterator();
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
