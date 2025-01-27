# Bubbles

[![Discord](https://discord.com/api/guilds/939962855476846614/widget.png)](https://discord.gg/pEGGF785zp)
[![Maven Central](https://img.shields.io/maven-metadata/v/https/repo1.maven.org/maven2/io/github/revxrsal/bubbles/maven-metadata.xml.svg?label=maven%20central&colorB=brightgreen)](https://search.maven.org/artifact/io.github.revxrsal/bubbles)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build](https://github.com/Revxrsal/Bubbles/actions/workflows/gradle.yml/badge.svg)](https://github.com/Revxrsal/Bubbles/actions/workflows/gradle.yml)
[![CodeFactor](https://www.codefactor.io/repository/github/revxrsal/bubbles/badge)](https://www.codefactor.io/repository/github/revxrsal/bubbles)

A library for generating beautiful, commented, type-safe YML through interfaces

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
    default ArenaType atype() {
        return ArenaType.SINGLE;
    }

    enum ArenaType {
        TEAMS,
        SINGLE
    }
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
atype: single
```