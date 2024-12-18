package revxrsal.bubbles.asm;

import revxrsal.bubbles.Config;

public class Test {

    private final Config config;

    public Test(Config config) {
        this.config = config;
        config.parse("home", String.class);
    }

    public int home() {
        return config.parse("home", int.class);
    }

}
