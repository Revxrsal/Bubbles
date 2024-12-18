package revxrsal.bubbles.asm;

import revxrsal.bubbles.Config;

public class Test {

    private final Config config;
    private int home;

    public Test(Config config) {
        this.config = config;
        config.parse("home", String.class);
        reload();
    }

    public int home() {
        return config.parse("home", int.class);
    }

    public void reload() {
        home = home();
    }
}
