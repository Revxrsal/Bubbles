package revxrsal.bubbles.sample;

import revxrsal.bubbles.annotation.Comment;
import revxrsal.bubbles.annotation.Key;

public interface HomeConfig {

    int home();

    @Key("help.capacitor")
    @Comment("Ensures that the user has enough capacity")
    default String capacitor() {
        return String.valueOf(Math.random());
    }
}
