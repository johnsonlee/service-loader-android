package io.johnsonlee.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.concurrent.Callable;
import java.util.function.Function;

class ServiceRegistry {

    private final static Map<Class<?>, List<Callable<?>>> REGISTRY = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public static <S> List<S> get(final Class<S> service) {
        final List<Callable<?>> creators = REGISTRY.getOrDefault(service, Collections.emptyList());
        if (creators.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            final ArrayList<S> instances = new ArrayList<>(creators.size());
            for (final Callable<?> callable : creators) {
                instances.add(((S) callable.call()));
            }
            return Collections.unmodifiableList(instances);
        } catch (final Exception e) {
            throw new ServiceConfigurationError("Load providers of " + service.getName() + " error", e);
        }
    }

    public static <S> void register(final Class<S> service, final Callable<? extends S> creator) {
        REGISTRY.computeIfAbsent(service, new Function<Class<?>, List<Callable<?>>>() {
            @Override
            public List<Callable<?>> apply(final Class<?> clazz) {
                return new ArrayList<>();
            }
        }).add(creator);
    }

    private ServiceRegistry() {
    }
}
