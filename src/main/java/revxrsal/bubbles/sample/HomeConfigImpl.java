package revxrsal.bubbles.sample;

import revxrsal.bubbles.Config;

public final class HomeConfigImpl implements HomeConfig {

    private final Config config;

    public HomeConfigImpl(Config config) {
        this.config = config;
    }

    @Override
    public int home() {
        if (config.exists("home")) {
            return config.parse("home", int.class);
        }
        throw new IllegalArgumentException("Missing property: 'home'");
    }

    @Override
    public String capacitor() {
        if (config.exists("home")) {
            return config.parse("home", int.class);
        }
        return HomeConfig.super.capacitor();
    }
}
