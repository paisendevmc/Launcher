package pro.gravit.launchserver.auth;

import pro.gravit.launchserver.LaunchServer;
import pro.gravit.launchserver.auth.core.AuthCoreProvider;
import pro.gravit.launchserver.auth.core.AuthSocialProvider;
import pro.gravit.launchserver.auth.handler.AuthHandler;
import pro.gravit.launchserver.auth.provider.AuthProvider;
import pro.gravit.launchserver.auth.texture.TextureProvider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class AuthProviderPair {
    public boolean isDefault = true;
    public AuthCoreProvider core;
    public AuthSocialProvider social;
    public AuthProvider provider;
    public AuthHandler handler;
    public TextureProvider textureProvider;
    public Map<String, String> links;
    public transient String name;
    public transient Set<String> features;
    public String displayName;

    public AuthProviderPair(AuthProvider provider, AuthHandler handler, TextureProvider textureProvider) {
        this.provider = provider;
        this.handler = handler;
        this.textureProvider = textureProvider;
    }

    public AuthProviderPair(AuthCoreProvider core, TextureProvider textureProvider) {
        this.core = core;
        this.textureProvider = textureProvider;
    }

    public AuthProviderPair(AuthCoreProvider core, AuthSocialProvider social) {
        this.core = core;
        this.social = social;
    }

    public AuthProviderPair(AuthCoreProvider core, AuthSocialProvider social, TextureProvider textureProvider) {
        this.core = core;
        this.social = social;
        this.textureProvider = textureProvider;
    }

    public static Set<String> getFeatures(Class<?> clazz) {
        Set<String> list = new HashSet<>();
        getFeatures(clazz, list);
        return list;
    }

    public static void getFeatures(Class<?> clazz, Set<String> list) {
        Features features = clazz.getAnnotation(Features.class);
        if (features != null) {
            for (Feature feature : features.value()) {
                list.add(feature.value());
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            getFeatures(superClass, list);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> i : interfaces) {
            getFeatures(i, list);
        }
    }

    public final <T> T isSupport(Class<T> clazz) {
        if (core == null) return null;
        T result = null;
        if (social != null) result = social.isSupport(clazz);
        if (result == null) result = core.isSupport(clazz);
        return result;
    }

    public final void init(LaunchServer srv, String name) {
        this.name = name;
        if (links != null) link(srv);
        if (core == null) {
            if (provider == null) throw new NullPointerException(String.format("Auth %s provider null", name));
            if (handler == null) throw new NullPointerException(String.format("Auth %s handler null", name));
            if (social != null)
                throw new IllegalArgumentException(String.format("Auth %s social can't be used in provider/handler method", name));
            provider.init(srv);
            handler.init(srv);
        } else {
            if (provider != null) throw new IllegalArgumentException(String.format("Auth %s provider not null", name));
            if (handler != null) throw new IllegalArgumentException(String.format("Auth %s handler not null", name));
            core.init(srv);
            features = new HashSet<>();
            getFeatures(core.getClass(), features);
            if (social != null) {
                social.init(srv, core);
                getFeatures(social.getClass(), features);
            }
        }
    }

    public final void link(LaunchServer srv) {
        links.forEach((k, v) -> {
            AuthProviderPair pair = srv.config.getAuthProviderPair(v);
            if (pair == null) {
                throw new NullPointerException(String.format("Auth %s link failed. Pair %s not found", name, v));
            }
            if ("provider".equals(k)) {
                if (pair.provider == null)
                    throw new NullPointerException(String.format("Auth %s link failed. %s.provider is null", name, v));
                provider = pair.provider;
            } else if ("handler".equals(k)) {
                if (pair.handler == null)
                    throw new NullPointerException(String.format("Auth %s link failed. %s.handler is null", name, v));
                handler = pair.handler;
            } else if ("textureProvider".equals(k)) {
                if (pair.textureProvider == null)
                    throw new NullPointerException(String.format("Auth %s link failed. %s.textureProvider is null", name, v));
                textureProvider = pair.textureProvider;
            } else if ("core".equals(k)) {
                if (pair.core == null)
                    throw new NullPointerException(String.format("Auth %s link failed. %s.core is null", name, v));
                core = pair.core;
            }
        });
    }

    public final void close() throws IOException {
        if (social != null) {
            social.close();
        }
        if (core == null) {
            provider.close();
            handler.close();
        } else {
            core.close();
        }
        if (textureProvider != null) {
            textureProvider.close();
        }
    }

    public final boolean isUseCore() {
        return core != null;
    }

    public final boolean isUseSocial() {
        return core != null && social != null;
    }

    public final boolean isUseProviderAndHandler() {
        return !isUseCore();
    }
}
