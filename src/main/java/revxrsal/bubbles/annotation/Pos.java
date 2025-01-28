package revxrsal.bubbles.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the index of the blueprint field. Shortened for brevity. Lower values
 * come first
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pos {

    /**
     * The index value
     *
     * @return The index value
     */
    int value();

}
