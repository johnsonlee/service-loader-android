package io.johnsonlee.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static java.util.Collections.emptyList;


/**
 * Represents a registry of service provider
 *
 * @author johnsonlee
 */
public class ServiceRegistry {

    private final static Map<Class<?>, List<Callable<?>>> sRegistry = new HashMap<>();

    /**
     * Returns the implementations of the specified type of service
     *
     * @param service The class of service
     * @param <S>     The type of service
     * @return The instances of all implementations
     */
    @SuppressWarnings("unchecked")
    public static <S> List<S> get(final Class<S> service) {
        final List<Callable<?>> creators = sRegistry.getOrDefault(service, emptyList());
        if (creators.isEmpty()) {
            return emptyList();
        }

        try {
            final ArrayList<S> instances = new ArrayList<>(creators.size());
            for (final Callable<?> callable : creators) {
                instances.add(((S) callable.call()));
            }
            return Collections.unmodifiableList(instances);
        } catch (final Exception e) {
            throw new ServiceConfigurationError(service.getName(), e);
        }
    }

    /**
     * Returns a instance of the specified service
     *
     * @param service The class of service
     * @param <S>     The type of service
     * @return an instance of service
     */
    @SuppressWarnings("unchecked")
    public static <S> S single(final Class<S> service) {
        final Iterator<Callable<?>> i = sRegistry.getOrDefault(service, emptyList()).iterator();
        if (i.hasNext()) {
            try {
                return (S) i.next().call();
            } catch (final Exception e) {
                throw new ServiceConfigurationError(service.getName(), e);
            }
        }
        return null;
    }

    /**
     * Returns a instance of the specified service
     *
     * @param service    The class of service
     * @param comparator A comparator of service creator
     * @param <S>        The type of service
     * @return an instance of service
     */
    @SuppressWarnings("unchecked")
    public static <S> S single(final Class<S> service, final Comparator<Callable<?>> comparator) {
        final ArrayList<Callable<?>> creators = new ArrayList<>(sRegistry.getOrDefault(service, emptyList()));
        creators.sort(comparator);

        final Iterator<Callable<?>> i = creators.iterator();
        if (i.hasNext()) {
            try {
                return (S) i.next().call();
            } catch (final Exception e) {
                throw new ServiceConfigurationError(service.getName(), e);
            }
        }

        return null;
    }

    /**
     * Register a creator for the specified service
     *
     * @param service The class of service
     * @param creator The service creator
     * @param <S>     The type of service
     */
    public static <S> void register(final Class<S> service, final Callable<? extends S> creator) {
        sRegistry.computeIfAbsent(service, new Function<Class<?>, List<Callable<?>>>() {
            @Override
            public List<Callable<?>> apply(final Class<?> clazz) {
                return new ArrayList<>();
            }
        }).add(creator);
    }

    private ServiceRegistry() {
    }
}
