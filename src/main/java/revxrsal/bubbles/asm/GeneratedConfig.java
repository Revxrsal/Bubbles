package revxrsal.bubbles.asm;

import org.jetbrains.annotations.NotNull;
import revxrsal.bubbles.io.Emitter;

public interface GeneratedConfig {

    void reload();

    void emit(@NotNull Emitter emitter);

}
