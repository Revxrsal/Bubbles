package revxrsal.bubbles.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;
import revxrsal.Container;
import revxrsal.bubbles.Config;
import revxrsal.bubbles.sample.HomeConfig;

import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Generators {

    private static final Config CONFIG = new Config() {

        private Map<String, Object> data = new HashMap<String, Object>() {{
//            put("capacitor", "Sooooooooooo");
            put("home", 21);
        }};

        @Override
        public <T> T parse(String key, Type type) {
            return (T) data.get(key);
        }

        @Override
        public boolean exists(String key) {
            System.out.println(key);
            return data.containsKey(key);
        }
    };

    public static void main(String[] args) throws Throwable {
        ClassGenerator generator = new ClassGenerator(new Container(HomeConfig.class));
        generator.generate();
        Class<? extends HomeConfig> load = generator.load();
        HomeConfig homeConfig = load.getDeclaredConstructor(Config.class).newInstance(CONFIG);
        System.out.println(homeConfig.capacitor());
        System.out.println(homeConfig.capacitor());
//        System.out.println(homeConfig);
        generator.output("output.class");
//        TraceClassVisitor trace = new TraceClassVisitor(generator.getWriter(), new PrintWriter(System.out));
//        ClassReader reader = new ClassReader(generator.getWriter().toByteArray());
//        reader.accept(trace, ClassReader.EXPAND_FRAMES);
    }
}
