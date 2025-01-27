# Bubbles

[![Discord](https://discord.com/api/guilds/939962855476846614/widget.png)](https://discord.gg/pEGGF785zp)
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/io/github/revxrsal/bubbles/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/io.github.revxrsal/bubbles)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build](https://github.com/Revxrsal/Bubbles/actions/workflows/gradle.yml/badge.svg)](https://github.com/Revxrsal/Bubbles/actions/workflows/gradle.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/revxrsal/bubbles/badge)](https://www.codefactor.io/repository/github/revxrsal/bubbles)

A library for generating beautiful, commented, type-safe YML through interfaces

### Features
- Create interfaces (blueprints) that define your configuration
- Create default values with default methods
- Supports comments for blueprints using `@Comment` 🔥
- Recursively use blueprints as arrays, lists, values of maps, etc.
- Blueprints support setters
- ASM-generated implementations for lightning performance
- Uses Gson under the hood for deserializing (to be improved)

### Example

```java
@Blueprint
public interface Arena {

    @Key("arena-name")
    @Comment("The arena name")
    default String name() {
        return "Default name";
    }

    @Comment("The arena capacity")
    int capacity();

    @Comment({
            "The arena type. Values: 'teams' or 'single'",
            "",
            "Default value: teams"
    })
    default ArenaType type() {
        return ArenaType.SINGLE;
    }

    enum ArenaType {
        TEAMS,
        SINGLE
    }
}
```

```java
public static void main(String[] args) {
    // BlueprintClass allows us to examine blueprints and
    // read their properties
    BlueprintClass blueprintClass = Blueprints.from(Arena.class);

    // Creates an instance of Arena with all default values
    Arena arena = blueprintClass.createDefault();
    System.out.println(arena);

    // Use our specialized CommentedConfiguration class
    CommentedConfiguration config = CommentedConfiguration.from(
            Paths.get("config.yml")
    );

    // Set the content of the configuration to the arena
    config.setTo(arena);

    // Set the comments to the blueprint class comments
    config.setComments(blueprintClass.comments());

    // Save the configuration
    config.save();

    // Reads the content of the configuration as an Arena
    Arena deserializedArena = config.getAs(Arena.class);

    // Prints: Arena(name=default name, capacity=0, type=SINGLE)
    System.out.println(deserializedArena);
}
```

Outputs:
```yml
# The arena name
arena-name: Default name

# The arena capacity
capacity: 0.0

# The arena type. Values: 'teams' or 'single'
# 
# Default value: teams
type: single
```

